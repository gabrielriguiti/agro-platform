package br.com.gabrielriguiti.agro.domain.registry;

/** Inscrição fiscal (CPF ou CNPJ) com formato ou dígitos verificadores inválidos. */
public class InvalidTaxIdException extends DomainValidationException {

    /** Nome do campo da inscrição fiscal no contrato da API. */
    public static final String FIELD = "inscricaoFiscal";

    public InvalidTaxIdException() {
        super(FIELD, "CPF/CNPJ inválido");
    }
}
