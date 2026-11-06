package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.TaxId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de criação e atualização de laboratório e de propriedade.
 *
 * @param nome nome
 * @param inscricaoFiscal CPF ou CNPJ, com ou sem máscara (laboratório exige CNPJ)
 * @param endereco endereço
 */
@Schema(description = "Dados cadastrais de laboratório ou propriedade")
public record IdentityRequest(
        @Schema(description = "Nome", example = "Fazenda Santa Clara")
        @NotBlank(message = "nome é obrigatório") @Size(max = 200, message = "nome deve ter no máximo 200 caracteres")
        String nome,
        @Schema(description = "CPF ou CNPJ, com ou sem máscara", example = "11.222.333/0001-81")
        @NotBlank(message = "inscricaoFiscal é obrigatória") @TaxIdFormat
        String inscricaoFiscal,
        @Schema(description = "Endereço", example = "Rodovia BR-163, km 12, Sorriso/MT")
        @NotBlank(message = "endereco é obrigatório")
        @Size(max = 500, message = "endereco deve ter no máximo 500 caracteres")
        String endereco) {

    /** @return os atributos globais validados pelo domínio */
    Identity toIdentity() {
        return new Identity(nome, TaxId.of(inscricaoFiscal), endereco);
    }
}
