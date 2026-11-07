package br.com.gabrielriguiti.agro.infrastructure.web.integration;

import br.com.gabrielriguiti.agro.application.integration.MessageSummary;
import br.com.gabrielriguiti.agro.domain.integration.OutboxMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Mensagem da outbox. O {@code payload} só vem preenchido no detalhe.
 *
 * @param id identificador
 * @param batchId lote
 * @param type tipo da entidade
 * @param externalId id do item no parceiro
 * @param status {@code PENDING}, {@code PROCESSING}, {@code DONE} ou {@code DEAD}
 * @param attempts tentativas iniciadas
 * @param nextAttemptAt próxima tentativa
 * @param lastError último erro
 * @param payload JSON original (apenas no detalhe)
 */
@Schema(description = "Mensagem da outbox")
public record MessageResponse(
        long id,
        long batchId,
        String type,
        String externalId,
        String status,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        @Schema(nullable = true) String payload) {

    static MessageResponse from(MessageSummary m) {
        return new MessageResponse(m.id(), m.batchId(), m.type().name(), m.externalId(), m.status().name(),
                m.attempts(), m.nextAttemptAt(), m.lastError(), null);
    }

    static MessageResponse from(OutboxMessage m) {
        return new MessageResponse(m.id(), m.batchId(), m.type().name(), m.externalId(), m.status().name(),
                m.attempts(), m.nextAttemptAt(), m.lastError(), m.payload());
    }
}
