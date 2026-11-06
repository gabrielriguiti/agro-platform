package br.com.gabrielriguiti.agro.infrastructure.web.financial;

import br.com.gabrielriguiti.agro.application.financial.FinancialQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de consulta do valor financeiro calculado (nada é persistido). */
@RestController
@RequestMapping("/api/financial")
@Tag(name = "Financeiro", description = "Valor calculado por natureza: produtor, propriedade e laboratório")
public class FinancialController {

    private final FinancialQueryService service;

    public FinancialController(FinancialQueryService service) {
        this.service = service;
    }

    /**
     * @param id identificador do produtor
     * @return comissão de produção com as parcelas
     */
    @GetMapping("/producers/{id}")
    @Operation(summary = "Valor financeiro de um produtor",
            description = "Dias de vigência × comissão diária + propriedades × comissão por propriedade.")
    @ApiResponse(responseCode = "404", description = "Produtor não encontrado")
    public FinancialResponse producer(@PathVariable long id) {
        return FinancialResponse.from(service.producer(id));
    }

    /**
     * @param id identificador da propriedade
     * @return taxas operacional e territorial com as parcelas
     */
    @GetMapping("/properties/{id}")
    @Operation(summary = "Valor financeiro de uma propriedade", description = "Taxa operacional + taxa territorial.")
    @ApiResponse(responseCode = "404", description = "Propriedade não encontrada")
    public FinancialResponse property(@PathVariable long id) {
        return FinancialResponse.from(service.property(id));
    }

    /**
     * @param id identificador do laboratório
     * @return taxas do laboratório com as parcelas
     */
    @GetMapping("/laboratories/{id}")
    @Operation(summary = "Valor financeiro de um laboratório",
            description = "Taxa operacional + produtores × taxa territorial por produtor.")
    @ApiResponse(responseCode = "404", description = "Laboratório não encontrado")
    public FinancialResponse laboratory(@PathVariable long id) {
        return FinancialResponse.from(service.laboratory(id));
    }
}
