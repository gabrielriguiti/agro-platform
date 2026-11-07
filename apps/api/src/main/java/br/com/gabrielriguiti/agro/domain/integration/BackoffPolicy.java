package br.com.gabrielriguiti.agro.domain.integration;

import java.time.Duration;

/**
 * Atraso entre tentativas: {@code min(max, base × 2^(n-1)) × (1 + jitter × r)}, com {@code r} em [0,1) injetado
 * para manter o cálculo determinístico (ADR-0014).
 *
 * @param base atraso após a primeira falha
 * @param max teto do atraso exponencial (antes do jitter)
 * @param jitter fração máxima de variação adicional (ex.: 0,1 = até +10%)
 */
public record BackoffPolicy(Duration base, Duration max, double jitter) {

    /** Padrões: base 5 s, teto 10 min, jitter 0,1. */
    public static BackoffPolicy defaults() {
        return new BackoffPolicy(Duration.ofSeconds(5), Duration.ofMinutes(10), 0.1);
    }

    /**
     * @throws IllegalArgumentException se base ou teto não forem positivos, ou o jitter for negativo
     */
    public BackoffPolicy {
        if (base == null || max == null || base.isNegative() || base.isZero() || max.compareTo(base) < 0) {
            throw new IllegalArgumentException("base deve ser positiva e o teto não pode ser menor que a base");
        }
        if (jitter < 0) {
            throw new IllegalArgumentException("jitter não pode ser negativo");
        }
    }

    /**
     * @param attempt número da tentativa que falhou, a partir de 1
     * @param r fator aleatório em [0,1)
     * @return atraso até a próxima tentativa
     */
    public Duration delay(int attempt, double r) {
        if (attempt < 1) {
            throw new IllegalArgumentException("a tentativa começa em 1");
        }
        long baseMillis = base.toMillis();
        long maxMillis = max.toMillis();
        long exponential = maxMillis;
        if (attempt - 1 < 62 && baseMillis <= (maxMillis >> (attempt - 1))) {
            exponential = baseMillis << (attempt - 1);
        }
        long capped = Math.min(maxMillis, exponential);
        return Duration.ofMillis(Math.round(capped * (1 + jitter * r)));
    }
}
