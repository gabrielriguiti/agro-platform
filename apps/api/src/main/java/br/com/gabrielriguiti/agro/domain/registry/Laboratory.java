package br.com.gabrielriguiti.agro.domain.registry;

/**
 * Laboratório de análise; existe de forma autônoma e agrupa produtores.
 *
 * @param id identificador gerado pelo banco; {@code null} antes de persistir
 * @param identity nome, inscrição fiscal (obrigatoriamente CNPJ) e endereço
 */
public record Laboratory(Long id, Identity identity) {

    /**
     * @throws DomainValidationException se a identidade faltar ou a inscrição não for um CNPJ
     */
    public Laboratory {
        if (identity == null) {
            throw new DomainValidationException("nome", "identidade é obrigatória");
        }
        if (!identity.taxId().isCnpj()) {
            throw new DomainValidationException(InvalidTaxIdException.FIELD, "O laboratório exige um CNPJ válido");
        }
    }

    /** @return laboratório novo, ainda sem id */
    public static Laboratory create(Identity identity) {
        return new Laboratory(null, identity);
    }

    /** @return cópia com a identidade substituída (atualização) */
    public Laboratory withIdentity(Identity newIdentity) {
        return new Laboratory(id, newIdentity);
    }
}
