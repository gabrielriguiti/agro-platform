package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProducerJpaRepository extends JpaRepository<ProducerEntity, Long> {

    boolean existsByLaboratoryId(Long laboratoryId);

    long countByLaboratoryId(Long laboratoryId);
}
