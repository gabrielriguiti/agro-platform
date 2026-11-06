package br.com.gabrielriguiti.agro.application.registry;

import br.com.gabrielriguiti.agro.domain.registry.Property;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Porta de saída: persistência de propriedades e do vínculo com o produtor. */
public interface PropertyRepository {

    /**
     * Insere ou atualiza os dados da propriedade (o vínculo muda apenas por {@link #link} e {@link #unlinkExcept}).
     *
     * @throws ConflictException se a inscrição fiscal já existir
     */
    Property save(Property property);

    /** @return a propriedade, se existir */
    Optional<Property> findById(long id);

    /** @return todas as propriedades, ordenadas por nome */
    List<Property> findAllByName();

    /**
     * @param producerId produtor em edição, ou {@code null}
     * @return propriedades sem produtor mais as do produtor informado, ordenadas por nome
     */
    List<Property> findAvailable(Long producerId);

    /** @return as propriedades existentes entre os ids informados */
    List<Property> findAllById(Collection<Long> ids);

    /**
     * Vincula ao produtor, de forma atômica, apenas as propriedades livres ou já dele.
     *
     * @return quantas propriedades ficaram vinculadas; menor que {@code ids.size()} indica propriedade alheia
     */
    int link(long producerId, Collection<Long> ids);

    /** Libera as propriedades do produtor que não estão em {@code keep}. */
    void unlinkExcept(long producerId, Collection<Long> keep);

    /** Remove a propriedade (e, com isso, o vínculo). */
    void deleteById(long id);
}
