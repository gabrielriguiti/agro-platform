package br.com.gabrielriguiti.agro.application.financial;

/** Natureza da entidade que define qual regra financeira se aplica. */
public enum FinancialNature {
    /** Produtor: comissão de produção. */
    PRODUCER,
    /** Propriedade: taxas operacional e territorial. */
    PROPERTY,
    /** Laboratório: taxa operacional e taxa territorial por produtor. */
    LABORATORY
}
