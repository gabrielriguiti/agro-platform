package br.com.gabrielriguiti.agro.domain.integration;

import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.Producer;
import java.util.Set;

/**
 * Fusão pura entre o registro existente e o recebido, conforme a {@link MergeStrategy} (ADR-0015). Sem acesso a
 * banco, testada em milissegundos.
 */
public final class ReconciliationPolicy {

    private ReconciliationPolicy() {
    }

    /**
     * Escolhe o valor de um campo.
     *
     * @param existing valor atual (pode ser nulo ou vazio)
     * @param received valor recebido (pode ser nulo ou vazio)
     * @param strategy estratégia do lote
     * @return {@code MERGE}: o existente, ou o recebido se o existente estiver vazio; {@code OVERWRITE}: o recebido,
     *     ou o existente se o recebido vier vazio; {@code KEEP_EXISTING}: o existente
     */
    public static <T> T pick(T existing, T received, MergeStrategy strategy) {
        return switch (strategy) {
            case MERGE -> isEmpty(existing) ? received : existing;
            case OVERWRITE -> isEmpty(received) ? existing : received;
            case KEEP_EXISTING -> existing;
        };
    }

    /** @return identidade fundida campo a campo */
    public static Identity identity(Identity existing, Identity received, MergeStrategy strategy) {
        return new Identity(
                pick(existing.name(), received.name(), strategy),
                pick(existing.taxId(), received.taxId(), strategy),
                pick(existing.address(), received.address(), strategy));
    }

    /**
     * Funde os dados do produtor. O conjunto de propriedades nunca é reduzido: o do existente é preservado
     * (vínculos preservados, spec idempotent-processing).
     *
     * @param existing produtor gravado
     * @param received produtor recebido, com o laboratório já resolvido
     */
    public static Producer producer(Producer existing, Producer received, MergeStrategy strategy) {
        return new Producer(
                existing.id(),
                identity(existing.identity(), received.identity(), strategy),
                pick(existing.dataInicial(), received.dataInicial(), strategy),
                pick(existing.dataFinal(), received.dataFinal(), strategy),
                pick(existing.observacoes(), received.observacoes(), strategy),
                pick(existing.laboratoryId(), received.laboratoryId(), strategy),
                Set.copyOf(existing.propertyIds()));
    }

    private static boolean isEmpty(Object value) {
        return value == null || (value instanceof String s && s.isBlank());
    }
}
