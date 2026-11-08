## Purpose

Acompanhar a sincronização, inspecionar falhas e reprocessar.

## ADDED Requirements

### Requirement: Lista de lotes
O painel DEVE listar os lotes com origem, data, estado (em andamento, concluído, concluído com falhas) e contagens por status, e atualizar sozinho a cada 5 segundos.

#### Scenario: Lote em andamento
- **WHEN** um lote tem mensagens pendentes
- **THEN** o estado é "em andamento" e as contagens se atualizam até concluir

### Requirement: Mensagens e DLQ
Ao abrir um lote, o painel DEVE listar suas mensagens com tipo, id externo, status, tentativas, próxima tentativa e último erro, com filtro por status e destaque para `DEAD`.

#### Scenario: Ver o erro de uma mensagem morta
- **WHEN** o usuário abre uma mensagem `DEAD`
- **THEN** vê o motivo da falha, o número de tentativas e o payload original

### Requirement: Reprocessamento
O painel DEVE permitir reprocessar uma mensagem ou todas as mortas de um lote, mostrando o resultado.

#### Scenario: Reprocessar lote
- **WHEN** o usuário aciona "Reprocessar mortas" em um lote
- **THEN** as mensagens voltam a `PENDING` e o painel reflete a mudança

### Requirement: Enviar lote de exemplo
O painel DEVE oferecer um formulário para colar ou editar um JSON de lote e enviá-lo, com um exemplo pronto (incluindo um item com inscrição fiscal inválida para demonstrar a DLQ).

#### Scenario: Demonstrar a DLQ
- **WHEN** o usuário envia o lote de exemplo
- **THEN** os itens válidos terminam `DONE` e o inválido aparece como `DEAD` com o motivo

#### Scenario: Enviar o exemplo mais de uma vez
- **WHEN** o usuário carrega o exemplo e o envia de novo depois de já tê-lo enviado
- **THEN** o novo lote também recebe as quatro mensagens, porque cada carregamento do exemplo gera ids externos novos (reenviar os mesmos ids viraria duplicado e o lote ficaria vazio)
