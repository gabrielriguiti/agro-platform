# 0010. Motor financeiro como Strategy puro com taxas configuráveis

- Status: aceito
- Data: 2026-11-06
- Decisores: Gabriel
- Change relacionado: `openspec/changes/financial-engine`

## Contexto

O enunciado pede um serviço de cálculo financeiro extensível e desacoplado, que varia conforme a natureza da entidade (comissão de produção para o produtor; taxas operacional e territorial para propriedade e laboratório). O enunciado **não define as fórmulas**. O valor do laboratório alimenta a consulta analítica (change `lab-analytics`).

## Opções consideradas

1. **`switch` por natureza dentro do serviço:** simples, mas cada nova regra altera código existente (viola aberto/fechado) e mistura cálculo com acesso a dados.
2. **Strategy por natureza, sem Spring, com entrada desacoplada do domínio (escolhida):** cada regra é uma classe, o motor só escolhe, e tudo é testável em milissegundos.
3. **Regras em tabela ou DSL no banco:** flexível, mas é complexidade sem demanda e dificulta o teste.

## Decisão

O motor vive em `application.financial`, sem Spring (verificado pelo ArchUnit). `FinancialCalculator` (`supports` + `calculate`) tem uma implementação por natureza; `FinancialEngine` escolhe a primeira que suporta a natureza e falha com `UnsupportedFinancialNatureException` se não houver. A entrada é `FinancialSubject` (dias de vigência, nº de propriedades, nº de produtores), nunca a entidade. As taxas ficam em `FinancialRates`, preenchidas de `agro.financial.*` (`@ConfigurationProperties` em `infrastructure.config`, que também cria os beans). Valores são `BigDecimal` com escala 2 e `HALF_EVEN`.

**Fórmulas (premissa a validar com o negócio):**

| Natureza | Fórmula | Padrões (BRL) |
|---|---|---|
| Produtor | `diasDeVigência × comissãoDiária + propriedades × comissãoPorPropriedade` | 120,00 por dia; 50,00 por propriedade |
| Propriedade | `taxaOperacional + taxaTerritorial` | 300,00; 150,00 |
| Laboratório | `taxaOperacional + produtores × taxaTerritorialPorProdutor` | 500,00; 25,00 por produtor |

Dias de vigência = intervalo entre `dataInicial` e `dataFinal` arredondado para cima em dias inteiros, mínimo 1.

## Consequências

- Positivas: regras isoladas e trocáveis (mudar uma fórmula é trocar uma classe); taxas alteráveis sem recompilar; sem `double` nem erro de arredondamento.
- Negativas e riscos: as fórmulas são suposição e podem diferir do esperado pelo negócio; nada é persistido (cada consulta recalcula), o que é aceitável pelo volume e evita valores desatualizados.
- O que precisa mudar na documentação ou nas specs: o README traz a tabela de fórmulas e um exemplo numérico; se o negócio corrigir uma fórmula, registrar um novo ADR que substitua este.
