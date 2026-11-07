package br.com.gabrielriguiti.agro.infrastructure.persistence.integration;

import br.com.gabrielriguiti.agro.application.integration.BatchInfo;
import br.com.gabrielriguiti.agro.application.integration.BatchSummary;
import br.com.gabrielriguiti.agro.application.integration.MessageSummary;
import br.com.gabrielriguiti.agro.application.integration.NewMessage;
import br.com.gabrielriguiti.agro.application.integration.OutboxPort;
import br.com.gabrielriguiti.agro.application.registry.PageResult;
import br.com.gabrielriguiti.agro.domain.integration.MergeStrategy;
import br.com.gabrielriguiti.agro.domain.integration.MessageStatus;
import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import br.com.gabrielriguiti.agro.domain.integration.OutboxMessage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JDBC da {@link OutboxPort}: a fila é a tabela {@code outbox_message} (ADR-0011).
 *
 * <p>A reserva usa {@code FOR UPDATE SKIP LOCKED} em um único comando {@code UPDATE ... RETURNING}, de modo que
 * duas instâncias nunca recebem a mesma mensagem (ADR-0012). A ingestão participa da transação do chamador, o que
 * torna o lote atômico.
 */
@Component
@Transactional
class JdbcOutboxAdapter implements OutboxPort {

    private static final String SUMMARY_SELECT = """
            SELECT b.id, b.source, b.merge_strategy, b.created_at,
                   count(m.id) AS total,
                   count(m.id) FILTER (WHERE m.status = 'PENDING') AS pending,
                   count(m.id) FILTER (WHERE m.status = 'PROCESSING') AS processing,
                   count(m.id) FILTER (WHERE m.status = 'DONE') AS done,
                   count(m.id) FILTER (WHERE m.status = 'DEAD') AS dead
            FROM integration_batch b
            LEFT JOIN outbox_message m ON m.batch_id = b.id
            """;

    private static final RowMapper<OutboxMessage> MESSAGE = (rs, i) -> new OutboxMessage(
            rs.getLong("id"), rs.getLong("batch_id"), rs.getString("idempotency_key"),
            MessageType.valueOf(rs.getString("type")), rs.getString("external_id"), rs.getString("payload"),
            MessageStatus.valueOf(rs.getString("status")), rs.getInt("attempts"),
            instant(rs, "next_attempt_at"), instant(rs, "locked_until"), rs.getString("last_error"));

    private static final RowMapper<BatchSummary> SUMMARY = (rs, i) -> new BatchSummary(
            new BatchInfo(rs.getLong("id"), rs.getString("source"),
                    MergeStrategy.valueOf(rs.getString("merge_strategy")), instant(rs, "created_at")),
            rs.getLong("total"), rs.getLong("pending"), rs.getLong("processing"), rs.getLong("done"),
            rs.getLong("dead"));

    private final JdbcTemplate jdbc;

    JdbcOutboxAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long createBatch(String source, MergeStrategy strategy) {
        Long id = jdbc.queryForObject(
                "INSERT INTO integration_batch (source, merge_strategy) VALUES (?, ?) RETURNING id",
                Long.class, source, strategy.name());
        return id;
    }

    @Override
    public boolean enqueue(long batchId, NewMessage message, Instant now) {
        Timestamp at = Timestamp.from(now);
        return jdbc.update("""
                INSERT INTO outbox_message (batch_id, idempotency_key, type, external_id, payload, next_attempt_at,
                                            created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (idempotency_key) DO NOTHING
                """, batchId, message.idempotencyKey(), message.type().name(), message.externalId(),
                message.payload(), at, at, at) == 1;
    }

    @Override
    public List<OutboxMessage> reserve(int limit, Instant now, Duration lease) {
        return jdbc.query("""
                WITH picked AS (
                    SELECT id FROM outbox_message
                    WHERE (status = 'PENDING' AND next_attempt_at <= ?)
                       OR (status = 'PROCESSING' AND locked_until < ?)
                    ORDER BY next_attempt_at, id
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE outbox_message m
                SET status = 'PROCESSING', attempts = m.attempts + 1, locked_until = ?, updated_at = ?
                FROM picked
                WHERE m.id = picked.id
                RETURNING m.*
                """, MESSAGE, Timestamp.from(now), Timestamp.from(now), limit, Timestamp.from(now.plus(lease)),
                Timestamp.from(now));
    }

    @Override
    public void update(OutboxMessage message) {
        jdbc.update("""
                UPDATE outbox_message
                SET status = ?, attempts = ?, next_attempt_at = ?, locked_until = ?, last_error = ?,
                    updated_at = now()
                WHERE id = ?
                """, message.status().name(), message.attempts(), Timestamp.from(message.nextAttemptAt()),
                message.lockedUntil() == null ? null : Timestamp.from(message.lockedUntil()), message.lastError(),
                message.id());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BatchInfo> findBatch(long id) {
        return jdbc.query("SELECT id, source, merge_strategy, created_at FROM integration_batch WHERE id = ?",
                (rs, i) -> new BatchInfo(rs.getLong("id"), rs.getString("source"),
                        MergeStrategy.valueOf(rs.getString("merge_strategy")), instant(rs, "created_at")),
                id).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BatchSummary> findBatchSummary(long id) {
        return jdbc.query(SUMMARY_SELECT + " WHERE b.id = ? GROUP BY b.id", SUMMARY, id).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchSummary> findRecentBatches(int limit) {
        return jdbc.query(SUMMARY_SELECT + " GROUP BY b.id ORDER BY b.id DESC LIMIT ?", SUMMARY, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OutboxMessage> findMessage(long id) {
        return jdbc.query("SELECT * FROM outbox_message WHERE id = ?", MESSAGE, id).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxMessage> findDeadByBatch(long batchId) {
        return jdbc.query("SELECT * FROM outbox_message WHERE batch_id = ? AND status = 'DEAD' ORDER BY id",
                MESSAGE, batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MessageSummary> findMessages(MessageStatus status, Long batchId, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE true");
        List<Object> args = new ArrayList<>();
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status.name());
        }
        if (batchId != null) {
            where.append(" AND batch_id = ?");
            args.add(batchId);
        }
        Long total = jdbc.queryForObject("SELECT count(*) FROM outbox_message" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<MessageSummary> content = jdbc.query("""
                SELECT id, batch_id, type, external_id, status, attempts, next_attempt_at, last_error
                FROM outbox_message""" + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, i) -> new MessageSummary(rs.getLong("id"), rs.getLong("batch_id"),
                        MessageType.valueOf(rs.getString("type")), rs.getString("external_id"),
                        MessageStatus.valueOf(rs.getString("status")), rs.getInt("attempts"),
                        instant(rs, "next_attempt_at"), rs.getString("last_error")),
                pageArgs.toArray());
        return new PageResult<>(content, page, size, total);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
