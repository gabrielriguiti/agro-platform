package br.com.gabrielriguiti.agro.domain.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifica os cenários da spec tax-id. */
class TaxIdTest {

    @Test
    void cpfValidoComMascaraEhNormalizado() {
        TaxId taxId = TaxId.of("529.982.247-25");
        assertThat(taxId.digits()).isEqualTo("52998224725");
        assertThat(taxId.isCpf()).isTrue();
        assertThat(taxId.formatted()).isEqualTo("529.982.247-25");
    }

    @Test
    void cpfComDigitoVerificadorErradoEhRejeitado() {
        assertThatThrownBy(() -> TaxId.of("123.456.789-00"))
                .isInstanceOf(InvalidTaxIdException.class)
                .hasMessage("CPF/CNPJ inválido");
    }

    @ParameterizedTest
    @ValueSource(strings = {"111.111.111-11", "00000000000", "11.111.111/1111-11", "99999999999999"})
    void sequenciaRepetidaEhRejeitada(String raw) {
        assertThatThrownBy(() -> TaxId.of(raw)).isInstanceOf(InvalidTaxIdException.class);
    }

    @Test
    void cnpjValidoEhNormalizadoEFormatado() {
        TaxId taxId = TaxId.of("11.222.333/0001-81");
        assertThat(taxId.digits()).isEqualTo("11222333000181");
        assertThat(taxId.isCnpj()).isTrue();
        assertThat(taxId.formatted()).isEqualTo("11.222.333/0001-81");
    }

    @Test
    void cnpjInvalidoEhRejeitado() {
        assertThatThrownBy(() -> TaxId.of("11.222.333/0001-44")).isInstanceOf(InvalidTaxIdException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5299822472A", "abc", "", "  ", "1234567890", "123456789012345"})
    void caracteresInvalidosOuTamanhoErradoSaoRejeitados(String raw) {
        assertThatThrownBy(() -> TaxId.of(raw)).isInstanceOf(InvalidTaxIdException.class);
    }

    @Test
    void nuloEhRejeitado() {
        assertThatThrownBy(() -> TaxId.of(null)).isInstanceOf(InvalidTaxIdException.class);
    }

    @Test
    void aceitaEspacosNaEntrada() {
        assertThat(TaxId.of(" 529 982 247 25 ").digits()).isEqualTo("52998224725");
    }

    @Test
    void erroApontaOCampoDaInscricao() {
        assertThatThrownBy(() -> TaxId.of("1")).isInstanceOfSatisfying(InvalidTaxIdException.class,
                e -> assertThat(e.field()).isEqualTo("inscricaoFiscal"));
    }
}
