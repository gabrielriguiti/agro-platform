package br.com.gabrielriguiti.agro.application.integration;

import br.com.gabrielriguiti.agro.domain.integration.MessageStatus;
import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import java.time.Instant;

/**
 * Visão de listagem de uma mensagem, sem o payload.
 *
 * @param id identificador
 * @param batchId lote
 * @param type tipo da entidade
 * @param externalId id do item no parceiro
 * @param status estado
 * @param attempts tentativas iniciadas
 * @param nextAttemptAt próxima tentativa
 * @param lastError último erro, ou {@code null}
 */
public record MessageSummary(
        long id,
        long batchId,
        MessageType type,
        String externalId,
        MessageStatus status,
        int attempts,
        Instant nextAttemptAt,
        String lastError) {
}
