package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LaboratoryJpaRepository extends JpaRepository<LaboratoryEntity, Long> {

    @Query("select l from LaboratoryEntity l order by l.name, l.id")
    List<LaboratoryEntity> findAllOrdered();

    @Query("select l.id from LaboratoryEntity l where l.taxId = :taxId")
    Optional<Long> findIdByTaxId(@Param("taxId") String taxId);
}
