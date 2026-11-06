package br.com.gabrielriguiti.agro.infrastructure.web.financial;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.AbstractIntegrationTest;
import br.com.gabrielriguiti.agro.TaxIds;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Verifica {@code GET /api/financial/...} com os valores padrão da spec (700,00 / 450,00 / 600,00). */
class FinancialApiIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;

    private static Map<String, Object> identity(String name, String taxId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nome", name);
        body.put("inscricaoFiscal", taxId);
        body.put("endereco", "Rua " + name);
        return body;
    }

    private long create(String path, Map<String, Object> body) {
        ResponseEntity<JsonNode> r = rest.postForEntity(path, body, JsonNode.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().get("id").asLong();
    }

    private long producer(long labId, List<Long> propertyIds, String start, String end) {
        Map<String, Object> body = identity("Produtor", TaxIds.cpf());
        body.put("dataInicial", start);
        body.put("dataFinal", end);
        body.put("laboratorioId", labId);
        body.put("propriedadeIds", propertyIds);
        return create("/api/producers", body);
    }

    @Test
    void valoresPadraoDeProdutorPropriedadeELaboratorio() {
        long lab = create("/api/laboratories", identity("Lab Fin", TaxIds.cnpj()));
        long p1 = create("/api/properties", identity("Prop 1", TaxIds.cpf()));
        long p2 = create("/api/properties", identity("Prop 2", TaxIds.cpf()));
        long producer = producer(lab, List.of(p1, p2), "2025-06-11T00:00:00Z", "2025-06-16T00:00:00Z");
        producer(lab, List.of(), "2025-01-01T00:00:00Z", "2025-01-02T00:00:00Z");
        producer(lab, List.of(), "2025-01-01T00:00:00Z", "2025-01-02T00:00:00Z");
        producer(lab, List.of(), "2025-01-01T00:00:00Z", "2025-01-02T00:00:00Z");

        ResponseEntity<JsonNode> prod = rest.getForEntity("/api/financial/producers/" + producer, JsonNode.class);
        assertThat(prod.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(prod.getBody().get("valor").decimalValue()).isEqualByComparingTo("700.00");
        assertThat(prod.getBody().get("natureza").asText()).isEqualTo("PRODUCER");
        assertThat(prod.getBody().get("componentes")).hasSize(2);
        assertThat(prod.getBody().get("componentes").get(0).get("quantidade").asLong()).isEqualTo(5);

        JsonNode prop = rest.getForEntity("/api/financial/properties/" + p1, JsonNode.class).getBody();
        assertThat(prop.get("valor").decimalValue()).isEqualByComparingTo("450.00");
        assertThat(prop.get("natureza").asText()).isEqualTo("PROPERTY");

        JsonNode laboratory = rest.getForEntity("/api/financial/laboratories/" + lab, JsonNode.class).getBody();
        assertThat(laboratory.get("valor").decimalValue()).isEqualByComparingTo("600.00");
        assertThat(laboratory.get("natureza").asText()).isEqualTo("LABORATORY");
    }

    @Test
    void entidadeInexistenteResponde404() {
        for (String path : List.of("producers", "properties", "laboratories")) {
            ResponseEntity<JsonNode> r = rest.getForEntity("/api/financial/" + path + "/999999", JsonNode.class);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(r.getHeaders().getContentType().toString()).contains("problem+json");
        }
    }
}
