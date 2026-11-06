package br.com.gabrielriguiti.agro.domain.registry;

/**
 * Violação de uma invariante do domínio de cadastro.
 *
 * <p>Carrega o nome do campo (como aparece no contrato da API) para que a borda REST possa apontá-lo no erro.
 */
public class DomainValidationException extends RuntimeException {

    private final String field;

    /**
     * @param field nome do campo inválido, como no payload da API (ex.: {@code endereco})
     * @param message descrição do problema, em português
     */
    public DomainValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** @return nome do campo inválido, como no payload da API */
    public String field() {
        return field;
    }
}
