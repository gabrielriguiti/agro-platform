package br.com.gabrielriguiti.agro.application.analytics;

import br.com.gabrielriguiti.agro.domain.registry.DomainValidationException;
import java.time.Instant;

/**
 * Filtros da consulta analítica. Os filtros incidem sobre os produtores; os intervalos são inclusivos e opcionais.
 *
 * @param minProducers quantidade mínima de produtores que atendem aos filtros (maior ou igual a 1)
 * @param dataInicialFrom limite inferior de {@code dataInicial} do produtor; nulo para não filtrar
 * @param dataInicialTo limite superior de {@code dataInicial} do produtor; nulo para não filtrar
 * @param dataFinalFrom limite inferior de {@code dataFinal} do produtor; nulo para não filtrar
 * @param dataFinalTo limite superior de {@code dataFinal} do produtor; nulo para não filtrar
 * @param text trecho (literal) buscado nas observações do produtor; nulo ou em branco para não filtrar
 */
public record LabAnalyticsFilter(int minProducers, Instant dataInicialFrom, Instant dataInicialTo,
        Instant dataFinalFrom, Instant dataFinalTo, String text) {

    /**
     * @throws DomainValidationException se a quantidade mínima for menor que 1 ou algum intervalo estiver invertido
     */
    public LabAnalyticsFilter {
        if (minProducers < 1) {
            throw new DomainValidationException("qtdMinimaProdutores", "deve ser maior ou igual a 1");
        }
        if (dataInicialFrom != null && dataInicialTo != null && dataInicialFrom.isAfter(dataInicialTo)) {
            throw new DomainValidationException("dataInicialInicio", "não pode ser posterior a dataInicialFim");
        }
        if (dataFinalFrom != null && dataFinalTo != null && dataFinalFrom.isAfter(dataFinalTo)) {
            throw new DomainValidationException("dataFinalInicio", "não pode ser posterior a dataFinalFim");
        }
        text = text == null || text.isBlank() ? null : text;
    }

    /** @return verdadeiro quando algum filtro de {@code dataInicial} foi informado (ativa o segundo critério de ordem) */
    public boolean hasDataInicialFilter() {
        return dataInicialFrom != null || dataInicialTo != null;
    }
}
