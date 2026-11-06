package br.com.gabrielriguiti.agro.application.registry;

import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de propriedade: criar, consultar, listar (com filtro de disponíveis), atualizar e excluir. */
@Service
@Transactional
public class PropertyService {

    private final PropertyRepository properties;

    public PropertyService(PropertyRepository properties) {
        this.properties = properties;
    }

    /** @return a propriedade criada, sem produtor */
    public Property create(Identity identity) {
        return properties.save(Property.create(identity));
    }

    /**
     * @throws ResourceNotFoundException se não existir
     */
    @Transactional(readOnly = true)
    public Property get(long id) {
        return properties.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Propriedade", id));
    }

    /**
     * @param onlyAvailable {@code true} para devolver só as sem produtor
     * @param producerId produtor em edição: suas propriedades também são devolvidas quando {@code onlyAvailable}
     * @return propriedades ordenadas por nome
     */
    @Transactional(readOnly = true)
    public List<Property> list(boolean onlyAvailable, Long producerId) {
        return onlyAvailable ? properties.findAvailable(producerId) : properties.findAllByName();
    }

    /**
     * @throws ResourceNotFoundException se não existir
     * @throws ConflictException se a inscrição fiscal pertencer a outra propriedade
     */
    public Property update(long id, Identity identity) {
        return properties.save(get(id).withIdentity(identity));
    }

    /**
     * @throws ResourceNotFoundException se não existir
     */
    public void delete(long id) {
        get(id);
        properties.deleteById(id);
    }
}
