package br.com.gabrielriguiti.agro.infrastructure.persistence.integration;

import br.com.gabrielriguiti.agro.application.integration.ExternalReferencePort;
import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JDBC de {@link ExternalReferencePort} sobre a tabela {@code external_reference}. Participa da
 * transação da mensagem, então o vínculo é gravado junto com o registro.
 */
@Component
@Transactional
class JdbcExternalReferenceAdapter implements ExternalReferencePort {

    private final JdbcTemplate jdbc;

    JdbcExternalReferenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Long> find(MessageType type, String source, String externalId) {
        // A tabela vem de um enum fixo; o EXISTS ignora vínculos órfãos de registros já excluídos.
        String sql = "SELECT r.entity_id FROM external_reference r WHERE r.entity_type = ? AND r.source = ? "
                + "AND r.external_id = ? AND EXISTS (SELECT 1 FROM " + table(type) + " t WHERE t.id = r.entity_id)";
        return jdbc.queryForList(sql, Long.class, type.name(), source, externalId).stream().findFirst();
    }

    @Override
    public void link(MessageType type, long entityId, String source, String externalId) {
        jdbc.update("""
                INSERT INTO external_reference (entity_type, entity_id, source, external_id)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (entity_type, source, external_id) DO UPDATE SET entity_id = EXCLUDED.entity_id
                """, type.name(), entityId, source, externalId);
    }

    private static String table(MessageType type) {
        return switch (type) {
            case PRODUCER -> "producers";
            case PROPERTY -> "properties";
            case LABORATORY -> "laboratories";
        };
    }
}
