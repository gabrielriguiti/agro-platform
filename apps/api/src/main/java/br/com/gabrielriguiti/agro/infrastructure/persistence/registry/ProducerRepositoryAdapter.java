package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import br.com.gabrielriguiti.agro.application.registry.PageResult;
import br.com.gabrielriguiti.agro.application.registry.ProducerDetails;
import br.com.gabrielriguiti.agro.application.registry.ProducerRepository;
import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import br.com.gabrielriguiti.agro.domain.registry.Producer;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA da porta {@link ProducerRepository}.
 *
 * <p>A listagem executa no máximo 3 consultas, independente do tamanho da página: contagem, página já unida ao
 * laboratório e propriedades da página por {@code IN (...)}; a montagem final é em memória.
 */
@Component
@Transactional
class ProducerRepositoryAdapter implements ProducerRepository {

    private final ProducerJpaRepository jpa;
    private final PropertyJpaRepository properties;
    private final EntityManager em;

    ProducerRepositoryAdapter(ProducerJpaRepository jpa, PropertyJpaRepository properties, EntityManager em) {
        this.jpa = jpa;
        this.properties = properties;
        this.em = em;
    }

    @Override
    public Producer save(Producer producer) {
        ProducerEntity target = producer.id() == null
                ? new ProducerEntity()
                : jpa.findById(producer.id()).orElseGet(ProducerEntity::new);
        try {
            ProducerEntity saved = jpa.saveAndFlush(RegistryMappers.toEntity(producer, target));
            return RegistryMappers.toDomain(saved, producer.propertyIds());
        } catch (DataIntegrityViolationException e) {
            throw PersistenceErrors.toConflict(e);
        }
    }

    @Override
    public boolean existsById(long id) {
        return jpa.existsById(id);
    }

    @Override
    public void deleteById(long id) {
        // ON DELETE SET NULL libera as propriedades no banco; o contexto é limpo para não manter linhas obsoletas.
        jpa.deleteById(id);
        jpa.flush();
        em.clear();
    }

    @Override
    public Optional<ProducerDetails> findDetailsById(long id) {
        return jpa.findById(id).map(producer -> {
            LaboratoryEntity lab = em.find(LaboratoryEntity.class, producer.laboratoryId);
            return assemble(List.of(new ProducerAndLab(producer, lab)), loadProperties(List.of(id))).get(0);
        });
    }

    @Override
    public PageResult<ProducerDetails> findPage(int page, int size) {
        long total = em.createQuery("select count(p) from ProducerEntity p", Long.class).getSingleResult();
        List<ProducerAndLab> rows = em.createQuery(
                        "select p, l from ProducerEntity p join LaboratoryEntity l on l.id = p.laboratoryId "
                                + "order by p.name, p.id", Object[].class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList().stream()
                .map(r -> new ProducerAndLab((ProducerEntity) r[0], (LaboratoryEntity) r[1]))
                .toList();
        if (rows.isEmpty()) {
            return new PageResult<>(List.of(), page, size, total);
        }
        List<Long> ids = rows.stream().map(r -> r.producer().id).toList();
        return new PageResult<>(assemble(rows, loadProperties(ids)), page, size, total);
    }

    private Map<Long, List<Property>> loadProperties(Collection<Long> producerIds) {
        return properties.findByProducerIds(producerIds).stream()
                .map(RegistryMappers::toDomain)
                .collect(Collectors.groupingBy(Property::producerId, LinkedHashMap::new, Collectors.toList()));
    }

    private static List<ProducerDetails> assemble(List<ProducerAndLab> rows, Map<Long, List<Property>> byProducer) {
        return rows.stream().map(row -> {
            List<Property> owned = byProducer.getOrDefault(row.producer().id, List.of());
            Set<Long> propertyIds = owned.stream().map(Property::id).collect(Collectors.toCollection(LinkedHashSet::new));
            Laboratory laboratory = RegistryMappers.toDomain(row.laboratory());
            return new ProducerDetails(RegistryMappers.toDomain(row.producer(), propertyIds), laboratory, owned);
        }).toList();
    }

    private record ProducerAndLab(ProducerEntity producer, LaboratoryEntity laboratory) {
    }
}
