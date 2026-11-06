package br.com.gabrielriguiti.agro.application.registry;

import br.com.gabrielriguiti.agro.domain.registry.Producer;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de produtor. Cada operação roda em uma transação: qualquer falha (laboratório ou propriedade
 * inexistente, propriedade de outro produtor) desfaz tudo e nada é gravado.
 */
@Service
@Transactional
public class ProducerService {

    private final ProducerRepository producers;
    private final LaboratoryRepository laboratories;
    private final PropertyRepository properties;

    public ProducerService(
            ProducerRepository producers, LaboratoryRepository laboratories, PropertyRepository properties) {
        this.producers = producers;
        this.laboratories = laboratories;
        this.properties = properties;
    }

    /**
     * Cria o produtor e vincula as propriedades informadas.
     *
     * @param producer produtor sem id
     * @return o produtor criado, com laboratório e propriedades
     * @throws ResourceNotFoundException se o laboratório ou alguma propriedade não existir
     * @throws ConflictException se a inscrição fiscal duplicar ou alguma propriedade pertencer a outro produtor
     */
    public ProducerDetails create(Producer producer) {
        Set<Long> propertyIds = producer.propertyIds();
        requireReferences(producer.laboratoryId(), propertyIds);
        Producer saved = producers.save(producer);
        linkAll(saved.id(), propertyIds);
        return details(saved.id());
    }

    /**
     * Atualiza o produtor; o conjunto de propriedades informado substitui o anterior.
     *
     * @param id produtor a atualizar
     * @param producer novos dados (o id é ignorado)
     * @throws ResourceNotFoundException se o produtor, o laboratório ou alguma propriedade não existir
     * @throws ConflictException se a inscrição fiscal duplicar ou alguma propriedade pertencer a outro produtor
     */
    public ProducerDetails update(long id, Producer producer) {
        if (!producers.existsById(id)) {
            throw ResourceNotFoundException.of("Produtor", id);
        }
        Set<Long> propertyIds = producer.propertyIds();
        requireReferences(producer.laboratoryId(), propertyIds);
        producers.save(new Producer(id, producer.identity(), producer.dataInicial(), producer.dataFinal(),
                producer.observacoes(), producer.laboratoryId(), propertyIds));
        properties.unlinkExcept(id, propertyIds);
        linkAll(id, propertyIds);
        return details(id);
    }

    /**
     * @throws ResourceNotFoundException se não existir
     */
    @Transactional(readOnly = true)
    public ProducerDetails get(long id) {
        return details(id);
    }

    /** @return página de produtores (a partir de 0) ordenados por nome, sem consultas N+1 */
    @Transactional(readOnly = true)
    public PageResult<ProducerDetails> list(int page, int size) {
        return producers.findPage(page, size);
    }

    /**
     * Exclui o produtor; as propriedades continuam cadastradas e livres.
     *
     * @throws ResourceNotFoundException se não existir
     */
    public void delete(long id) {
        if (!producers.existsById(id)) {
            throw ResourceNotFoundException.of("Produtor", id);
        }
        producers.deleteById(id);
    }

    private ProducerDetails details(long id) {
        return producers.findDetailsById(id).orElseThrow(() -> ResourceNotFoundException.of("Produtor", id));
    }

    private void requireReferences(long laboratoryId, Set<Long> propertyIds) {
        if (laboratories.findById(laboratoryId).isEmpty()) {
            throw ResourceNotFoundException.of("Laboratório", laboratoryId);
        }
        if (propertyIds.isEmpty()) {
            return;
        }
        Set<Long> found = properties.findAllById(propertyIds).stream()
                .map(Property::id)
                .collect(Collectors.toSet());
        List<Long> missing = propertyIds.stream().filter(p -> !found.contains(p)).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Propriedade(s) " + missing + " não encontrada(s)");
        }
    }

    private void linkAll(long producerId, Set<Long> propertyIds) {
        if (!propertyIds.isEmpty() && properties.link(producerId, propertyIds) != propertyIds.size()) {
            throw new ConflictException("Alguma propriedade informada já pertence a outro produtor");
        }
    }
}
