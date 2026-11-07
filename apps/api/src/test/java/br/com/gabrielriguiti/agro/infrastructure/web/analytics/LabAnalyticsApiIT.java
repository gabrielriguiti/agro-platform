package br.com.gabrielriguiti.agro.infrastructure.web.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.AbstractIntegrationTest;
import br.com.gabrielriguiti.agro.infrastructure.persistence.analytics.AnalyticsData;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica {@code GET /api/laboratories/analytics}: contrato de resposta e validação dos parâmetros (400/200). */
class LabAnalyticsApiIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;

    private void assertBadRequest(String query) {
        ResponseEntity<JsonNode> r = rest.getForEntity("/api/laboratories/analytics" + query, JsonNode.class);
        assertThat(r.getStatusCode()).as(query).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType().toString()).contains("problem+json");
    }

    @Test
    void parametroObrigatorioAusenteOuInvalidoResponde400() {
        assertBadRequest("");
        assertBadRequest("?qtdMinimaProdutores=0");
        assertBadRequest("?qtdMinimaProdutores=-3");
        assertBadRequest("?qtdMinimaProdutores=abc");
    }

    @Test
    void ausenciaDoParametroApontaOParametro() {
        ResponseEntity<JsonNode> r = rest.getForEntity("/api/laboratories/analytics", JsonNode.class);
        assertThat(r.getBody().get("detail").asText()).contains("qtdMinimaProdutores");
    }

    @Test
    void intervalosInvertidosEDatasMalFormadasResponde400() {
        assertBadRequest("?qtdMinimaProdutores=1&dataInicialInicio=2025-02-01T00:00:00Z&dataInicialFim=2025-01-01T00:00:00Z");
        assertBadRequest("?qtdMinimaProdutores=1&dataFinalInicio=2025-02-01T00:00:00Z&dataFinalFim=2025-01-01T00:00:00Z");
        assertBadRequest("?qtdMinimaProdutores=1&dataInicialInicio=ontem");
    }

    @Test
    void respondeComNomeEmCaixaAltaTotalEValorFinanceiro() {
        long lab = AnalyticsData.lab(jdbc, "Laboratório Modelo 1");
        String marker = "api" + lab;
        for (int i = 0; i < 4; i++) {
            AnalyticsData.producer(jdbc, lab, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z", marker + " obs");
        }

        ResponseEntity<JsonNode> r = rest.getForEntity("/api/laboratories/analytics?qtdMinimaProdutores=4"
                + "&buscaTexto=" + marker + "&dataInicialInicio=2025-01-01T00:00:00Z"
                + "&dataInicialFim=2025-01-01T00:00:00Z", JsonNode.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).hasSize(1);
        JsonNode item = r.getBody().get(0);
        assertThat(item.get("id").asLong()).isEqualTo(lab);
        assertThat(item.get("nome").asText()).isEqualTo("LABORATÓRIO MODELO 1");
        assertThat(item.get("totalProdutores").asInt()).isEqualTo(4);
        assertThat(item.get("valorFinanceiroConsolidado").decimalValue()).isEqualByComparingTo("600.00");
    }

    @Test
    void rotaDeAnaliticoNaoEhConfundidaComIdDeLaboratorio() {
        ResponseEntity<JsonNode> r = rest.getForEntity("/api/laboratories/analytics?qtdMinimaProdutores=1",
                JsonNode.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().isArray()).isTrue();
    }
}
