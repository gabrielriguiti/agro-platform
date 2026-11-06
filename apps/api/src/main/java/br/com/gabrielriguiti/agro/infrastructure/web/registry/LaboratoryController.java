package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import br.com.gabrielriguiti.agro.application.registry.LaboratoryService;
import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** API de laboratórios. */
@RestController
@RequestMapping("/api/laboratories")
@Tag(name = "Laboratórios", description = "Cadastro de laboratórios")
public class LaboratoryController {

    private final LaboratoryService service;

    public LaboratoryController(LaboratoryService service) {
        this.service = service;
    }

    /**
     * Cria um laboratório.
     *
     * @param request dados cadastrais (inscrição deve ser um CNPJ)
     * @return 201 com {@code Location} e o laboratório criado
     */
    @PostMapping
    @Operation(summary = "Cria um laboratório", description = "Exige nome, CNPJ válido e endereço.")
    @ApiResponse(responseCode = "201", description = "Criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado")
    public ResponseEntity<LaboratoryResponse> create(@Valid @RequestBody IdentityRequest request) {
        Laboratory created = service.create(request.toIdentity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(LaboratoryResponse.from(created));
    }

    /** @return todos os laboratórios, ordenados por nome */
    @GetMapping
    @Operation(summary = "Lista os laboratórios", description = "Ordenados por nome.")
    public List<LaboratoryResponse> list() {
        return service.list().stream().map(LaboratoryResponse::from).toList();
    }

    /**
     * @param id identificador do laboratório
     * @return o laboratório
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consulta um laboratório")
    @ApiResponse(responseCode = "404", description = "Laboratório não encontrado")
    public LaboratoryResponse get(@PathVariable long id) {
        return LaboratoryResponse.from(service.get(id));
    }

    /**
     * @param id identificador do laboratório
     * @param request novos dados cadastrais
     * @return o laboratório atualizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um laboratório")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Laboratório não encontrado")
    @ApiResponse(responseCode = "409", description = "CNPJ pertence a outro laboratório")
    public LaboratoryResponse update(@PathVariable long id, @Valid @RequestBody IdentityRequest request) {
        return LaboratoryResponse.from(service.update(id, request.toIdentity()));
    }

    /**
     * @param id identificador do laboratório
     * @return 204 quando excluído
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um laboratório", description = "Só é possível sem produtores vinculados.")
    @ApiResponse(responseCode = "204", description = "Excluído")
    @ApiResponse(responseCode = "404", description = "Laboratório não encontrado")
    @ApiResponse(responseCode = "409", description = "Há produtores vinculados")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
