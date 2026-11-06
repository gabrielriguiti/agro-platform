package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.AbstractIntegrationTest;
import br.com.gabrielriguiti.agro.TaxIds;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Verifica os endpoints do cadastro e o formato {@code ProblemDetail} (task 3.3). */
class RegistryApiIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;

    @LocalServerPort int serverPort;

    private static Map<String, Object> identity(String name, String taxId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nome", name);
        body.put("inscricaoFiscal", taxId);
        body.put("endereco", "Rua " + name);
        return body;
    }

    private ResponseEntity<JsonNode> post(String path, Object body) {
        return rest.postForEntity(path, body, JsonNode.class);
    }

    private ResponseEntity<JsonNode> put(String path, Object body) {
        return rest.exchange(path, HttpMethod.PUT, new HttpEntity<>(body), JsonNode.class);
    }

    private ResponseEntity<JsonNode> delete(String path) {
        return rest.exchange(path, HttpMethod.DELETE, null, JsonNode.class);
    }

    private long newLab(String name) {
        ResponseEntity<JsonNode> r = post("/api/laboratories", identity(name, TaxIds.cnpj()));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().get("id").asLong();
    }

    private long newProperty(String name) {
        ResponseEntity<JsonNode> r = post("/api/properties", identity(name, TaxIds.cpf()));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().get("id").asLong();
    }

    private Map<String, Object> producerBody(String name, long labId, List<Long> propertyIds) {
        Map<String, Object> body = identity(name, TaxIds.cpf());
        body.put("dataInicial", "2025-01-01T00:00:00Z");
        body.put("dataFinal", "2025-12-31T23:59:59Z");
        body.put("observacoes", "obs");
        body.put("laboratorioId", labId);
        body.put("propriedadeIds", propertyIds);
        return body;
    }

    private long newProducer(String name, long labId, List<Long> propertyIds) {
        ResponseEntity<JsonNode> r = post("/api/producers", producerBody(name, labId, propertyIds));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody().get("id").asLong();
    }

    private static List<Long> ids(JsonNode array, String field) {
        List<Long> out = new ArrayList<>();
        array.forEach(n -> out.add(n.get(field).asLong()));
        return out;
    }

    // ---- laboratório

    @Test
    void criaLaboratorioComLocationEMascaraNaSaida() {
        ResponseEntity<JsonNode> r = post("/api/laboratories", identity("Lab API", "11.222.333/0001-81"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = r.getBody().get("id").asLong();
        assertThat(r.getHeaders().getLocation()).isEqualTo(URI.create("http://localhost:" + serverPort + "/api/laboratories/" + id));
        assertThat(r.getBody().get("inscricaoFiscal").asText()).isEqualTo("11.222.333/0001-81");

        ResponseEntity<JsonNode> get = rest.getForEntity("/api/laboratories/" + id, JsonNode.class);
        assertThat(get.getBody().get("inscricaoFiscal").asText()).isEqualTo("11.222.333/0001-81");
    }

    @Test
    void enderecoAusenteApontaOCampoEmProblemJson() {
        Map<String, Object> body = identity("Sem endereço", TaxIds.cnpj());
        body.remove("endereco");
        ResponseEntity<JsonNode> r = post("/api/laboratories", body);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType().toString()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(r.getBody().get("status").asInt()).isEqualTo(400);
        assertThat(r.getBody().get("title").asText()).isNotBlank();
        assertThat(r.getBody().get("detail").asText()).isNotBlank();
        assertThat(fields(r.getBody())).containsExactly("endereco");
    }

    private static List<String> fields(JsonNode problem) {
        List<String> out = new ArrayList<>();
        problem.get("errors").forEach(n -> out.add(n.get("field").asText()));
        return out;
    }

    @Test
    void laboratorioComCpfEhRejeitadoPeloDominio() {
        ResponseEntity<JsonNode> r = post("/api/laboratories", identity("Lab CPF", TaxIds.cpf()));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(fields(r.getBody())).containsExactly("inscricaoFiscal");
    }

    @Test
    void laboratorioInexistenteResponde404() {
        ResponseEntity<JsonNode> r = rest.getForEntity("/api/laboratories/999999", JsonNode.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getHeaders().getContentType().toString()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(r.getBody().get("status").asInt()).isEqualTo(404);
    }

    @Test
    void cnpjDuplicadoResponde409() {
        String cnpj = TaxIds.cnpj();
        assertThat(post("/api/laboratories", identity("Lab 1", cnpj)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<JsonNode> dup = post("/api/laboratories", identity("Lab 2", cnpj));
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(dup.getBody().get("status").asInt()).isEqualTo(409);
    }

    @Test
    void cnpjMascaradoEDigitosSaoOMesmoRegistro() {
        String digits = TaxIds.cnpj();
        String masked = digits.substring(0, 2) + "." + digits.substring(2, 5) + "." + digits.substring(5, 8) + "/"
                + digits.substring(8, 12) + "-" + digits.substring(12);
        assertThat(post("/api/laboratories", identity("Lab M", masked)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(post("/api/laboratories", identity("Lab D", digits)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void atualizaELista() {
        long id = newLab("Zz Lab Antigo");
        ResponseEntity<JsonNode> updated = put("/api/laboratories/" + id, identity("Aa Lab Novo", TaxIds.cnpj()));
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("nome").asText()).isEqualTo("Aa Lab Novo");
        JsonNode list = rest.getForEntity("/api/laboratories", JsonNode.class).getBody();
        assertThat(ids(list, "id")).contains(id);
        List<String> names = new ArrayList<>();
        list.forEach(n -> names.add(n.get("nome").asText()));
        assertThat(names).isSorted();
    }

    @Test
    void laboratorioComProdutoresNaoEhExcluido() {
        long lab = newLab("Lab Excl");
        for (int i = 0; i < 3; i++) {
            newProducer("Prod Excl " + i, lab, List.of());
        }
        assertThat(delete("/api/laboratories/" + lab).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(rest.getForEntity("/api/laboratories/" + lab, JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void laboratorioSemProdutoresEhExcluidoCom204() {
        long lab = newLab("Lab Livre");
        assertThat(delete("/api/laboratories/" + lab).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.getForEntity("/api/laboratories/" + lab, JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- propriedade

    @Test
    void propriedadeDisponivelEFiltroPorProdutorEmEdicao() {
        long lab = newLab("Lab Filtro");
        long mine = newProperty("Minha");
        long free = newProperty("Livre");
        long theirs = newProperty("Alheia");
        long seven = newProducer("Produtor 7", lab, List.of(mine));
        newProducer("Produtor 8", lab, List.of(theirs));

        JsonNode filtered = rest.getForEntity(
                "/api/properties?disponiveis=true&produtorId=" + seven, JsonNode.class).getBody();
        assertThat(ids(filtered, "id")).contains(mine, free).doesNotContain(theirs);

        JsonNode onlyFree = rest.getForEntity("/api/properties?disponiveis=true", JsonNode.class).getBody();
        assertThat(ids(onlyFree, "id")).contains(free).doesNotContain(mine, theirs);

        JsonNode all = rest.getForEntity("/api/properties", JsonNode.class).getBody();
        assertThat(ids(all, "id")).contains(mine, free, theirs);
    }

    @Test
    void propriedadeNovaTemProdutorNuloEExcluirDesvincula() {
        long lab = newLab("Lab Prop");
        long prop = newProperty("Vai sair");
        long keep = newProperty("Fica");
        long producer = newProducer("Dono", lab, List.of(prop, keep));

        JsonNode created = rest.getForEntity("/api/properties/" + keep, JsonNode.class).getBody();
        assertThat(created.get("produtorId").asLong()).isEqualTo(producer);

        assertThat(delete("/api/properties/" + prop).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        JsonNode read = rest.getForEntity("/api/producers/" + producer, JsonNode.class).getBody();
        assertThat(ids(read.get("propriedades"), "id")).containsExactly(keep);
    }

    @Test
    void propriedadeAutonomaTemProdutorNulo() {
        long prop = newProperty("Autônoma");
        JsonNode read = rest.getForEntity("/api/properties/" + prop, JsonNode.class).getBody();
        assertThat(read.get("produtorId").isNull()).isTrue();
    }

    // ---- produtor

    @Test
    void criacaoCompletaDevolveContratoDoEnunciado() {
        long lab = newLab("Lab Contrato");
        long a = newProperty("Prop A");
        long b = newProperty("Prop B");

        ResponseEntity<JsonNode> r = post("/api/producers", producerBody("Prod Contrato", lab, List.of(a, b)));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = r.getBody().get("id").asLong();
        assertThat(r.getHeaders().getLocation().getPath()).isEqualTo("/api/producers/" + id);

        JsonNode read = rest.getForEntity("/api/producers/" + id, JsonNode.class).getBody();
        assertThat(read.get("laboratorio").get("id").asLong()).isEqualTo(lab);
        assertThat(read.get("laboratorio").get("inscricaoFiscal").asText()).matches("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}");
        assertThat(ids(read.get("propriedades"), "id")).containsExactlyInAnyOrder(a, b);
        assertThat(read.get("propriedades").get(0).get("inscricaoFiscal").asText()).matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
        assertThat(read.get("dataInicial").asText()).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(read.get("inscricaoFiscal").asText()).matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
        assertThat(read.get("observacoes").asText()).isEqualTo("obs");
    }

    @Test
    void semLaboratorioResponde400() {
        Map<String, Object> body = producerBody("Sem Lab", 1, List.of());
        body.remove("laboratorioId");
        ResponseEntity<JsonNode> r = post("/api/producers", body);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(fields(r.getBody())).containsExactly("laboratorioId");
    }

    @Test
    void laboratorioInexistenteResponde404ENadaEhGravado() {
        Map<String, Object> body = producerBody("Lab Fantasma", 999_999, List.of());
        ResponseEntity<JsonNode> r = post("/api/producers", body);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        long lab = newLab("Lab Real");
        body.put("laboratorioId", lab);
        assertThat(post("/api/producers", body).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void periodoInvertidoResponde400ComMensagemClara() {
        long lab = newLab("Lab Período");
        Map<String, Object> body = producerBody("Período", lab, List.of());
        body.put("dataInicial", "2025-12-31T00:00:00Z");
        body.put("dataFinal", "2025-01-01T00:00:00Z");
        ResponseEntity<JsonNode> r = post("/api/producers", body);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("detail").asText()).contains("dataFinal").contains("dataInicial");
        assertThat(fields(r.getBody())).containsExactly("dataFinal");
    }

    @Test
    void dataForaDeIso8601Responde400() {
        long lab = newLab("Lab Data");
        Map<String, Object> body = producerBody("Data ruim", lab, List.of());
        body.put("dataInicial", "ontem");
        ResponseEntity<JsonNode> r = post("/api/producers", body);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType().toString()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }

    @Test
    void validacaoListaTodosOsCamposInvalidos() {
        long lab = newLab("Lab Multi");
        Map<String, Object> body = producerBody("", lab, List.of());
        body.put("inscricaoFiscal", "123.456.789-00");
        ResponseEntity<JsonNode> r = post("/api/producers", body);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(fields(r.getBody())).containsExactlyInAnyOrder("nome", "inscricaoFiscal");
        assertThat(r.getBody().get("errors").toString()).contains("CPF/CNPJ inválido");
    }

    @Test
    void atualizacaoSubstituiConjuntoEPropriedadeAlheiaEh409() {
        long lab = newLab("Lab Upd");
        long a = newProperty("A");
        long b = newProperty("B");
        long c = newProperty("C");
        long alien = newProperty("Alheia");
        long producer = newProducer("Alvo", lab, List.of(a, b));
        newProducer("Dono", lab, List.of(alien));

        Map<String, Object> body = producerBody("Alvo editado", lab, List.of(b, c));
        ResponseEntity<JsonNode> ok = put("/api/producers/" + producer, body);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(ok.getBody().get("propriedades"), "id")).containsExactlyInAnyOrder(b, c);
        assertThat(rest.getForEntity("/api/properties/" + a, JsonNode.class).getBody().get("produtorId").isNull()).isTrue();

        body.put("propriedadeIds", List.of(b, alien));
        body.put("nome", "Não deve gravar");
        ResponseEntity<JsonNode> conflict = put("/api/producers/" + producer, body);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode after = rest.getForEntity("/api/producers/" + producer, JsonNode.class).getBody();
        assertThat(after.get("nome").asText()).isEqualTo("Alvo editado");
        assertThat(ids(after.get("propriedades"), "id")).containsExactlyInAnyOrder(b, c);
    }

    @Test
    void produtorInexistenteResponde404() {
        long lab = newLab("Lab 404");
        assertThat(rest.getForEntity("/api/producers/999999", JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(put("/api/producers/999999", producerBody("X", lab, List.of())).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(delete("/api/producers/999999").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void excluirProdutorMantemPropriedadesLivres() {
        long lab = newLab("Lab Del");
        long a = newProperty("A");
        long b = newProperty("B");
        long producer = newProducer("Prod Del", lab, List.of(a, b));

        assertThat(delete("/api/producers/" + producer).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.getForEntity("/api/producers/" + producer, JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        for (long id : new long[] {a, b}) {
            JsonNode prop = rest.getForEntity("/api/properties/" + id, JsonNode.class).getBody();
            assertThat(prop.get("produtorId").isNull()).isTrue();
        }
    }

    @Test
    void listagemPaginadaDevolveMetadadosEConteudoCompleto() {
        long lab = newLab("Lab Grid");
        for (int i = 0; i < 5; i++) {
            long p = newProperty("Grid prop " + i);
            newProducer("Grid prod %02d".formatted(i), lab, List.of(p));
        }
        JsonNode page = rest.getForEntity("/api/producers?page=0&size=2", JsonNode.class).getBody();
        assertThat(page.get("content")).hasSize(2);
        assertThat(page.get("size").asInt()).isEqualTo(2);
        assertThat(page.get("page").asInt()).isZero();
        assertThat(page.get("totalElements").asLong()).isGreaterThanOrEqualTo(5);
        assertThat(page.get("totalPages").asInt()).isGreaterThanOrEqualTo(3);
        JsonNode first = page.get("content").get(0);
        assertThat(first.get("laboratorio").get("id").asLong()).isPositive();
        assertThat(first.get("propriedades").isArray()).isTrue();
    }

    @Test
    void parametrosDePaginaInvalidosResponde400() {
        ResponseEntity<JsonNode> r = rest.getForEntity("/api/producers?size=0", JsonNode.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getHeaders().getContentType().toString()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(rest.getForEntity("/api/producers?size=101", JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rest.getForEntity("/api/producers?page=-1", JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rest.getForEntity("/api/producers?page=abc", JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void openApiDocumentaOsEndpointsDoCadastro() {
        String docs = rest.getForObject("/v3/api-docs", String.class);
        assertThat(docs).contains("/api/producers", "/api/laboratories", "/api/properties", "/api/producers/{id}");
    }
}
