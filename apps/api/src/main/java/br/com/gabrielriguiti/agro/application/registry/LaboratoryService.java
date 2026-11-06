package br.com.gabrielriguiti.agro.application.registry;

import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de laboratório: criar, consultar, listar, atualizar e excluir. */
@Service
@Transactional
public class LaboratoryService {

    private final LaboratoryRepository laboratories;

    public LaboratoryService(LaboratoryRepository laboratories) {
        this.laboratories = laboratories;
    }

    /** @return o laboratório criado, já com id */
    public Laboratory create(Identity identity) {
        return laboratories.save(Laboratory.create(identity));
    }

    /**
     * @throws ResourceNotFoundException se não existir
     */
    @Transactional(readOnly = true)
    public Laboratory get(long id) {
        return laboratories.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Laboratório", id));
    }

    /** @return todos os laboratórios, ordenados por nome */
    @Transactional(readOnly = true)
    public List<Laboratory> list() {
        return laboratories.findAllByName();
    }

    /**
     * @throws ResourceNotFoundException se não existir
     * @throws ConflictException se a inscrição fiscal pertencer a outro laboratório
     */
    public Laboratory update(long id, Identity identity) {
        return laboratories.save(get(id).withIdentity(identity));
    }

    /**
     * @throws ResourceNotFoundException se não existir
     * @throws ConflictException se houver produtores vinculados
     */
    public void delete(long id) {
        get(id);
        if (laboratories.hasProducers(id)) {
            throw new ConflictException("Laboratório possui produtores vinculados e não pode ser excluído");
        }
        laboratories.deleteById(id);
    }
}
