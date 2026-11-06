package br.com.gabrielriguiti.agro.application.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifica a spec {@code financial-calculation} sem Spring: fórmulas, vigência, arredondamento e extensão. */
class FinancialEngineTest {

    private static FinancialEngine engine(FinancialRates rates) {
        return new FinancialEngine(List.of(
                new ProducerCommissionCalculator(rates),
                new PropertyFeeCalculator(rates),
                new LaboratoryFeeCalculator(rates)));
    }

    private static final Instant START = Instant.parse("2025-06-11T12:00:00Z");

    private static FinancialSubject producer5days2properties() {
        return FinancialSubject.producer(START, START.plusSeconds(5 * 86_400L), 2);
    }

    @Test
    void produtorCom5DiasE2PropriedadesValeSetecentos() {
        FinancialResult result = engine(FinancialRates.defaults()).calculate(producer5days2properties());

        assertThat(result.nature()).isEqualTo(FinancialNature.PRODUCER);
        assertThat(result.value()).isEqualTo(new BigDecimal("700.00"));
        assertThat(result.components()).extracting(FinancialComponent::subtotal)
                .containsExactly(new BigDecimal("600.00"), new BigDecimal("100.00"));
    }

    @Test
    void propriedadeValeQuatrocentosECinquenta() {
        FinancialResult result = engine(FinancialRates.defaults()).calculate(FinancialSubject.property());
        assertThat(result.value()).isEqualTo(new BigDecimal("450.00"));
    }

    @Test
    void laboratorioCom4ProdutoresValeSeiscentos() {
        FinancialResult result = engine(FinancialRates.defaults()).calculate(FinancialSubject.laboratory(4));
        assertThat(result.value()).isEqualTo(new BigDecimal("600.00"));
    }

    @Test
    void alterarAComissaoDiariaMudaOResultado() {
        FinancialRates d = FinancialRates.defaults();
        FinancialRates changed = new FinancialRates(new BigDecimal("100.00"), d.producerPropertyCommission(),
                d.propertyOperationalFee(), d.propertyTerritorialFee(), d.laboratoryOperationalFee(),
                d.laboratoryTerritorialFeePerProducer());

        assertThat(engine(changed).calculate(producer5days2properties()).value()).isEqualByComparingTo("600.00");
    }

    @Test
    void intervaloFracionadoArredondaParaCima() {
        Instant from = Instant.parse("2025-06-11T12:00:00Z");
        Instant to = Instant.parse("2025-06-15T18:00:00Z");
        assertThat(FinancialSubject.validityDays(from, to)).isEqualTo(5);
    }

    @Test
    void intervaloExatoNaoArredondaEOMinimoEUm() {
        Instant from = Instant.parse("2025-06-11T12:00:00Z");
        assertThat(FinancialSubject.validityDays(from, from.plusSeconds(3 * 86_400L))).isEqualTo(3);
        assertThat(FinancialSubject.validityDays(from, from)).isEqualTo(1);
        assertThat(FinancialSubject.validityDays(from, from.plusSeconds(60))).isEqualTo(1);
        assertThat(FinancialSubject.validityDays(from, from.plusSeconds(86_400L).plusNanos(1))).isEqualTo(2);
    }

    @Test
    void taxaComMaisCasasEArredondadaHalfEvenParaDuasCasas() {
        FinancialRates rates = new FinancialRates(new BigDecimal("0.125"), new BigDecimal("0.135"),
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        assertThat(rates.producerDailyCommission()).isEqualTo(new BigDecimal("0.12"));
        assertThat(rates.producerPropertyCommission()).isEqualTo(new BigDecimal("0.14"));
    }

    @Test
    void taxaNegativaOuAusenteERejeitada() {
        FinancialRates d = FinancialRates.defaults();
        assertThatThrownBy(() -> new FinancialRates(new BigDecimal("-1"), d.producerPropertyCommission(),
                d.propertyOperationalFee(), d.propertyTerritorialFee(), d.laboratoryOperationalFee(),
                d.laboratoryTerritorialFeePerProducer())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FinancialRates(null, d.producerPropertyCommission(),
                d.propertyOperationalFee(), d.propertyTerritorialFee(), d.laboratoryOperationalFee(),
                d.laboratoryTerritorialFeePerProducer())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void naturezaSemCalculadoraFalhaComErroExplicito() {
        FinancialEngine onlyProperty = new FinancialEngine(List.of(new PropertyFeeCalculator(
                FinancialRates.defaults())));

        assertThatThrownBy(() -> onlyProperty.calculate(FinancialSubject.laboratory(1)))
                .isInstanceOf(UnsupportedFinancialNatureException.class)
                .hasMessageContaining("LABORATORY");
    }

    @Test
    void novaCalculadoraEntraSemAlterarOMotor() {
        FinancialCalculator flat = new FinancialCalculator() {
            @Override
            public boolean supports(FinancialNature nature) {
                return nature == FinancialNature.LABORATORY;
            }

            @Override
            public FinancialResult calculate(FinancialSubject subject) {
                return new FinancialResult(FinancialNature.LABORATORY, new BigDecimal("1.00"), List.of());
            }
        };

        assertThat(new FinancialEngine(List.of(flat)).calculate(FinancialSubject.laboratory(9)).value())
                .isEqualByComparingTo("1.00");
    }
}
