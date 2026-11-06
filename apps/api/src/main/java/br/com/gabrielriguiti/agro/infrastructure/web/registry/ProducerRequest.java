package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.Producer;
import br.com.gabrielriguiti.agro.domain.registry.TaxId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

/**
 * Corpo de criação e atualização de produtor.
 *
 * @param nome nome
 * @param inscricaoFiscal CPF ou CNPJ, com ou sem máscara
 * @param endereco endereço
 * @param dataInicial início do período (ISO-8601)
 * @param dataFinal fim do período (ISO-8601), não anterior a {@code dataInicial}
 * @param observacoes texto livre opcional
 * @param laboratorioId laboratório existente (obrigatório)
 * @param propriedadeIds propriedades livres a vincular; na atualização substitui o conjunto anterior
 */
@Schema(description = "Dados do produtor")
public record ProducerRequest(
        @Schema(example = "Maria Aparecida da Silva")
        @NotBlank(message = "nome é obrigatório") @Size(max = 200, message = "nome deve ter no máximo 200 caracteres")
        String nome,
        @Schema(description = "CPF ou CNPJ, com ou sem máscara", example = "529.982.247-25")
        @NotBlank(message = "inscricaoFiscal é obrigatória") @TaxIdFormat
        String inscricaoFiscal,
        @Schema(example = "Linha 5, lote 12, Lucas do Rio Verde/MT")
        @NotBlank(message = "endereco é obrigatório")
        @Size(max = 500, message = "endereco deve ter no máximo 500 caracteres")
        String endereco,
        @Schema(description = "Início do período (ISO-8601)", example = "2025-01-01T00:00:00Z")
        @NotNull(message = "dataInicial é obrigatória")
        Instant dataInicial,
        @Schema(description = "Fim do período (ISO-8601)", example = "2025-12-31T23:59:59Z")
        @NotNull(message = "dataFinal é obrigatória")
        Instant dataFinal,
        @Schema(description = "Observações livres", nullable = true) String observacoes,
        @Schema(description = "Laboratório do produtor", example = "1")
        @NotNull(message = "laboratorioId é obrigatório")
        Long laboratorioId,
        @Schema(description = "Propriedades a vincular (devem estar livres ou já ser do produtor)")
        Set<@NotNull(message = "propriedadeIds não aceita valores nulos") Long> propriedadeIds) {

    /** @return o produtor do domínio (sem id), que valida o período e o laboratório */
    Producer toDomain() {
        return new Producer(null, new Identity(nome, TaxId.of(inscricaoFiscal), endereco), dataInicial, dataFinal,
                observacoes, laboratorioId, propriedadeIds);
    }
}
