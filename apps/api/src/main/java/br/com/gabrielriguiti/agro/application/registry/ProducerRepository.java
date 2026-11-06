package br.com.gabrielriguiti.agro.application.registry;

import br.com.gabrielriguiti.agro.domain.registry.Producer;
import java.util.Optional;

/** Porta de saída: persistência e leitura de produtores. */
public interface ProducerRepository {

    /**
     * Insere ou atualiza as colunas do produtor; o vínculo das propriedades é feito por {@link PropertyRepository}.
     *
     * @throws ConflictException se a inscrição fiscal já existir
     */
    Producer save(Producer producer);

    /** @return {@code true} se o produtor existir */
    boolean existsById(long id);

    /** Remove o produtor; suas propriedades ficam livres. */
    void deleteById(long id);

    /** @return o produtor com laboratório e propriedades resolvidos, se existir */
    Optional<ProducerDetails> findDetailsById(long id);

    /**
     * Lista uma página de produtores, ordenados por nome, com laboratório e propriedades, em número constante de
     * consultas (no máximo 3).
     *
     * @param page número da página, a partir de 0
     * @param size itens por página
     */
    PageResult<ProducerDetails> findPage(int page, int size);
}
