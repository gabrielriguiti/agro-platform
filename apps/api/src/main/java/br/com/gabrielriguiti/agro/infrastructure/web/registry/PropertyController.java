package br.com.gabrielriguiti.agro.infrastructure.web.registry;

import br.com.gabrielriguiti.agro.application.registry.PropertyService;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** API de propriedades. */
@RestController
@RequestMapping("/api/properties")
@Tag(name = "Propriedades", description = "Cadastro de propriedades e consulta das disponíveis para vínculo")
public class PropertyController {

    private final PropertyService service;

    public PropertyController(PropertyService service) {
        this.service = service;
    }

    /**
     * Cria uma propriedade sem produtor.
     *
     * @param request dados cadastrais
     * @return 201 com {@code Location} e a propriedade criada
     */
    @PostMapping
    @Operation(summary = "Cria uma propriedade", description = "Nasce sem produtor (disponível).")
    @ApiResponse(responseCode = "201", description = "Criada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "409", description = "Inscrição fiscal já cadastrada")
    public ResponseEntity<PropertyResponse> create(@Valid @RequestBody IdentityRequest request) {
        Property created = service.create(request.toIdentity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(PropertyResponse.from(created));
    }

    /**
     * @param disponiveis {@code true} para devolver apenas as propriedades sem produtor
     * @param produtorId produtor em edição: com {@code disponiveis=true}, suas propriedades também são devolvidas
     * @return propriedades ordenadas por nome
     */
    @GetMapping
    @Operation(summary = "Lista propriedades",
            description = "Com disponiveis=true devolve só as sem produtor, mais as do produtor em edição.")
    public List<PropertyResponse> list(
            @Parameter(description = "Somente propriedades disponíveis") @RequestParam(defaultValue = "false")
            boolean disponiveis,
            @Parameter(description = "Produtor em edição") @RequestParam(required = false) Long produtorId) {
        return service.list(disponiveis, produtorId).stream().map(PropertyResponse::from).toList();
    }

    /**
     * @param id identificador da propriedade
     * @return a propriedade
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma propriedade")
    @ApiResponse(responseCode = "404", description = "Propriedade não encontrada")
    public PropertyResponse get(@PathVariable long id) {
        return PropertyResponse.from(service.get(id));
    }

    /**
     * @param id identificador da propriedade
     * @param request novos dados cadastrais (o vínculo com o produtor não muda)
     * @return a propriedade atualizada
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma propriedade", description = "O vínculo com o produtor não é alterado aqui.")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Propriedade não encontrada")
    @ApiResponse(responseCode = "409", description = "Inscrição fiscal pertence a outra propriedade")
    public PropertyResponse update(@PathVariable long id, @Valid @RequestBody IdentityRequest request) {
        return PropertyResponse.from(service.update(id, request.toIdentity()));
    }

    /**
     * @param id identificador da propriedade
     * @return 204 quando excluída (o produtor continua existindo sem ela)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma propriedade", description = "Desvincula do produtor sem afetar o restante.")
    @ApiResponse(responseCode = "204", description = "Excluída")
    @ApiResponse(responseCode = "404", description = "Propriedade não encontrada")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
