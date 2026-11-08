package br.com.gabrielriguiti.agro.infrastructure.config;

import br.com.gabrielriguiti.agro.application.integration.IngestionService;
import br.com.gabrielriguiti.agro.domain.integration.MergeStrategy;
import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Carga de dados fictícios do profile {@code demo}. Os dados entram pelo próprio pipeline de integração (um lote na
 * outbox, processado pelo worker), então a demonstração exercita o caminho real. É idempotente: se o lote da origem
 * {@value #SOURCE} já existe, nada é gravado. Os CPFs e CNPJs são válidos e inexistentes na vida real.
 */
@Component
@Profile("demo")
class DemoSeed implements ApplicationRunner {

    static final String SOURCE = "demo-seed";
    private static final Logger LOG = LoggerFactory.getLogger(DemoSeed.class);

    private final IngestionService ingestion;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    DemoSeed(IngestionService ingestion, JdbcTemplate jdbc, ObjectMapper mapper) {
        this.ingestion = ingestion;
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM integration_batch WHERE source = ?",
                Integer.class, SOURCE);
        if (existing != null && existing > 0) {
            LOG.info("Seed demo já carregado; nada a fazer");
            return;
        }
        List<IngestionService.Item> items = new ArrayList<>();
        items.add(item(MessageType.LABORATORY, "L-1",
                pessoa("Laboratório Solo Fértil", "90000001000129", "Av. das Palmeiras, 100, Ribeirão Preto/SP")));
        items.add(item(MessageType.LABORATORY, "L-2",
                pessoa("Laboratório Terra Viva", "90000002000173", "Rua do Cerrado, 45, Uberlândia/MG")));
        items.add(item(MessageType.PROPERTY, "F-1",
                pessoa("Fazenda Boa Esperança", "10000003115", "Estrada Rural km 12, Sertãozinho/SP")));
        items.add(item(MessageType.PROPERTY, "F-2",
                pessoa("Sítio Água Clara", "90000003000118", "Linha 7, zona rural, Patos de Minas/MG")));
        items.add(produtor("P-1", "Maria das Dores Lima", "10000001171", "Rua das Flores, 20, Franca/SP", "L-1"));
        items.add(produtor("P-2", "João Batista Rocha", "10000002143", "Praça Central, 3, Araguari/MG", "L-2"));
        // Item inválido de propósito: CPF com dígitos verificadores errados, para aparecer na DLQ do monitor.
        items.add(produtor("P-3", "Produtor com CPF inválido", "11111111112", "Rua Sem Saída, 0", "L-1"));
        var result = ingestion.ingest(new IngestionService.Command(SOURCE, MergeStrategy.MERGE, items));
        LOG.info("Seed demo enviado ao pipeline: lote {} com {} mensagens", result.batchId(), result.accepted());
    }

    private IngestionService.Item produtor(String id, String nome, String taxId, String endereco, String labId) {
        ObjectNode data = pessoa(nome, taxId, endereco);
        data.put("dataInicial", "2025-01-01T00:00:00Z");
        data.put("dataFinal", "2025-12-31T23:59:59Z");
        data.put("observacoes", "Dado fictício do demo");
        ObjectNode lab = pessoa(labId.equals("L-1") ? "Laboratório Solo Fértil" : "Laboratório Terra Viva",
                labId.equals("L-1") ? "90000001000129" : "90000002000173",
                labId.equals("L-1") ? "Av. das Palmeiras, 100, Ribeirão Preto/SP" : "Rua do Cerrado, 45, Uberlândia/MG");
        lab.put("id", labId);
        data.set("laboratorio", lab);
        return item(MessageType.PRODUCER, id, data);
    }

    private ObjectNode pessoa(String nome, String taxId, String endereco) {
        ObjectNode node = mapper.createObjectNode();
        node.put("nome", nome);
        node.put("inscricaoFiscal", taxId);
        node.put("endereco", endereco);
        return node;
    }

    private static IngestionService.Item item(MessageType type, String externalId, JsonNode data) {
        return new IngestionService.Item(type, externalId, data);
    }
}
