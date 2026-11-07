package br.com.gabrielriguiti.agro.infrastructure.config;

import br.com.gabrielriguiti.agro.application.integration.IntegrationSettings;
import br.com.gabrielriguiti.agro.domain.integration.BackoffPolicy;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da integração ({@code agro.integration.*}).
 *
 * @param maxBatchSize itens por lote (padrão 1000)
 * @param maxAttempts tentativas antes da DLQ (padrão 5)
 * @param workerBatchSize mensagens por ciclo do worker (padrão 20)
 * @param lease duração da reserva (padrão 2 minutos)
 * @param pollInterval intervalo do agendador (padrão 5 segundos)
 * @param schedulerEnabled liga o agendador do worker (padrão {@code true})
 * @param manualRunEnabled habilita {@code POST /api/integration/worker/run} (padrão {@code false})
 * @param backoffBase atraso após a primeira falha (padrão 5 s)
 * @param backoffMax teto do atraso (padrão 10 min)
 * @param backoffJitter variação adicional máxima (padrão 0,1)
 */
@ConfigurationProperties("agro.integration")
public record IntegrationProperties(
        Integer maxBatchSize,
        Integer maxAttempts,
        Integer workerBatchSize,
        Duration lease,
        Duration pollInterval,
        Boolean schedulerEnabled,
        Boolean manualRunEnabled,
        Duration backoffBase,
        Duration backoffMax,
        Double backoffJitter) {

    /** @return os parâmetros da aplicação, com os padrões onde não houver configuração */
    IntegrationSettings toSettings() {
        BackoffPolicy defaults = BackoffPolicy.defaults();
        return new IntegrationSettings(
                maxBatchSize == null ? 1000 : maxBatchSize,
                maxAttempts == null ? 5 : maxAttempts,
                workerBatchSize == null ? 20 : workerBatchSize,
                lease == null ? Duration.ofMinutes(2) : lease,
                new BackoffPolicy(
                        backoffBase == null ? defaults.base() : backoffBase,
                        backoffMax == null ? defaults.max() : backoffMax,
                        backoffJitter == null ? defaults.jitter() : backoffJitter),
                manualRunEnabled != null && manualRunEnabled);
    }

    /** @return intervalo entre ciclos do agendador */
    Duration pollIntervalOrDefault() {
        return pollInterval == null ? Duration.ofSeconds(5) : pollInterval;
    }
}
