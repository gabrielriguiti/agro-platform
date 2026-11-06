## 1. Execução local

- [ ] 1.1 Criar os Dockerfiles de `apps/api` (multi-stage, usuário sem privilégios) e `apps/web` (build + nginx com repasse de `/api`); verificar `docker build` de ambos
- [ ] 1.2 Criar o `docker-compose.yml` com PostgreSQL, API e web e health checks; verificar `docker compose up --build` do zero
- [ ] 1.3 Implementar o seed fictício do profile `demo`, idempotente; verificar duas subidas seguidas sem duplicatas

## 2. CI

- [ ] 2.1 Criar o workflow de CI (`pnpm verify`, Testcontainers, relatórios e sumário); verificar execução verde e vermelha de propósito
- [ ] 2.2 Criar `scripts/check-docs.mjs` (links internos e ADRs no índice); verificar que um ADR fora do índice falha

## 3. CD e VPS

- [ ] 3.1 Criar o workflow de deploy (decisão, build e push no GHCR, SSH restrito, health check, `workflow_dispatch` para reverter); verificar com a VPS
- [ ] 3.2 Criar `infra/vps/` com o script `agro-deploy`, o `docker-compose.prod.yml`, o `agro.caddy` (basicauth na escrita) e o `.env.example`; verificar o script com `shellcheck`
- [ ] 3.3 Escrever o runbook `docs/deploy.md` (DNS, chave de publicação, banco dedicado e role, registro no proxy, secrets do GitHub); verificar seguindo o passo a passo
- [ ] 3.4 Primeiro deploy e teste de reversão; verificar `/actuator/health`, `/actuator/info` com o SHA e o 401 na escrita anônima

## 4. Documentação final

- [ ] 4.1 Escrever o README (visão geral, diagramas, execução, decisões, testes, premissas, uso de IA); verificar em máquina limpa
- [ ] 4.2 Escrever os cenários BDD em `docs/bdd/` apontando para os testes; verificar o mapeamento
- [ ] 4.3 Registrar os ADRs 0018 e 0019; verificar o índice
