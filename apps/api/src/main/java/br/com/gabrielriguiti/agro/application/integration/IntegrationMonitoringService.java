package br.com.gabrielriguiti.agro.application.integration;

import br.com.gabrielriguiti.agro.application.registry.ConflictException;
import br.com.gabrielriguiti.agro.application.registry.PageResult;
import br.com.gabrielriguiti.agro.application.registry.ResourceNotFoundException;
import br.com.gabrielriguiti.agro.domain.integration.MessageStatus;
import br.com.gabrielriguiti.agro.domain.integration.OutboxMessage;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de lotes, mensagens e DLQ, reprocessamento manual e execução manual do worker. */
@Service
public class IntegrationMonitoringService {

    private static final int MAX_BATCHES = 100;
    private static final int MAX_PAGE_SIZE = 200;

    private final OutboxPort outbox;
    private final MessageProcessor processor;
    private final IntegrationSettings settings;
    private final Clock clock;

    public IntegrationMonitoringService(
            OutboxPort outbox, MessageProcessor processor, IntegrationSettings settings, Clock clock) {
        this.outbox = outbox;
        this.processor = processor;
        this.settings = settings;
        this.clock = clock;
    }

    /** @return os lotes mais recentes (até 100) com contagens e estado derivado */
    @Transactional(readOnly = true)
    public List<BatchSummary> recentBatches(int limit) {
        return outbox.findRecentBatches(Math.max(1, Math.min(limit, MAX_BATCHES)));
    }

    /**
     * @throws ResourceNotFoundException se o lote não existir
     */
    @Transactional(readOnly = true)
    public BatchSummary batch(long id) {
        return outbox.findBatchSummary(id).orElseThrow(() -> ResourceNotFoundException.of("Lote", id));
    }

    /** @return página de mensagens filtráveis por status e lote, da mais recente para a mais antiga */
    @Transactional(readOnly = true)
    public PageResult<MessageSummary> messages(MessageStatus status, Long batchId, int page, int size) {
        return outbox.findMessages(status, batchId, Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
    }

    /**
     * @throws ResourceNotFoundException se a mensagem não existir
     */
    @Transactional(readOnly = true)
    public OutboxMessage message(long id) {
        return outbox.findMessage(id).orElseThrow(() -> ResourceNotFoundException.of("Mensagem", id));
    }

    /**
     * Reenfileira uma mensagem {@code DEAD}: volta a {@code PENDING} com zero tentativas.
     *
     * @throws ResourceNotFoundException se a mensagem não existir
     * @throws ConflictException se a mensagem não estiver em {@code DEAD}
     */
    @Transactional
    public OutboxMessage reprocessMessage(long id) {
        OutboxMessage message = message(id);
        if (message.status() != MessageStatus.DEAD) {
            throw new ConflictException("Só mensagens DEAD podem ser reprocessadas (status atual: "
                    + message.status() + ")");
        }
        OutboxMessage requeued = message.requeue(clock.instant());
        outbox.update(requeued);
        return requeued;
    }

    /**
     * Reenfileira todas as mensagens {@code DEAD} do lote.
     *
     * @return quantas mensagens foram reenfileiradas (zero se não houver nenhuma morta)
     * @throws ResourceNotFoundException se o lote não existir
     */
    @Transactional
    public int reprocessBatch(long batchId) {
        if (outbox.findBatch(batchId).isEmpty()) {
            throw ResourceNotFoundException.of("Lote", batchId);
        }
        List<OutboxMessage> dead = outbox.findDeadByBatch(batchId);
        dead.forEach(m -> outbox.update(m.requeue(clock.instant())));
        return dead.size();
    }

    /**
     * Processa um ciclo do worker, se a execução manual estiver habilitada.
     *
     * @return quantas mensagens foram tratadas
     * @throws ResourceNotFoundException se a execução manual estiver desabilitada
     */
    public int runWorkerOnce() {
        if (!settings.manualRunEnabled()) {
            throw new ResourceNotFoundException("Recurso não encontrado");
        }
        return processor.runCycle();
    }
}
