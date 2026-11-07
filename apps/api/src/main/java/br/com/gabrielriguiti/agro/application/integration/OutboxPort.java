package br.com.gabrielriguiti.agro.application.integration;

import br.com.gabrielriguiti.agro.application.registry.PageResult;
import br.com.gabrielriguiti.agro.domain.integration.MergeStrategy;
import br.com.gabrielriguiti.agro.domain.integration.MessageStatus;
import br.com.gabrielriguiti.agro.domain.integration.OutboxMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída: a fila durável (outbox) e as consultas de monitoramento (ADR-0011). Trocar o PostgreSQL por um
 * broker afetaria apenas o adaptador desta porta.
 */
public interface OutboxPort {

    /** @return o id do lote criado */
    long createBatch(String source, MergeStrategy strategy);

    /**
     * Enfileira uma mensagem {@code PENDING}, elegível em {@code now}, ignorando chave já existente.
     *
     * @return {@code true} se gravou; {@code false} se a chave de idempotência já existia (duplicata)
     */
    boolean enqueue(long batchId, NewMessage message, Instant now);

    /**
     * Reserva até {@code limit} mensagens elegíveis ({@code PENDING} vencidas ou {@code PROCESSING} com lease
     * vencido) com {@code FOR UPDATE SKIP LOCKED}, incrementando as tentativas (ADR-0012).
     */
    List<OutboxMessage> reserve(int limit, Instant now, Duration lease);

    /** Grava o estado da mensagem (status, tentativas, agendamento, lease e erro). */
    void update(OutboxMessage message);

    /** @return o lote, se existir */
    Optional<BatchInfo> findBatch(long id);

    /** @return o lote com contagens, se existir */
    Optional<BatchSummary> findBatchSummary(long id);

    /** @return os {@code limit} lotes mais recentes com contagens */
    List<BatchSummary> findRecentBatches(int limit);

    /** @return a mensagem completa (com payload), se existir */
    Optional<OutboxMessage> findMessage(long id);

    /** @return as mensagens {@code DEAD} do lote */
    List<OutboxMessage> findDeadByBatch(long batchId);

    /**
     * @param status filtro de status, ou {@code null}
     * @param batchId filtro de lote, ou {@code null}
     * @param page página, a partir de 0
     * @param size itens por página
     */
    PageResult<MessageSummary> findMessages(MessageStatus status, Long batchId, int page, int size);
}
