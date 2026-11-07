# agro-platform

Plataforma full-stack para gestão dos cadastros do ecossistema agrícola — **Produtor**, **Propriedade** e **Laboratório** — com integrador de dados resiliente, motor financeiro extensível e consulta analítica de laboratórios. Projeto de portfólio, desenvolvido change a change com [OpenSpec](openspec/) e decisões registradas em [ADRs](docs/adr/README.md).

> **Estado atual:** change `foundation` (base verificável): a API sobe, migra o banco, publica OpenAPI e logs em JSON, e o build falha se a arquitetura for violada. As regras de negócio chegam nos changes seguintes ([ordem dos changes](docs/ordem-dos-changes.md)).

## Stack

- **API:** Java 17, Spring Boot 3.3, Maven, PostgreSQL + Flyway, springdoc (OpenAPI 3), logs JSON, JUnit 5, Testcontainers e ArchUnit.
- **Web:** React + TypeScript (Vite), roteamento com React Router.
- **Monorepo:** pnpm + Turborepo ([ADR-0007](docs/adr/0007-monorepo-pnpm-turborepo-com-api-java.md)): `apps/api`, `apps/web` e `packages/api-client` (tipos gerados do OpenAPI).

## Como rodar

Pré-requisitos: JDK 17, Maven 3.9+, Node 20+, pnpm 10+ e Docker (PostgreSQL local e testes de integração).

```bash
pnpm install

# banco local (credenciais de desenvolvimento: agro/agro)
docker run -d --name agro-pg -e POSTGRES_USER=agro -e POSTGRES_PASSWORD=agro -e POSTGRES_DB=agro -p 5432:5432 postgres:16-alpine

# API em http://localhost:8080 (profile local = logs em texto)
cd apps/api && mvn spring-boot:run -Dspring-boot.run.profiles=local

# web em http://localhost:5173 (em outro terminal, a partir da raiz)
pnpm --filter @agro/api-client build   # gera os tipos do contrato
pnpm --filter web dev
```

Endereços da API: `/actuator/health`, `/v3/api-docs` e `/swagger-ui.html`. Configuração por variáveis de ambiente: `DB_URL`, `DB_USER`, `DB_PASSWORD`.

> Se a máquina tiver mais de um JDK, use o 17 (`JAVA_HOME`): o projeto compila com `release 17`.

## Verificação

```bash
pnpm verify      # API (testes unitários, ArchUnit e *IT com Testcontainers), web (typecheck, testes, build) e pacotes
pnpm docs:check  # links relativos da documentação
```

A API também funciona sem Node: `cd apps/api && mvn verify`. Testes `*Test` rodam no Surefire (sem Docker); testes `*IT` rodam no Failsafe e exigem Docker.

### Contrato OpenAPI e cliente do web

O contrato da API fica versionado em `packages/api-client/openapi.json`; `openapi-typescript` gera os tipos usados pelo web. Ao mudar a API, suba-a e rode `scripts/export-openapi.sh`: o `typecheck` do web quebra se ele usar algo que saiu do contrato.

## Arquitetura

Hexagonal com DDD pragmático ([ADR-0001](docs/adr/0001-arquitetura-hexagonal-com-ddd-pragmatico.md)), com fronteiras verificadas por ArchUnit no build ([ADR-0005](docs/adr/0005-fronteiras-de-arquitetura-verificadas-com-archunit.md)).

```mermaid
flowchart LR
    Web["apps/web<br/>React + TypeScript"] -- "HTTP / OpenAPI" --> Infra
    subgraph API["apps/api — br.com.gabrielriguiti.agro"]
        Infra["infrastructure<br/>controllers, JPA/JDBC, config"] --> App["application<br/>casos de uso e portas"]
        App --> Domain["domain<br/>regras e invariantes<br/>(sem frameworks)"]
        Infra -. "implementa as portas" .-> App
    end
    Infra --> DB[("PostgreSQL<br/>Flyway")]
```

Regras (testadas em `ArchitectureTest`): `domain` só depende de Java padrão; `application` nunca depende de `infrastructure`; controllers não acessam a persistência; o motor financeiro não importa Spring.

