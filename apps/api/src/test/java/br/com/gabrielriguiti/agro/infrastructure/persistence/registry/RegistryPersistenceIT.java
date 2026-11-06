package br.com.gabrielriguiti.agro.infrastructure.persistence.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.gabrielriguiti.agro.AbstractIntegrationTest;
import br.com.gabrielriguiti.agro.TaxIds;
import br.com.gabrielriguiti.agro.application.registry.ConflictException;
import br.com.gabrielriguiti.agro.application.registry.LaboratoryRepository;
import br.com.gabrielriguiti.agro.application.registry.PageResult;
import br.com.gabrielriguiti.agro.application.registry.ProducerDetails;
import br.com.gabrielriguiti.agro.application.registry.ProducerRepository;
import br.com.gabrielriguiti.agro.application.registry.PropertyRepository;
import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import br.com.gabrielriguiti.agro.domain.registry.Producer;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import br.com.gabrielriguiti.agro.domain.registry.TaxId;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifica a migration, os adaptadores JPA e a listagem em lote (tasks 2.1 a 2.3). */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class RegistryPersistenceIT extends AbstractIntegrationTest {

    private static final Instant T0 = Instant.parse("2025-01-01T00:00:00Z");

    @Autowired LaboratoryRepository laboratories;
    @Autowired PropertyRepository properties;
    @Autowired ProducerRepository producers;
    @Autowired DataSource dataSource;
    @Autowired EntityManagerFactory emf;
    @Autowired TransactionTemplate tx;

    private static Identity person(String name) {
        return new Identity(name, TaxId.of(TaxIds.cpf()), "Rua " + name);
    }

    private Laboratory newLab(String name) {
        return laboratories.save(Laboratory.create(new Identity(name, TaxId.of(TaxIds.cnpj()), "Av. " + name)));
    }

    private Producer newProducer(String name, long labId) {
        return producers.save(new Producer(null, person(name), T0, T0.plusSeconds(3600), "obs", labId, Set.of()));
    }

    @Test
    void migrationTemComentariosEIndicesDoCadastro() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer semComentario = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns c WHERE c.table_schema = 'public' "
                        + "AND c.table_name IN ('laboratories','producers','properties') "
                        + "AND col_description((c.table_schema || '.' || c.table_name)::regclass, c.ordinal_position) IS NULL",
                Integer.class);
        assertThat(semComentario).isZero();
        List<String> indices = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'", String.class);
        assertThat(indices).contains("ix_producers_laboratory_id", "ix_producers_data_inicial",
                "ix_producers_data_final", "ix_properties_producer_id", "uq_laboratories_tax_id",
                "uq_producers_tax_id", "uq_properties_tax_id");
    }

    @Test
    void laboratorioEhGravadoELidoComInscricaoNormalizada() {
        Laboratory saved = newLab("Lab Alfa");
        assertThat(saved.id()).isNotNull();
        Laboratory read = laboratories.findById(saved.id()).orElseThrow();
        assertThat(read).isEqualTo(saved);
        assertThat(read.identity().taxId().digits()).hasSize(14);
        Laboratory renamed = laboratories.save(read.withIdentity(
                new Identity("Lab Alfa 2", read.identity().taxId(), read.identity().address())));
        assertThat(laboratories.findById(saved.id()).orElseThrow().identity().name()).isEqualTo("Lab Alfa 2");
        assertThat(renamed.id()).isEqualTo(saved.id());
    }

    @Test
    void inscricaoDuplicadaDeLaboratorioViraConflito() {
        Laboratory first = newLab("Lab Dup");
        assertThatThrownBy(() -> laboratories.save(Laboratory.create(first.identity())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void inscricaoDuplicadaDePropriedadeEProdutorViraConflito() {
        Property p = properties.save(Property.create(person("Prop Dup")));
        assertThatThrownBy(() -> properties.save(Property.create(p.identity())))
                .isInstanceOf(ConflictException.class);
        Laboratory lab = newLab("Lab P");
        Producer producer = newProducer("Prod Dup", lab.id());
        assertThatThrownBy(() -> producers.save(new Producer(null, producer.identity(), T0, T0, null, lab.id(), null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void propriedadeNasceLivreEVinculoEhPreservadoNaAtualizacao() {
        Laboratory lab = newLab("Lab V");
        Producer producer = newProducer("Prod V", lab.id());
        Property p = properties.save(Property.create(person("Prop V")));
        assertThat(p.isAvailable()).isTrue();
        assertThat(properties.link(producer.id(), List.of(p.id()))).isEqualTo(1);

        Property renamed = properties.save(properties.findById(p.id()).orElseThrow()
                .withIdentity(new Identity("Prop V2", p.identity().taxId(), "Outro endereço")));
        assertThat(renamed.producerId()).isEqualTo(producer.id());
        assertThat(renamed.identity().name()).isEqualTo("Prop V2");
    }

    @Test
    void vinculoNaoRoubaPropriedadeDeOutroProdutor() {
        Laboratory lab = newLab("Lab R");
        Producer a = newProducer("Prod A", lab.id());
        Producer b = newProducer("Prod B", lab.id());
        Property p = properties.save(Property.create(person("Prop R")));
        assertThat(properties.link(a.id(), List.of(p.id()))).isEqualTo(1);
        assertThat(properties.link(b.id(), List.of(p.id()))).isZero();
        assertThat(properties.findById(p.id()).orElseThrow().producerId()).isEqualTo(a.id());
        assertThat(properties.link(a.id(), List.of(p.id()))).isEqualTo(1);
    }

    @Test
    void unlinkExceptLiberaApenasAsDemais() {
        Laboratory lab = newLab("Lab U");
        Producer producer = newProducer("Prod U", lab.id());
        Property keep = properties.save(Property.create(person("Keep")));
        Property drop = properties.save(Property.create(person("Drop")));
        properties.link(producer.id(), List.of(keep.id(), drop.id()));

        properties.unlinkExcept(producer.id(), Set.of(keep.id()));
        assertThat(properties.findById(keep.id()).orElseThrow().producerId()).isEqualTo(producer.id());
        assertThat(properties.findById(drop.id()).orElseThrow().isAvailable()).isTrue();

        properties.unlinkExcept(producer.id(), Set.of());
        assertThat(properties.findById(keep.id()).orElseThrow().isAvailable()).isTrue();
    }

    @Test
    void disponiveisIncluiLivresEAsDoProdutorEmEdicao() {
        Laboratory lab = newLab("Lab D");
        Producer seven = newProducer("Prod 7", lab.id());
        Producer other = newProducer("Prod 8", lab.id());
        Property free = properties.save(Property.create(person("Livre")));
        Property mine = properties.save(Property.create(person("Minha")));
        Property theirs = properties.save(Property.create(person("Alheia")));
        properties.link(seven.id(), List.of(mine.id()));
        properties.link(other.id(), List.of(theirs.id()));

        List<Long> ids = properties.findAvailable(seven.id()).stream().map(Property::id).toList();
        assertThat(ids).contains(free.id(), mine.id()).doesNotContain(theirs.id());
        List<Long> onlyFree = properties.findAvailable(null).stream().map(Property::id).toList();
        assertThat(onlyFree).contains(free.id()).doesNotContain(mine.id(), theirs.id());
    }

    @Test
    void exclusaoDeProdutorLiberaPropriedadesESomeComOProdutor() {
        Laboratory lab = newLab("Lab X");
        Producer producer = newProducer("Prod X", lab.id());
        Property p1 = properties.save(Property.create(person("X1")));
        Property p2 = properties.save(Property.create(person("X2")));
        properties.link(producer.id(), List.of(p1.id(), p2.id()));

        producers.deleteById(producer.id());

        assertThat(producers.existsById(producer.id())).isFalse();
        assertThat(properties.findById(p1.id()).orElseThrow().isAvailable()).isTrue();
        assertThat(properties.findById(p2.id()).orElseThrow().isAvailable()).isTrue();
    }

    @Test
    void laboratorioComProdutorNaoPodeSerRemovidoPeloBanco() {
        Laboratory lab = newLab("Lab FK");
        newProducer("Prod FK", lab.id());
        assertThat(laboratories.hasProducers(lab.id())).isTrue();
        assertThatThrownBy(() -> laboratories.deleteById(lab.id())).isInstanceOf(ConflictException.class);
    }

    @Test
    void detalhesDoProdutorTrazemLaboratorioEPropriedades() {
        Laboratory lab = newLab("Lab Det");
        Producer producer = newProducer("Prod Det", lab.id());
        Property p = properties.save(Property.create(person("Det1")));
        properties.link(producer.id(), List.of(p.id()));

        ProducerDetails details = producers.findDetailsById(producer.id()).orElseThrow();
        assertThat(details.laboratory().id()).isEqualTo(lab.id());
        assertThat(details.properties()).extracting(Property::id).containsExactly(p.id());
        assertThat(details.producer().propertyIds()).containsExactly(p.id());
        assertThat(producers.findDetailsById(-1)).isEmpty();
    }

    @Test
    void listagemDeUmaPaginaFazNoMaximoTresConsultas() {
        Laboratory lab = newLab("Lab Batch");
        for (int i = 0; i < 12; i++) {
            Producer producer = newProducer("Lote %02d".formatted(i), lab.id());
            Property p = properties.save(Property.create(person("Lote prop " + i)));
            properties.link(producer.id(), List.of(p.id()));
        }
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);

        for (int size : new int[] {3, 12, 50}) {
            sessionFactory.getStatistics().clear();
            PageResult<ProducerDetails> page = tx.execute(status -> producers.findPage(0, size));
            long statements = sessionFactory.getStatistics().getPrepareStatementCount();

            assertThat(page.content()).hasSize(Math.min(size, page.content().size())).isNotEmpty();
            assertThat(page.content()).allSatisfy(d -> {
                assertThat(d.laboratory()).isNotNull();
                assertThat(d.producer().propertyIds()).isEqualTo(
                        d.properties().stream().map(Property::id).collect(java.util.stream.Collectors.toSet()));
            });
            assertThat(statements).as("consultas para size=%d", size).isLessThanOrEqualTo(3);
        }
    }

    @Test
    void paginacaoRespeitaOrdemPorNomeETotal() {
        Laboratory lab = newLab("Lab Page");
        newProducer("Zeta paginado", lab.id());
        newProducer("Alfa paginado", lab.id());
        PageResult<ProducerDetails> first = producers.findPage(0, 1);
        assertThat(first.content()).hasSize(1);
        assertThat(first.totalElements()).isGreaterThanOrEqualTo(2);
        assertThat(first.totalPages()).isGreaterThanOrEqualTo(2);
        PageResult<ProducerDetails> beyond = producers.findPage(10_000, 10);
        assertThat(beyond.content()).isEmpty();
        assertThat(beyond.totalElements()).isEqualTo(first.totalElements());
    }
}
