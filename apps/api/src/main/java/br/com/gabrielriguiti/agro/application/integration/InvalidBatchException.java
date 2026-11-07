package br.com.gabrielriguiti.agro.application.integration;

/** Envelope do lote inválido (item sem dados, por exemplo); a API responde 400 apontando o campo. */
public class InvalidBatchException extends RuntimeException {

    private final String field;

    /**
     * @param field caminho do campo, como no contrato (ex.: {@code items[2].data})
     * @param message descrição do problema, em português
     */
    public InvalidBatchException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** @return caminho do campo inválido */
    public String field() {
        return field;
    }
}
