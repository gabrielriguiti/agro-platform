package br.com.gabrielriguiti.agro.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.gabrielriguiti.agro.application.financial.FinancialEngine;
import br.com.gabrielriguiti.agro.application.financial.FinancialSubject;
import br.com.gabrielriguiti.agro.application.registry.LaboratoryRepository;
import br.com.gabrielriguiti.agro.application.registry.ProducerRepository;
import br.com.gabrielriguiti.agro.application.registry.PropertyRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Verifica a ligação de {@code agro.financial.*} ao motor, sem banco nem Docker. */
class FinancialConfigTest {

    private static final Instant START = Instant.parse("2025-06-11T12:00:00Z");
    private static final FinancialSubject PRODUCER =
            FinancialSubject.producer(START, START.plusSeconds(5 * 86_400L), 2);

    /** As portas do cadastro são dublês: só as taxas, as calculadoras e o motor importam aqui. */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(FinancialConfig.class)
            .withBean(ProducerRepository.class, () -> mock(ProducerRepository.class))
            .withBean(PropertyRepository.class, () -> mock(PropertyRepository.class))
            .withBean(LaboratoryRepository.class, () -> mock(LaboratoryRepository.class));

    @Test
    void padroesValemSemConfiguracao() {
        runner.run(context -> assertThat(context.getBean(FinancialEngine.class).calculate(PRODUCER).value())
                .isEqualByComparingTo("700.00"));
    }

    @Test
    void alterarAComissaoDiariaPorPropriedadeMudaOValor() {
        runner.withPropertyValues("agro.financial.producer.daily-commission=100.00")
                .run(context -> assertThat(context.getBean(FinancialEngine.class).calculate(PRODUCER).value())
                        .isEqualByComparingTo("600.00"));
    }

    @Test
    void demaisTaxasSaoConfiguraveis() {
        runner.withPropertyValues(
                        "agro.financial.property.operational-fee=10",
                        "agro.financial.property.territorial-fee=5.50",
                        "agro.financial.laboratory.operational-fee=1",
                        "agro.financial.laboratory.territorial-fee-per-producer=2",
                        "agro.financial.producer.property-commission=0")
                .run(context -> {
                    FinancialEngine engine = context.getBean(FinancialEngine.class);
                    assertThat(engine.calculate(FinancialSubject.property()).value()).isEqualByComparingTo("15.50");
                    assertThat(engine.calculate(FinancialSubject.laboratory(4)).value()).isEqualByComparingTo("9.00");
                    assertThat(engine.calculate(PRODUCER).value()).isEqualByComparingTo("600.00");
                });
    }
}
