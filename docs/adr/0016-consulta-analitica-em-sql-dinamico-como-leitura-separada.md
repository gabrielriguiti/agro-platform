# 0016. Consulta analítica em SQL dinâmico como leitura separada

- Status: aceito
- Data: 2026-11-06
- Decisores: Gabriel
- Change relacionado: `openspec/changes/lab-analytics`

## Contexto

A área operacional compara laboratórios (quantos produtores atendem, desde quando e quanto representam), com filtros opcionais de período e de texto e um corte por quantidade mínima. É uma leitura agregada, sem escrita, em que a estrutura da consulta muda conforme os filtros informados, e que precisa rodar em uma única instrução sem N+1. O enunciado não distingue "total de vínculos" de "total filtrado".

## Opções consideradas

1. **JPQL/Criteria sobre as entidades do cadastro, com `(:p is null or ...)`:** reaproveita o mapeamento, mas parâmetros nulos de data no PostgreSQL exigem casts, o plano fica escondido e a agregação com `HAVING` e ordenação condicional fica forçada.
2. **Agregar em memória a partir do repositório do cadastro:** simples, mas traz todos os produtores e quebra o requisito de agregar no banco.
3. **Porta de leitura própria com adaptador JDBC e SQL montado só com as condições presentes (escolhida).**

## Decisão

- A leitura é separada da escrita (ADR-0001): a porta `LabAnalyticsPort` devolve linhas agregadas e o adaptador JDBC (`NamedParameterJdbcTemplate`) executa **uma** instrução com `GROUP BY` e `HAVING`.
- Só a **estrutura** é dinâmica (quais condições entram no `WHERE` e qual o `ORDER BY`); todo valor segue como parâmetro vinculado, nunca concatenado. A montagem fica em um único ponto (`LabAnalyticsSql`), testada por combinação de filtros e por um teste que garante a ausência de concatenação.
- **Semântica do total:** os filtros incidem sobre os produtores; `totalProdutores` conta só os que atendem a todos os filtros, e `qtdMinimaProdutores` é aplicado a essa contagem (`HAVING COUNT(p.id) >= :min`). Laboratórios sem produtores que atendam não aparecem. A escolha é uma premissa a validar com o negócio.
- **Ordenação:** `totalProdutores` decrescente; com filtro de `dataInicial`, segundo critério `MIN(p.data_inicial)` crescente; desempate final por `id`.
- **Busca textual literal:** `ILIKE` com `ESCAPE '\'`, escapando `%`, `_` e `\` do termo.
- **Valor financeiro** é calculado em memória no caso de uso pelo `FinancialEngine`, a partir do `totalProdutores` da linha, sem ir ao banco por laboratório.
- **Índices:** `producers(laboratory_id)`, `producers(data_inicial)` e `producers(data_final)` já existem desde o `V1__schema.sql`; nenhum índice novo foi necessário. O plano com filtro de data usa `ix_producers_data_inicial`.

## Consequências

- Positivas: consulta única e previsível, plano visível, sem N+1; o motor financeiro continua puro.
- Negativas e riscos: SQL dinâmico é propenso a erro (mitigado por montagem concentrada e testes); `ILIKE '%x%'` não usa índice b-tree e pode custar em volume alto: `pg_trgm` (índice GIN em `observacoes`) fica registrado como evolução; sem paginação, pois o número de laboratórios é pequeno.
- O que precisa mudar na documentação ou nas specs: README com exemplos de chamada, semântica do total e plano de execução.
