package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import br.com.gabrielriguiti.agro.application.registry.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Página de resultados devolvida pela API.
 *
 * @param content itens da página
 * @param page número da página, a partir de 0
 * @param size tamanho solicitado
 * @param totalElements total de itens
 * @param totalPages total de páginas
 * @param <T> tipo do item
 */
@Schema(description = "Página de resultados")
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(result.content(), result.page(), result.size(), result.totalElements(),
                result.totalPages());
    }
}
