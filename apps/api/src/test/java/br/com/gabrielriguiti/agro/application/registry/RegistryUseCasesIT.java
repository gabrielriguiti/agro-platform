package br.com.gabrielriguiti.agro.application.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.gabrielriguiti.agro.AbstractIntegrationTest;
import br.com.gabrielriguiti.agro.TaxIds;
import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import br.com.gabrielriguiti.agro.domain.registry.Producer;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import br.com.gabrielriguiti.agro.domain.registry.TaxId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica os cenários das specs de laboratório, propriedade e produtor no nível dos casos de uso (3.1 e 3.2). */
class RegistryUseCasesIT extends AbstractIntegrationTest {

    private static final Instant T0 = Instant.parse("2025-01-01T00:00:00Z");

    @Autowired LaboratoryService laboratories;
    @Autowired PropertyService properties;
    @Autowired ProducerService producers;
    @Autowired DataSource dataSource;

    private static Identity person(String name) {
        return new Identity(name, TaxId.of(TaxIds.cpf()), "Rua " + name);
    }

    private Laboratory lab(String name) {
        return laboratories.create(new Identity(name, TaxId.of(TaxIds.cnpj()), "Av. " + name));
    }

    private Property property(String name) {
        return properties.create(person(name));
    }

    private Producer draft(String name, long labId, Set<Long> propertyIds) {
        return new Producer(null, person(name), T0, T0.plusSeconds(86_400), null, labId, propertyIds);
    }

    private List<Long> ownedBy(long producerId) {
        return producers.get(producerId).properties().stream().map(Property::id).toList();
    }

    // ---- laboratório (3.1)

    @Test
    void laboratorioCriadoTemIdEPodeSerConsultado() {
        Laboratory created = lab("Lab UC");
        assertThat(created.id()).isNotNull();
        assertThat(laboratories.get(created.id())).isEqualTo(created);
        assertThat(laboratories.list()).extracting(Laboratory::id).contains(created.id());
    }

