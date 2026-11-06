package br.com.gabrielriguiti.agro.domain.registry;

/**
 * Propriedade rural; existe sem produtor e pertence a no máximo um (ADR-0008).
 *
 * @param id identificador gerado pelo banco; {@code null} antes de persistir
 * @param identity nome, inscrição fiscal e endereço
 * @param producerId produtor dono, ou {@code null} quando disponível
 */
public record Property(Long id, Identity identity, Long producerId) {

    /**
     * @throws DomainValidationException se a identidade faltar
     */
    public Property {
        if (identity == null) {
            throw new DomainValidationException("nome", "identidade é obrigatória");
        }
    }

    /** @return propriedade nova, sem id e sem produtor */
    public static Property create(Identity identity) {
        return new Property(null, identity, null);
    }

    /** @return cópia com a identidade substituída (atualização), preservando o vínculo */
    public Property withIdentity(Identity newIdentity) {
        return new Property(id, newIdentity, producerId);
    }

    /** @return {@code true} se não pertence a nenhum produtor */
    public boolean isAvailable() {
        return producerId == null;
    }
}
