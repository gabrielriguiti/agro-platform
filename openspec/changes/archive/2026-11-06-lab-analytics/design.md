## Context

Leitura agregada e dinâmica (filtros opcionais, ordenação condicional). Segue a decisão de separar leitura de escrita ([ADR-0001](../../../docs/adr/0001-arquitetura-hexagonal-com-ddd-pragmatico.md)).

## Goals / Non-Goals

**Goals:**
- Uma consulta, agregada no banco, previsível e testada.

**Non-Goals:**
- Paginação, exportação, cache.

## Decisions

1. **Porta `LabAnalyticsPort` com adaptador JDBC (`NamedParameterJdbcTemplate`)** e SQL montado dinamicamente apenas com condições presentes. Alternativa: JPQL com `(:p is null or ...)`, descartada: parâmetros nulos de data no PostgreSQL exigem casts e escondem o plano.
2. **Parâmetros sempre vinculados** (nunca concatenados); só a estrutura (quais condições e qual `ORDER BY`) é dinâmica.
3. **Filtros no `WHERE` sobre `producers`; `HAVING COUNT(p.id) >= :min`**; `MIN(p.data_inicial)` calculado só quando há filtro de data inicial.
4. **Escape de `LIKE`** com `ESCAPE '\'` e `ILIKE`; a busca é literal.
5. **Valor financeiro** calculado no `application` pelo `FinancialEngine`, sem ir ao banco por laboratório.
6. **Índices:** `producers(laboratory_id)`, `producers(data_inicial)`, `producers(data_final)`; para texto, `pg_trgm` é registrado como evolução se o volume exigir.

ADR a gerar: 0016 Consulta analítica em SQL dinâmico como leitura separada.

## Risks / Trade-offs

- [SQL dinâmico propenso a erro] → montagem em um único método, testes por combinação de filtros e teste de que não há concatenação de valores.
- [`ILIKE '%x%'` sem índice em volume alto] → documentado; `pg_trgm` como evolução.
