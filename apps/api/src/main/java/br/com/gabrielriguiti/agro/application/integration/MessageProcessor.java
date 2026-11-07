package br.com.gabrielriguiti.agro.application.integration;

import br.com.gabrielriguiti.agro.domain.integration.OutboxMessage;
import br.com.gabrielriguiti.agro.domain.integration.PermanentIntegrationException;
import br.com.gabrielriguiti.agro.domain.registry.DomainValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Worker: reserva mensagens da outbox e as processa, uma por transação própria ({@code REQUIRES_NEW}), classificando
 * as falhas (ADR-0014). O resultado ({@code DONE}, reagendamento ou {@code DEAD}) é gravado fora da transação do
 * processamento, para que uma falha não desfaça o registro do erro.
 *
 * <p>Este método não deve ser chamado dentro de outra transação: a reserva precisa ser confirmada antes do
 * processamento.
 */
@Component
public class MessageProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessor.class);

    private final OutboxPort outbox;
    private final RegistrySynchronizer synchronizer;
    private final IntegrationSettings settings;
    private final Clock clock;
    private final RandomGenerator random;
    private final ObjectMapper mapper = new ObjectMapper();
    private final TransactionTemplate perMessage;

    public MessageProcessor(
            OutboxPort outbox,
            RegistrySynchronizer synchronizer,
            IntegrationSettings settings,
            Clock clock,
            RandomGenerator random,
            PlatformTransactionManager transactionManager) {
        this.outbox = outbox;
        this.synchronizer = synchronizer;
        this.settings = settings;
        this.clock = clock;
        this.random = random;
        this.perMessage = new TransactionTemplate(transactionManager);
        this.perMessage.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Executa um ciclo: reserva até {@code workerBatchSize} mensagens elegíveis e processa cada uma.
     *
     * @return quantas mensagens foram tratadas (concluídas, reagendadas ou mortas)
     */
    public int runCycle() {
        List<OutboxMessage> reserved = outbox.reserve(settings.workerBatchSize(), clock.instant(), settings.lease());
        Map<Long, BatchInfo> batches = new HashMap<>();
        for (OutboxMessage message : reserved) {
            process(message, batches);
        }
        return reserved.size();
    }

    private void process(OutboxMessage message, Map<Long, BatchInfo> batches) {
        try {
            BatchInfo batch = batches.computeIfAbsent(message.batchId(),
                    id -> outbox.findBatch(id).orElseThrow(() -> new IllegalStateException("Lote " + id + " ausente")));
            JsonNode data = parse(message.payload());
            perMessage.executeWithoutResult(status ->
                    synchronizer.sync(message.type(), batch.source(), message.externalId(), data,
                            batch.mergeStrategy()));
            outbox.update(message.done());
        } catch (PermanentIntegrationException | DomainValidationException e) {
            LOG.warn("Mensagem {} vai para a DLQ: {}", message.id(), e.getMessage());
            outbox.update(message.dead(e.getMessage()));
        } catch (RuntimeException e) {
            OutboxMessage next = message.failTransient(describe(e), clock.instant(), settings.maxAttempts(),
                    settings.backoff(), random.nextDouble());
            LOG.warn("Mensagem {} falhou na tentativa {} ({}): {}", message.id(), message.attempts(), next.status(),
                    e.toString());
            outbox.update(next);
        }
    }

    private JsonNode parse(String payload) {
        try {
            return mapper.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new PermanentIntegrationException("JSON malformado: " + e.getOriginalMessage(), e);
        }
    }

    private static String describe(RuntimeException e) {
        String message = e.getMessage();
        return e.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
