package br.com.gabrielriguiti.agro.application.financial;

import br.com.gabrielriguiti.agro.application.registry.LaboratoryRepository;
import br.com.gabrielriguiti.agro.application.registry.ProducerDetails;
import br.com.gabrielriguiti.agro.application.registry.ProducerRepository;
import br.com.gabrielriguiti.agro.application.registry.PropertyRepository;
import br.com.gabrielriguiti.agro.application.registry.ResourceNotFoundException;

/**
 * Caso de uso de consulta financeira: resolve os números da entidade nas portas do cadastro e delega ao motor.
 * Sem Spring; o bean é criado na configuração da infraestrutura.
 */
public class FinancialQueryService {

    private final FinancialEngine engine;
    private final ProducerRepository producers;
    private final PropertyRepository properties;
    private final LaboratoryRepository laboratories;

    /** Recebe o motor e as portas de leitura do cadastro. */
    public FinancialQueryService(FinancialEngine engine, ProducerRepository producers, PropertyRepository properties,
            LaboratoryRepository laboratories) {
        this.engine = engine;
        this.producers = producers;
        this.properties = properties;
        this.laboratories = laboratories;
    }

    /**
     * @param id produtor
     * @return comissão de produção do produtor
     * @throws ResourceNotFoundException se o produtor não existir
     */
    public FinancialResult producer(long id) {
        ProducerDetails details = producers.findDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Produtor", id));
        return engine.calculate(FinancialSubject.producer(details.producer().dataInicial(),
                details.producer().dataFinal(), details.properties().size()));
    }

    /**
     * @param id propriedade
     * @return taxas da propriedade
     * @throws ResourceNotFoundException se a propriedade não existir
     */
    public FinancialResult property(long id) {
        properties.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Propriedade", id));
        return engine.calculate(FinancialSubject.property());
    }

    /**
     * @param id laboratório
     * @return taxas do laboratório
     * @throws ResourceNotFoundException se o laboratório não existir
     */
    public FinancialResult laboratory(long id) {
        laboratories.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Laboratório", id));
        return engine.calculate(FinancialSubject.laboratory(Math.toIntExact(laboratories.countProducers(id))));
    }
}
