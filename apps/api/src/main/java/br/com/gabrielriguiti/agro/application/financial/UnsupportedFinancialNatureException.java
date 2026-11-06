package br.com.gabrielriguiti.agro.application.financial;

/** Nenhuma calculadora registrada trata a natureza pedida. */
public class UnsupportedFinancialNatureException extends RuntimeException {

    /** @param nature natureza sem calculadora */
    public UnsupportedFinancialNatureException(FinancialNature nature) {
        super("Não há calculadora financeira registrada para a natureza " + nature);
    }
}
