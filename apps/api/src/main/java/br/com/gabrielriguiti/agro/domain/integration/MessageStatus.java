package br.com.gabrielriguiti.agro.domain.integration;

/** Estado de uma mensagem da outbox. {@link #DEAD} é a dead letter queue. */
public enum MessageStatus {
    /** Aguardando (ou reagendada) para processamento. */
    PENDING,
    /** Reservada por um worker, dentro do prazo de lease. */
    PROCESSING,
    /** Processada com sucesso. */
    DONE,
    /** Falha permanente ou tentativas esgotadas (dead letter). */
    DEAD
}
