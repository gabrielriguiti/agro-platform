## Why

O enunciado pede uma interface com quatro módulos: formulário de produtores, grid operacional, dashboard analítico e painel de monitoramento da integração. Para o formulário de produtor funcionar de ponta a ponta, o change inclui também o cadastro de laboratórios e de propriedades. A interface é simples, desacoplada e consome a API só pelo contrato OpenAPI; o visual segue o handoff em `ui-mockups/`.

## What Changes

- Formulário de produtor com multi-select de propriedades e seleção única de laboratório.
- Grid paginada com edição e exclusão.
- Dashboard com filtros do endpoint analítico e visualização do resultado.
- Painel de integração: lotes, mensagens, DLQ, reprocessamento e envio de lote de exemplo.
- Cadastro de laboratórios e de propriedades pelo web (listagem, criação, edição e exclusão), para o formulário de produtor ter o que selecionar.
- Cliente tipado gerado do OpenAPI em `packages/api-client`.

## Capabilities

### New Capabilities
- `producer-form`: criação e edição de produtores.
- `producer-grid`: listagem operacional, edição e exclusão.
- `analytics-dashboard`: consulta e visualização analítica de laboratórios.
- `integration-monitor`: monitoramento e reprocessamento da integração.
- `laboratory-management`: listagem, criação, edição e exclusão de laboratórios.
- `property-management`: listagem, criação, edição e exclusão de propriedades.

### Modified Capabilities

## Impact

`apps/web` (Vite, React, TypeScript) e `packages/api-client`. Depende de `registry`, `lab-analytics` e `integration-pipeline`. Toca também `apps/api`: o contrato OpenAPI não declarava a resposta de sucesso (200) de 7 operações que o web usa (produtor por id e atualização, analítico, lote e mensagem por id, reprocessamentos), então foram acrescentadas anotações `@ApiResponse(200)` nos controllers e o `packages/api-client/openapi.json` foi regenerado. Nenhuma regra de negócio da API muda.

**Não objetivos:** autenticação, i18n (só pt-BR), temas, PWA, vincular propriedade a produtor pela tela de propriedade (o vínculo é feito no formulário de produtor).

## Decisão: telas de cadastro de laboratório e propriedade

Ao testar o formulário de produtor, percebemos que num banco vazio ele não tem laboratório nem propriedade para escolher. Consideramos deixar esses cadastros só pela integração (o lote de exemplo os cria) e **decidimos o contrário**: o web também cadastra laboratórios e propriedades, com telas simples. Motivo: o formulário de produtor precisa funcionar de ponta a ponta sem depender de um lote ou de `curl`. A integração continua sendo o caminho para carga em volume e para demonstrar a DLQ, mas não é o único.

## Documentação

ADR previsto: 0017 Front simples com Vite, React e cliente tipado. README com capturas de tela.
