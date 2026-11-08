package br.com.gabrielriguiti.agro.infrastructure.web.analytics;

import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsFilter;
import br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API da consulta analítica de laboratórios (somente leitura). */
@RestController
@RequestMapping("/api/laboratories/analytics")
@Tag(name = "Análise de laboratórios", description = "Comparativo de laboratórios por produtores e valor financeiro")
public class LabAnalyticsController {

    private final LabAnalyticsService service;

    public LabAnalyticsController(LabAnalyticsService service) {
        this.service = service;
    }

    /**
     * @param qtdMinimaProdutores quantidade mínima de produtores (obrigatório, maior ou igual a 1)
     * @param dataInicialInicio limite inferior inclusivo de {@code dataInicial} do produtor
     * @param dataInicialFim limite superior inclusivo de {@code dataInicial} do produtor
     * @param dataFinalInicio limite inferior inclusivo de {@code dataFinal} do produtor
     * @param dataFinalFim limite superior inclusivo de {@code dataFinal} do produtor
     * @param buscaTexto trecho buscado, sem distinção de caixa, em {@code observacoes} do produtor
     * @return laboratórios ordenados por total decrescente (e menor data inicial, quando há filtro de data inicial)
     */
    @GetMapping
    @Operation(summary = "Analítico de laboratórios",
            description = "Os filtros incidem sobre os produtores: totalProdutores conta só os que atendem a todos "
                    + "e qtdMinimaProdutores é aplicado a essa contagem. Datas em ISO-8601 (instante UTC).")
    @ApiResponse(responseCode = "200", description = "Laboratórios com o total de produtores e o valor consolidado")
    @ApiResponse(responseCode = "400", description = "Parâmetro ausente, fora do intervalo ou intervalo invertido")
    public List<LabAnalyticsResponse> analytics(
            @Parameter(description = "Quantidade mínima de produtores filtrados", required = true, example = "2")
            @RequestParam @Min(value = 1, message = "deve ser maior ou igual a 1") int qtdMinimaProdutores,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicialInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicialFim,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFinalInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFinalFim,
            @Parameter(description = "Busca parcial e literal (% _ \\ não são curingas) em observacoes")
            @RequestParam(required = false) String buscaTexto) {
        LabAnalyticsFilter filter = new LabAnalyticsFilter(qtdMinimaProdutores, dataInicialInicio, dataInicialFim,
                dataFinalInicio, dataFinalFim, buscaTexto);
        return service.query(filter).stream().map(LabAnalyticsResponse::from).toList();
    }
}
