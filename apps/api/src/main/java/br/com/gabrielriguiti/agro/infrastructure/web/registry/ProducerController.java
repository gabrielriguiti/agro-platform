package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import br.com.gabrielriguiti.agro.application.registry.ProducerDetails;
import br.com.gabrielriguiti.agro.application.registry.ProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** API de produtores. */
@RestController
@RequestMapping("/api/producers")
@Tag(name = "Produtores", description = "Cadastro de produtores com laboratório e propriedades")
public class ProducerController {

    private final ProducerService service;

    public ProducerController(ProducerService service) {
        this.service = service;
    }

    /**
     * Cria um produtor, vinculado a um laboratório existente e a propriedades livres.
     *
     * @param request dados do produtor
     * @return 201 com {@code Location} e o produtor criado
     */
    @PostMapping
    @Operation(summary = "Cria um produtor",
            description = "Exige laboratório existente; as propriedades informadas devem estar livres.")
    @ApiResponse(responseCode = "201", description = "Criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Laboratório ou propriedade inexistente")
    @ApiResponse(responseCode = "409", description = "Inscrição duplicada ou propriedade de outro produtor")
    public ResponseEntity<ProducerResponse> create(@Valid @RequestBody ProducerRequest request) {
        ProducerDetails created = service.create(request.toDomain());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.producer().id()).toUri();
        return ResponseEntity.created(location).body(ProducerResponse.from(created));
    }

    /**
     * @param page número da página, a partir de 0
     * @param size itens por página (1 a 100)
     * @return página de produtores com laboratório e propriedades, ordenados por nome
     */
    @GetMapping
    @Operation(summary = "Lista produtores (paginado)",
            description = "Carrega laboratórios e propriedades em número constante de consultas.")
    public PageResponse<ProducerResponse> list(
            @Parameter(description = "Página, a partir de 0") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page deve ser maior ou igual a 0") int page,
            @Parameter(description = "Itens por página (1 a 100)") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size deve ser no mínimo 1")
            @Max(value = 100, message = "size deve ser no máximo 100") int size) {
        return PageResponse.from(service.list(page, size).map(ProducerResponse::from));
    }

    /**
     * @param id identificador do produtor
     * @return o produtor com laboratório e propriedades embutidos
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consulta um produtor", description = "Devolve laboratório e propriedades embutidos.")
    @ApiResponse(responseCode = "200", description = "Produtor com laboratório e propriedades")
    @ApiResponse(responseCode = "404", description = "Produtor não encontrado")
    public ProducerResponse get(@PathVariable long id) {
        return ProducerResponse.from(service.get(id));
    }

    /**
     * Atualiza o produtor; {@code propriedadeIds} substitui o conjunto anterior.
     *
     * @param id identificador do produtor
     * @param request novos dados
     * @return o produtor atualizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um produtor",
            description = "O conjunto de propriedades informado substitui o anterior; as removidas ficam livres.")
    @ApiResponse(responseCode = "200", description = "Produtor atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Produtor, laboratório ou propriedade inexistente")
    @ApiResponse(responseCode = "409", description = "Inscrição duplicada ou propriedade de outro produtor")
    public ProducerResponse update(@PathVariable long id, @Valid @RequestBody ProducerRequest request) {
        return ProducerResponse.from(service.update(id, request.toDomain()));
    }

    /**
     * @param id identificador do produtor
     * @return 204 quando excluído (as propriedades continuam cadastradas e livres)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um produtor", description = "Suas propriedades ficam livres.")
    @ApiResponse(responseCode = "204", description = "Excluído")
    @ApiResponse(responseCode = "404", description = "Produtor não encontrado")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
