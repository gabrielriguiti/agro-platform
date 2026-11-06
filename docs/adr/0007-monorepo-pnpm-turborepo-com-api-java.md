# 0007. Monorepo pnpm + Turborepo com API Java

- Status: aceito
- Data: 2026-11-05
- Decisores: Gabriel
- Change relacionado: `openspec/changes/foundation`

## Contexto

O projeto tem uma API Java, um web React e um contrato OpenAPI que liga os dois. O monorepo pnpm + Turborepo, com `apps/` e `packages/`, `pnpm verify` e CI em um workflow só, é um padrão que o autor domina e que reduz o custo de manutenção.

## Opções consideradas

1. **Repositórios separados:** isolamento total, mas o contrato OpenAPI e as mudanças que cruzam API e web viram dois PRs.
2. **Pastas `backend/` e `frontend/` com scripts soltos:** simples, sem um comando único de verificação.
3. **pnpm + Turborepo, com a API Java como um "app" cujo `package.json` chama o Maven (escolhida):** um comando para tudo, tarefas em cache e um lugar para o pacote do cliente da API.

## Decisão

Estrutura `apps/api` (Maven, Spring Boot), `apps/web` (Vite, React, TypeScript) e `packages/api-client` (tipos TypeScript gerados do OpenAPI). Na raiz, `pnpm-workspace.yaml`, `turbo.json` e `package.json` com `build`, `test`, `typecheck`, `verify` e `docs:check`. O `apps/api/package.json` é só uma casca cujos scripts chamam `mvn`, para o Turbo orquestrar a API junto do resto.

## Consequências

- Positivas: um comando (`pnpm verify`) verifica tudo; mudança de contrato e do consumidor no mesmo commit; estrutura convencional de `apps/` e `packages/`.
- Negativas e riscos: rodar a API pela raiz exige Node instalado, embora o Maven continue funcionando direto em `apps/api`; o cache do Turbo sobre saídas do Maven exige entradas declaradas com cuidado.
- Documentação: README explica os dois jeitos de rodar (pela raiz e direto no Maven).
