package br.com.gabrielriguiti.agro.application.integration;

import br.com.gabrielriguiti.agro.domain.integration.BackoffPolicy;
import java.time.Duration;

/**
 * Parâmetros da integração, preenchidos de {@code agro.integration.*}.
 *
 * @param maxBatchSize itens por lote aceitos na ingestão (acima disso, 413)
 * @param maxAttempts tentativas antes de a mensagem ir para a DLQ
 * @param workerBatchSize mensagens reservadas por ciclo do worker
 * @param lease duração da reserva de uma mensagem
 * @param backoff política de atraso entre tentativas
 * @param manualRunEnabled se o endpoint de execução manual do worker está habilitado
 */
public record IntegrationSettings(
        int maxBatchSize,
        int maxAttempts,
        int workerBatchSize,
        Duration lease,
        BackoffPolicy backoff,
        boolean manualRunEnabled) {
}
