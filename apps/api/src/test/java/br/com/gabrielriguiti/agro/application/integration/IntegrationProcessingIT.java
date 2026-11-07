package br.com.gabrielriguiti.agro.application.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.IntegrationTestSupport;
import br.com.gabrielriguiti.agro.TaxIds;
import br.com.gabrielriguiti.agro.application.registry.LaboratoryService;
import br.com.gabrielriguiti.agro.application.registry.ProducerService;
import br.com.gabrielriguiti.agro.application.registry.PropertyService;
import br.com.gabrielriguiti.agro.domain.integration.MergeStrategy;
import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import br.com.gabrielriguiti.agro.domain.registry.Producer;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import br.com.gabrielriguiti.agro.domain.registry.TaxId;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Worker de ponta a ponta: reconciliação, entidades aninhadas, retry até a DLQ, corrida por CNPJ e atomicidade por
 * mensagem (tasks 4.1 a 4.5).
 */
class IntegrationProcessingIT extends IntegrationTestSupport {

    @Autowired LaboratoryService laboratories;
    @Autowired PropertyService properties;
    @Autowired ProducerService producers;

    private long labsWithTaxId(String taxId) {
        return jdbc.queryForObject("SELECT count(*) FROM laboratories WHERE tax_id = ?", Long.class, taxId);
    }

    private Map<String, Object> lab(String taxId) {
        return jdbc.queryForMap("SELECT * FROM laboratories WHERE tax_id = ?", taxId);
    }

    private long refs(String type, String source, String externalId) {
        return jdbc.queryForObject("SELECT count(*) FROM external_reference WHERE entity_type = ? AND source = ? "
                + "AND external_id = ?", Long.class, type, source, externalId);
    }

    // ---- 4.1 laboratório e propriedade ----

    @Test
    void laboratorioNovoEGravadoComVinculoExterno() {
        String cnpj = TaxIds.cnpj();
        long batch = ingest("erp", MergeStrategy.MERGE, MessageType.LABORATORY, "L-1", identity("Lab Novo", cnpj))
                .batchId();

        assertThat(runCycleNow()).isEqualTo(1);

        assertThat(statusOf(batch, "L-1")).isEqualTo("DONE");
        assertThat(lab(cnpj)).containsEntry("name", "Lab Novo");
        assertThat(refs("LABORATORY", "erp", "L-1")).isEqualTo(1);
    }

