package br.com.gabrielriguiti.agro.infrastructure.web.integration;

import br.com.gabrielriguiti.agro.application.integration.IngestionService;
import br.com.gabrielriguiti.agro.domain.integration.MergeStrategy;
import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Envelope de um lote de integração. A validação do conteúdo de {@code data} é feita no processamento.
 *
 * @param source sistema parceiro de origem
 * @param mergeStrategy {@code MERGE}, {@code OVERWRITE} ou {@code KEEP_EXISTING}
 * @param items itens do lote (até o limite configurado, padrão 1000)
 */
@Schema(description = "Lote de integração")
public record BatchRequest(
        @Schema(description = "Sistema parceiro de origem", example = "erp-fazendas")
        @NotBlank(message = "source é obrigatório") @Size(max = 100, message = "source deve ter no máximo 100 caracteres")
        String source,
        @Schema(description = "Estratégia de fusão com registros existentes", example = "MERGE")
        @NotNull(message = "mergeStrategy é obrigatório")
        @Pattern(regexp = "MERGE|OVERWRITE|KEEP_EXISTING",
                message = "mergeStrategy deve ser MERGE, OVERWRITE ou KEEP_EXISTING")
        String mergeStrategy,
        @NotEmpty(message = "items não pode ser vazio") List<@Valid @NotNull(message = "item inválido") Item> items) {

    /**
     * Item do lote.
     *
     * @param type {@code PRODUCER}, {@code PROPERTY} ou {@code LABORATORY}
     * @param externalId identificador do item no sistema parceiro
     * @param data conteúdo da entidade
     */
    @Schema(description = "Item do lote")
    public record Item(
            @Schema(example = "PRODUCER")
            @NotNull(message = "type é obrigatório")
            @Pattern(regexp = "PRODUCER|PROPERTY|LABORATORY",
                    message = "type deve ser PRODUCER, PROPERTY ou LABORATORY")
            String type,
            @Schema(example = "P-9")
            @NotBlank(message = "externalId é obrigatório")
            @Size(max = 100, message = "externalId deve ter no máximo 100 caracteres")
            String externalId,
            @Schema(description = "Conteúdo da entidade (objeto JSON)", type = "object")
            JsonNode data) {
    }

    /** @return o comando de ingestão correspondente */
    IngestionService.Command toCommand() {
        return new IngestionService.Command(source.strip(), MergeStrategy.valueOf(mergeStrategy),
                items.stream()
                        .map(i -> new IngestionService.Item(MessageType.valueOf(i.type()), i.externalId().strip(),
                                i.data()))
                        .toList());
    }
}
