package br.com.gabrielriguiti.agro.application.financial;

/** Propriedade: {@code taxaOperacional + taxaTerritorial}. */
public class PropertyFeeCalculator implements FinancialCalculator {

    private final FinancialRates rates;

    /** @param rates taxas vigentes */
    public PropertyFeeCalculator(FinancialRates rates) {
        this.rates = rates;
    }

    @Override
    public boolean supports(FinancialNature nature) {
        return nature == FinancialNature.PROPERTY;
    }

    @Override
    public FinancialResult calculate(FinancialSubject subject) {
        return FinancialResult.of(FinancialNature.PROPERTY,
                FinancialComponent.of("operationalFee", 1, rates.propertyOperationalFee()),
                FinancialComponent.of("territorialFee", 1, rates.propertyTerritorialFee()));
    }
}
