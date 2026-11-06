package br.com.gabrielriguiti.agro.infrastructure.config;

import br.com.gabrielriguiti.agro.application.financial.FinancialRates;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Taxas do motor financeiro em {@code agro.financial.*}, em BRL; os padrões valem sem configuração (ADR-0010).
 *
 * @param producer comissões do produtor
 * @param property taxas da propriedade
 * @param laboratory taxas do laboratório
 */
@ConfigurationProperties("agro.financial")
public record FinancialProperties(
        @DefaultValue Producer producer, @DefaultValue Property property, @DefaultValue Laboratory laboratory) {

    /**
     * @param dailyCommission comissão por dia de vigência
     * @param propertyCommission comissão por propriedade
     */
    public record Producer(
            @DefaultValue("120.00") BigDecimal dailyCommission,
            @DefaultValue("50.00") BigDecimal propertyCommission) {
    }

    /**
     * @param operationalFee taxa operacional
     * @param territorialFee taxa territorial
     */
    public record Property(
            @DefaultValue("300.00") BigDecimal operationalFee,
            @DefaultValue("150.00") BigDecimal territorialFee) {
    }

    /**
     * @param operationalFee taxa operacional
     * @param territorialFeePerProducer taxa territorial por produtor
     */
    public record Laboratory(
            @DefaultValue("500.00") BigDecimal operationalFee,
            @DefaultValue("25.00") BigDecimal territorialFeePerProducer) {
    }

    /** @return as taxas no formato do motor */
    public FinancialRates toRates() {
        return new FinancialRates(producer.dailyCommission(), producer.propertyCommission(),
                property.operationalFee(), property.territorialFee(),
                laboratory.operationalFee(), laboratory.territorialFeePerProducer());
    }
}
