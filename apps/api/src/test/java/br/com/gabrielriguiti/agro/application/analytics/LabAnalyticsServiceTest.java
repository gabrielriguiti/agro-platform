package br.com.gabrielriguiti.agro.application.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.gabrielriguiti.agro.application.financial.FinancialEngine;
import br.com.gabrielriguiti.agro.application.financial.FinancialRates;
import br.com.gabrielriguiti.agro.application.financial.LaboratoryFeeCalculator;
import br.com.gabrielriguiti.agro.domain.registry.DomainValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifica o caso de uso da consulta analítica sem Spring nem banco: nome, valor financeiro e validação. */
class LabAnalyticsServiceTest {

    private static final FinancialEngine ENGINE =
            new FinancialEngine(List.of(new LaboratoryFeeCalculator(FinancialRates.defaults())));

    private static LabAnalyticsFilter filter() {
        return new LabAnalyticsFilter(1, null, null, null, null, null);
    }

    @Test
    void nomeEmCaixaAltaPreservandoAcentos() {
        LabAnalyticsPort port = f -> List.of(new LabAnalyticsRow(1, "Laboratório Modelo 1", 4),
                new LabAnalyticsRow(2, "açaí & lição", 1));

        List<LabAnalyticsItem> items = new LabAnalyticsService(port, ENGINE).query(filter());

        assertThat(items).extracting(LabAnalyticsItem::name).containsExactly("LABORATÓRIO MODELO 1", "AÇAÍ & LIÇÃO");
    }

    @Test
    void valorFinanceiroVemDoMotorComOTotalDoProprioResultado() {
        LabAnalyticsPort port = f -> List.of(new LabAnalyticsRow(1, "Lab", 4), new LabAnalyticsRow(2, "Lab", 1));

        List<LabAnalyticsItem> items = new LabAnalyticsService(port, ENGINE).query(filter());

        assertThat(items.get(0).consolidatedValue()).isEqualTo(new BigDecimal("600.00"));
        assertThat(items.get(1).consolidatedValue()).isEqualTo(new BigDecimal("525.00"));
    }

    @Test
    void mantemAOrdemEntregueAPorta() {
        List<LabAnalyticsRow> rows = new ArrayList<>(List.of(new LabAnalyticsRow(9, "B", 5),
                new LabAnalyticsRow(3, "A", 2)));
        LabAnalyticsPort port = f -> rows;

        assertThat(new LabAnalyticsService(port, ENGINE).query(filter())).extracting(LabAnalyticsItem::id)
                .containsExactly(9L, 3L);
    }

    @Test
    void quantidadeMinimaMenorQueUmEhInvalida() {
        assertThatThrownBy(() -> new LabAnalyticsFilter(0, null, null, null, null, null))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).field()).isEqualTo("qtdMinimaProdutores");
    }

    @Test
    void intervalosInvertidosSaoInvalidos() {
        Instant early = Instant.parse("2025-01-01T00:00:00Z");
        Instant late = Instant.parse("2025-02-01T00:00:00Z");

        assertThatThrownBy(() -> new LabAnalyticsFilter(1, late, early, null, null, null))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).field()).isEqualTo("dataInicialInicio");
        assertThatThrownBy(() -> new LabAnalyticsFilter(1, null, null, late, early, null))
                .isInstanceOf(DomainValidationException.class)
                .extracting(e -> ((DomainValidationException) e).field()).isEqualTo("dataFinalInicio");
        assertThat(new LabAnalyticsFilter(1, early, early, early, early, null)).isNotNull();
    }

    @Test
    void textoEmBrancoNaoFiltra() {
        assertThat(new LabAnalyticsFilter(1, null, null, null, null, "   ").text()).isNull();
    }
}
