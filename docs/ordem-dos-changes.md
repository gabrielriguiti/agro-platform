# Ordem e dependências dos changes

Todos os changes têm as specs escritas em `openspec/changes/`; a implementação segue a ordem abaixo. Cada change entregue vira um commit de entrega ([padrão de commits](commits.md)).

| Ordem | Change | Depende de | Specs | ADRs previstos |
|---|---|---|---|---|
| 1 | `foundation` | (nenhum) | architecture-boundaries, engineering-conventions, runtime-baseline | 0001 a 0007 (escritos) |
| 2 | `registry` | 1 | tax-id, laboratory-registry, property-registry, producer-registry | 0008, 0009 |
| 3 | `financial-engine` | 1 (o endpoint usa o cadastro do 2) | financial-calculation | 0010 |
| 4 | `integration-pipeline` | 1, 2 | batch-ingestion, idempotent-processing, retry-and-dead-letter, reconciliation, integration-monitoring | 0011 a 0015 |
| 5 | `lab-analytics` | 2, 3 | lab-analytics-query | 0016 |
| 6 | `web-ui` | 2, 4, 5 (contrato OpenAPI de cada um) | producer-form, producer-grid, analytics-dashboard, integration-monitor | 0017 |
| 7 | `delivery` | 1 a 6 (o deploy final); Dockerfiles e CI podem começar após o 1 | local-runtime, ci-pipeline, vps-deployment, project-documentation | 0018, 0019 |

## Paralelismo possível

- O núcleo puro do `financial-engine` (calculadoras e motor) pode andar junto do `registry`; só o endpoint espera o cadastro.
- No `web-ui`, grid e formulário de produtores só precisam do `registry`; o dashboard espera o `lab-analytics` e o monitor espera o `integration-pipeline`.
- No `delivery`, os Dockerfiles, o Compose e o CI entram logo após o `foundation`, para que todo commit seguinte já seja verificado; o deploy na VPS e o README final fecham o projeto.

## Marcos

| Marco | Changes | O que fica demonstrável |
|---|---|---|
| Base verificável | 1 | Aplicação que sobe, migra o banco, publica OpenAPI e falha o build se a arquitetura for violada |
| Cadastro | 2, 3 | CRUD completo com validação fiscal e cálculo financeiro |
| Resiliência | 4 | Ingestão idempotente, retry, DLQ e reconciliação |
| Análise | 5 | Consulta agregada de laboratórios |
| Produto | 6 | As quatro telas |
| Entrega | 7 | Compose, CI/CD e demo publicado |
