## 1. Execução local

- [x] 1.1 Criar os Dockerfiles de `apps/api` (multi-stage, usuário sem privilégios) e `apps/web` (build + nginx com repasse de `/api`); verificar `docker build` de ambos
- [x] 1.2 Criar o `docker-compose.yml` com PostgreSQL, API e web e health checks; verificar `docker compose up --build` do zero
- [x] 1.3 Implementar o seed fictício do profile `demo`, idempotente; verificar duas subidas seguidas sem duplicatas

## 2. CI

- [x] 2.1 Criar o workflow de CI (`pnpm verify`, Testcontainers, relatórios e sumário); verificar execução verde e vermelha de propósito
- [x] 2.2 Criar `scripts/check-docs.mjs` (links internos e ADRs no índice); verificar que um ADR fora do índice falha

## 3. CD e VPS

- [x] 3.1 Criar o workflow de deploy (decisão, build e push no GHCR, SSH restrito, health check, `workflow_dispatch` para reverter); verificar com a VPS
- [x] 3.2 Criar `infra/vps/` com o script `agro-deploy`, o `docker-compose.prod.yml`, o `agro.caddy` (basicauth na escrita) e o `.env.example` (com `DB_NETWORK`); verificar o script com `shellcheck`
- [x] 3.3 Escrever o runbook `docs/deploy.md` (DNS, chave de publicação, banco dedicado e role, registro no proxy, secrets do GitHub); verificar seguindo o passo a passo
- [x] 3.4 Primeiro deploy e teste de reversão; verificar `/actuator/health`, `/actuator/info` com o SHA e o 401 na escrita anônima
- [x] 3.5 Criar `infra/vps/proxy/` (compose do Caddy, Caddyfile e `.env.example`); verificar `caddy validate` e a emissão do certificado

## 4. Documentação final

- [x] 4.1 Escrever o README (visão geral, diagramas, execução, decisões, testes, premissas, uso de IA); verificar em máquina limpa
- [x] 4.2 Escrever os cenários BDD em `docs/bdd/` apontando para os testes; verificar o mapeamento
- [x] 4.3 Registrar os ADRs 0020 e 0021 (0018 e 0019 já são endereço em linha única e sem autenticação); verificar o índice
