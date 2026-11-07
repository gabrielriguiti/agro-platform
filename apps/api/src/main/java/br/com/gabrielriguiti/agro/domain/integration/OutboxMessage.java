package br.com.gabrielriguiti.agro.domain.integration;

import java.time.Duration;
import java.time.Instant;

/**
 * Mensagem da outbox e suas transições de estado (ADR-0011). Imutável: cada transição devolve uma cópia.
 *
 * <p>{@code attempts} é incrementado na reserva ({@link #reserve}), de modo que um worker que morre não deixa a
 * tentativa sem contagem.
 *
 * @param id identificador
 * @param batchId lote de origem
 * @param idempotencyKey hash de origem, tipo, id externo e payload canônico
 * @param type tipo da entidade
 * @param externalId id do item no sistema parceiro
 * @param payload JSON original
 * @param status estado atual
 * @param attempts tentativas já iniciadas
 * @param nextAttemptAt a partir de quando é elegível
 * @param lockedUntil fim do lease, ou {@code null}
 * @param lastError último erro, ou {@code null}
 */
public record OutboxMessage(
        long id,
        long batchId,
        String idempotencyKey,
        MessageType type,
        String externalId,
        String payload,
        MessageStatus status,
        int attempts,
        Instant nextAttemptAt,
        Instant lockedUntil,
        String lastError) {

    /** Tamanho máximo do erro registrado. */
    private static final int ERROR_MAX = 2000;

    /**
     * Reserva a mensagem para um worker.
     *
     * @param now instante atual
     * @param lease duração da reserva
     * @return cópia em {@code PROCESSING} com uma tentativa a mais
     */
    public OutboxMessage reserve(Instant now, Duration lease) {
        return new OutboxMessage(id, batchId, idempotencyKey, type, externalId, payload, MessageStatus.PROCESSING,
                attempts + 1, nextAttemptAt, now.plus(lease), lastError);
    }

    /** @return cópia concluída com sucesso */
    public OutboxMessage done() {
        return new OutboxMessage(id, batchId, idempotencyKey, type, externalId, payload, MessageStatus.DONE,
                attempts, nextAttemptAt, null, null);
    }

    /**
     * Registra uma falha transitória: reagenda com backoff, ou manda para a DLQ se as tentativas se esgotaram.
     *
     * @param error motivo
     * @param now instante atual
     * @param maxAttempts máximo de tentativas
     * @param backoff política de atraso
     * @param r fator aleatório do jitter em [0,1)
     */
    public OutboxMessage failTransient(String error, Instant now, int maxAttempts, BackoffPolicy backoff, double r) {
        if (attempts >= maxAttempts) {
            return dead(error);
        }
        return new OutboxMessage(id, batchId, idempotencyKey, type, externalId, payload, MessageStatus.PENDING,
                attempts, now.plus(backoff.delay(attempts, r)), null, truncate(error));
    }

    /** @return cópia na DLQ, preservando tentativas, payload e o erro */
    public OutboxMessage dead(String error) {
        return new OutboxMessage(id, batchId, idempotencyKey, type, externalId, payload, MessageStatus.DEAD,
                attempts, nextAttemptAt, null, truncate(error));
    }

    /**
     * Reenfileira uma mensagem morta: volta a {@code PENDING} com zero tentativas.
     *
     * @param now instante atual
     * @throws IllegalStateException se a mensagem não estiver em {@code DEAD}
     */
    public OutboxMessage requeue(Instant now) {
        if (status != MessageStatus.DEAD) {
            throw new IllegalStateException("Só mensagens DEAD podem ser reprocessadas");
        }
        return new OutboxMessage(id, batchId, idempotencyKey, type, externalId, payload, MessageStatus.PENDING, 0,
                now, null, lastError);
    }

    private static String truncate(String error) {
        if (error == null) {
            return "erro desconhecido";
        }
        return error.length() <= ERROR_MAX ? error : error.substring(0, ERROR_MAX);
    }
}
