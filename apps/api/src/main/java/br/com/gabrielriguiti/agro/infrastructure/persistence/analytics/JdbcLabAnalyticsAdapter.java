package br.com.gabrielriguiti.agro.infrastructure.persistence.analytics;

import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsFilter;
import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsPort;
import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsRow;
import java.util.List;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JDBC de {@link LabAnalyticsPort}: executa uma única instrução com {@code GROUP BY}/{@code HAVING}
 * montada por {@link LabAnalyticsSql}. Somente leitura.
 */
@Component
@Transactional(readOnly = true)
class JdbcLabAnalyticsAdapter implements LabAnalyticsPort {

    private final NamedParameterJdbcTemplate jdbc;

    JdbcLabAnalyticsAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<LabAnalyticsRow> find(LabAnalyticsFilter filter) {
        LabAnalyticsSql.Query query = LabAnalyticsSql.build(filter);
        return jdbc.query(query.sql(), query.params(),
                (rs, i) -> new LabAnalyticsRow(rs.getLong("id"), rs.getString("name"), rs.getInt("total_producers")));
    }
}
