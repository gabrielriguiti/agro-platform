## Why

O sistema precisa sincronizar Produtores, Propriedades e Laboratórios com sistemas parceiros (ERP, LIMS, órgãos de governo) em carga pontual ou em lote, sem duplicar registros, sem perder dados quando algo falha e sem quebrar vínculos existentes.

## What Changes

- Endpoint de ingestão em lote que grava as mensagens em uma tabela de outbox, de forma atômica, e responde 202.
- Worker assíncrono que reserva mensagens com `SKIP LOCKED` e lease, sincroniza as entidades e registra o resultado.
- Idempotência por chave de mensagem e por chaves naturais (id externo e inscrição fiscal), garantida pelo banco.
- Retry com backoff exponencial e jitter para falhas transitórias; Dead Letter (status `DEAD`) para falhas permanentes ou tentativas esgotadas; reprocessamento manual.
- Reconciliação por inscrição fiscal com estratégias configuráveis de fusão (`MERGE`, `OVERWRITE`, `KEEP_EXISTING`).
- API de monitoramento de lotes, mensagens e DLQ.

## Capabilities

### New Capabilities
- `batch-ingestion`: recebimento e persistência atômica de lotes.
- `idempotent-processing`: processamento idempotente, concorrente e seguro.
- `retry-and-dead-letter`: classificação de falhas, backoff, DLQ e reprocessamento.
- `reconciliation`: resolução de identidade e estratégias de fusão.
- `integration-monitoring`: consulta de status de lotes, mensagens e falhas.

### Modified Capabilities

## Impact

Tabelas `integration_batch` e `outbox_message`; colunas `external_source`/`external_id` nas entidades do cadastro; contexto `integration` em `domain`/`application`/`infrastructure`. Usado pelo painel de monitoramento do web. Depende de `registry`.

**Premissas:** o "Transactional Outbox" aqui funciona como caixa de entrada (inbox/staging) em PostgreSQL, sem broker externo: a tabela é a fila durável, gravada na mesma transação da ingestão. A troca por RabbitMQ/Kafka afetaria só o adaptador de fila.

**Não objetivos:** broker externo, ordenação global entre mensagens, exatamente-uma-vez (o alvo é entrega ao-menos-uma-vez com processamento idempotente), autenticação dos parceiros.

## Documentação

ADRs previstos: 0011 Outbox como inbox em PostgreSQL; 0012 Reserva por `SKIP LOCKED` com lease; 0013 Idempotência por chave da mensagem e chaves naturais; 0014 Classificação de falhas e backoff com jitter; 0015 Estratégias de reconciliação. Diagrama de sequência e de estados da mensagem no README.
