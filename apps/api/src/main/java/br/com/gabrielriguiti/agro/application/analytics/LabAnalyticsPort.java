package br.com.gabrielriguiti.agro.application.analytics;

import java.util.List;

/** Porta de leitura da consulta analítica: agrega, filtra e ordena no banco, em uma única consulta. */
public interface LabAnalyticsPort {

    /**
     * @param filter filtros e quantidade mínima
     * @return laboratórios com ao menos {@code minProducers} produtores que atendem aos filtros, já na ordem final:
     *         total decrescente, menor {@code dataInicial} crescente (só com filtro de data inicial) e id crescente
     */
    List<LabAnalyticsRow> find(LabAnalyticsFilter filter);
}
