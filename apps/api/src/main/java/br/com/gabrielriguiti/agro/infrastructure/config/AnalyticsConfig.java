package br.com.gabrielriguiti.agro.infrastructure.config;

import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsPort;
import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsService;
import br.com.gabrielriguiti.agro.application.financial.FinancialEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Liga o caso de uso da consulta analítica (puro) à porta JDBC e ao motor financeiro. */
@Configuration
public class AnalyticsConfig {

    @Bean
    LabAnalyticsService labAnalyticsService(LabAnalyticsPort port, FinancialEngine engine) {
        return new LabAnalyticsService(port, engine);
    }
}
