package br.com.gabrielriguiti.agro.application.registry;

/** Recurso referenciado não existe; a API responde 404. */
public class ResourceNotFoundException extends RuntimeException {

    /** @param message descrição do recurso ausente, em português */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * @param resource nome do recurso (ex.: {@code Laboratório})
     * @param id identificador procurado
     * @return exceção com mensagem padronizada
     */
    public static ResourceNotFoundException of(String resource, long id) {
        return new ResourceNotFoundException(resource + " " + id + " não encontrado(a)");
    }
}
