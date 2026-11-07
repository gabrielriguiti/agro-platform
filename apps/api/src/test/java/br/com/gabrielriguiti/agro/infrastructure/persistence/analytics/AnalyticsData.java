package br.com.gabrielriguiti.agro.infrastructure.persistence.analytics;

import br.com.gabrielriguiti.agro.TaxIds;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;

/** Massa de dados dos testes analíticos: insere laboratórios e produtores direto no banco. */
public final class AnalyticsData {

    private AnalyticsData() {
    }

    /** @return id do laboratório criado */
    public static long lab(JdbcTemplate jdbc, String name) {
        return jdbc.queryForObject("INSERT INTO laboratories (name, tax_id, address) VALUES (?, ?, 'Rua Lab') "
                + "RETURNING id", Long.class, name, TaxIds.cnpj());
    }

    /** @return id do produtor criado no laboratório */
    public static long producer(JdbcTemplate jdbc, long labId, String dataInicial, String dataFinal, String obs) {
        return jdbc.queryForObject("INSERT INTO producers (name, tax_id, address, data_inicial, data_final, "
                + "observacoes, laboratory_id) VALUES ('Produtor', ?, 'Rua P', ?, ?, ?, ?) RETURNING id",
                Long.class, TaxIds.cpf(), Timestamp.from(Instant.parse(dataInicial)),
                Timestamp.from(Instant.parse(dataFinal)), obs, labId);
    }
}
