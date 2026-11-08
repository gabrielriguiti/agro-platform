package br.com.gabrielriguiti.agro.infrastructure.web.integration;

import br.com.gabrielriguiti.agro.application.integration.IntegrationMonitoringService;
import br.com.gabrielriguiti.agro.application.registry.PageResult;
import br.com.gabrielriguiti.agro.domain.integration.MessageStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Monitoramento de lotes, mensagens e DLQ, e reprocessamento manual. */
@RestController
@RequestMapping("/api/integration")
@Tag(name = "Integração", description = "Ingestão em lote e monitoramento da sincronização")
public class IntegrationMonitoringController {

    /** Página de mensagens. */
    @Schema(description = "Página de mensagens")
    public record MessagePage(List<MessageResponse> content, int page, int size, long totalElements, int totalPages) {
    }

    /** Resultado de um ciclo manual do worker. */
    @Schema(description = "Resultado de um ciclo do worker")
    public record WorkerRunResponse(int processed) {
    }

    /** Resultado do reprocessamento de um lote. */
    @Schema(description = "Resultado do reprocessamento de um lote")
    public record ReprocessResponse(int requeued) {
    }

    private final IntegrationMonitoringService service;

    public IntegrationMonitoringController(IntegrationMonitoringService service) {
        this.service = service;
    }

    /** @return os lotes mais recentes com contagens por status e estado derivado */
    @GetMapping("/batches")
    @Operation(summary = "Lista os lotes mais recentes")
    public List<BatchResponse> batches(@RequestParam(defaultValue = "20") int limit) {
        return service.recentBatches(limit).stream().map(BatchResponse::from).toList();
    }

    /**
     * @param id identificador do lote
     * @return o resumo do lote
     */
    @GetMapping("/batches/{id}")
    @Operation(summary = "Detalha um lote")
    @ApiResponse(responseCode = "200", description = "Lote")
    @ApiResponse(responseCode = "404", description = "Lote inexistente")
    public BatchResponse batch(@PathVariable long id) {
        return BatchResponse.from(service.batch(id));
    }

    /**
     * @param status filtro de status (ex.: {@code DEAD} para a DLQ)
     * @param batchId filtro de lote
     * @param page página, a partir de 0
     * @param size itens por página (máximo 200)
     * @return mensagens paginadas, da mais recente para a mais antiga, sem o payload
     */
    @GetMapping("/messages")
    @Operation(summary = "Lista mensagens", description = "Filtráveis por status e lote; o payload fica no detalhe.")
    public MessagePage messages(
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(required = false) Long batchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<MessageResponse> result = service.messages(status, batchId, page, size).map(MessageResponse::from);
        return new MessagePage(result.content(), result.page(), result.size(), result.totalElements(),
                result.totalPages());
    }

    /**
     * @param id identificador da mensagem
     * @return a mensagem com o payload completo
     */
    @GetMapping("/messages/{id}")
    @Operation(summary = "Detalha uma mensagem", description = "Inclui o payload completo.")
    @ApiResponse(responseCode = "200", description = "Mensagem com o payload completo")
    @ApiResponse(responseCode = "404", description = "Mensagem inexistente")
    public MessageResponse message(@PathVariable long id) {
        return MessageResponse.from(service.message(id));
    }

    /**
     * Reenfileira uma mensagem morta.
     *
     * @param id identificador da mensagem
     * @return a mensagem de volta em {@code PENDING}
     */
    @PostMapping("/messages/{id}/reprocess")
    @Operation(summary = "Reprocessa uma mensagem da DLQ")
    @ApiResponse(responseCode = "200", description = "Mensagem recolocada na fila")
    @ApiResponse(responseCode = "404", description = "Mensagem inexistente")
    @ApiResponse(responseCode = "409", description = "A mensagem não está em DEAD")
    public MessageResponse reprocessMessage(@PathVariable long id) {
        return MessageResponse.from(service.reprocessMessage(id));
    }

    /**
     * Reenfileira todas as mensagens mortas do lote.
     *
     * @param id identificador do lote
     * @return quantas mensagens foram reenfileiradas
     */
    @PostMapping("/batches/{id}/reprocess")
    @Operation(summary = "Reprocessa as mensagens mortas de um lote")
    @ApiResponse(responseCode = "200", description = "Quantidade de mensagens recolocadas na fila")
    @ApiResponse(responseCode = "404", description = "Lote inexistente")
    public ReprocessResponse reprocessBatch(@PathVariable long id) {
        return new ReprocessResponse(service.reprocessBatch(id));
    }

    /**
     * Processa um ciclo do worker (útil em demonstrações e testes).
     *
     * @return quantas mensagens foram tratadas
     */
    @PostMapping("/worker/run")
    @Operation(summary = "Executa um ciclo do worker",
            description = "Disponível apenas com agro.integration.manual-run-enabled=true; caso contrário, 404.")
    @ApiResponse(responseCode = "404", description = "Execução manual desabilitada")
    public WorkerRunResponse runWorker() {
        return new WorkerRunResponse(service.runWorkerOnce());
    }
}
