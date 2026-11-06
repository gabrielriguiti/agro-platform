package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import br.com.gabrielriguiti.agro.application.registry.ProducerDetails;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Produtor devolvido pela API, no contrato do enunciado: laboratório e propriedades embutidos.
 *
 * @param id identificador
 * @param nome nome
 * @param inscricaoFiscal CPF ou CNPJ formatado
 * @param endereco endereço
 * @param dataInicial início do período
 * @param dataFinal fim do período
 * @param observacoes observações livres
 * @param laboratorio laboratório do produtor
 * @param propriedades propriedades do produtor, ordenadas por nome
 */
@Schema(description = "Produtor com laboratório e propriedades")
public record ProducerResponse(
        @Schema(example = "7") Long id,
        String nome,
        @Schema(description = "CPF ou CNPJ formatado", example = "529.982.247-25") String inscricaoFiscal,
        String endereco,
        @Schema(example = "2025-01-01T00:00:00Z") Instant dataInicial,
        @Schema(example = "2025-12-31T23:59:59Z") Instant dataFinal,
        @Schema(nullable = true) String observacoes,
        LaboratoryResponse laboratorio,
        List<PropertySummary> propriedades) {

    /**
     * Propriedade embutida no produtor.
     *
     * @param id identificador
     * @param nome nome
     * @param inscricaoFiscal CPF ou CNPJ formatado
     * @param endereco endereço
     */
    @Schema(description = "Propriedade embutida no produtor")
    public record PropertySummary(Long id, String nome, String inscricaoFiscal, String endereco) {

        static PropertySummary from(Property p) {
            return new PropertySummary(p.id(), p.identity().name(), p.identity().taxId().formatted(),
                    p.identity().address());
        }
    }

    static ProducerResponse from(ProducerDetails d) {
        var p = d.producer();
        return new ProducerResponse(p.id(), p.identity().name(), p.identity().taxId().formatted(),
                p.identity().address(), p.dataInicial(), p.dataFinal(), p.observacoes(),
                LaboratoryResponse.from(d.laboratory()), d.properties().stream().map(PropertySummary::from).toList());
    }
}
