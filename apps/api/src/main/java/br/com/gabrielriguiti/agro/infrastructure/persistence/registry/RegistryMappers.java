package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import br.com.gabrielriguiti.agro.domain.registry.Producer;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import br.com.gabrielriguiti.agro.domain.registry.TaxId;
import java.util.Set;

/** Tradução entre as entidades JPA e os tipos do domínio de cadastro. */
final class RegistryMappers {

    private RegistryMappers() {
    }

    static Laboratory toDomain(LaboratoryEntity e) {
        return new Laboratory(e.id, new Identity(e.name, new TaxId(e.taxId), e.address));
    }

    static LaboratoryEntity toEntity(Laboratory l, LaboratoryEntity target) {
        target.id = l.id();
        target.name = l.identity().name();
        target.taxId = l.identity().taxId().digits();
        target.address = l.identity().address();
        return target;
    }

    static Property toDomain(PropertyEntity e) {
        return new Property(e.id, new Identity(e.name, new TaxId(e.taxId), e.address), e.producerId);
    }

    /** Copia os dados da propriedade; o vínculo ({@code producerId}) é preservado do alvo existente. */
    static PropertyEntity toEntity(Property p, PropertyEntity target) {
        target.id = p.id();
        target.name = p.identity().name();
        target.taxId = p.identity().taxId().digits();
        target.address = p.identity().address();
        if (p.id() == null) {
            target.producerId = p.producerId();
        }
        return target;
    }

    static Producer toDomain(ProducerEntity e, Set<Long> propertyIds) {
        return new Producer(e.id, new Identity(e.name, new TaxId(e.taxId), e.address), e.dataInicial, e.dataFinal,
                e.observacoes, e.laboratoryId, propertyIds);
    }

    static ProducerEntity toEntity(Producer p, ProducerEntity target) {
        target.id = p.id();
        target.name = p.identity().name();
        target.taxId = p.identity().taxId().digits();
        target.address = p.identity().address();
        target.dataInicial = p.dataInicial();
        target.dataFinal = p.dataFinal();
        target.observacoes = p.observacoes();
        target.laboratoryId = p.laboratoryId();
        return target;
    }
}
