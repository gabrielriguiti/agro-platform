package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import br.com.gabrielriguiti.agro.application.registry.LaboratoryRepository;
import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA da porta {@link LaboratoryRepository}. */
@Component
@Transactional
class LaboratoryRepositoryAdapter implements LaboratoryRepository {

    private final LaboratoryJpaRepository jpa;
    private final ProducerJpaRepository producers;

    LaboratoryRepositoryAdapter(LaboratoryJpaRepository jpa, ProducerJpaRepository producers) {
        this.jpa = jpa;
        this.producers = producers;
    }

    @Override
    public Laboratory save(Laboratory laboratory) {
        LaboratoryEntity target = laboratory.id() == null
                ? new LaboratoryEntity()
                : jpa.findById(laboratory.id()).orElseGet(LaboratoryEntity::new);
        try {
            return RegistryMappers.toDomain(jpa.saveAndFlush(RegistryMappers.toEntity(laboratory, target)));
        } catch (DataIntegrityViolationException e) {
            throw PersistenceErrors.toConflict(e);
        }
    }

    @Override
    public Optional<Laboratory> findById(long id) {
        return jpa.findById(id).map(RegistryMappers::toDomain);
    }

    @Override
    public List<Laboratory> findAllByName() {
        return jpa.findAllOrdered().stream().map(RegistryMappers::toDomain).toList();
    }

    @Override
    public boolean hasProducers(long id) {
        return producers.existsByLaboratoryId(id);
    }

    @Override
    public long countProducers(long id) {
        return producers.countByLaboratoryId(id);
    }

    @Override
    public void deleteById(long id) {
        try {
            jpa.deleteById(id);
            jpa.flush();
        } catch (DataIntegrityViolationException e) {
            throw PersistenceErrors.toConflict(e);
        }
    }

    @Override
    public Optional<Long> findIdByTaxId(String digits) {
        return jpa.findIdByTaxId(digits);
    }
}
