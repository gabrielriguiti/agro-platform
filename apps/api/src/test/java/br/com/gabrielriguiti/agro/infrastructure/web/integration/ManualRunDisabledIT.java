package br.com.gabrielriguiti.agro.infrastructure.web.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/** Com {@code agro.integration.manual-run-enabled=false}, a execução manual do worker responde 404 (task 5.3). */
@TestPropertySource(properties = "agro.integration.manual-run-enabled=false")
class ManualRunDisabledIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;

    @Test
    void execucaoManualDesabilitadaResponde404() {
        ResponseEntity<JsonNode> r = rest.exchange("/api/integration/worker/run", HttpMethod.POST, HttpEntity.EMPTY,
                JsonNode.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
