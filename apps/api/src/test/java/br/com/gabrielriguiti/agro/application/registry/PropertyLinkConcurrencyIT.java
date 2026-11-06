package br.com.gabrielriguiti.agro.application.registry;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.AbstractIntegrationTest;
import br.com.gabrielriguiti.agro.TaxIds;
import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import br.com.gabrielriguiti.agro.domain.registry.Producer;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import br.com.gabrielriguiti.agro.domain.registry.TaxId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Verifica que, ao vincular a mesma propriedade por dois produtores ao mesmo tempo, só um vence (task 3.4). */
class PropertyLinkConcurrencyIT extends AbstractIntegrationTest {

    private static final Instant T0 = Instant.parse("2025-01-01T00:00:00Z");
    private static final int ROUNDS = 15;

    @Autowired LaboratoryService laboratories;
    @Autowired PropertyService properties;
    @Autowired ProducerService producers;

    private Producer draft(String name, long labId, Set<Long> propertyIds) {
        return new Producer(null, new Identity(name, TaxId.of(TaxIds.cpf()), "Rua " + name), T0, T0, null, labId,
                propertyIds);
    }

    /** Executa as duas tarefas ao mesmo tempo e devolve o resultado de cada uma (null = sucesso, senão a exceção). */
    private static List<Throwable> race(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CyclicBarrier barrier = new CyclicBarrier(2);
            List<Future<Throwable>> futures = new ArrayList<>();
            for (Callable<?> task : List.of(first, second)) {
                futures.add(pool.submit(() -> {
                    barrier.await();
                    try {
                        task.call();
                        return null;
                    } catch (Throwable t) {
                        return t;
                    }
                }));
            }
            List<Throwable> results = new ArrayList<>();
            for (Future<Throwable> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void doisProdutoresCriadosAoMesmoTempoDisputandoUmaPropriedade_soUmVence() throws Exception {
        Laboratory lab = laboratories.create(new Identity("Lab Corrida", TaxId.of(TaxIds.cnpj()), "Av. Corrida"));
        for (int round = 0; round < ROUNDS; round++) {
            Property disputed = properties.create(new Identity("Disputada " + round, TaxId.of(TaxIds.cpf()), "Rua"));

            List<Throwable> results = race(
                    () -> producers.create(draft("Corredor A", lab.id(), Set.of(disputed.id()))),
                    () -> producers.create(draft("Corredor B", lab.id(), Set.of(disputed.id()))));

            long winners = results.stream().filter(r -> r == null).count();
            long conflicts = results.stream().filter(r -> r instanceof ConflictException).count();
            assertThat(winners).as("rodada %d: vencedores (%s)", round, results).isEqualTo(1);
            assertThat(conflicts).as("rodada %d: conflitos (%s)", round, results).isEqualTo(1);
            assertThat(properties.get(disputed.id()).producerId()).isNotNull();
        }
    }

    @Test
    void doisProdutoresAtualizadosAoMesmoTempoDisputandoUmaPropriedade_soUmVence() throws Exception {
        Laboratory lab = laboratories.create(new Identity("Lab Corrida 2", TaxId.of(TaxIds.cnpj()), "Av. Corrida"));
        for (int round = 0; round < ROUNDS; round++) {
            Property disputed = properties.create(new Identity("Disputada U" + round, TaxId.of(TaxIds.cpf()), "Rua"));
            Producer a = producers.create(draft("Atualizado A", lab.id(), Set.of())).producer();
            Producer b = producers.create(draft("Atualizado B", lab.id(), Set.of())).producer();

            List<Throwable> results = race(
                    () -> producers.update(a.id(), a.update(a.identity(), T0, T0, null, lab.id(), Set.of(disputed.id()))),
                    () -> producers.update(b.id(), b.update(b.identity(), T0, T0, null, lab.id(), Set.of(disputed.id()))));

            assertThat(results.stream().filter(r -> r == null).count()).as("rodada %d: %s", round, results).isEqualTo(1);
            assertThat(results.stream().filter(r -> r instanceof ConflictException).count())
                    .as("rodada %d: %s", round, results).isEqualTo(1);
            long owner = properties.get(disputed.id()).producerId();
            long winnerOwned = producers.get(owner).properties().stream().filter(p -> p.id().equals(disputed.id())).count();
            assertThat(winnerOwned).isEqualTo(1);
        }
    }
}
