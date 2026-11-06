package br.com.gabrielriguiti.agro.application.financial;

/** Produtor: {@code diasDeVigência × comissãoDiária + propriedades × comissãoPorPropriedade}. */
public class ProducerCommissionCalculator implements FinancialCalculator {

    private final FinancialRates rates;

    /** @param rates taxas vigentes */
    public ProducerCommissionCalculator(FinancialRates rates) {
        this.rates = rates;
    }

    @Override
    public boolean supports(FinancialNature nature) {
        return nature == FinancialNature.PRODUCER;
    }

    @Override
    public FinancialResult calculate(FinancialSubject subject) {
        return FinancialResult.of(FinancialNature.PRODUCER,
                FinancialComponent.of("dailyCommission", subject.validityDays(), rates.producerDailyCommission()),
                FinancialComponent.of("propertyCommission", subject.propertyCount(),
                        rates.producerPropertyCommission()));
    }
}
