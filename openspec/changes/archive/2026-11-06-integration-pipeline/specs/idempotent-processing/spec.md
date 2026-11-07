## Purpose

Garante que repetir, reordenar ou paralelizar o processamento não duplica registros nem corrompe vínculos.

## ADDED Requirements

### Requirement: Chave de idempotência da mensagem
Cada mensagem DEVE ter uma chave derivada de `source`, `type`, `externalId` e do payload canônico (chaves ordenadas); uma chave já existente NÃO DEVE gerar nova mensagem e DEVE ser contada em `duplicates`.

#### Scenario: Reenvio idêntico
- **WHEN** o mesmo lote é enviado duas vezes
- **THEN** a segunda resposta tem `accepted: 0` e `duplicates` igual ao tamanho do lote, e nenhum registro novo é criado

#### Scenario: Mesmo payload com ordem de campos diferente
- **WHEN** o payload chega com os campos em outra ordem
- **THEN** a chave é a mesma e o item é tratado como duplicata

### Requirement: Upsert por chave externa
O processamento DEVE identificar o registro por (`source`, `externalId`); um reenvio com o mesmo id externo e payload alterado DEVE atualizar o registro existente segundo a estratégia, sem criar outro.

#### Scenario: Payload corrigido
- **WHEN** o parceiro reenvia o produtor `P-9` com o endereço corrigido
- **THEN** existe um único produtor `P-9` com o endereço conforme a estratégia do lote

### Requirement: Unicidade garantida pelo banco
A não duplicação DEVE ser garantida por constraints únicas (`source`+`externalId` e inscrição fiscal), e não apenas por verificação em código.

#### Scenario: Corrida entre dois workers
- **WHEN** dois workers processam simultaneamente mensagens diferentes do mesmo CNPJ
- **THEN** um cria o registro, o outro recebe a violação, é reagendado e na tentativa seguinte encontra e reconcilia o registro existente

### Requirement: Reserva exclusiva com lease
As mensagens DEVEM ser reservadas com `SELECT ... FOR UPDATE SKIP LOCKED` e um prazo de lease; duas instâncias NÃO DEVEM processar a mesma mensagem ao mesmo tempo, e uma reserva vencida (worker que morreu) DEVE voltar a ser elegível.

#### Scenario: Dois workers concorrentes
- **WHEN** dois workers reservam mensagens ao mesmo tempo
- **THEN** os conjuntos reservados são disjuntos

#### Scenario: Worker que morreu
- **WHEN** a reserva de uma mensagem em `PROCESSING` vence sem conclusão
- **THEN** a mensagem é reservada de novo e a tentativa é contada

### Requirement: Atomicidade por mensagem
Cada mensagem DEVE ser processada em sua própria transação: ou todas as entidades da mensagem são gravadas, ou nenhuma.

#### Scenario: Falha ao gravar propriedade de um produtor
- **WHEN** a gravação da segunda propriedade aninhada de um produtor falha
- **THEN** nem o produtor, nem o laboratório, nem a primeira propriedade daquela mensagem ficam gravados

### Requirement: Vínculos preservados
Reprocessar ou reenviar um produtor NÃO DEVE remover propriedades já vinculadas a ele que não estejam no payload nem duplicar vínculos existentes.

#### Scenario: Reenvio parcial
- **WHEN** um produtor com as propriedades A e B recebe um payload só com a propriedade A
- **THEN** A e B continuam vinculadas e sem duplicata
