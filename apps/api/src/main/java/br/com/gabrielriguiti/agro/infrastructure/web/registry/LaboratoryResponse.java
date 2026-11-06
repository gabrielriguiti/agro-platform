package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Laboratório devolvido pela API (também embutido na consulta de produtor).
 *
 * @param id identificador
 * @param nome nome
 * @param inscricaoFiscal CNPJ formatado
 * @param endereco endereço
 */
@Schema(description = "Laboratório")
public record LaboratoryResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Laboratório Solo Vivo") String nome,
        @Schema(description = "CNPJ formatado", example = "11.222.333/0001-81") String inscricaoFiscal,
        @Schema(example = "Av. das Análises, 100, Cuiabá/MT") String endereco) {

    static LaboratoryResponse from(Laboratory l) {
        return new LaboratoryResponse(l.id(), l.identity().name(), l.identity().taxId().formatted(),
                l.identity().address());
    }
}
