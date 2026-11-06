## Context

O motor fica no `application`, sem Spring, para ser testado em milissegundos e verificado pelo teste de arquitetura ([spec architecture-boundaries](../../specs/architecture-boundaries/spec.md)).

## Goals / Non-Goals

**Goals:**
- Regras isoladas, trocáveis e testáveis sem contexto.
- Taxas por configuração.

**Non-Goals:**
- Histórico de cálculos, impostos, multimoeda.

## Decisions

1. **Strategy por natureza** (`FinancialCalculator.supports()`), registro em `FinancialEngine`. Alternativa: `switch` no serviço (descartada: viola aberto/fechado).
2. **Entrada como `FinancialSubject`** (dias, nº de propriedades, nº de produtores), não a entidade: a calculadora não conhece o domínio persistido.
3. **Taxas em `FinancialRates`**, preenchidas de `@ConfigurationProperties` na camada de infraestrutura, que também instancia as calculadoras como beans.
4. **`BigDecimal` com escala 2 e `HALF_EVEN`**; nunca `double`.
5. **Fórmulas como premissa explícita**, registradas em ADR para validação posterior.

ADR a gerar: 0010 Motor financeiro como Strategy puro com taxas configuráveis.

## Risks / Trade-offs

- [Fórmulas diferentes das esperadas pelo negócio] → configuráveis e documentadas como suposição; trocar uma fórmula é trocar uma classe.
