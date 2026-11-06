## Context

O enunciado pede ingestão em lote ou stream, Outbox ou mensageria, idempotência, retry com backoff, DLQ e reconciliação. O banco já é PostgreSQL ([ADR-0003](../../../docs/adr/0003-postgresql-flyway-e-schema-validado.md)). O objetivo é demonstrar resiliência com poucas peças, mantendo o broker substituível.

## Goals / Non-Goals

**Goals:**
- Durabilidade: nada aceito se perde.
- Segurança sob concorrência e repetição.
- Observabilidade das falhas e reprocessamento simples.

**Non-Goals:**
- Broker externo; ordenação global; exatamente-uma-vez.

## Decisions

1. **Tabela `outbox_message` como fila durável** (ingestão e fila na mesma transação do banco). Alternativas: RabbitMQ/Kafka (mais peças para operar e dupla gravação sem outbox). A porta `OutboxPort` isola a escolha.
2. **Reserva com `SELECT ... FOR UPDATE SKIP LOCKED` + lease (`locked_until`)**, incrementando `attempts` na reserva: um worker que morre não deixa mensagem presa e não causa laço infinito sem contagem.
3. **Duas camadas de idempotência:** chave da mensagem (`UNIQUE idempotency_key`, `INSERT ... ON CONFLICT DO NOTHING`) barra reenvios idênticos; chaves naturais (`source`+`externalId`, inscrição fiscal) tornam o processamento um upsert. Constraints do banco são a última palavra.
4. **Falhas classificadas por tipo de exceção:** `PermanentIntegrationException` vai a `DEAD` direto; as demais (incluindo `DataIntegrityViolation` por corrida) são transitórias até o teto de tentativas.
5. **Backoff exponencial com jitter** em `BackoffPolicy` (domínio, determinístico via `r` injetado).
6. **Processamento por mensagem em transação própria** (`REQUIRES_NEW`); o resultado (`DONE`/retry/`DEAD`) é gravado fora dela, para que uma falha não desfaça o registro do erro.
7. **Reconciliação** em função pura (`ReconciliationPolicy.merge(existente, recebido, estratégia)`), testada sem banco.
8. **Worker agendado** (`@Scheduled`) com ciclo exposto também por endpoint manual configurável, para demo e testes determinísticos.
9. **Payload como `TEXT`** com JSON canônico para a chave; o parse acontece no worker, de modo que payload inválido vira `DEAD` e não erro de ingestão.

ADRs a gerar: 0011 a 0015 (títulos na proposta).

## Risks / Trade-offs

- [Tabela de outbox cresce] → política de limpeza de `DONE` antigos documentada como evolução; índices por `(status, next_attempt_at)`.
- [Polling consome banco] → intervalo configurável e consulta indexada; adequado ao porte.
- [Mensagens `DEAD` esquecidas] → painel de monitoramento destaca a DLQ.
- [Reordenação entre mensagens do mesmo registro] → upsert idempotente e `OVERWRITE` determinístico por chegada; ordenação estrita fica fora de escopo.
