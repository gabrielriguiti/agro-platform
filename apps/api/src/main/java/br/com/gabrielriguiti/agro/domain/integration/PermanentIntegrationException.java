package br.com.gabrielriguiti.agro.domain.integration;

/**
 * Falha sem conserto por nova tentativa (payload malformado, campo ausente, inscrição inválida, conflito de
 * identidade): a mensagem vai direto para {@link MessageStatus#DEAD} (ADR-0014).
 */
public class PermanentIntegrationException extends RuntimeException {

    /** @param message motivo, em português, registrado como último erro */
    public PermanentIntegrationException(String message) {
        super(message);
    }

    /**
     * @param message motivo, em português
     * @param cause causa original
     */
    public PermanentIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
