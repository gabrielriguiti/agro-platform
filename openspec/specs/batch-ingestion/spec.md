# batch-ingestion Specification

## Purpose

Recebe cargas pontuais ou em lote de parceiros e as torna duráveis antes de qualquer processamento.

## Requirements

### Requirement: Endpoint de ingestão
`POST /api/integration/batches` DEVE receber `source`, `mergeStrategy` e uma lista `items` com `type` (`PRODUCER`, `PROPERTY` ou `LABORATORY`), `externalId` e `data`, e DEVE responder 202 com `batchId`, `accepted` e `duplicates`.

#### Scenario: Lote válido
- **WHEN** um lote com 3 itens é enviado
- **THEN** a API responde 202 com `accepted: 3` e `duplicates: 0` sem esperar o processamento

### Requirement: Persistência atômica do lote
Todos os itens de um lote DEVEM ser gravados na outbox na mesma transação; se a gravação falhar, nenhum item DEVE ser gravado.

#### Scenario: Falha no meio da gravação
- **WHEN** a gravação do terceiro item de um lote falha
- **THEN** nenhuma mensagem do lote existe na outbox e a API responde erro

### Requirement: Validação do envelope
A ingestão DEVE rejeitar com 400 lotes sem `source`, itens sem `externalId`, `type` desconhecido, `data` ausente ou lote vazio, e com 413 lotes acima do limite configurado (padrão 1000 itens). A validação do conteúdo de `data` é feita no processamento, não na ingestão.

#### Scenario: Tipo desconhecido
- **WHEN** um item chega com `type: "FARM"`
- **THEN** a API responde 400 indicando o item e o campo

### Requirement: Ingestão pontual
Um único item DEVE poder ser enviado como lote de um item, pelo mesmo endpoint.

#### Scenario: Carga pontual
- **WHEN** um lote com 1 item é enviado
- **THEN** ele é aceito e processado como qualquer outro

### Requirement: Estado inicial
Toda mensagem aceita DEVE nascer `PENDING`, com zero tentativas e elegível para processamento imediato.

#### Scenario: Mensagem recém-aceita
- **WHEN** um item é aceito
- **THEN** sua mensagem tem status `PENDING`, `attempts` 0 e `nextAttemptAt` não posterior ao momento da gravação
