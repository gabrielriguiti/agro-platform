package br.com.gabrielriguiti.agro.domain.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifica as invariantes de {@link Producer}, {@link Property} e {@link Laboratory}. */
class RegistryEntitiesTest {

    private static final Identity PERSON = new Identity("Maria", TaxId.of("529.982.247-25"), "Rua A");
    private static final Identity COMPANY = new Identity("Lab", TaxId.of("11.222.333/0001-81"), "Rua B");
    private static final Instant T0 = Instant.parse("2025-01-01T00:00:00Z");

    @Test
    void produtorValidoCopiaOConjuntoDePropriedades() {
        Producer producer = new Producer(null, PERSON, T0, T0.plusSeconds(60), null, 1L, Set.of(1L, 2L));
        assertThat(producer.propertyIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThatThrownBy(() -> producer.propertyIds().add(3L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void produtorAceitaPeriodoDeUmInstante() {
        assertThat(new Producer(null, PERSON, T0, T0, null, 1L, null).propertyIds()).isEmpty();
    }

    @Test
    void periodoInvertidoEhRejeitado() {
        assertThatThrownBy(() -> new Producer(null, PERSON, T0, T0.minusSeconds(1), null, 1L, null))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        e -> assertThat(e.field()).isEqualTo("dataFinal"));
    }

    @Test
    void laboratorioEhObrigatorio() {
        assertThatThrownBy(() -> new Producer(null, PERSON, T0, T0, null, null, null))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        e -> assertThat(e.field()).isEqualTo("laboratorioId"));
    }

    @Test
    void periodoEhObrigatorio() {
        assertThatThrownBy(() -> new Producer(null, PERSON, null, T0, null, 1L, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new Producer(null, PERSON, T0, null, null, 1L, null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void laboratorioExigeCnpj() {
        assertThat(Laboratory.create(COMPANY).identity()).isEqualTo(COMPANY);
        assertThatThrownBy(() -> Laboratory.create(PERSON)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void propriedadeNovaNasceDisponivel() {
        Property property = Property.create(PERSON);
        assertThat(property.isAvailable()).isTrue();
        assertThat(new Property(1L, PERSON, 7L).isAvailable()).isFalse();
    }

    @Test
    void atualizacaoPreservaIdEVinculo() {
        Property updated = new Property(5L, PERSON, 7L).withIdentity(COMPANY);
        assertThat(updated.id()).isEqualTo(5L);
        assertThat(updated.producerId()).isEqualTo(7L);
        assertThat(updated.identity()).isEqualTo(COMPANY);
    }
}
