package br.com.gabrielriguiti.agro.infrastructure.web.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.gabrielriguiti.agro.IntegrationTestSupport;
import br.com.gabrielriguiti.agro.TaxIds;
import br.com.gabrielriguiti.agro.application.integration.IngestionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** {@code POST /api/integration/batches}: envelope, limite, atomicidade e reenvio (tasks 3.1 a 3.3). */
class BatchIngestionApiIT extends IntegrationTestSupport {

    @Autowired TestRestTemplate rest;

    private static ObjectNode item(String type, String externalId, JsonNode data) {
        ObjectNode n = JSON.createObjectNode();
        if (type != null) {
            n.put("type", type);
        }
        if (externalId != null) {
            n.put("externalId", externalId);
        }
        if (data != null) {
            n.set("data", data);
        }
        return n;
    }

    private static ObjectNode envelope(String source, String strategy, ObjectNode... items) {
        ObjectNode n = JSON.createObjectNode();
        if (source != null) {
            n.put("source", source);
        }
        if (strategy != null) {
            n.put("mergeStrategy", strategy);
        }
        ArrayNode list = n.putArray("items");
        for (ObjectNode i : items) {
            list.add(i);
        }
        return n;
    }

    private ResponseEntity<JsonNode> post(ObjectNode body) {
        return rest.postForEntity("/api/integration/batches", body, JsonNode.class);
    }

    private ObjectNode lab(String id) {
        return item("LABORATORY", id, identity("Lab " + id, TaxIds.cnpj()));
    }

    @Test
    void loteValidoResponde202SemEsperarOProcessamento() {
        long labsBefore = count("laboratories");
        ResponseEntity<JsonNode> r = post(envelope("erp", "MERGE", lab("L-1"), lab("L-2"), lab("L-3")));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(r.getBody().get("accepted").asInt()).isEqualTo(3);
        assertThat(r.getBody().get("duplicates").asInt()).isZero();
        long batchId = r.getBody().get("batchId").asLong();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_message WHERE batch_id = ? AND status = 'PENDING' "
                + "AND attempts = 0", Long.class, batchId)).isEqualTo(3);
        assertThat(count("laboratories")).as("nada foi processado ainda").isEqualTo(labsBefore);
    }

    @Test
    void cargaPontualDeUmItemEAceita() {
        ResponseEntity<JsonNode> r = post(envelope("erp", "OVERWRITE", lab("L-1")));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(r.getBody().get("accepted").asInt()).isEqualTo(1);
    }

    @Test
    void tipoDesconhecidoResponde400IndicandoItemECampo() {
        ResponseEntity<JsonNode> r = post(envelope("erp", "MERGE", lab("L-1"),
                item("FARM", "F-1", identity("Fazenda", TaxIds.cpf()))));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("errors").get(0).get("field").asText()).isEqualTo("items[1].type");
        assertThat(count("outbox_message")).isZero();
    }

    @Test
    void envelopeInvalidoResponde400() {
        assertThat(post(envelope(null, "MERGE", lab("L-1"))).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post(envelope("erp", null, lab("L-1"))).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post(envelope("erp", "MESCLAR", lab("L-1"))).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post(envelope("erp", "MERGE")).getStatusCode()).as("lote vazio").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post(envelope("erp", "MERGE", item("LABORATORY", null, JSON.createObjectNode())))
                .getStatusCode()).as("sem externalId").isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<JsonNode> semData = post(envelope("erp", "MERGE", item("LABORATORY", "L-1", null)));
        assertThat(semData.getStatusCode()).as("sem data").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(semData.getBody().get("errors").get(0).get("field").asText()).isEqualTo("items[0].data");
        assertThat(count("integration_batch")).isZero();
    }

    @Test
    void loteAcimaDoLimiteResponde413() {
        List<ObjectNode> items = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            items.add(item("LABORATORY", "L-" + i, JSON.createObjectNode()));
        }
        ResponseEntity<JsonNode> r = post(envelope("erp", "MERGE", items.toArray(new ObjectNode[0])));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(count("outbox_message")).isZero();
    }

    @Test
    void conteudoInvalidoDeDataNaoBarraAIngestao() {
        ResponseEntity<JsonNode> r = post(envelope("erp", "MERGE",
                item("LABORATORY", "L-1", JSON.createObjectNode().put("lixo", true))));
        assertThat(r.getStatusCode()).as("o conteúdo só é validado no processamento").isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void falhaNoMeioDaGravacaoNaoDeixaNadaNaOutbox() {
        String tooLong = "X".repeat(101); // estoura a coluna external_id (100) no 3º item, já dentro do banco
        IngestionService.Command command = new IngestionService.Command("erp",
                br.com.gabrielriguiti.agro.domain.integration.MergeStrategy.MERGE, List.of(
                        new IngestionService.Item(br.com.gabrielriguiti.agro.domain.integration.MessageType.LABORATORY,
                                "L-1", identity("A", TaxIds.cnpj())),
                        new IngestionService.Item(br.com.gabrielriguiti.agro.domain.integration.MessageType.LABORATORY,
                                "L-2", identity("B", TaxIds.cnpj())),
                        new IngestionService.Item(br.com.gabrielriguiti.agro.domain.integration.MessageType.LABORATORY,
                                tooLong, identity("C", TaxIds.cnpj()))));

        assertThatThrownBy(() -> ingestion.ingest(command)).isInstanceOf(RuntimeException.class);

        assertThat(count("outbox_message")).isZero();
        assertThat(count("integration_batch")).isZero();
    }

    @Test
    void reenvioIdenticoContaDuplicatasENaoCriaMensagens() {
        ObjectNode body = envelope("erp", "MERGE", lab("L-1"), lab("L-2"));
        ResponseEntity<JsonNode> first = post(body);
        ResponseEntity<JsonNode> second = post(body);

        assertThat(first.getBody().get("accepted").asInt()).isEqualTo(2);
        assertThat(second.getBody().get("accepted").asInt()).isZero();
        assertThat(second.getBody().get("duplicates").asInt()).isEqualTo(2);
        assertThat(count("outbox_message")).isEqualTo(2);
    }

    @Test
    void mesmoPayloadComCamposEmOutraOrdemEDuplicata() throws Exception {
        String taxId = TaxIds.cnpj();
        JsonNode a = JSON.readTree("{\"nome\":\"X\",\"inscricaoFiscal\":\"" + taxId + "\",\"endereco\":\"R\"}");
        JsonNode b = JSON.readTree("{\"endereco\":\"R\",\"inscricaoFiscal\":\"" + taxId + "\",\"nome\":\"X\"}");

        assertThat(post(envelope("erp", "MERGE", item("LABORATORY", "L-1", a))).getBody().get("accepted").asInt())
                .isEqualTo(1);
        ResponseEntity<JsonNode> second = post(envelope("erp", "MERGE", item("LABORATORY", "L-1", b)));
        assertThat(second.getBody().get("duplicates").asInt()).isEqualTo(1);
        assertThat(count("outbox_message")).isEqualTo(1);
    }
}
