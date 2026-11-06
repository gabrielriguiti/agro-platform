package br.com.gabrielriguiti.agro.application.financial;

import java.math.BigDecimal;

/**
 * Taxas usadas pelas calculadoras, todas em BRL. Preenchidas pela configuração {@code agro.financial.*}.
 *
 * @param producerDailyCommission comissão por dia de vigência do produtor
 * @param producerPropertyCommission comissão por propriedade do produtor
 * @param propertyOperationalFee taxa operacional da propriedade
 * @param propertyTerritorialFee taxa territorial da propriedade
 * @param laboratoryOperationalFee taxa operacional do laboratório
 * @param laboratoryTerritorialFeePerProducer taxa territorial do laboratório por produtor
 */
public record FinancialRates(
        BigDecimal producerDailyCommission,
        BigDecimal producerPropertyCommission,
        BigDecimal propertyOperationalFee,
        BigDecimal propertyTerritorialFee,
        BigDecimal laboratoryOperationalFee,
        BigDecimal laboratoryTerritorialFeePerProducer) {

    /**
     * Normaliza cada taxa para duas casas decimais (HALF_EVEN).
     *
     * @throws IllegalArgumentException se alguma taxa faltar ou for negativa
     */
    public FinancialRates {
        producerDailyCommission = Money.of(producerDailyCommission, "producerDailyCommission");
        producerPropertyCommission = Money.of(producerPropertyCommission, "producerPropertyCommission");
        propertyOperationalFee = Money.of(propertyOperationalFee, "propertyOperationalFee");
        propertyTerritorialFee = Money.of(propertyTerritorialFee, "propertyTerritorialFee");
        laboratoryOperationalFee = Money.of(laboratoryOperationalFee, "laboratoryOperationalFee");
        laboratoryTerritorialFeePerProducer =
                Money.of(laboratoryTerritorialFeePerProducer, "laboratoryTerritorialFeePerProducer");
    }

    /** @return os valores padrão documentados no ADR-0010 */
    public static FinancialRates defaults() {
        return new FinancialRates(
                new BigDecimal("120.00"), new BigDecimal("50.00"),
                new BigDecimal("300.00"), new BigDecimal("150.00"),
                new BigDecimal("500.00"), new BigDecimal("25.00"));
    }
}
