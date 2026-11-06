package br.com.gabrielriguiti.agro.domain.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Verifica os atributos globais obrigatórios. */
class IdentityTest {

    private static final TaxId CPF = TaxId.of("529.982.247-25");

    @Test
    void criaComAtributosValidosAparandoEspacos() {
        Identity identity = new Identity("  Fazenda Boa  ", CPF, " Rua A, 1 ");
        assertThat(identity.name()).isEqualTo("Fazenda Boa");
        assertThat(identity.address()).isEqualTo("Rua A, 1");
    }

    @Test
    void nomeVazioApontaOCampoNome() {
        assertThatThrownBy(() -> new Identity("  ", CPF, "Rua A"))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        e -> assertThat(e.field()).isEqualTo("nome"));
    }

    @Test
    void enderecoAusenteApontaOCampoEndereco() {
        assertThatThrownBy(() -> new Identity("Nome", CPF, null))
                .isInstanceOfSatisfying(DomainValidationException.class,
                        e -> assertThat(e.field()).isEqualTo("endereco"));
    }

    @Test
    void inscricaoAusenteEhRejeitada() {
        assertThatThrownBy(() -> new Identity("Nome", null, "Rua A")).isInstanceOf(InvalidTaxIdException.class);
    }

    @Test
    void nomeAcimaDoLimiteEhRejeitado() {
        assertThatThrownBy(() -> new Identity("x".repeat(201), CPF, "Rua A"))
                .isInstanceOf(DomainValidationException.class);
    }
}
