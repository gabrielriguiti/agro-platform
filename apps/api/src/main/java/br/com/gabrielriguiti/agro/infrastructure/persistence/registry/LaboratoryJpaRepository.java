package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LaboratoryJpaRepository extends JpaRepository<LaboratoryEntity, Long> {

    @Query("select l from LaboratoryEntity l order by l.name, l.id")
    List<LaboratoryEntity> findAllOrdered();
}
