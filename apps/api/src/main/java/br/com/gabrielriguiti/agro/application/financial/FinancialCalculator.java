package br.com.gabrielriguiti.agro.application.financial;

/**
 * Estratégia de cálculo de uma natureza. Para uma nova regra, basta uma nova implementação registrada no motor,
 * sem alterar o motor nem as demais calculadoras.
 */
public interface FinancialCalculator {

    /** @return {@code true} se esta calculadora trata a natureza informada */
    boolean supports(FinancialNature nature);

    /** @return o resultado para o sujeito (cuja natureza é aceita por {@link #supports}) */
    FinancialResult calculate(FinancialSubject subject);
}
