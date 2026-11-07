package br.com.gabrielriguiti.agro.application.integration;

import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import java.util.Optional;

/** Porta de saída: vínculos entre registros do cadastro e identificadores de sistemas parceiros (ADR-0013). */
public interface ExternalReferencePort {

    /** @return o id do registro vinculado a (source, externalId), se o registro ainda existir */
    Optional<Long> find(MessageType type, String source, String externalId);

    /** Vincula (source, externalId) ao registro; se o par já existir, passa a apontar para este registro. */
    void link(MessageType type, long entityId, String source, String externalId);
}
