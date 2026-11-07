package br.com.gabrielriguiti.agro.application.integration;

import br.com.gabrielriguiti.agro.domain.integration.MergeStrategy;
import java.time.Instant;

/**
 * Dados de um lote de integração.
 *
 * @param id identificador do lote
 * @param source sistema parceiro
 * @param mergeStrategy estratégia de fusão do lote
 * @param createdAt instante do recebimento
 */
public record BatchInfo(long id, String source, MergeStrategy mergeStrategy, Instant createdAt) {
}
