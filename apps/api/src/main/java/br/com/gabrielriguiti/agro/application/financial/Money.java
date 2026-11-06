package br.com.gabrielriguiti.agro.application.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Aritmética monetária: sempre {@link BigDecimal} com duas casas e {@code HALF_EVEN}; nunca {@code double}. */
final class Money {

    static final int SCALE = 2;
    static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private Money() {
    }

    /** @return o valor com escala 2 */
    static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    /** @return o valor com escala 2, validando que existe e não é negativo */
    static BigDecimal of(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("taxa " + name + " deve ser informada e não negativa");
        }
        return round(value);
    }

    /** @return {@code unit × quantity} com escala 2 */
    static BigDecimal times(BigDecimal unit, long quantity) {
        return round(unit.multiply(BigDecimal.valueOf(quantity)));
    }
}