## Motor financeiro

O valor é calculado por uma calculadora (Strategy) por natureza, em `application.financial`, sem Spring ([ADR-0010](docs/adr/0010-motor-financeiro-como-strategy-puro-com-taxas-configuraveis.md)). **As fórmulas são uma premissa**: o enunciado não as define e elas devem ser validadas com o negócio.

| Natureza | Fórmula | Padrões (BRL) |
|---|---|---|
| Produtor | `diasDeVigência × comissãoDiária + propriedades × comissãoPorPropriedade` | 120,00 por dia; 50,00 por propriedade |
| Propriedade | `taxaOperacional + taxaTerritorial` | 300,00; 150,00 |
| Laboratório | `taxaOperacional + produtores × taxaTerritorialPorProdutor` | 500,00; 25,00 por produtor |

Os dias de vigência são o intervalo entre `dataInicial` e `dataFinal` arredondado para cima (mínimo 1), e todo valor tem duas casas (`HALF_EVEN`).

**Exemplo:** produtor com 5 dias de vigência e 2 propriedades = 5 × 120,00 + 2 × 50,00 = **700,00**. Um laboratório com 4 produtores = 500,00 + 4 × 25,00 = **600,00**; uma propriedade = 300,00 + 150,00 = **450,00**.

```bash
curl localhost:8080/api/financial/producers/10     # também: /properties/{id} e /laboratories/{id}
# {"valor":700.00,"natureza":"PRODUCER","componentes":[{"nome":"dailyCommission","quantidade":5,"valorUnitario":120.00,"subtotal":600.00}, ...]}
```

As taxas mudam por configuração, sem recompilar (por exemplo, a variável `AGRO_FINANCIAL_PRODUCER_DAILY_COMMISSION=100.00`):

| Propriedade | Padrão |
|---|---|
| `agro.financial.producer.daily-commission` / `.property-commission` | 120.00 / 50.00 |
| `agro.financial.property.operational-fee` / `.territorial-fee` | 300.00 / 150.00 |
| `agro.financial.laboratory.operational-fee` / `.territorial-fee-per-producer` | 500.00 / 25.00 |

Para uma nova regra, implemente `FinancialCalculator` e declare-a como bean: o motor e as calculadoras existentes não mudam.

## Integração de dados

Parceiros (ERP, LIMS, órgãos de governo) enviam produtores, propriedades e laboratórios em lote ou de forma pontual. A API só **grava** (202) e um worker **processa** depois, com idempotência, retry e DLQ ([ADR-0011](docs/adr/0011-outbox-como-inbox-em-postgresql.md) a [0015](docs/adr/0015-estrategias-de-reconciliacao.md)). A tabela `outbox_message` no PostgreSQL é a fila durável; sem broker externo, e a porta `OutboxPort` mantém a escolha substituível.

```bash
curl -X POST localhost:8080/api/integration/batches -H 'Content-Type: application/json' -d '{
  "source": "erp-fazendas", "mergeStrategy": "MERGE",
  "items": [{ "type": "PRODUCER", "externalId": "P-9", "data": {
    "nome": "Maria", "inscricaoFiscal": "529.982.247-25", "endereco": "Linha 5, Sorriso/MT",
    "dataInicial": "2025-01-01T00:00:00Z", "dataFinal": "2025-12-31T23:59:59Z",
    "laboratorio": { "id": "LAB-1", "nome": "Lab Central", "inscricaoFiscal": "11.222.333/0001-81", "endereco": "Av. Brasil, 1" },
    "propriedades": [ { "id": "PR-1", "nome": "Fazenda Sol", "inscricaoFiscal": "390.533.447-05", "endereco": "BR-163, km 12" } ]
  } }]
}'
# 202 {"batchId":1,"accepted":1,"duplicates":0}
```

