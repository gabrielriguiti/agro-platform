package br.com.gabrielriguiti.agro.application.financial;

import java.math.BigDecimal;

/**
 * Uma parcela do cálculo: {@code quantity × unitValue = subtotal}.
 *
 * @param name identificador da parcela (ex.: {@code dailyCommission})
 * @param quantity quantidade multiplicada (1 para taxas fixas)
 * @param unitValue valor unitário, escala 2
 * @param subtotal resultado da parcela, escala 2
 */
public record FinancialComponent(String name, long quantity, BigDecimal unitValue, BigDecimal subtotal) {

    /** @return parcela calculada a partir da quantidade e do valor unitário */
    static FinancialComponent of(String name, long quantity, BigDecimal unitValue) {
        return new FinancialComponent(name, quantity, unitValue, Money.times(unitValue, quantity));
    }
}
