# integration-monitoring Specification

## Purpose

Dá visibilidade do estado da sincronização: lotes, mensagens, falhas e DLQ.

## Requirements

### Requirement: Lotes com contagens
`GET /api/integration/batches` DEVE listar os lotes mais recentes com `source`, estratégia, data e contagens por status (`total`, `pending`, `processing`, `done`, `dead`) e um estado derivado: `EM_ANDAMENTO`, `CONCLUIDO` ou `CONCLUIDO_COM_FALHAS`.

#### Scenario: Lote com uma falha
- **WHEN** um lote de 10 itens termina com 9 `DONE` e 1 `DEAD`
- **THEN** o lote aparece como `CONCLUIDO_COM_FALHAS` com `done: 9` e `dead: 1`

### Requirement: Detalhe do lote
`GET /api/integration/batches/{id}` DEVE devolver o resumo do lote; 404 se não existir.

#### Scenario: Lote inexistente
- **WHEN** um id inexistente é consultado
- **THEN** a API responde 404

### Requirement: Mensagens filtráveis
`GET /api/integration/messages` DEVE listar mensagens paginadas, filtráveis por `status` e `batchId`, com tipo, id externo, tentativas, próxima tentativa e último erro; o payload completo DEVE ficar disponível no detalhe da mensagem.

#### Scenario: Ver a DLQ
- **WHEN** `GET /api/integration/messages?status=DEAD` é chamado
- **THEN** são listadas apenas as mensagens mortas, com `lastError`

### Requirement: Execução manual do worker
Quando habilitada por `agro.integration.manual-run-enabled`, `POST /api/integration/worker/run` DEVE processar um ciclo do worker e devolver quantas mensagens foram tratadas; desabilitada, DEVE responder 404.

#### Scenario: Teste determinístico
- **WHEN** um teste envia um lote e chama a execução manual
- **THEN** as mensagens são processadas sem esperar o agendador