| Endpoint | Função |
|---|---|
| `POST /api/integration/batches` | ingere um lote (1 a 1000 itens) |
| `GET /api/integration/batches` e `/{id}` | lotes com contagens por status e estado (`EM_ANDAMENTO`, `CONCLUIDO`, `CONCLUIDO_COM_FALHAS`) |
| `GET /api/integration/messages?status=DEAD&batchId=` e `/{id}` | mensagens paginadas (DLQ com `status=DEAD`); o payload vem no detalhe |
| `POST /api/integration/messages/{id}/reprocess` e `/batches/{id}/reprocess` | reenfileira o que está em `DEAD` (409 fora dele) |
| `POST /api/integration/worker/run` | executa um ciclo do worker (só com `manual-run-enabled=true`; senão, 404) |

### Ingestão

```mermaid
sequenceDiagram
    participant P as Parceiro
    participant A as API
    participant DB as PostgreSQL
    P->>A: POST /api/integration/batches
    A->>A: valida o envelope (400) e o limite (413)
    A->>DB: BEGIN
    A->>DB: INSERT integration_batch
    loop cada item
        A->>DB: INSERT outbox_message ON CONFLICT (idempotency_key) DO NOTHING
    end
    A->>DB: COMMIT (tudo ou nada)
    A-->>P: 202 {batchId, accepted, duplicates}
```

### Processamento

```mermaid
sequenceDiagram
    participant W as Worker
    participant DB as PostgreSQL
    loop a cada ciclo
        W->>DB: UPDATE ... FROM (SELECT ... FOR UPDATE SKIP LOCKED) RETURNING
        Note over W,DB: PROCESSING, lease e attempts + 1
        W->>DB: BEGIN (REQUIRES_NEW, uma por mensagem)
        W->>DB: resolve identidade, reconcilia e grava as entidades
        alt sucesso
            W->>DB: COMMIT
            W->>DB: status = DONE
        else falha
            W->>DB: ROLLBACK (nada da mensagem fica)
            W->>DB: PENDING com backoff, ou DEAD
        end
    end
```

### Estados da mensagem

```mermaid
stateDiagram-v2
    [*] --> PENDING: ingestão
    PENDING --> PROCESSING: reserva (attempts + 1)
    PROCESSING --> DONE: sucesso
    PROCESSING --> PENDING: falha transitória (backoff com jitter)
    PROCESSING --> DEAD: falha permanente ou tentativas esgotadas
    PROCESSING --> PROCESSING: lease vencido (worker morreu)
    DEAD --> PENDING: reprocesso manual (attempts = 0)
    DONE --> [*]
```

### Falhas, retry e reconciliação

- **Permanentes** (JSON malformado, campo ausente, CPF/CNPJ inválido, conflito de identidade) vão direto para `DEAD`. **Transitórias** (banco, corrida por CNPJ, desconhecidas) reagendam com `min(10 min, 5 s × 2^(n-1)) × (1 + 0,1 × r)` e vão para `DEAD` na 5ª tentativa.
- A identidade é resolvida por (`source`, `externalId`) e, na ausência, pela inscrição fiscal. Estratégias: `MERGE` (preserva o existente, preenche vazios), `OVERWRITE` (o recebido vence) e `KEEP_EXISTING` (ignora o recebido).
- Reenvio idêntico (mesmo `source`, tipo, id e payload, em qualquer ordem de campos) é `duplicates`; propriedades já vinculadas nunca são removidas por um reenvio.
- Cenários em Gherkin: [docs/bdd/](docs/bdd/README.md).

| Propriedade (`agro.integration.*`) | Padrão |
|---|---|
| `max-batch-size` / `max-attempts` / `worker-batch-size` | 1000 / 5 / 20 |
| `lease` / `poll-interval` | `PT2M` / `PT5S` |
| `backoff-base` / `backoff-max` / `backoff-jitter` | `PT5S` / `PT10M` / 0.1 |
| `scheduler-enabled` / `manual-run-enabled` | `true` / `false` |

## Análise de laboratórios

`GET /api/laboratories/analytics` compara laboratórios por produtores e valor financeiro, em **uma única consulta** com `GROUP BY`/`HAVING` ([ADR-0016](docs/adr/0016-consulta-analitica-em-sql-dinamico-como-leitura-separada.md)). Retorna `id`, `nome` em CAIXA ALTA, `totalProdutores` e `valorFinanceiroConsolidado` (motor financeiro aplicado ao `totalProdutores` do próprio resultado: 4 produtores = 600,00).

