package br.com.gabrielriguiti.agro.infrastructure.persistence.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.AbstractIntegrationTest;
import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsFilter;
import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsItem;
import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsPort;
import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsRow;
import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsService;
import br.com.gabrielriguiti.agro.application.financial.FinancialEngine;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Verifica a spec {@code lab-analytics-query} contra o PostgreSQL real: combinações de filtros, busca literal,
 * ordenação e consulta única. O banco é compartilhado entre as classes, então cada teste olha só os laboratórios
 * que criou.
 */
class LabAnalyticsQueryIT extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired LabAnalyticsPort port;
    @Autowired FinancialEngine engine;
    @Autowired DataSource dataSource;

    private static Instant at(String iso) {
        return Instant.parse(iso);
    }

    private static LabAnalyticsFilter filter(int min, Instant iniFrom, Instant iniTo, Instant finFrom, Instant finTo,
            String text) {
        return new LabAnalyticsFilter(min, iniFrom, iniTo, finFrom, finTo, text);
    }

    /** Linhas do resultado restritas aos laboratórios do teste, na ordem devolvida. */
    private List<LabAnalyticsRow> only(LabAnalyticsFilter filter, Long... labIds) {
        Set<Long> ids = Set.of(labIds);
        return port.find(filter).stream().filter(r -> ids.contains(r.id())).toList();
    }

    private record Seed(String ini, String fin, String obs) {
    }

    private static final List<Seed> SEEDS = List.of(
            new Seed("2025-01-10T00:00:00Z", "2025-02-10T00:00:00Z", "integração inicial"),
            new Seed("2025-03-10T00:00:00Z", "2025-04-10T00:00:00Z", "Teste de INTEGRAÇÃO"),
            new Seed("2025-05-10T00:00:00Z", "2025-06-10T00:00:00Z", "100% pronto"),
            new Seed("2025-07-10T00:00:00Z", "2025-08-10T00:00:00Z", "1000 pronto"),
            new Seed("2025-09-10T00:00:00Z", "2025-10-10T00:00:00Z", "a_b"),
            new Seed("2025-11-10T00:00:00Z", "2025-12-10T00:00:00Z", "axb"));

    private long labWithSeeds(String name) {
        long lab = AnalyticsData.lab(jdbc, name);
        SEEDS.forEach(s -> AnalyticsData.producer(jdbc, lab, s.ini(), s.fin(), s.obs()));
        return lab;
    }

    @Test
    void totalCorrespondeAExatamenteOsProdutoresQueAtendemACadaCombinacaoDeFiltros() {
        long lab = labWithSeeds("Lab Combinações");
        Instant iniFrom = at("2025-03-10T00:00:00Z"); // limites inclusivos: p2..p4
        Instant iniTo = at("2025-07-10T00:00:00Z");
        Instant finFrom = at("2025-04-10T00:00:00Z"); // p2..p5
        Instant finTo = at("2025-10-10T00:00:00Z");

        for (int mask = 0; mask < 8; mask++) {
            boolean useIni = (mask & 1) != 0;
            boolean useFin = (mask & 2) != 0;
            boolean useText = (mask & 4) != 0;
            Predicate<Seed> expected = s -> (!useIni || (!at(s.ini()).isBefore(iniFrom) && !at(s.ini()).isAfter(iniTo)))
                    && (!useFin || (!at(s.fin()).isBefore(finFrom) && !at(s.fin()).isAfter(finTo)))
                    && (!useText || s.obs().toLowerCase().contains("integração"));
            long count = SEEDS.stream().filter(expected).count();

            List<LabAnalyticsRow> rows = only(filter(1, useIni ? iniFrom : null, useIni ? iniTo : null,
                    useFin ? finFrom : null, useFin ? finTo : null, useText ? "INTEGRAÇÃO" : null), lab);

            assertThat(rows.stream().mapToInt(LabAnalyticsRow::totalProducers).sum())
                    .as("ini=%s fin=%s texto=%s", useIni, useFin, useText).isEqualTo((int) count);
        }
    }

    @Test
    void totalFiltradoNaoEhTotalDeVinculos() {
        long lab = AnalyticsData.lab(jdbc, "Lab Total Filtrado");
        String m = "tf" + lab;
        AnalyticsData.producer(jdbc, lab, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", m + " sim");
        AnalyticsData.producer(jdbc, lab, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", m + " sim");
        AnalyticsData.producer(jdbc, lab, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", m + " nao");
        AnalyticsData.producer(jdbc, lab, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", m + " nao");

        assertThat(only(filter(1, null, null, null, null, m + " sim"), lab)).extracting(LabAnalyticsRow::totalProducers)
                .containsExactly(2);
        assertThat(only(filter(1, null, null, null, null, m), lab)).extracting(LabAnalyticsRow::totalProducers)
                .containsExactly(4);
    }

    @Test
    void qtdMinimaCortaPeloTotalFiltradoELaboratorioSemProdutorNaoAparece() {
        long five = AnalyticsData.lab(jdbc, "Lab 5");
        long three = AnalyticsData.lab(jdbc, "Lab 3");
        long one = AnalyticsData.lab(jdbc, "Lab 1");
        long none = AnalyticsData.lab(jdbc, "Lab 0");
        String m = "qm" + five;
        for (int i = 0; i < 5; i++) {
            AnalyticsData.producer(jdbc, five, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", m);
        }
        for (int i = 0; i < 3; i++) {
            AnalyticsData.producer(jdbc, three, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", m);
        }
        AnalyticsData.producer(jdbc, one, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", m);

        assertThat(only(filter(3, null, null, null, null, m), five, three, one, none))
                .extracting(LabAnalyticsRow::id, LabAnalyticsRow::totalProducers)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(five, 5), org.assertj.core.groups.Tuple.tuple(three, 3));
        assertThat(only(filter(1, null, null, null, null, m), five, three, one, none)).hasSize(3);
    }

    @Test
    void buscaTextualEhParcialSemDistincaoDeCaixaECuringasSaoLiterais() {
        long lab = labWithSeeds("Lab Texto");

        assertThat(only(filter(1, null, null, null, null, "integração"), lab).get(0).totalProducers()).isEqualTo(2);
        assertThat(only(filter(1, null, null, null, null, "100%"), lab).get(0).totalProducers()).isEqualTo(1);
        assertThat(only(filter(1, null, null, null, null, "a_b"), lab).get(0).totalProducers()).isEqualTo(1);
        assertThat(only(filter(1, null, null, null, null, "%"), lab).get(0).totalProducers()).isEqualTo(1);
        assertThat(only(filter(1, null, null, null, null, "_"), lab).get(0).totalProducers()).isEqualTo(1);
    }

    @Test
    void barraInvertidaEhLiteral() {
        long lab = AnalyticsData.lab(jdbc, "Lab Barra");
        AnalyticsData.producer(jdbc, lab, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", "pasta c:\\dados");
        AnalyticsData.producer(jdbc, lab, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", "pasta c:dados");

        assertThat(only(filter(1, null, null, null, null, "c:\\dados"), lab).get(0).totalProducers()).isEqualTo(1);
    }

    @Test
    void semFiltroDeDataInicialEmpateDesempataPorId() {
        long first = AnalyticsData.lab(jdbc, "Lab Empate A");
        long second = AnalyticsData.lab(jdbc, "Lab Empate B");
        String m = "em" + first;
        // o segundo laboratório tem o produtor mais antigo, mas isso não conta sem filtro de data inicial
        AnalyticsData.producer(jdbc, first, "2025-06-01T00:00:00Z", "2025-07-01T00:00:00Z", m);
        AnalyticsData.producer(jdbc, second, "2020-01-01T00:00:00Z", "2020-02-01T00:00:00Z", m);

        assertThat(only(filter(1, null, null, null, null, m), second, first)).extracting(LabAnalyticsRow::id)
                .containsExactly(first, second);
        // filtro só de data final não ativa o segundo critério
        assertThat(only(filter(1, null, null, at("2000-01-01T00:00:00Z"), at("2030-01-01T00:00:00Z"), m), second, first))
                .extracting(LabAnalyticsRow::id).containsExactly(first, second);
    }

    @Test
    void comFiltroDeDataInicialEmpateVaiParaQuemTemAMenorDataInicial() {
        long first = AnalyticsData.lab(jdbc, "Lab Data A");
        long second = AnalyticsData.lab(jdbc, "Lab Data B");
        String m = "dt" + first;
        AnalyticsData.producer(jdbc, first, "2025-06-01T00:00:00Z", "2025-07-01T00:00:00Z", m);
        AnalyticsData.producer(jdbc, second, "2020-01-01T00:00:00Z", "2020-02-01T00:00:00Z", m);

        assertThat(only(filter(1, at("2019-01-01T00:00:00Z"), at("2030-01-01T00:00:00Z"), null, null, m), first, second))
                .extracting(LabAnalyticsRow::id).containsExactly(second, first);
    }

    @Test
    void totalDecrescenteTemPrecedenciaSobreAData() {
        long few = AnalyticsData.lab(jdbc, "Lab Poucos");
        long many = AnalyticsData.lab(jdbc, "Lab Muitos");
        String m = "pm" + few;
        AnalyticsData.producer(jdbc, few, "2010-01-01T00:00:00Z", "2010-02-01T00:00:00Z", m);
        AnalyticsData.producer(jdbc, many, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", m);
        AnalyticsData.producer(jdbc, many, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", m);

        assertThat(only(filter(1, at("2000-01-01T00:00:00Z"), null, null, null, m), few, many))
                .extracting(LabAnalyticsRow::id).containsExactly(many, few);
    }

    @Test
    void cinquentaLaboratoriosSaoLidosEmUmaUnicaConsulta() throws SQLException {
        String m = "uq" + System.nanoTime();
        List<Long> labs = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            long lab = AnalyticsData.lab(jdbc, "Lab Única " + i);
            labs.add(lab);
            AnalyticsData.producer(jdbc, lab, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", m);
        }
        AtomicInteger statements = new AtomicInteger();
        NamedParameterJdbcTemplate counting = new NamedParameterJdbcTemplate(new JdbcTemplate(
                new CountingDataSource(dataSource, statements)));
        LabAnalyticsService service = new LabAnalyticsService(new JdbcLabAnalyticsAdapter(counting), engine);

        List<LabAnalyticsItem> items = service.query(filter(1, null, null, null, null, m));

        assertThat(items).hasSize(50);
        assertThat(items).allSatisfy(i -> assertThat(i.consolidatedValue().toPlainString()).isEqualTo("525.00"));
        assertThat(statements).hasValue(1);
    }

    /** DataSource que conta as instruções SQL preparadas ou criadas nas conexões que entrega. */
    private static final class CountingDataSource extends DelegatingDataSource {

        private final AtomicInteger statements;

        CountingDataSource(DataSource target, AtomicInteger statements) {
            super(target);
            this.statements = statements;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection real = super.getConnection();
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("prepareStatement") || method.getName().equals("createStatement")
                                || method.getName().equals("prepareCall")) {
                            statements.incrementAndGet();
                        }
                        try {
                            return method.invoke(real, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
        }
    }
}
