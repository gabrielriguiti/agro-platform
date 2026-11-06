## Why

O enunciado pede uma interface com quatro módulos: formulário de produtores, grid operacional, dashboard analítico e painel de monitoramento da integração. A interface é simples, desacoplada e consome a API só pelo contrato OpenAPI.

## What Changes

- Formulário de produtor com multi-select de propriedades e seleção única de laboratório.
- Grid paginada com edição e exclusão.
- Dashboard com filtros do endpoint analítico e visualização do resultado.
- Painel de integração: lotes, mensagens, DLQ, reprocessamento e envio de lote de exemplo.
- Cliente tipado gerado do OpenAPI em `packages/api-client`.

## Capabilities

### New Capabilities
- `producer-form`: criação e edição de produtores.
- `producer-grid`: listagem operacional, edição e exclusão.
- `analytics-dashboard`: consulta e visualização analítica de laboratórios.
- `integration-monitor`: monitoramento e reprocessamento da integração.

### Modified Capabilities

## Impact

`apps/web` (Vite, React, TypeScript) e `packages/api-client`. Depende de `registry`, `lab-analytics` e `integration-pipeline`.

**Não objetivos:** cadastro de propriedade/laboratório pelo web, autenticação, i18n (só pt-BR), temas, PWA.

## Documentação

ADR previsto: 0017 Front simples com Vite, React e cliente tipado. README com capturas de tela.
