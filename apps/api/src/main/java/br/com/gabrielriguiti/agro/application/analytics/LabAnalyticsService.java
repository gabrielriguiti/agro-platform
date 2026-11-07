package br.com.gabrielriguiti.agro.application.analytics;

import br.com.gabrielriguiti.agro.application.financial.FinancialEngine;
import br.com.gabrielriguiti.agro.application.financial.FinancialSubject;
import java.util.List;
import java.util.Locale;

/**
 * Caso de uso da consulta analítica: uma chamada à porta e, em memória, o nome em caixa alta e o valor financeiro
 * calculado pelo motor a partir do total do próprio resultado. Sem Spring.
 */
public class LabAnalyticsService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final LabAnalyticsPort port;
    private final FinancialEngine engine;

    /** Recebe a porta de leitura e o motor financeiro. */
    public LabAnalyticsService(LabAnalyticsPort port, FinancialEngine engine) {
        this.port = port;
        this.engine = engine;
    }

    /**
     * @param filter filtros da consulta
     * @return laboratórios na ordem definida pela porta, com nome em caixa alta e valor consolidado
     */
    public List<LabAnalyticsItem> query(LabAnalyticsFilter filter) {
        return port.find(filter).stream()
                .map(row -> new LabAnalyticsItem(row.id(), row.name().toUpperCase(PT_BR), row.totalProducers(),
                        engine.calculate(FinancialSubject.laboratory(row.totalProducers())).value()))
                .toList();
    }
}
