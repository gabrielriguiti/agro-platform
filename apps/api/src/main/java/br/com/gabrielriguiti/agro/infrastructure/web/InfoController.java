package br.com.gabrielriguiti.agro.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Informações básicas da API; primeiro endpoint do contrato OpenAPI consumido pelo web. */
@RestController
@Tag(name = "Sistema")
public class InfoController {

    private final String name;

    InfoController(@Value("${spring.application.name}") String name) {
        this.name = name;
    }

    /**
     * Devolve o nome da aplicação.
     *
     * @return identificação da API
     */
    @GetMapping("/api/info")
    @Operation(summary = "Informações da API", description = "Nome da aplicação em execução.")
    public InfoResponse info() {
        return new InfoResponse(name);
    }

    /**
     * Corpo de {@code GET /api/info}.
     *
     * @param name nome da aplicação
     */
    @Schema(description = "Informações básicas da API")
    public record InfoResponse(@Schema(description = "Nome da aplicação", example = "agro-api") String name) {
    }
}
