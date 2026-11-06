## Context

Projeto novo, público, de portfólio. Back-end Java/Spring, front React/TypeScript, banco PostgreSQL. As decisões de base ([ADR-0001](../../../docs/adr/0001-arquitetura-hexagonal-com-ddd-pragmatico.md) a [0006](../../../docs/adr/0006-logs-estruturados-e-openapi.md)) valem para todos os changes.

## Goals / Non-Goals

**Goals:**
- Estrutura de pacotes que reflita a arquitetura e a imponha por teste.
- Ambiente reprodutível: Maven no back, Vite no front, Compose local.

**Non-Goals:**
- Qualquer regra de negócio, tela ou deploy.

## Decisions

1. **Pacotes** `br.com.gabrielriguiti.agro.{domain,application,infrastructure}`; dentro de cada um, subpacotes por contexto (`registry`, `integration`, `financial`, `analytics`). Alternativa: um módulo Maven por camada (descartada por excesso de estrutura).
2. **ArchUnit no Surefire** (teste unitário, sem Docker): falha rápida e barata.
3. **Monorepo pnpm + Turborepo** ([ADR-0007](../../../docs/adr/0007-monorepo-pnpm-turborepo-com-api-java.md)): `apps/api` (Java), `apps/web` (React), `packages/api-client`; a API ganha um `package.json` fino para o Turbo orquestrar o Maven.
4. **Testes `*IT` no Failsafe** com Testcontainers, separados dos unitários para o ciclo local rápido.
5. **Perfis:** `local` (logs em texto), padrão (JSON). Configuração por variáveis de ambiente (`DB_URL`, `DB_USER`, `DB_PASSWORD`).

ADRs gerados: 0001 a 0007.

## Risks / Trade-offs

- [Ambiente de desenvolvimento sem acesso ao Maven Central] → o primeiro `mvn verify` completo roda no GitHub Actions; o domínio puro é validado também com `javac` direto.
- [Turbo cacheando saída do Maven] → o `turbo.json` declara `target/**` e `pom.xml`/fontes como entradas; no CI o cache do Turbo fica desligado para a API.
- [Regras do ArchUnit desatualizadas] → o teste faz parte do build e falha cedo.
