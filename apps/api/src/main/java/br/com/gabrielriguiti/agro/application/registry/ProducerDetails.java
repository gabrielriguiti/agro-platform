package br.com.gabrielriguiti.agro.application.registry;

import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import br.com.gabrielriguiti.agro.domain.registry.Producer;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import java.util.List;

/**
 * Visão de leitura de um produtor com o laboratório e as propriedades já resolvidos (contrato de consulta).
 *
 * @param producer o produtor
 * @param laboratory seu laboratório
 * @param properties suas propriedades, ordenadas por nome
 */
public record ProducerDetails(Producer producer, Laboratory laboratory, List<Property> properties) {
}
