package br.com.gabrielriguiti.agro.application.integration;

import br.com.gabrielriguiti.agro.domain.integration.MergeStrategy;
import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingestão de lotes: grava o lote e todas as suas mensagens na outbox em uma única transação, sem processar nada
 * (ADR-0011). Se qualquer gravação falhar, nada fica gravado.
 */
@Service
public class IngestionService {

    /** Item de um lote. */
    public record Item(MessageType type, String externalId, JsonNode data) {
    }

    /** Lote a ingerir. */
    public record Command(String source, MergeStrategy mergeStrategy, List<Item> items) {
    }

    /**
     * Resultado da ingestão.
     *
     * @param batchId lote criado
     * @param accepted mensagens novas gravadas
     * @param duplicates itens descartados por chave de idempotência já existente
     */
    public record Result(long batchId, int accepted, int duplicates) {
    }

    private final OutboxPort outbox;
    private final IntegrationSettings settings;
    private final Clock clock;

    public IngestionService(OutboxPort outbox, IntegrationSettings settings, Clock clock) {
        this.outbox = outbox;
        this.settings = settings;
        this.clock = clock;
    }

    /**
     * Grava o lote de forma atômica.
     *
     * @throws BatchTooLargeException se houver mais itens que o limite configurado
     * @throws InvalidBatchException se algum item não trouxer {@code data}
     */
    @Transactional
    public Result ingest(Command command) {
        if (command.items().size() > settings.maxBatchSize()) {
            throw new BatchTooLargeException(
                    "O lote excede o limite de " + settings.maxBatchSize() + " itens");
        }
        for (int i = 0; i < command.items().size(); i++) {
            JsonNode data = command.items().get(i).data();
            if (data == null || data.isNull() || data.isMissingNode()) {
                throw new InvalidBatchException("items[" + i + "].data", "data é obrigatório");
            }
        }
        long batchId = outbox.createBatch(command.source(), command.mergeStrategy());
        int accepted = 0;
        for (Item item : command.items()) {
            String key = IdempotencyKey.of(command.source(), item.type(), item.externalId(), item.data());
            NewMessage message = new NewMessage(key, item.type(), item.externalId(), item.data().toString());
            if (outbox.enqueue(batchId, message, clock.instant())) {
                accepted++;
            }
        }
        return new Result(batchId, accepted, command.items().size() - accepted);
    }
}
