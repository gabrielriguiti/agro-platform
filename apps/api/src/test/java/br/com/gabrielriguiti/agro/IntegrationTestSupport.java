package br.com.gabrielriguiti.agro;

import br.com.gabrielriguiti.agro.application.integration.IngestionService;
import br.com.gabrielriguiti.agro.application.integration.MessageProcessor;
import br.com.gabrielriguiti.agro.domain.integration.MergeStrategy;
import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base dos testes do pipeline de integração: limpa a outbox, os lotes e os vínculos externos antes de cada teste
 * (o banco é compartilhado entre as classes) e oferece atalhos para montar payloads e conduzir o worker.
 */
public abstract class IntegrationTestSupport extends AbstractIntegrationTest {

    protected static final ObjectMapper JSON = new ObjectMapper();

    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected IngestionService ingestion;
    @Autowired protected MessageProcessor processor;

    @BeforeEach
    void cleanIntegrationTables() {
        jdbc.update("DELETE FROM outbox_message");
        jdbc.update("DELETE FROM integration_batch");
        jdbc.update("DELETE FROM external_reference");
    }

    /** @return objeto com nome, inscrição fiscal e endereço (nomes do contrato da API) */
    protected static ObjectNode identity(String name, String taxId) {
        ObjectNode n = JSON.createObjectNode();
        n.put("nome", name);
        n.put("inscricaoFiscal", taxId);
        n.put("endereco", "Rua " + name);
        return n;
    }

    /** @return objeto de entidade aninhada: identidade mais {@code id} externo */
    protected static ObjectNode nested(String id, String name, String taxId) {
        ObjectNode n = identity(name, taxId);
        n.put("id", id);
        return n;
    }

    /** @return payload de produtor com laboratório e propriedades aninhados */
    protected static ObjectNode producer(String name, String taxId, ObjectNode laboratory, ObjectNode... properties) {
        ObjectNode n = identity(name, taxId);
        n.put("dataInicial", "2025-01-01T00:00:00Z");
        n.put("dataFinal", "2025-12-31T23:59:59Z");
        n.set("laboratorio", laboratory);
        ArrayNode list = n.putArray("propriedades");
        for (ObjectNode p : properties) {
            list.add(p);
        }
        return n;
    }

    /** Ingere um item e devolve o resultado. */
    protected IngestionService.Result ingest(
            String source, MergeStrategy strategy, MessageType type, String externalId, JsonNode data) {
        return ingestion.ingest(new IngestionService.Command(source, strategy,
                List.of(new IngestionService.Item(type, externalId, data))));
    }

    /** Torna todas as mensagens pendentes elegíveis agora (pula o backoff) e roda um ciclo do worker. */
    protected int runCycleNow() {
        jdbc.update("UPDATE outbox_message SET next_attempt_at = now() - interval '1 second' WHERE status = 'PENDING'");
        return processor.runCycle();
    }

    protected String statusOf(long batchId, String externalId) {
        return jdbc.queryForObject("SELECT status FROM outbox_message WHERE batch_id = ? AND external_id = ?",
                String.class, batchId, externalId);
    }

    protected long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
