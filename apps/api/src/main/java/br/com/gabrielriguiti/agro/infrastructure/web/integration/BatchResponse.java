package br.com.gabrielriguiti.agro.infrastructure.web.integration;

import br.com.gabrielriguiti.agro.application.integration.BatchSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Lote com contagens por status e estado derivado.
 *
 * @param id identificador
 * @param source sistema parceiro
 * @param mergeStrategy estratégia de fusão
 * @param createdAt instante do recebimento
 * @param total total de mensagens
 * @param pending pendentes
 * @param processing em processamento
 * @param done concluídas
 * @param dead na DLQ
 * @param state {@code EM_ANDAMENTO}, {@code CONCLUIDO} ou {@code CONCLUIDO_COM_FALHAS}
 */
@Schema(description = "Resumo de um lote de integração")
public record BatchResponse(
        long id,
        String source,
        String mergeStrategy,
        Instant createdAt,
        long total,
        long pending,
        long processing,
        long done,
        long dead,
        String state) {

    static BatchResponse from(BatchSummary s) {
        return new BatchResponse(s.batch().id(), s.batch().source(), s.batch().mergeStrategy().name(),
                s.batch().createdAt(), s.total(), s.pending(), s.processing(), s.done(), s.dead(),
                s.state().name());
    }
}
