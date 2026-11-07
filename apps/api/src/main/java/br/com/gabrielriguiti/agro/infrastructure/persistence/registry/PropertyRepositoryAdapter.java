package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import br.com.gabrielriguiti.agro.application.registry.PropertyRepository;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA da porta {@link PropertyRepository}. */
@Component
@Transactional
class PropertyRepositoryAdapter implements PropertyRepository {

    private final PropertyJpaRepository jpa;

    PropertyRepositoryAdapter(PropertyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Property save(Property property) {
        PropertyEntity target = property.id() == null
                ? new PropertyEntity()
                : jpa.findById(property.id()).orElseGet(PropertyEntity::new);
        try {
            return RegistryMappers.toDomain(jpa.saveAndFlush(RegistryMappers.toEntity(property, target)));
        } catch (DataIntegrityViolationException e) {
            throw PersistenceErrors.toConflict(e);
        }
    }

    @Override
    public Optional<Property> findById(long id) {
        return jpa.findById(id).map(RegistryMappers::toDomain);
    }

    @Override
    public List<Property> findAllByName() {
        return jpa.findAllOrdered().stream().map(RegistryMappers::toDomain).toList();
    }

    @Override
    public List<Property> findAvailable(Long producerId) {
        return jpa.findAvailable(producerId).stream().map(RegistryMappers::toDomain).toList();
    }

    @Override
    public List<Property> findAllById(Collection<Long> ids) {
        return jpa.findAllById(ids).stream().map(RegistryMappers::toDomain).toList();
    }

    @Override
    public int link(long producerId, Collection<Long> ids) {
        return ids.isEmpty() ? 0 : jpa.link(producerId, ids);
    }

    @Override
    public void unlinkExcept(long producerId, Collection<Long> keep) {
        if (keep.isEmpty()) {
            jpa.unlinkAll(producerId);
        } else {
            jpa.unlinkExcept(producerId, keep);
        }
    }

    @Override
    public void deleteById(long id) {
        jpa.deleteById(id);
    }

    @Override
    public Optional<Long> findIdByTaxId(String digits) {
        return jpa.findIdByTaxId(digits);
    }
}
