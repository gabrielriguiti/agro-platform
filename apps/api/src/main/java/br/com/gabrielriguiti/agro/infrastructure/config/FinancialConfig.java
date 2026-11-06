package br.com.gabrielriguiti.agro.infrastructure.config;

import br.com.gabrielriguiti.agro.application.financial.FinancialCalculator;
import br.com.gabrielriguiti.agro.application.financial.FinancialEngine;
import br.com.gabrielriguiti.agro.application.financial.FinancialQueryService;
import br.com.gabrielriguiti.agro.application.financial.FinancialRates;
import br.com.gabrielriguiti.agro.application.financial.LaboratoryFeeCalculator;
import br.com.gabrielriguiti.agro.application.financial.ProducerCommissionCalculator;
import br.com.gabrielriguiti.agro.application.financial.PropertyFeeCalculator;
import br.com.gabrielriguiti.agro.application.registry.LaboratoryRepository;
import br.com.gabrielriguiti.agro.application.registry.ProducerRepository;
import br.com.gabrielriguiti.agro.application.registry.PropertyRepository;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga o motor financeiro puro ao Spring: taxas vindas de {@code agro.financial.*}, uma calculadora por natureza
 * e o motor, que recebe todos os {@link FinancialCalculator} registrados. Uma nova calculadora declarada como bean
 * entra no motor sem alterá-lo.
 */
@Configuration
@EnableConfigurationProperties(FinancialProperties.class)
public class FinancialConfig {

    @Bean
    FinancialRates financialRates(FinancialProperties properties) {
        return properties.toRates();
    }

    @Bean
    FinancialCalculator producerCommissionCalculator(FinancialRates rates) {
        return new ProducerCommissionCalculator(rates);
    }

    @Bean
    FinancialCalculator propertyFeeCalculator(FinancialRates rates) {
        return new PropertyFeeCalculator(rates);
    }

    @Bean
    FinancialCalculator laboratoryFeeCalculator(FinancialRates rates) {
        return new LaboratoryFeeCalculator(rates);
    }

    @Bean
    FinancialEngine financialEngine(List<FinancialCalculator> calculators) {
        return new FinancialEngine(calculators);
    }

    @Bean
    FinancialQueryService financialQueryService(FinancialEngine engine, ProducerRepository producers,
            PropertyRepository properties, LaboratoryRepository laboratories) {
        return new FinancialQueryService(engine, producers, properties, laboratories);
    }
}
