package br.com.gabrielriguiti.agro.infrastructure.persistence.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.IntegrationTestSupport;
import br.com.gabrielriguiti.agro.application.integration.NewMessage;
import br.com.gabrielriguiti.agro.application.integration.OutboxPort;
import br.com.gabrielriguiti.agro.domain.integration.MergeStrategy;
import br.com.gabrielriguiti.agro.domain.integration.MessageStatus;
import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import br.com.gabrielriguiti.agro.domain.integration.OutboxMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Adaptador JDBC da outbox: enfileiramento, reserva com SKIP LOCKED e lease (tasks 2.1 a 2.3). */
class OutboxPortIT extends IntegrationTestSupport {

    @Autowired OutboxPort outbox;

    private long batchWith(int messages, Instant now) {
        long batch = outbox.createBatch("teste", MergeStrategy.MERGE);
        for (int i = 0; i < messages; i++) {
            outbox.enqueue(batch, new NewMessage("key-" + batch + "-" + i, MessageType.LABORATORY, "L-" + i, "{}"), now);
        }
        return batch;
    }

    @Test
    void enfileiraComoPendingEIgnoraChaveRepetida() {
        Instant now = Instant.now();
        long batch = outbox.createBatch("teste", MergeStrategy.OVERWRITE);
        NewMessage message = new NewMessage("k-1", MessageType.PRODUCER, "P-1", "{\"a\":1}");

        assertThat(outbox.enqueue(batch, message, now)).isTrue();
        assertThat(outbox.enqueue(batch, message, now)).isFalse();

        OutboxMessage saved = outbox.findMessage(jdbc.queryForObject("SELECT id FROM outbox_message", Long.class))
                .orElseThrow();
        assertThat(saved.status()).isEqualTo(MessageStatus.PENDING);
        assertThat(saved.attempts()).isZero();
        assertThat(saved.nextAttemptAt()).isBeforeOrEqualTo(now.plusMillis(1));
        assertThat(count("outbox_message")).isEqualTo(1);
    }

    @Test
    void reservaMarcaProcessingContaTentativaEDefineLease() {
        Instant now = Instant.now();
        batchWith(3, now);

        List<OutboxMessage> reserved = outbox.reserve(2, now.plusSeconds(1), Duration.ofMinutes(2));

        assertThat(reserved).hasSize(2);
        assertThat(reserved).allSatisfy(m -> {
            assertThat(m.status()).isEqualTo(MessageStatus.PROCESSING);
            assertThat(m.attempts()).isEqualTo(1);
            assertThat(m.lockedUntil()).isAfter(now);
        });
        assertThat(outbox.reserve(10, now.plusSeconds(1), Duration.ofMinutes(2))).hasSize(1);
    }

    @Test
    void naoReservaMensagemAindaNaoElegivel() {
        Instant now = Instant.now();
        batchWith(1, now.plus(Duration.ofMinutes(5)));
        assertThat(outbox.reserve(10, now, Duration.ofMinutes(1))).isEmpty();
    }

    @Test
    void doisWorkersReservandoAoMesmoTempoRecebemConjuntosDisjuntos() throws Exception {
        Instant now = Instant.now();
        batchWith(60, now);
        Instant later = now.plusSeconds(1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CyclicBarrier barrier = new CyclicBarrier(2);
            List<Future<List<OutboxMessage>>> futures = new ArrayList<>();
            for (int w = 0; w < 2; w++) {
                futures.add(pool.submit(() -> {
                    barrier.await();
                    List<OutboxMessage> mine = new ArrayList<>();
                    for (int i = 0; i < 6; i++) {
                        mine.addAll(outbox.reserve(5, later, Duration.ofMinutes(2)));
                    }
                    return mine;
                }));
            }
            Set<Long> first = futures.get(0).get().stream().map(OutboxMessage::id).collect(Collectors.toSet());
            Set<Long> second = futures.get(1).get().stream().map(OutboxMessage::id).collect(Collectors.toSet());

            Set<Long> overlap = new HashSet<>(first);
            overlap.retainAll(second);
            assertThat(overlap).as("mensagens reservadas por ambos").isEmpty();
            assertThat(first.size() + second.size()).isEqualTo(60);
            assertThat(jdbc.queryForObject("SELECT max(attempts) FROM outbox_message", Integer.class)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void leaseVencidoTornaAMensagemElegivelDeNovoComATentativaContada() {
        Instant now = Instant.now();
        batchWith(1, now);

        OutboxMessage first = outbox.reserve(1, now, Duration.ofMinutes(1)).get(0);
        assertThat(first.attempts()).isEqualTo(1);
        // Dentro do lease, nada a reservar (o worker ainda "está vivo").
        assertThat(outbox.reserve(1, now.plusSeconds(30), Duration.ofMinutes(1))).isEmpty();

        // O worker morreu: passado o lease, a mensagem volta e a segunda tentativa é contada.
        List<OutboxMessage> again = outbox.reserve(1, now.plus(Duration.ofMinutes(2)), Duration.ofMinutes(1));
        assertThat(again).hasSize(1);
        assertThat(again.get(0).id()).isEqualTo(first.id());
        assertThat(again.get(0).attempts()).isEqualTo(2);
    }

    @Test
    void updateGravaOEstadoDaMensagem() {
        Instant now = Instant.now();
        batchWith(1, now);
        OutboxMessage reserved = outbox.reserve(1, now, Duration.ofMinutes(1)).get(0);

        outbox.update(reserved.dead("falha permanente"));

        OutboxMessage saved = outbox.findMessage(reserved.id()).orElseThrow();
        assertThat(saved.status()).isEqualTo(MessageStatus.DEAD);
        assertThat(saved.lastError()).isEqualTo("falha permanente");
        assertThat(saved.attempts()).isEqualTo(1);
        assertThat(saved.lockedUntil()).isNull();
    }
}
