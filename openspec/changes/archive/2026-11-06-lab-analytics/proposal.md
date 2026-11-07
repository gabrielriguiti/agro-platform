## Why

A área operacional precisa comparar laboratórios: quantos produtores atendem, desde quando, e quanto representam financeiramente. A consulta agrega dados de produtores por laboratório com filtros de período e texto, e precisa ser eficiente.

## What Changes

- `GET /api/laboratories/analytics` com filtros opcionais de `dataInicial`, `dataFinal` e texto em `observacoes`, e `qtdMinimaProdutores` obrigatório.
- Resposta com `id`, `nome` em CAIXA ALTA, `totalProdutores` e `valorFinanceiroConsolidado`.
- Ordenação por `totalProdutores` decrescente e, condicionalmente, pela menor `dataInicial` crescente.
- Consulta única com `GROUP BY`/`HAVING`, sem N+1.

## Capabilities

### New Capabilities
- `lab-analytics-query`: contrato, filtros, ordenação, desempenho e valor financeiro.

### Modified Capabilities

## Impact

Porta de leitura `LabAnalyticsPort` com adaptador JDBC; índices em `producers`. Usa o motor do `financial-engine`. Alimenta o dashboard do web. Depende de `registry` e `financial-engine`.

**Premissa de semântica:** os filtros incidem sobre os produtores; `totalProdutores` conta apenas os produtores que atendem aos filtros, e `qtdMinimaProdutores` é aplicado a essa contagem (`HAVING`). Laboratórios sem produtores que atendam não aparecem. O enunciado não distingue "total de vínculos" de "total filtrado"; a escolha fica registrada em ADR.

**Não objetivos:** exportação, paginação do resultado (o número de laboratórios é pequeno), cache.

## Documentação

ADR previsto: 0016 Consulta analítica em SQL dinâmico como leitura separada. Exemplos de chamada e o plano de execução comentado no README.
