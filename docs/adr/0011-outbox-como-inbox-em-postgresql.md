# 0011. Outbox como inbox em PostgreSQL

- Status: aceito
- Data: 2026-11-06
- Decisores: Gabriel
- Change relacionado: `openspec/changes/integration-pipeline`

## Contexto

O enunciado pede ingestão em lote ou stream com Outbox ou mensageria. O banco da plataforma já é o PostgreSQL ([ADR-0003](0003-postgresql-flyway-e-schema-validado.md)) e o objetivo é demonstrar resiliência com poucas peças: nada aceito pode se perder, e a API não pode esperar o processamento.

## Opções consideradas

1. **Broker externo (RabbitMQ ou Kafka):** é a escolha natural em escala, mas exige operar mais um serviço e, sem um outbox, cria dupla gravação (banco e broker) que pode divergir.
2. **Tabela `outbox_message` como fila durável no PostgreSQL (escolhida):** a ingestão grava o lote e as mensagens na mesma transação, e o worker consome da mesma tabela.
3. **Processar na própria requisição:** simples, mas perde itens se o processo cair e prende o parceiro até o fim.

## Decisão

A tabela `outbox_message` funciona como **caixa de entrada** (inbox/staging): a fila é a própria tabela, gravada atomicamente com o lote (`integration_batch`). A porta `OutboxPort` isola essa escolha; trocar por um broker afetaria só o adaptador (`JdbcOutboxAdapter`). A garantia é **entrega ao-menos-uma-vez com processamento idempotente** ([ADR-0013](0013-idempotencia-por-chave-da-mensagem-e-chaves-naturais.md)), não exatamente-uma-vez. O payload é guardado como `TEXT` e só é interpretado no worker, de modo que payload inválido vira `DEAD` e não erro de ingestão.

## Consequências

- Positivas: durabilidade e atomicidade do lote sem peças extras; consulta e depuração por SQL; DLQ é apenas um status.
- Negativas e riscos: a tabela cresce (a limpeza de `DONE` antigos é uma evolução futura) e o worker consulta o banco em intervalo fixo; adequado ao porte do projeto, não a volumes de broker.
- O que precisa mudar na documentação ou nas specs: o README descreve o fluxo de ingestão e processamento com diagramas.
