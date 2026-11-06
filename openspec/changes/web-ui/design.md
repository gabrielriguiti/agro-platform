## Context

Interface simples e desacoplada: só conhece a API pelo contrato OpenAPI ([ADR-0006](../../../docs/adr/0006-logs-estruturados-e-openapi.md)).

## Goals / Non-Goals

**Goals:**
- Quatro módulos funcionais, claros e testados.
- Tipos da API gerados, não escritos à mão.

**Non-Goals:**
- Design elaborado, i18n, autenticação.

## Decisions

1. **Vite + React + TypeScript**, `react-router` para as quatro rotas. Alternativa: Next.js (descartada: sem necessidade de SSR).
2. **Cliente tipado em `packages/api-client`**, gerado com `openapi-typescript` a partir do contrato da API; uma função `fetch` fina por cima. Mudar o contrato quebra o typecheck.
3. **Estado do servidor com `@tanstack/react-query`**: cache, repetição, atualização periódica do monitor.
4. **CSS próprio, sem biblioteca de componentes**; gráfico de barras em SVG simples.
5. **Testes com Vitest e Testing Library** para validações, fluxo do formulário e estados das telas.
6. **Servido por nginx** na imagem Docker, com `/api` repassado ao back-end (mesmo host, sem CORS).

ADR a gerar: 0017 Front simples com Vite, React e cliente tipado.

## Risks / Trade-offs

- [Contrato e tipos divergirem] → tipos gerados e verificados no `pnpm verify`.
- [Tela sem biblioteca de componentes fica crua] → escopo é correção e clareza, não acabamento visual.
