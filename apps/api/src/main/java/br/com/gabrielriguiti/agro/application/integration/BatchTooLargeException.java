package br.com.gabrielriguiti.agro.application.integration;

/** Lote acima do limite configurado; a API responde 413. */
public class BatchTooLargeException extends RuntimeException {

    /** @param message descrição do limite, em português */
    public BatchTooLargeException(String message) {
        super(message);
    }
}
