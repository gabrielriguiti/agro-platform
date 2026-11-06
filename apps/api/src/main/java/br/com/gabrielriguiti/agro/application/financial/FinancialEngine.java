package br.com.gabrielriguiti.agro.application.financial;

import java.util.List;

/** Escolhe a calculadora pela natureza do sujeito e delega o cálculo. */
public class FinancialEngine {

    private final List<FinancialCalculator> calculators;

    /** @param calculators calculadoras registradas; a primeira que suportar a natureza é usada */
    public FinancialEngine(List<FinancialCalculator> calculators) {
        this.calculators = List.copyOf(calculators);
    }

    /**
     * @param subject sujeito do cálculo
     * @return o resultado da calculadora da natureza
     * @throws UnsupportedFinancialNatureException se nenhuma calculadora tratar a natureza
     */
    public FinancialResult calculate(FinancialSubject subject) {
        return calculators.stream()
                .filter(calculator -> calculator.supports(subject.nature()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFinancialNatureException(subject.nature()))
                .calculate(subject);
    }
}
