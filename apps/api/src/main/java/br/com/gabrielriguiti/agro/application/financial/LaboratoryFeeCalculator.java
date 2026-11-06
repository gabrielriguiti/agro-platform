package br.com.gabrielriguiti.agro.application.financial;

/** Laboratório: {@code taxaOperacional + produtores × taxaTerritorialPorProdutor}. */
public class LaboratoryFeeCalculator implements FinancialCalculator {

    private final FinancialRates rates;

    /** @param rates taxas vigentes */
    public LaboratoryFeeCalculator(FinancialRates rates) {
        this.rates = rates;
    }

    @Override
    public boolean supports(FinancialNature nature) {
        return nature == FinancialNature.LABORATORY;
    }

    @Override
    public FinancialResult calculate(FinancialSubject subject) {
        return FinancialResult.of(FinancialNature.LABORATORY,
                FinancialComponent.of("operationalFee", 1, rates.laboratoryOperationalFee()),
                FinancialComponent.of("territorialFeePerProducer", subject.producerCount(),
                        rates.laboratoryTerritorialFeePerProducer()));
    }
}
