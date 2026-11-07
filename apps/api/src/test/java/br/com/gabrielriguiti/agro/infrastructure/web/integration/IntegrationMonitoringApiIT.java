package br.com.gabrielriguiti.agro.infrastructure.web.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.IntegrationTestSupport;
import br.com.gabrielriguiti.agro.TaxIds;
import br.com.gabrielriguiti.agro.application.integration.IngestionService;
import br.com.gabrielriguiti.agro.domain.integration.MergeStrategy;
import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Monitoramento de lotes e mensagens, reprocessamento e execução manual (tasks 5.1 a 5.2 e 5.3 habilitada). */
class IntegrationMonitoringApiIT extends IntegrationTestSupport {

    @Autowired TestRestTemplate rest;

    private ResponseEntity<JsonNode> get(String path) {
        return rest.getForEntity(path, JsonNode.class);
    }

    private ResponseEntity<JsonNode> post(String path) {
        return rest.exchange(path, HttpMethod.POST, HttpEntity.EMPTY, JsonNode.class);
    }

    /** Lote de 10 laboratórios, o último com CNPJ inválido (vira DEAD). Devolve o id do lote. */
    private long batchWithOneFailure() {
        List<IngestionService.Item> items = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            items.add(new IngestionService.Item(MessageType.LABORATORY, "L-" + i, identity("Lab " + i, TaxIds.cnpj())));
        }
        items.add(new IngestionService.Item(MessageType.LABORATORY, "L-BAD", identity("Lab ruim", "11.111.111/1111-11")));
        return ingestion.ingest(new IngestionService.Command("erp", MergeStrategy.MERGE, items)).batchId();
    }

    @Test
    void loteComUmaFalhaApareceComoConcluidoComFalhas() {
        long batch = batchWithOneFailure();

        JsonNode before = get("/api/integration/batches/" + batch).getBody();
        assertThat(before.get("state").asText()).isEqualTo("EM_ANDAMENTO");
        assertThat(before.get("pending").asInt()).isEqualTo(10);

        processor.runCycle();
        processor.runCycle();

        JsonNode after = get("/api/integration/batches/" + batch).getBody();
        assertThat(after.get("total").asInt()).isEqualTo(10);
        assertThat(after.get("done").asInt()).isEqualTo(9);
        assertThat(after.get("dead").asInt()).isEqualTo(1);
        assertThat(after.get("state").asText()).isEqualTo("CONCLUIDO_COM_FALHAS");
        assertThat(after.get("source").asText()).isEqualTo("erp");
        assertThat(after.get("mergeStrategy").asText()).isEqualTo("MERGE");
    }

    @Test
    void loteSemFalhasEConcluido() {
        long batch = ingest("erp", MergeStrategy.MERGE, MessageType.LABORATORY, "L-1",
                identity("Lab", TaxIds.cnpj())).batchId();
        processor.runCycle();
        assertThat(get("/api/integration/batches/" + batch).getBody().get("state").asText()).isEqualTo("CONCLUIDO");
    }

    @Test
    void listaOsLotesMaisRecentesPrimeiro() {
        long first = ingest("a", MergeStrategy.MERGE, MessageType.LABORATORY, "L-1", identity("A", TaxIds.cnpj()))
                .batchId();
        long second = ingest("b", MergeStrategy.MERGE, MessageType.LABORATORY, "L-2", identity("B", TaxIds.cnpj()))
                .batchId();

        JsonNode list = get("/api/integration/batches?limit=10").getBody();

        assertThat(list.get(0).get("id").asLong()).isEqualTo(second);
        assertThat(list.get(1).get("id").asLong()).isEqualTo(first);
    }

    @Test
    void loteInexistenteResponde404() {
        assertThat(get("/api/integration/batches/999999").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void dlqListaApenasMensagensMortasComUltimoErro() {
        long batch = batchWithOneFailure();
        processor.runCycle();
        processor.runCycle();

        JsonNode page = get("/api/integration/messages?status=DEAD&batchId=" + batch).getBody();

        assertThat(page.get("totalElements").asInt()).isEqualTo(1);
        JsonNode dead = page.get("content").get(0);
        assertThat(dead.get("externalId").asText()).isEqualTo("L-BAD");
        assertThat(dead.get("status").asText()).isEqualTo("DEAD");
        assertThat(dead.get("lastError").asText()).contains("CPF/CNPJ");
        assertThat(dead.get("payload").isNull()).as("payload só no detalhe").isTrue();

        JsonNode detail = get("/api/integration/messages/" + dead.get("id").asLong()).getBody();
        assertThat(detail.get("payload").asText()).contains("Lab ruim");
    }

    @Test
    void mensagensSaoPaginadas() {
        batchWithOneFailure();
        JsonNode page = get("/api/integration/messages?size=4&page=2").getBody();
        assertThat(page.get("content")).hasSize(2);
        assertThat(page.get("totalElements").asInt()).isEqualTo(10);
        assertThat(page.get("totalPages").asInt()).isEqualTo(3);
    }

    @Test
    void reprocessarMensagemMortaVoltaAPendingComZeroTentativas() {
        long batch = batchWithOneFailure();
        processor.runCycle();
        processor.runCycle();
        long deadId = jdbc.queryForObject("SELECT id FROM outbox_message WHERE status = 'DEAD'", Long.class);

        ResponseEntity<JsonNode> r = post("/api/integration/messages/" + deadId + "/reprocess");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("status").asText()).isEqualTo("PENDING");
        assertThat(r.getBody().get("attempts").asInt()).isZero();
        assertThat(get("/api/integration/batches/" + batch).getBody().get("state").asText())
                .isEqualTo("EM_ANDAMENTO");
    }

    @Test
    void reprocessarMensagemForaDeDeadResponde409ENaoMudaNada() {
        long batch = batchWithOneFailure();
        processor.runCycle();
        processor.runCycle();
        long doneId = jdbc.queryForObject("SELECT id FROM outbox_message WHERE status = 'DONE' LIMIT 1", Long.class);

        ResponseEntity<JsonNode> r = post("/api/integration/messages/" + doneId + "/reprocess");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT status FROM outbox_message WHERE id = ?", String.class, doneId))
                .isEqualTo("DONE");
        assertThat(post("/api/integration/messages/999999/reprocess").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(batch).isPositive();
    }

    @Test
    void reprocessarLoteReenfileiraTodasAsMortasEAposACorrecaoConclui() {
        long batch = batchWithOneFailure();
        processor.runCycle();
        processor.runCycle();
        // Corrige a causa (o CNPJ inválido) direto no payload preservado e reprocessa o lote.
        jdbc.update("UPDATE outbox_message SET payload = ? WHERE status = 'DEAD'",
                identity("Lab corrigido", TaxIds.cnpj()).toString());

        ResponseEntity<JsonNode> r = post("/api/integration/batches/" + batch + "/reprocess");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("requeued").asInt()).isEqualTo(1);

        processor.runCycle();
        JsonNode summary = get("/api/integration/batches/" + batch).getBody();
        assertThat(summary.get("done").asInt()).isEqualTo(10);
        assertThat(summary.get("state").asText()).isEqualTo("CONCLUIDO");
        assertThat(post("/api/integration/batches/" + batch + "/reprocess").getBody().get("requeued").asInt())
                .as("nada morto a reenfileirar").isZero();
        assertThat(post("/api/integration/batches/999999/reprocess").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void execucaoManualProcessaUmCicloSemEsperarOAgendador() {
        ingest("erp", MergeStrategy.MERGE, MessageType.LABORATORY, "L-1", identity("Lab", TaxIds.cnpj()));

        ResponseEntity<JsonNode> r = post("/api/integration/worker/run");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("processed").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM outbox_message", String.class)).isEqualTo("DONE");
    }
}
