package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProducerJpaRepository extends JpaRepository<ProducerEntity, Long> {

    boolean existsByLaboratoryId(Long laboratoryId);

    long countByLaboratoryId(Long laboratoryId);

    @Query("select p.id from ProducerEntity p where p.taxId = :taxId")
    Optional<Long> findIdByTaxId(@Param("taxId") String taxId);
}
