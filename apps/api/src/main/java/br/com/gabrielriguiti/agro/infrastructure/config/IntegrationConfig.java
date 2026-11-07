package br.com.gabrielriguiti.agro.infrastructure.config;

import br.com.gabrielriguiti.agro.application.integration.IntegrationSettings;
import br.com.gabrielriguiti.agro.application.integration.MessageProcessor;
import java.time.Clock;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Liga a integração ao Spring: parâmetros de {@code agro.integration.*}, relógio, fonte de aleatoriedade do jitter e
 * o agendador do worker (desligável por {@code agro.integration.scheduler-enabled=false}).
 */
@Configuration
@EnableConfigurationProperties(IntegrationProperties.class)
public class IntegrationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationConfig.class);

    @Bean
    IntegrationSettings integrationSettings(IntegrationProperties properties) {
        return properties.toSettings();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RandomGenerator randomGenerator() {
        return RandomGenerator.getDefault();
    }

    /** Agenda o ciclo do worker em intervalo fixo; uma falha no ciclo não interrompe os seguintes. */
    @Configuration
    @ConditionalOnProperty(name = "agro.integration.scheduler-enabled", havingValue = "true", matchIfMissing = true)
    static class WorkerSchedule {

        @Bean(destroyMethod = "shutdown")
        TaskScheduler integrationWorkerScheduler(MessageProcessor processor, IntegrationProperties properties) {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.setThreadNamePrefix("integration-worker-");
            scheduler.initialize();
            scheduler.scheduleWithFixedDelay(() -> {
                try {
                    int handled = processor.runCycle();
                    if (handled > 0) {
                        LOG.info("Ciclo do worker tratou {} mensagem(ns)", handled);
                    }
                } catch (RuntimeException e) {
                    LOG.error("Falha no ciclo do worker", e);
                }
            }, properties.pollIntervalOrDefault());
            return scheduler;
        }
    }
}
