package br.com.gabrielriguiti.agro;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Teste que falha de propósito, só para ver o CI vermelho (tarefa 2.1). Existe apenas na branch ci-vermelho. */
class FalhaDePropositoTest {

    @Test
    void falhaDeProposito() {
        assertThat(1 + 1).isEqualTo(3);
    }
}
