package br.com.gabrielriguiti.agro.infrastructure.web.financial;

import br.com.gabrielriguiti.agro.application.financial.FinancialComponent;
import br.com.gabrielriguiti.agro.application.financial.FinancialResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * Valor financeiro calculado.
 *
 * @param valor total em BRL, com duas casas decimais
 * @param natureza {@code PRODUCER}, {@code PROPERTY} ou {@code LABORATORY}
 * @param componentes parcelas cuja soma é o valor
 */
@Schema(description = "Valor financeiro calculado e as parcelas usadas")
public record FinancialResponse(
        @Schema(example = "700.00") BigDecimal valor,
        @Schema(example = "PRODUCER") String natureza,
        List<Componente> componentes) {

    /**
     * @param nome identificador da parcela
     * @param quantidade quantidade multiplicada
     * @param valorUnitario valor unitário em BRL
     * @param subtotal quantidade × valor unitário
     */
    public record Componente(String nome, long quantidade, BigDecimal valorUnitario, BigDecimal subtotal) {

        static Componente from(FinancialComponent c) {
            return new Componente(c.name(), c.quantity(), c.unitValue(), c.subtotal());
        }
    }

    /** @return resposta da API a partir do resultado do motor */
    public static FinancialResponse from(FinancialResult result) {
        return new FinancialResponse(result.value(), result.nature().name(),
                result.components().stream().map(Componente::from).toList());
    }
}
