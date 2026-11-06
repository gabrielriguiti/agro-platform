package br.com.gabrielriguiti.agro;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

/** Verifica que, fora do profile local, cada requisição gera um evento JSON com nível, mensagem e correlação. */
class StructuredLoggingIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void requisicaoGeraEventoJsonComCorrelacao() {
        PrintStream original = System.out;
        ByteArrayOutputStream capturado = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturado, true));
        try {
            rest.getForEntity("/actuator/health", String.class);
        } finally {
            System.setOut(original);
        }
        String saida = capturado.toString();
        assertThat(saida).contains("\"level\":\"INFO\"", "\"message\":\"GET /actuator/health -> 200\"", "\"correlationId\":\"");
    }
}
