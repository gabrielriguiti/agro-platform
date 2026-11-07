package br.com.gabrielriguiti.agro.infrastructure.persistence.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsFilter;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Verifica a montagem do SQL sem banco: condições presentes, ordenação condicional e ausência de concatenação. */
class LabAnalyticsSqlTest {

    private static final Instant T = Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void semFiltrosNaoHaWhereENemOrdemPorData() {
        String sql = LabAnalyticsSql.build(new LabAnalyticsFilter(1, null, null, null, null, null)).sql();

        assertThat(sql).doesNotContain("WHERE").doesNotContain("MIN(")
                .contains("HAVING COUNT(p.id) >= :min").contains("ORDER BY COUNT(p.id) DESC, l.id ASC");
    }

    @Test
    void soAsCondicoesInformadasEntramNoWhere() {
        String sql = LabAnalyticsSql.build(new LabAnalyticsFilter(1, T, null, null, T, "x")).sql();

        assertThat(sql).contains("p.data_inicial >= :dataInicialFrom").contains("p.data_final <= :dataFinalTo")
                .contains("p.observacoes ILIKE :text ESCAPE").doesNotContain(":dataInicialTo")
                .doesNotContain(":dataFinalFrom");
    }

    @Test
    void filtroDeDataInicialAtivaMenorDataComoSegundoCriterio() {
        String sql = LabAnalyticsSql.build(new LabAnalyticsFilter(1, null, T, null, null, null)).sql();

        assertThat(sql).contains("ORDER BY COUNT(p.id) DESC, MIN(p.data_inicial) ASC, l.id ASC");
    }

    @Test
    void filtroDeDataFinalNaoAtivaOrdemPorDataInicial() {
        String sql = LabAnalyticsSql.build(new LabAnalyticsFilter(1, null, null, T, T, null)).sql();

        assertThat(sql).doesNotContain("MIN(");
    }

    @Test
    void valoresNuncaSaoConcatenadosNoSql() {
        String hostile = "x'; DROP TABLE producers; --";
        LabAnalyticsSql.Query query = LabAnalyticsSql.build(new LabAnalyticsFilter(7, T, T, T, T, hostile));

        assertThat(query.sql()).doesNotContain("DROP").doesNotContain("2025").doesNotContain(" 7");
        assertThat(query.params().getValue("text")).isEqualTo("%" + hostile + "%");
        assertThat(query.params().getValue("min")).isEqualTo(7);
    }

    @Test
    void escapaCuringasDoLike() {
        assertThat(LabAnalyticsSql.escapeLike("100%_a\\b")).isEqualTo("100\\%\\_a\\\\b");
    }
}
