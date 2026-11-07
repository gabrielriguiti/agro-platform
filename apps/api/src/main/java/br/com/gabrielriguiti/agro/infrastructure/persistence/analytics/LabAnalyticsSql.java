package br.com.gabrielriguiti.agro.infrastructure.persistence.analytics;

import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsFilter;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Montagem do SQL da consulta analítica. Só a estrutura é dinâmica (quais condições e qual {@code ORDER BY});
 * todo valor do usuário segue como parâmetro vinculado, nunca concatenado. Fica concentrada neste único ponto.
 *
 * <p><b>Plano de execução</b> (PostgreSQL 16, 50 laboratórios e 200 mil produtores, após {@code ANALYZE}):
 * <ul>
 *   <li>com filtro de {@code data_inicial} (um mês, ~1,6% das linhas): {@code Bitmap Index Scan} em
 *       {@code ix_producers_data_inicial}, o filtro de texto ({@code ILIKE}) é aplicado só às linhas já filtradas
 *       pela data, depois {@code Merge Join} com laboratórios e {@code GroupAggregate};</li>
 *   <li>sem filtros: {@code Seq Scan} paralelo em {@code producers} com {@code Hash Join} e
 *       {@code HashAggregate}: ler tudo é o plano certo quando todas as linhas contam;</li>
 *   <li>a ordenação ({@code Sort}) opera sobre o resultado agregado (no máximo um item por laboratório).</li>
 * </ul>
 * O {@code ILIKE '%x%'} sozinho não usa índice b-tree; {@code pg_trgm} é a evolução se o volume exigir (ADR-0016).
 * O comando e a saída completa estão no README.
 */
final class LabAnalyticsSql {

    private LabAnalyticsSql() {
    }

    /** SQL e parâmetros prontos para o {@code NamedParameterJdbcTemplate}. */
    record Query(String sql, SqlParameterSource params) {
    }

    /** @return a consulta única, com {@code WHERE} só das condições informadas, {@code HAVING} e ordenação */
    static Query build(LabAnalyticsFilter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource("min", filter.minProducers());
        List<String> conditions = new ArrayList<>();
        addRange(conditions, params, "p.data_inicial", "dataInicialFrom", filter.dataInicialFrom(),
                "dataInicialTo", filter.dataInicialTo());
        addRange(conditions, params, "p.data_final", "dataFinalFrom", filter.dataFinalFrom(),
                "dataFinalTo", filter.dataFinalTo());
        if (filter.text() != null) {
            conditions.add("p.observacoes ILIKE :text ESCAPE '\\'");
            params.addValue("text", "%" + escapeLike(filter.text()) + "%");
        }

        StringBuilder sql = new StringBuilder("""
                SELECT l.id, l.name, COUNT(p.id) AS total_producers
                FROM laboratories l
                JOIN producers p ON p.laboratory_id = l.id
                """);
        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append('\n');
        }
        sql.append("GROUP BY l.id, l.name\nHAVING COUNT(p.id) >= :min\nORDER BY COUNT(p.id) DESC");
        if (filter.hasDataInicialFilter()) {
            sql.append(", MIN(p.data_inicial) ASC");
        }
        sql.append(", l.id ASC");
        return new Query(sql.toString(), params);
    }

    /** Escapa {@code \}, {@code %} e {@code _} para que a busca seja literal em {@code LIKE}. */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static void addRange(List<String> conditions, MapSqlParameterSource params, String column,
            String fromName, Instant from, String toName, Instant to) {
        if (from != null) {
            conditions.add(column + " >= :" + fromName);
            params.addValue(fromName, Timestamp.from(from));
        }
        if (to != null) {
            conditions.add(column + " <= :" + toName);
            params.addValue(toName, Timestamp.from(to));
        }
    }
}
