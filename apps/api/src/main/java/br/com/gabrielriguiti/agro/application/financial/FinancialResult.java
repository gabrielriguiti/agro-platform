package br.com.gabrielriguiti.agro.application.financial;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de um cálculo.
 *
 * @param nature natureza calculada
 * @param value total em BRL, com exatamente duas casas decimais
 * @param components parcelas cuja soma é {@code value}
 */
public record FinancialResult(FinancialNature nature, BigDecimal value, List<FinancialComponent> components) {

    /** @return resultado cujo total é a soma das parcelas */
    static FinancialResult of(FinancialNature nature, FinancialComponent... components) {
        BigDecimal total = BigDecimal.ZERO;
        for (FinancialComponent component : components) {
            total = total.add(component.subtotal());
        }
        return new FinancialResult(nature, Money.round(total), List.of(components));
    }
}
