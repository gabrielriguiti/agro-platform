package br.com.gabrielriguiti.agro.application.analytics;

/**
 * Linha agregada devolvida pela porta.
 *
 * @param id identificador do laboratório
 * @param name nome do laboratório, como cadastrado
 * @param totalProducers produtores que atendem a todos os filtros
 */
public record LabAnalyticsRow(long id, String name, int totalProducers) {
}