    @Test
    void laboratorioInexistenteLancaNaoEncontrado() {
        assertThatThrownBy(() -> laboratories.get(999_999)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> laboratories.delete(999_999)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void laboratorioComProdutoresNaoEhExcluido() {
        Laboratory lab = lab("Lab Com Produtores");
        for (int i = 0; i < 3; i++) {
            producers.create(draft("Prod " + i, lab.id(), Set.of()));
        }
        assertThatThrownBy(() -> laboratories.delete(lab.id())).isInstanceOf(ConflictException.class);
        assertThat(laboratories.get(lab.id())).isNotNull();
    }

    @Test
    void laboratorioSemProdutoresEhExcluido() {
        Laboratory lab = lab("Lab Sem Produtores");
        laboratories.delete(lab.id());
        assertThatThrownBy(() -> laboratories.get(lab.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cnpjDuplicadoEmLaboratorioEhConflitoESemNovoRegistro() {
        Laboratory first = lab("Lab Original");
        int before = laboratories.list().size();
        assertThatThrownBy(() -> laboratories.create(new Identity("Outro", first.identity().taxId(), "Rua")))
                .isInstanceOf(ConflictException.class);
        assertThat(laboratories.list()).hasSize(before);
    }

    @Test
    void laboratorioPodeSerAtualizado() {
        Laboratory lab = lab("Lab Antigo");
        Laboratory updated = laboratories.update(lab.id(),
                new Identity("Lab Novo", lab.identity().taxId(), "Novo endereço"));
        assertThat(updated.identity().name()).isEqualTo("Lab Novo");
        assertThat(laboratories.get(lab.id()).identity().address()).isEqualTo("Novo endereço");
    }

    // ---- propriedade (3.1)

    @Test
    void propriedadeSemProdutorEhAceitaEDisponivel() {
        Property p = property("Sem dono");
        assertThat(p.isAvailable()).isTrue();
        assertThat(properties.list(true, null)).extracting(Property::id).contains(p.id());
    }

    @Test
    void edicaoDeProdutorListaLivresEAsDele() {
        Laboratory lab = lab("Lab Sel");
        Property mine = property("Minha");
        Property free = property("Livre");
        Property theirs = property("Alheia");
        long seven = producers.create(draft("P7", lab.id(), Set.of(mine.id()))).producer().id();
        producers.create(draft("P8", lab.id(), Set.of(theirs.id())));

        List<Long> ids = properties.list(true, seven).stream().map(Property::id).toList();
        assertThat(ids).contains(mine.id(), free.id()).doesNotContain(theirs.id());
        assertThat(properties.list(false, null)).extracting(Property::id)
                .contains(mine.id(), free.id(), theirs.id());
    }

    @Test
    void excluirPropriedadeVinculadaMantemOProdutor() {
        Laboratory lab = lab("Lab Del");
        Property keep = property("Fica");
        Property gone = property("Sai");
        long id = producers.create(draft("Prod Del", lab.id(), Set.of(keep.id(), gone.id()))).producer().id();

        properties.delete(gone.id());

        assertThat(ownedBy(id)).containsExactly(keep.id());
        assertThatThrownBy(() -> properties.get(gone.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void propriedadeAtualizadaMantemOVinculo() {
        Laboratory lab = lab("Lab Upd");
        Property p = property("Antes");
        long id = producers.create(draft("Prod Upd", lab.id(), Set.of(p.id()))).producer().id();
        properties.update(p.id(), new Identity("Depois", p.identity().taxId(), "Rua"));
        assertThat(ownedBy(id)).containsExactly(p.id());
        assertThat(properties.get(p.id()).identity().name()).isEqualTo("Depois");
    }

    // ---- produtor (3.2)

    @Test
    void criacaoCompletaDevolveLaboratorioEPropriedades() {
        Laboratory lab = lab("Lab Completo");
        Property a = property("A");
        Property b = property("B");

        ProducerDetails created = producers.create(draft("Prod Completo", lab.id(), Set.of(a.id(), b.id())));
        ProducerDetails read = producers.get(created.producer().id());

        assertThat(read.laboratory().id()).isEqualTo(lab.id());
        assertThat(read.properties()).extracting(Property::id).containsExactlyInAnyOrder(a.id(), b.id());
    }

    @Test
    void laboratorioInexistenteNaoGravaNada() {
        Producer draft = draft("Prod Sem Lab", 999_999, Set.of());
        assertThatThrownBy(() -> producers.create(draft)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(countProducersWithTaxId(draft.identity().taxId().digits())).isZero();
    }

    @Test
    void propriedadeInexistenteNaoGravaNada() {
        Laboratory lab = lab("Lab Prop Inexistente");
        Producer draft = draft("Prod Prop Inexistente", lab.id(), Set.of(999_999L));
        assertThatThrownBy(() -> producers.create(draft)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(countProducersWithTaxId(draft.identity().taxId().digits())).isZero();
    }

    @Test
    void atualizarConjuntoDePropriedadesLiberaAMantemEVinculaC() {
        Laboratory lab = lab("Lab Conjunto");
        Property a = property("A");
        Property b = property("B");
        Property c = property("C");
        ProducerDetails created = producers.create(draft("Prod Conjunto", lab.id(), Set.of(a.id(), b.id())));
        long id = created.producer().id();

        producers.update(id, draft("Prod Conjunto", lab.id(), Set.of(b.id(), c.id())).update(
                created.producer().identity(), T0, T0.plusSeconds(1), "novo", lab.id(), Set.of(b.id(), c.id())));

        assertThat(ownedBy(id)).containsExactlyInAnyOrder(b.id(), c.id());
        assertThat(properties.get(a.id()).isAvailable()).isTrue();
        assertThat(producers.get(id).producer().observacoes()).isEqualTo("novo");
    }

    @Test
    void atualizarComConjuntoVazioLiberaTodas() {
        Laboratory lab = lab("Lab Vazio");
        Property a = property("A");
        ProducerDetails created = producers.create(draft("Prod Vazio", lab.id(), Set.of(a.id())));
        producers.update(created.producer().id(), created.producer().update(
                created.producer().identity(), T0, T0, null, lab.id(), Set.of()));
        assertThat(ownedBy(created.producer().id())).isEmpty();
        assertThat(properties.get(a.id()).isAvailable()).isTrue();
    }

    @Test
    void propriedadeDeOutroProdutorEhConflitoENadaMuda() {
        Laboratory lab = lab("Lab Alheia");
        Property a = property("A");
        Property b = property("B");
        Property alien = property("Alheia");
        long owner = producers.create(draft("Dono", lab.id(), Set.of(alien.id()))).producer().id();
        ProducerDetails target = producers.create(draft("Alvo", lab.id(), Set.of(a.id(), b.id())));
        long id = target.producer().id();

        Producer attempt = new Producer(null, target.producer().identity(), T0, T0, "mudou", lab.id(),
                Set.of(b.id(), alien.id()));
        assertThatThrownBy(() -> producers.update(id, attempt)).isInstanceOf(ConflictException.class);

        assertThat(ownedBy(id)).containsExactlyInAnyOrder(a.id(), b.id());
        assertThat(ownedBy(owner)).containsExactly(alien.id());
        assertThat(producers.get(id).producer().observacoes()).isNull();
    }

    @Test
    void criacaoComPropriedadeAlheiaEhConflitoENadaEhGravado() {
        Laboratory lab = lab("Lab Criacao Alheia");
        Property alien = property("Alheia");
        producers.create(draft("Dono", lab.id(), Set.of(alien.id())));
        Producer draft = draft("Intruso", lab.id(), Set.of(alien.id()));

        assertThatThrownBy(() -> producers.create(draft)).isInstanceOf(ConflictException.class);
        assertThat(countProducersWithTaxId(draft.identity().taxId().digits())).isZero();
    }

    @Test
    void atualizarProdutorInexistenteEhNaoEncontrado() {
        Laboratory lab = lab("Lab 404");
        assertThatThrownBy(() -> producers.update(999_999, draft("X", lab.id(), Set.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void excluirProdutorLiberaPropriedadesQueContinuamExistindo() {
        Laboratory lab = lab("Lab Excluir");
        Property a = property("A");
        Property b = property("B");
        long id = producers.create(draft("Prod Excluir", lab.id(), Set.of(a.id(), b.id()))).producer().id();

        producers.delete(id);

        assertThatThrownBy(() -> producers.get(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(properties.get(a.id()).isAvailable()).isTrue();
        assertThat(properties.get(b.id()).isAvailable()).isTrue();
        assertThatThrownBy(() -> producers.delete(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inscricaoDuplicadaDeProdutorEhConflito() {
        Laboratory lab = lab("Lab Dup Prod");
        ProducerDetails first = producers.create(draft("Primeiro", lab.id(), Set.of()));
        Producer dup = new Producer(null, new Identity("Segundo", first.producer().identity().taxId(), "Rua"), T0, T0,
                null, lab.id(), Set.of());
        assertThatThrownBy(() -> producers.create(dup)).isInstanceOf(ConflictException.class);
    }

    private int countProducersWithTaxId(String taxId) {
        return new JdbcTemplate(dataSource).queryForObject(
                "SELECT count(*) FROM producers WHERE tax_id = ?", Integer.class, taxId);
    }
}
