package br.com.gabrielriguiti.agro.domain.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Backoff exponencial, transições de estado da mensagem e DLQ (spec retry-and-dead-letter, task 1.1). */
class BackoffPolicyTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final BackoffPolicy NO_JITTER =
            new BackoffPolicy(Duration.ofSeconds(5), Duration.ofMinutes(10), 0);

    private static OutboxMessage pending() {
        return new OutboxMessage(1, 1, "k", MessageType.LABORATORY, "L-1", "{}", MessageStatus.PENDING, 0, NOW, null,
                null);
    }

    @Test
    void progressaoDe5A80SegundosSemJitter() {
        long[] seconds = java.util.stream.IntStream.rangeClosed(1, 5)
                .mapToLong(a -> NO_JITTER.delay(a, 0).toSeconds()).toArray();
        assertThat(seconds).containsExactly(5, 10, 20, 40, 80);
    }

    @Test
    void nuncaPassaDoTetoDeDezMinutos() {
        assertThat(NO_JITTER.delay(8, 0)).isEqualTo(Duration.ofMinutes(10));
        assertThat(NO_JITTER.delay(200, 0)).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void jitterAcrescentaAteODecimoDoAtraso() {
        BackoffPolicy p = BackoffPolicy.defaults();
        assertThat(p.delay(1, 0)).isEqualTo(Duration.ofSeconds(5));
        assertThat(p.delay(1, 0.5)).isEqualTo(Duration.ofMillis(5250));
    }

    @Test
    void rejeitaParametrosInvalidos() {
        assertThatThrownBy(() -> new BackoffPolicy(Duration.ZERO, Duration.ofMinutes(1), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NO_JITTER.delay(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reservaIncrementaTentativaEDefineLease() {
        OutboxMessage reserved = pending().reserve(NOW, Duration.ofMinutes(2));
        assertThat(reserved.status()).isEqualTo(MessageStatus.PROCESSING);
        assertThat(reserved.attempts()).isEqualTo(1);
        assertThat(reserved.lockedUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(2)));
    }

    @Test
    void falhaTransitoriaReagendaComBackoff() {
        OutboxMessage failed = pending().reserve(NOW, Duration.ofMinutes(2))
                .failTransient("banco indisponível", NOW, 5, NO_JITTER, 0);
        assertThat(failed.status()).isEqualTo(MessageStatus.PENDING);
        assertThat(failed.nextAttemptAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(failed.lastError()).isEqualTo("banco indisponível");
        assertThat(failed.lockedUntil()).isNull();
    }

    @Test
    void quintaFalhaTransitoriaMandaParaADlqPreservandoDados() {
        OutboxMessage m = pending();
        for (int i = 1; i <= 4; i++) {
            m = m.reserve(NOW, Duration.ofMinutes(1)).failTransient("x", NOW, 5, NO_JITTER, 0);
            assertThat(m.status()).isEqualTo(MessageStatus.PENDING);
        }
        OutboxMessage dead = m.reserve(NOW, Duration.ofMinutes(1)).failTransient("último erro", NOW, 5, NO_JITTER, 0);
        assertThat(dead.status()).isEqualTo(MessageStatus.DEAD);
        assertThat(dead.attempts()).isEqualTo(5);
        assertThat(dead.lastError()).isEqualTo("último erro");
        assertThat(dead.payload()).isEqualTo("{}");
    }

    @Test
    void reenfileirarZeraTentativasEExigeDead() {
        OutboxMessage dead = pending().reserve(NOW, Duration.ofMinutes(1)).dead("falha permanente");
        OutboxMessage again = dead.requeue(NOW);
        assertThat(again.status()).isEqualTo(MessageStatus.PENDING);
        assertThat(again.attempts()).isZero();
        assertThatThrownBy(() -> pending().done().requeue(NOW)).isInstanceOf(IllegalStateException.class);
    }
}
