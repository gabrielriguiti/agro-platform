package br.com.gabrielriguiti.agro.application.analytics;

import java.math.BigDecimal;

/**
 * Item do resultado analítico.
 *
 * @param id identificador do laboratório
 * @param name nome em CAIXA ALTA (pt-BR, acentos preservados)
 * @param totalProducers produtores que atendem aos filtros
 * @param consolidatedValue valor financeiro do laboratório para {@code totalProducers}, com duas casas
 */
public record LabAnalyticsItem(long id, String name, int totalProducers, BigDecimal consolidatedValue) {
}
