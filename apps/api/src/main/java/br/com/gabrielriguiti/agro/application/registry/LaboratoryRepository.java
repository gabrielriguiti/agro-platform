package br.com.gabrielriguiti.agro.application.registry;

import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import java.util.List;
import java.util.Optional;

/** Porta de saída: persistência de laboratórios. */
public interface LaboratoryRepository {

    /**
     * Insere ou atualiza.
     *
     * @throws ConflictException se a inscrição fiscal já existir
     */
    Laboratory save(Laboratory laboratory);

    /** @return o laboratório, se existir */
    Optional<Laboratory> findById(long id);

    /** @return todos os laboratórios, ordenados por nome */
    List<Laboratory> findAllByName();

    /** @return {@code true} se existir ao menos um produtor vinculado ao laboratório */
    boolean hasProducers(long id);

    /** @return quantidade de produtores vinculados ao laboratório */
    long countProducers(long id);

    /**
     * Remove o laboratório.
     *
     * @throws ConflictException se algum produtor ainda o referenciar
     */
    void deleteById(long id);

    /** @return o id do laboratório com esta inscrição fiscal (14 dígitos), se existir */
    Optional<Long> findIdByTaxId(String digits);
}
