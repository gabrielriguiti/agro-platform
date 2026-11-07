package br.com.gabrielriguiti.agro.infrastructure.web.integration;

import br.com.gabrielriguiti.agro.application.integration.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ingestão de lotes de integração. */
@RestController
@RequestMapping("/api/integration")
@Tag(name = "Integração", description = "Ingestão em lote e monitoramento da sincronização")
public class BatchIngestionController {

    private final IngestionService ingestion;

    public BatchIngestionController(IngestionService ingestion) {
        this.ingestion = ingestion;
    }

    /**
     * Recebe um lote (ou uma carga pontual, como lote de um item) e o grava na outbox, sem processar.
     *
     * @param request envelope do lote
     * @return 202 com o id do lote e as contagens de aceitos e duplicados
     */
    @PostMapping("/batches")
    @Operation(summary = "Ingere um lote",
            description = "Grava todas as mensagens na outbox em uma transação e responde 202; o processamento é assíncrono.")
    @ApiResponse(responseCode = "202", description = "Lote aceito")
    @ApiResponse(responseCode = "400", description = "Envelope inválido")
    @ApiResponse(responseCode = "413", description = "Lote acima do limite de itens")
    public ResponseEntity<BatchAcceptedResponse> ingest(@Valid @RequestBody BatchRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(BatchAcceptedResponse.from(ingestion.ingest(request.toCommand())));
    }
}
