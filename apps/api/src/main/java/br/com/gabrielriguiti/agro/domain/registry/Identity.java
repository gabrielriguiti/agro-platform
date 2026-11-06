package br.com.gabrielriguiti.agro.domain.registry;

/**
 * Atributos globais obrigatórios de toda entidade do cadastro: nome, inscrição fiscal e endereço.
 *
 * @param name nome (até 200 caracteres)
 * @param taxId inscrição fiscal válida
 * @param address endereço (até 500 caracteres)
 */
public record Identity(String name, TaxId taxId, String address) {

    private static final int NAME_MAX = 200;
    private static final int ADDRESS_MAX = 500;

    /**
     * Valida os três atributos.
     *
     * @throws DomainValidationException se nome, inscrição ou endereço faltarem ou excederem o limite
     */
    public Identity {
        name = required(name, "nome", NAME_MAX);
        address = required(address, "endereco", ADDRESS_MAX);
        if (taxId == null) {
            throw new InvalidTaxIdException();
        }
    }

    private static String required(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field, field + " é obrigatório");
        }
        String trimmed = value.strip();
        if (trimmed.length() > max) {
            throw new DomainValidationException(field, field + " deve ter no máximo " + max + " caracteres");
        }
        return trimmed;
    }
}
