## Why

Antes de qualquer regra de negócio, o projeto precisa de uma base que torne as decisões de arquitetura verificáveis: estrutura de pacotes, fronteiras impostas por teste, ambiente que sobe com um comando, API documentada, logs estruturados e convenções de trabalho (specs, ADRs e commits).

## What Changes

- Esqueleto do monorepo pnpm + Turborepo (`apps/api`, `apps/web`, `packages/`, `docs/`, `openspec/`), com build Maven na API e Vite no web, e `pnpm verify` rodando tudo.
- Aplicação Spring Boot que sobe, aplica as migrations do Flyway, expõe `/actuator/health`, OpenAPI e Swagger UI e escreve logs em JSON.
- Teste ArchUnit com as regras da arquitetura hexagonal.
- Convenções de engenharia: specs antes do código, ADR por decisão, commits por change e sem coautoria de IA.

## Capabilities

### New Capabilities
- `architecture-boundaries`: regras de dependência entre `domain`, `application` e `infrastructure`, verificadas no build.
- `engineering-conventions`: fluxo de specs, ADRs e commits do repositório.
- `runtime-baseline`: aplicação que sobe, migra o banco, publica saúde, contrato OpenAPI e logs estruturados.

### Modified Capabilities

## Impact

Todos os changes seguintes herdam a estrutura de pacotes `br.com.gabrielriguiti.agro` e as regras de fronteira. ADRs 0001 a 0007. Depende de nada.

**Não objetivos:** regras de negócio, telas, integração e deploy (changes seguintes). Autenticação de usuários fica fora do projeto; a proteção do demo público é feita no proxy.

## Documentação

ADRs 0001 a 0007; `docs/commits.md`; README com diagrama de arquitetura (a completar a cada change).
