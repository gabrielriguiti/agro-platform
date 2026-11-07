package br.com.gabrielriguiti.agro.infrastructure.web.analytics;

import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Item da consulta analítica.
 *
 * @param id identificador do laboratório
 * @param nome nome em CAIXA ALTA
 * @param totalProdutores produtores que atendem a todos os filtros informados
 * @param valorFinanceiroConsolidado valor financeiro do laboratório para esse total, em BRL com duas casas
 */
@Schema(description = "Laboratório com o total de produtores filtrados e o valor financeiro consolidado")
public record LabAnalyticsResponse(
        @Schema(example = "1") long id,
        @Schema(example = "LABORATÓRIO MODELO 1") String nome,
        @Schema(example = "4") int totalProdutores,
        @Schema(example = "600.00") BigDecimal valorFinanceiroConsolidado) {

    /** @return resposta da API a partir do item do caso de uso */
    public static LabAnalyticsResponse from(LabAnalyticsItem item) {
        return new LabAnalyticsResponse(item.id(), item.name(), item.totalProducers(), item.consolidatedValue());
    }
}
