package br.com.gabrielriguiti.agro;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica a spec runtime-baseline: migrations, saúde, OpenAPI e correlação. */
class RuntimeBaselineIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    DataSource dataSource;

    @Test
    void migrationsAplicadasEmBancoVazio() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer tabelas = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name IN ('laboratories','producers','properties','integration_batch','outbox_message')",
                Integer.class);
        assertThat(tabelas).isEqualTo(5);
        Integer comentadas = jdbc.queryForObject(
                "SELECT count(*) FROM pg_description d JOIN pg_class c ON c.oid = d.objoid "
                        + "WHERE c.relname IN ('laboratories','producers','properties','integration_batch','outbox_message') "
                        + "AND d.objsubid = 0",
                Integer.class);
        assertThat(comentadas).isEqualTo(5);
    }

    @Test
    void healthResponde() {
        ResponseEntity<String> r = rest.getForEntity("/actuator/health", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void openApiESwaggerResponde() {
        ResponseEntity<String> docs = rest.getForEntity("/v3/api-docs", String.class);
        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody()).contains("\"openapi\":\"3").contains("/api/info");

        ResponseEntity<String> ui = rest.getForEntity("/swagger-ui.html", String.class);
        assertThat(ui.getStatusCode().is2xxSuccessful() || ui.getStatusCode().is3xxRedirection()).isTrue();
    }

    @Test
    void devolveCabecalhoDeCorrelacao() {
        ResponseEntity<String> r = rest.getForEntity("/actuator/health", String.class);
        assertThat(r.getHeaders().getFirst("X-Correlation-Id")).isNotBlank();
    }
}
