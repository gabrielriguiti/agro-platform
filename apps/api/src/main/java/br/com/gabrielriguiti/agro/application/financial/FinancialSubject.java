package br.com.gabrielriguiti.agro.application.financial;

import java.time.Duration;
import java.time.Instant;

/**
 * Entrada de uma calculadora: apenas os números necessários, sem a entidade persistida.
 *
 * @param nature natureza da entidade
 * @param validityDays dias de vigência (produtor); 0 quando não se aplica
 * @param propertyCount quantidade de propriedades (produtor); 0 quando não se aplica
 * @param producerCount quantidade de produtores (laboratório); 0 quando não se aplica
 */
public record FinancialSubject(FinancialNature nature, long validityDays, int propertyCount, int producerCount) {

    /**
     * @throws IllegalArgumentException se a natureza faltar ou algum número for negativo
     */
    public FinancialSubject {
        if (nature == null) {
            throw new IllegalArgumentException("natureza é obrigatória");
        }
        if (validityDays < 0 || propertyCount < 0 || producerCount < 0) {
            throw new IllegalArgumentException("quantidades não podem ser negativas");
        }
    }

    /**
     * Sujeito de um produtor; os dias de vigência vêm de {@link #validityDays(Instant, Instant)}.
     *
     * @param dataInicial início do período
     * @param dataFinal fim do período
     * @param propertyCount quantidade de propriedades do produtor
     */
    public static FinancialSubject producer(Instant dataInicial, Instant dataFinal, int propertyCount) {
        return new FinancialSubject(FinancialNature.PRODUCER, validityDays(dataInicial, dataFinal), propertyCount, 0);
    }

    /** @return sujeito de uma propriedade (as taxas não dependem de quantidades) */
    public static FinancialSubject property() {
        return new FinancialSubject(FinancialNature.PROPERTY, 0, 0, 0);
    }

    /** @param producerCount quantidade de produtores vinculados ao laboratório */
    public static FinancialSubject laboratory(int producerCount) {
        return new FinancialSubject(FinancialNature.LABORATORY, 0, 0, producerCount);
    }

    /**
     * Dias de vigência: o intervalo arredondado para cima em dias inteiros, com mínimo de 1.
     *
     * @param dataInicial início do período
     * @param dataFinal fim do período
     * @return dias inteiros (4,25 dias resultam em 5; intervalo vazio resulta em 1)
     */
    public static long validityDays(Instant dataInicial, Instant dataFinal) {
        Duration interval = Duration.between(dataInicial, dataFinal);
        long days = interval.toDays();
        boolean hasRemainder = !interval.minusDays(days).isZero();
        return Math.max(1, hasRemainder ? days + 1 : days);
    }
}