    @Test
    void mesmoCnpjDeOutroSistemaReconciliaEGanhaSegundoVinculo() {
        String cnpj = TaxIds.cnpj();
        ingest("A", MergeStrategy.MERGE, MessageType.LABORATORY, "L-A", identity("Lab", cnpj));
        runCycleNow();
        ingest("B", MergeStrategy.MERGE, MessageType.LABORATORY, "L-B", identity("Lab", cnpj));
        runCycleNow();

        assertThat(labsWithTaxId(cnpj)).isEqualTo(1);
        assertThat(refs("LABORATORY", "A", "L-A")).isEqualTo(1);
        assertThat(refs("LABORATORY", "B", "L-B")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT entity_id) FROM external_reference "
                + "WHERE entity_type = 'LABORATORY'", Long.class)).isEqualTo(1);
    }

    @Test
    void mergePreservaOExistenteOverwriteSubstituiKeepExistingIgnora() {
        String cnpj = TaxIds.cnpj();
        ingest("erp", MergeStrategy.OVERWRITE, MessageType.LABORATORY, "L-1", identity("Original", cnpj));
        runCycleNow();

        ObjectNode changed = identity("Alterado", cnpj);
        changed.put("endereco", "Outro endereço");

        long keep = ingest("erp", MergeStrategy.KEEP_EXISTING, MessageType.LABORATORY, "L-1", changed).batchId();
        runCycleNow();
        assertThat(statusOf(keep, "L-1")).isEqualTo("DONE");
        assertThat(lab(cnpj)).containsEntry("name", "Original");

        ingest("erp", MergeStrategy.MERGE, MessageType.LABORATORY, "L-1", changed);
        runCycleNow();
        assertThat(lab(cnpj)).containsEntry("name", "Original").containsEntry("address", "Rua Original");

        // A chave de idempotência não inclui a estratégia: o mesmo payload seria duplicata, então este difere.
        ObjectNode corrected = changed.deepCopy();
        corrected.put("endereco", "Endereço final");
        ingest("erp", MergeStrategy.OVERWRITE, MessageType.LABORATORY, "L-1", corrected);
        runCycleNow();
        assertThat(lab(cnpj)).containsEntry("name", "Alterado").containsEntry("address", "Endereço final");
        assertThat(labsWithTaxId(cnpj)).isEqualTo(1);
    }

    @Test
    void propriedadeEReconciliadaPeloCpf() {
        String cpf = TaxIds.cpf();
        ingest("erp", MergeStrategy.OVERWRITE, MessageType.PROPERTY, "F-1", identity("Fazenda Velha", cpf));
        runCycleNow();
        ingest("erp", MergeStrategy.OVERWRITE, MessageType.PROPERTY, "F-1", identity("Fazenda Nova", cpf));
        runCycleNow();

        assertThat(jdbc.queryForList("SELECT name FROM properties WHERE tax_id = ?", String.class, cpf))
                .containsExactly("Fazenda Nova");
    }

    @Test
    void conflitoDeIdentidadeVaiParaDlqSemAlterarNada() {
        String cnpjX = TaxIds.cnpj();
        String cnpjY = TaxIds.cnpj();
        ingest("erp", MergeStrategy.OVERWRITE, MessageType.LABORATORY, "L-1", identity("Lab X", cnpjX));
        ingest("lims", MergeStrategy.OVERWRITE, MessageType.LABORATORY, "L-Y", identity("Lab Y", cnpjY));
        runCycleNow();

        long batch = ingest("erp", MergeStrategy.OVERWRITE, MessageType.LABORATORY, "L-1",
                identity("Lab X mudado", cnpjY)).batchId();
        runCycleNow();

        assertThat(statusOf(batch, "L-1")).isEqualTo("DEAD");
        assertThat(jdbc.queryForObject("SELECT last_error FROM outbox_message WHERE batch_id = ?", String.class,
                batch)).contains("conflito de identidade");
        assertThat(jdbc.queryForObject("SELECT attempts FROM outbox_message WHERE batch_id = ?", Integer.class,
                batch)).as("falha permanente: direto, na primeira tentativa").isEqualTo(1);
        assertThat(lab(cnpjX)).containsEntry("name", "Lab X");
        assertThat(lab(cnpjY)).containsEntry("name", "Lab Y");
    }

    // ---- 4.2 produtor com entidades aninhadas ----

    @Test
    void produtorComLaboratorioEDuasPropriedadesNovasEmBancoVazio() {
        String prodCpf = TaxIds.cpf();
        String labCnpj = TaxIds.cnpj();
        String p1 = TaxIds.cpf();
        String p2 = TaxIds.cpf();
        ObjectNode payload = producer("Maria", prodCpf, nested("LAB-1", "Lab Aninhado", labCnpj),
                nested("PR-1", "Fazenda 1", p1), nested("PR-2", "Fazenda 2", p2));

        long batch = ingest("erp", MergeStrategy.MERGE, MessageType.PRODUCER, "P-1", payload).batchId();
        runCycleNow();

        assertThat(statusOf(batch, "P-1")).isEqualTo("DONE");
        Map<String, Object> producerRow = jdbc.queryForMap("SELECT * FROM producers WHERE tax_id = ?", prodCpf);
        assertThat(producerRow.get("laboratory_id")).isEqualTo(lab(labCnpj).get("id"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM properties WHERE producer_id = ? AND tax_id IN (?, ?)",
                Long.class, producerRow.get("id"), p1, p2)).isEqualTo(2);
        assertThat(refs("PRODUCER", "erp", "P-1")).isEqualTo(1);
        assertThat(refs("LABORATORY", "erp", "LAB-1")).isEqualTo(1);
        assertThat(refs("PROPERTY", "erp", "PR-1")).isEqualTo(1);
    }

    @Test
    void reprocessarOMesmoProdutorNaoMudaContagensNemVinculos() {
        String prodCpf = TaxIds.cpf();
        ObjectNode payload = producer("Maria", prodCpf, nested("LAB-1", "Lab", TaxIds.cnpj()),
                nested("PR-1", "Fazenda 1", TaxIds.cpf()), nested("PR-2", "Fazenda 2", TaxIds.cpf()));
        ingest("erp", MergeStrategy.MERGE, MessageType.PRODUCER, "P-1", payload);
        runCycleNow();
        long producersBefore = count("producers");
        long propertiesBefore = count("properties");
        long labsBefore = count("laboratories");
        long refsBefore = count("external_reference");

        // Reenvio com a mesma identidade lógica, mas outra observação: nova mensagem (payload diferente).
        payload.put("observacoes", "reenviado");
        long batch = ingest("erp", MergeStrategy.OVERWRITE, MessageType.PRODUCER, "P-1", payload).batchId();
        runCycleNow();

        assertThat(statusOf(batch, "P-1")).isEqualTo("DONE");
        assertThat(count("producers")).isEqualTo(producersBefore);
        assertThat(count("properties")).isEqualTo(propertiesBefore);
        assertThat(count("laboratories")).isEqualTo(labsBefore);
        assertThat(count("external_reference")).isEqualTo(refsBefore);
        assertThat(jdbc.queryForObject("SELECT observacoes FROM producers WHERE tax_id = ?", String.class, prodCpf))
                .isEqualTo("reenviado");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM properties p JOIN producers d ON d.id = p.producer_id "
                + "WHERE d.tax_id = ?", Long.class, prodCpf)).isEqualTo(2);
    }

    @Test
    void reenvioParcialNaoRemoveNemDuplicaPropriedadesJaVinculadas() {
        String prodCpf = TaxIds.cpf();
        String labCnpj = TaxIds.cnpj();
        ObjectNode a = nested("PR-A", "Prop A", TaxIds.cpf());
        ObjectNode b = nested("PR-B", "Prop B", TaxIds.cpf());
        ingest("erp", MergeStrategy.MERGE, MessageType.PRODUCER, "P-9",
                producer("Produtor", prodCpf, nested("LAB", "Lab", labCnpj), a, b));
        runCycleNow();

        ingest("erp", MergeStrategy.OVERWRITE, MessageType.PRODUCER, "P-9",
                producer("Produtor corrigido", prodCpf, nested("LAB", "Lab", labCnpj), a));
        runCycleNow();

        assertThat(jdbc.queryForObject("SELECT name FROM producers WHERE tax_id = ?", String.class, prodCpf))
                .isEqualTo("Produtor corrigido");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM properties p JOIN producers d ON d.id = p.producer_id "
                + "WHERE d.tax_id = ?", Long.class, prodCpf)).isEqualTo(2);
    }

    // ---- 4.3 retry, backoff e DLQ ----

    /** Cria o cenário de falha transitória real: a propriedade aninhada já pertence a outro produtor. */
    private String propertyOwnedByAnotherProducer() {
        Laboratory lab = laboratories.create(new Identity("Lab do dono", TaxId.of(TaxIds.cnpj()), "Av"));
        String propertyCpf = TaxIds.cpf();
        Property owned = properties.create(new Identity("Propriedade ocupada", TaxId.of(propertyCpf), "Rua"));
        Instant t = Instant.parse("2025-01-01T00:00:00Z");
        producers.create(new Producer(null, new Identity("Dono", TaxId.of(TaxIds.cpf()), "Rua"), t, t, null,
                lab.id(), Set.of(owned.id())));
        return propertyCpf;
    }

    @Test
    void falhaTransitoriaReagendaComBackoffEEsgotaNaDlq() {
        String ownedCpf = propertyOwnedByAnotherProducer();
        ObjectNode payload = producer("Disputante", TaxIds.cpf(), nested("LAB", "Lab", TaxIds.cnpj()),
                nested("PR-X", "Propriedade ocupada", ownedCpf));
        long batch = ingest("erp", MergeStrategy.MERGE, MessageType.PRODUCER, "P-X", payload).batchId();

        processor.runCycle();
        assertThat(statusOf(batch, "P-X")).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT next_attempt_at > now() FROM outbox_message", Boolean.class))
                .as("reagendada com backoff no futuro").isTrue();
        assertThat(jdbc.queryForObject("SELECT attempts FROM outbox_message", Integer.class)).isEqualTo(1);
        assertThat(processor.runCycle()).as("ainda dentro do backoff, nada é elegível").isZero();

        for (int attempt = 2; attempt <= 4; attempt++) {
            runCycleNow();
            assertThat(statusOf(batch, "P-X")).isEqualTo("PENDING");
        }
        runCycleNow();

        assertThat(statusOf(batch, "P-X")).isEqualTo("DEAD");
        assertThat(jdbc.queryForObject("SELECT attempts FROM outbox_message", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT last_error FROM outbox_message", String.class))
                .contains("ConflictException");
        assertThat(jdbc.queryForObject("SELECT payload FROM outbox_message", String.class))
                .contains("\"Disputante\"");
    }

    @Test
    void falhaPermanenteVaiDiretoParaDlqNaPrimeiraTentativa() {
        ObjectNode invalid = identity("Lab", "123.456.789-00");
        long batch = ingest("erp", MergeStrategy.MERGE, MessageType.LABORATORY, "L-BAD", invalid).batchId();

        processor.runCycle();

        assertThat(statusOf(batch, "L-BAD")).isEqualTo("DEAD");
        assertThat(jdbc.queryForObject("SELECT attempts FROM outbox_message", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT last_error FROM outbox_message", String.class)).contains("CPF/CNPJ");
    }

    @Test
    void payloadQueNaoEObjetoEPermanente() {
        long batch = ingest("erp", MergeStrategy.MERGE, MessageType.LABORATORY, "L-STR",
                JSON.getNodeFactory().textNode("não sou um objeto")).batchId();
        processor.runCycle();
        assertThat(statusOf(batch, "L-STR")).isEqualTo("DEAD");
    }

    @Test
    void produtorSemLaboratorioEPermanente() {
        ObjectNode payload = identity("Sem lab", TaxIds.cpf());
        payload.put("dataInicial", "2025-01-01T00:00:00Z");
        payload.put("dataFinal", "2025-01-02T00:00:00Z");
        long batch = ingest("erp", MergeStrategy.MERGE, MessageType.PRODUCER, "P-NOLAB", payload).batchId();
        processor.runCycle();
        assertThat(statusOf(batch, "P-NOLAB")).isEqualTo("DEAD");
    }

    // ---- 4.4 corrida por CNPJ ----

    @Test
    void duasMensagensComOMesmoCnpjTerminamComUmUnicoRegistro() throws Exception {
        for (int round = 0; round < 8; round++) {
            String cnpj = TaxIds.cnpj();
            ingestion.ingest(new IngestionService.Command("erp", MergeStrategy.MERGE, List.of(
                    new IngestionService.Item(MessageType.LABORATORY, "L-A" + round, identity("Lab A", cnpj)),
                    new IngestionService.Item(MessageType.LABORATORY, "L-B" + round, identity("Lab B", cnpj)))));

            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                CyclicBarrier barrier = new CyclicBarrier(2);
                List<Future<Integer>> futures = List.of(
                        pool.submit(() -> { barrier.await(); return processor.runCycle(); }),
                        pool.submit(() -> { barrier.await(); return processor.runCycle(); }));
                for (Future<Integer> f : futures) {
                    f.get();
                }
            } finally {
                pool.shutdownNow();
            }
            // Se houve colisão, a perdedora foi reagendada; o próximo ciclo a reconcilia com o registro criado.
            runCycleNow();

            assertThat(labsWithTaxId(cnpj)).as("rodada %d", round).isEqualTo(1);
            assertThat(jdbc.queryForList("SELECT status FROM outbox_message", String.class))
                    .as("rodada %d", round).containsOnly("DONE");
            jdbc.update("DELETE FROM outbox_message");
        }
    }

    // ---- 4.5 atomicidade por mensagem ----

    @Test
    void falhaNaSegundaPropriedadeNaoDeixaNadaGravado() {
        long labsBefore = count("laboratories");
        long propsBefore = count("properties");
        long producersBefore = count("producers");
        ObjectNode secondInvalid = nested("PR-2", "Prop 2", "000.000.000-00");
        long batch = ingest("erp", MergeStrategy.MERGE, MessageType.PRODUCER, "P-ATOM",
                producer("Atômico", TaxIds.cpf(), nested("LAB", "Lab", TaxIds.cnpj()),
                        nested("PR-1", "Prop 1", TaxIds.cpf()), secondInvalid)).batchId();

        processor.runCycle();

        assertThat(statusOf(batch, "P-ATOM")).isEqualTo("DEAD");
        assertThat(count("laboratories")).isEqualTo(labsBefore);
        assertThat(count("properties")).isEqualTo(propsBefore);
        assertThat(count("producers")).isEqualTo(producersBefore);
        assertThat(count("external_reference")).isZero();
    }
}
