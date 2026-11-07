package br.com.gabrielriguiti.agro.infrastructure.web.integration;

import br.com.gabrielriguiti.agro.application.integration.IngestionService;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta da ingestão.
 *
 * @param batchId lote criado
 * @param accepted mensagens novas gravadas
 * @param duplicates itens descartados por já existirem (mesma chave de idempotência)
 */
@Schema(description = "Resultado da ingestão de um lote")
public record BatchAcceptedResponse(long batchId, int accepted, int duplicates) {

    static BatchAcceptedResponse from(IngestionService.Result result) {
        return new BatchAcceptedResponse(result.batchId(), result.accepted(), result.duplicates());
    }
}
