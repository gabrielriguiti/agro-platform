## Why

O enunciado pede um serviço extensível e desacoplado de cálculo financeiro conforme a natureza da entidade: comissões de produção para o produtor e taxas operacionais e territoriais para propriedade e laboratório. O valor consolidado do laboratório alimenta a consulta analítica.

## What Changes

- Motor financeiro com uma calculadora (Strategy) por natureza de entidade, sem dependência de Spring.
- Taxas configuráveis por propriedades da aplicação, com valores padrão documentados.
- Endpoint para consultar o valor calculado de um produtor, propriedade ou laboratório.

## Capabilities

### New Capabilities
- `financial-calculation`: cálculo por natureza, fórmulas configuráveis, arredondamento e extensibilidade.

### Modified Capabilities

## Impact

Pacote `application.financial` (puro) e configuração em `infrastructure.config`. Consumido por `lab-analytics`. Depende de `registry`.

**Premissa:** o enunciado não define as fórmulas. Adotamos fórmulas simples e explícitas, configuráveis, registradas em ADR como suposição a validar com o negócio:
- Produtor: `diasDeVigência × comissãoDiária + propriedades × comissãoPorPropriedade`.
- Propriedade: `taxaOperacional + taxaTerritorial`.
- Laboratório: `taxaOperacional + produtores × taxaTerritorialPorProdutor`.

**Não objetivos:** persistir valores calculados, moedas diferentes de BRL, impostos.

## Documentação

ADR previsto: "Motor financeiro como Strategy puro com taxas configuráveis". Tabela de fórmulas e exemplo numérico no README.
