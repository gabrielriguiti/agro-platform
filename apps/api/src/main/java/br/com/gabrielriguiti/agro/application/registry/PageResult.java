package br.com.gabrielriguiti.agro.application.registry;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados, independente de framework.
 *
 * @param content itens da página
 * @param page número da página, começando em 0
 * @param size tamanho solicitado da página
 * @param totalElements total de itens em todas as páginas
 * @param <T> tipo do item
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    /** @return número total de páginas */
    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    /** @return página com cada item convertido por {@code mapper} */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}
