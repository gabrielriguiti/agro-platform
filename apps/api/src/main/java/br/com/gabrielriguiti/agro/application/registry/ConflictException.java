package br.com.gabrielriguiti.agro.application.registry;

/** A operação conflita com o estado atual (duplicidade, vínculo alheio, dependentes); a API responde 409. */
public class ConflictException extends RuntimeException {

    /** @param message descrição do conflito, em português */
    public ConflictException(String message) {
        super(message);
    }
}
