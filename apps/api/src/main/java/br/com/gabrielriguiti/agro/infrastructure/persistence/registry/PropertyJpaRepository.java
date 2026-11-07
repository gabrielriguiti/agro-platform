package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PropertyJpaRepository extends JpaRepository<PropertyEntity, Long> {

    @Query("select p from PropertyEntity p order by p.name, p.id")
    List<PropertyEntity> findAllOrdered();

    @Query("select p from PropertyEntity p where p.producerId is null or p.producerId = :producerId "
            + "order by p.name, p.id")
    List<PropertyEntity> findAvailable(@Param("producerId") Long producerId);

    @Query("select p from PropertyEntity p where p.producerId in :producerIds order by p.name, p.id")
    List<PropertyEntity> findByProducerIds(@Param("producerIds") Collection<Long> producerIds);

    /** Vínculo atômico: só atualiza propriedades livres ou já do produtor (design, decisão 6). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PropertyEntity p set p.producerId = :producerId, p.updatedAt = current_timestamp "
            + "where p.id in :ids and (p.producerId is null or p.producerId = :producerId)")
    int link(@Param("producerId") long producerId, @Param("ids") Collection<Long> ids);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PropertyEntity p set p.producerId = null, p.updatedAt = current_timestamp "
            + "where p.producerId = :producerId and p.id not in :keep")
    int unlinkExcept(@Param("producerId") long producerId, @Param("keep") Collection<Long> keep);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PropertyEntity p set p.producerId = null, p.updatedAt = current_timestamp "
            + "where p.producerId = :producerId")
    int unlinkAll(@Param("producerId") long producerId);

    @Query("select p.id from PropertyEntity p where p.taxId = :taxId")
    Optional<Long> findIdByTaxId(@Param("taxId") String taxId);
}
