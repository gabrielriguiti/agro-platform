package br.com.gabrielriguiti.agro.domain.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.TaxIds;
import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.Producer;
import br.com.gabrielriguiti.agro.domain.registry.TaxId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Estratégias de fusão, sem banco (spec reconciliation, task 1.2). */
class ReconciliationPolicyTest {

    private static final TaxId TAX = TaxId.of(TaxIds.cnpj());
    private static final Identity EXISTING = new Identity("Nome Antigo", TAX, "Rua A, 1");
    private static final Identity RECEIVED = new Identity("Nome Novo", TAX, "Rua B, 2");

    @Test
    void mergePreservaOExistenteQuandoPreenchido() {
        Identity merged = ReconciliationPolicy.identity(EXISTING, RECEIVED, MergeStrategy.MERGE);
        assertThat(merged).isEqualTo(EXISTING);
    }

    @Test
    void mergePreencheApenasCamposVazios() {
        assertThat(ReconciliationPolicy.pick(null, "novo", MergeStrategy.MERGE)).isEqualTo("novo");
        assertThat(ReconciliationPolicy.pick("  ", "novo", MergeStrategy.MERGE)).isEqualTo("novo");
        assertThat(ReconciliationPolicy.pick("velho", "novo", MergeStrategy.MERGE)).isEqualTo("velho");
    }

    @Test
    void overwriteSubstituiPeloRecebido() {
        assertThat(ReconciliationPolicy.identity(EXISTING, RECEIVED, MergeStrategy.OVERWRITE)).isEqualTo(RECEIVED);
    }

    @Test
    void overwriteNaoApagaComValorVazio() {
        assertThat(ReconciliationPolicy.pick("velho", null, MergeStrategy.OVERWRITE)).isEqualTo("velho");
    }

    @Test
    void keepExistingIgnoraORecebido() {
        assertThat(ReconciliationPolicy.identity(EXISTING, RECEIVED, MergeStrategy.KEEP_EXISTING))
                .isEqualTo(EXISTING);
        assertThat(ReconciliationPolicy.pick(null, "novo", MergeStrategy.KEEP_EXISTING)).isNull();
    }

    @Test
    void produtorPreservaSempreOsVinculosDasPropriedades() {
        Instant t = Instant.parse("2025-01-01T00:00:00Z");
        Producer existing = new Producer(7L, EXISTING, t, t, "obs", 1L, Set.of(10L, 11L));
        Producer received = new Producer(null, RECEIVED, t.plusSeconds(60), t.plusSeconds(120), null, 2L, Set.of(10L));
        Producer merged = ReconciliationPolicy.producer(existing, received, MergeStrategy.OVERWRITE);
        assertThat(merged.id()).isEqualTo(7L);
        assertThat(merged.identity()).isEqualTo(RECEIVED);
        assertThat(merged.laboratoryId()).isEqualTo(2L);
        assertThat(merged.observacoes()).isEqualTo("obs");
        assertThat(merged.propertyIds()).containsExactlyInAnyOrder(10L, 11L);
    }
}
