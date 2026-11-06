package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import br.com.gabrielriguiti.agro.domain.registry.Property;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Propriedade devolvida por {@code /api/properties}.
 *
 * @param id identificador
 * @param nome nome
 * @param inscricaoFiscal CPF ou CNPJ formatado
 * @param endereco endereço
 * @param produtorId produtor dono, ou nulo quando disponível
 */
@Schema(description = "Propriedade rural")
public record PropertyResponse(
        @Schema(example = "10") Long id,
        @Schema(example = "Fazenda Santa Clara") String nome,
        @Schema(description = "CPF ou CNPJ formatado", example = "529.982.247-25") String inscricaoFiscal,
        @Schema(example = "Rodovia BR-163, km 12, Sorriso/MT") String endereco,
        @Schema(description = "Produtor dono; nulo quando a propriedade está disponível", nullable = true)
        Long produtorId) {

    static PropertyResponse from(Property p) {
        return new PropertyResponse(p.id(), p.identity().name(), p.identity().taxId().formatted(),
                p.identity().address(), p.producerId());
    }
}