| Parâmetro | Obrigatório | Efeito |
|---|---|---|
| `qtdMinimaProdutores` | sim (≥ 1) | só laboratórios com ao menos essa quantidade de produtores **que atendem aos filtros** |
| `dataInicialInicio` / `dataInicialFim` | não | intervalo inclusivo sobre `dataInicial` do produtor (ISO-8601, instante UTC) |
| `dataFinalInicio` / `dataFinalFim` | não | intervalo inclusivo sobre `dataFinal` do produtor |
| `buscaTexto` | não | correspondência parcial, sem distinção de caixa, em `observacoes`; `%`, `_` e `\` são literais |

```bash
curl -G localhost:8080/api/laboratories/analytics \
  --data-urlencode 'qtdMinimaProdutores=2' \
  --data-urlencode 'dataInicialInicio=2025-01-01T00:00:00Z' \
  --data-urlencode 'dataInicialFim=2025-12-31T23:59:59Z' \
  --data-urlencode 'buscaTexto=integração'   # --data-urlencode codifica acentos e símbolos; sem isso o Tomcat responde 400
# [{"id":3,"nome":"LABORATÓRIO MODELO 1","totalProdutores":4,"valorFinanceiroConsolidado":600.00}, ...]
```

**Semântica do total (premissa a validar com o negócio):** os filtros incidem sobre os produtores. `totalProdutores` conta só os que atendem a todos os filtros (um laboratório com 4 produtores, 2 deles com o texto buscado, aparece com `totalProdutores` 2), `qtdMinimaProdutores` é aplicado a essa contagem e laboratórios sem produtor que atenda não aparecem. Parâmetro ausente, `qtdMinimaProdutores` < 1 ou intervalo invertido respondem 400.

**Ordenação:** `totalProdutores` decrescente; havendo filtro de `dataInicial`, a menor `dataInicial` entre os produtores considerados (crescente); desempate por `id`.

**Desempenho:** os índices `ix_producers_laboratory_id`, `ix_producers_data_inicial` e `ix_producers_data_final` (já em `V1__schema.sql`) apoiam a consulta. Plano real (PostgreSQL 16, 50 laboratórios, 200 mil produtores, `EXPLAIN (ANALYZE, COSTS OFF)` com filtro de um mês em `dataInicial` e texto):

```text
Sort  (Sort Key: count DESC, min(data_inicial), l.id)
  -> GroupAggregate (rows=32)
       -> Merge Join (rows=458)
            -> Sort -> Seq Scan on laboratories l (rows=50)
            -> Sort -> Bitmap Heap Scan on producers p (rows=458; Filter: observacoes ILIKE; removidas 2742)
                         -> Bitmap Index Scan on ix_producers_data_inicial (rows=3200)
```

Sem filtros o plano é um `Seq Scan` paralelo com `HashAggregate`, que é o certo quando todas as linhas contam. `ILIKE '%x%'` não usa índice b-tree: se o volume exigir, a evolução é `pg_trgm` com índice GIN em `observacoes`.

## Documentação e convenções

- Decisões: [docs/adr/](docs/adr/README.md). Commits: [docs/commits.md](docs/commits.md). Specs: [openspec/](openspec/).
- Identificadores, rotas, tabelas e colunas em inglês; documentação, specs e textos da interface em português.
- O banco tem `COMMENT ON` em todas as tabelas e colunas ([`V1__schema.sql`](apps/api/src/main/resources/db/migration/V1__schema.sql) e [`V2__external_reference.sql`](apps/api/src/main/resources/db/migration/V2__external_reference.sql)).

## Uso de IA

Este projeto foi desenvolvido com apoio de assistente de IA (Claude Code) para redigir specs, código, testes e documentação, sob direção e revisão do autor, que responde pelas decisões. Os commits não levam coautoria de IA; o uso é declarado aqui ([ADR-0004](docs/adr/0004-commits-por-change-sem-coautoria-de-ia.md)).
