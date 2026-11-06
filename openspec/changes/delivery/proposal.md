## Why

O enunciado exige entrega em repositório do GitHub com README (diagrama de arquitetura, guia de execução, decisões de design e testes). Para o portfólio, o projeto também precisa rodar com um comando, ter integração contínua e estar publicado em uma VPS própria com deploy automatizado.

## What Changes

- Execução local com `docker compose up` (PostgreSQL, API e web) e seed opcional com dados fictícios.
- Pipeline de CI no GitHub Actions: `pnpm verify`, testes com Testcontainers e verificação da documentação.
- Pipeline de CD: imagens no GHCR com tag igual ao SHA, deploy por SSH restrito na VPS, health check, reversão manual.
- Publicação em `agro.gabrielriguiti.com.br` atrás do proxy de borda compartilhado, com proteção de escrita e banco dedicado.
- README completo, diagramas Mermaid, cenários BDD e seção de uso de IA.

## Capabilities

### New Capabilities
- `local-runtime`: execução local reprodutível e seed fictício.
- `ci-pipeline`: integração contínua.
- `vps-deployment`: entrega contínua e operação na VPS.
- `project-documentation`: README, diagramas, BDD e declaração de uso de IA.

### Modified Capabilities

## Impact

`docker-compose.yml`, Dockerfiles de `apps/api` e `apps/web`, `.github/workflows/`, `infra/vps/` (scripts e arquivo do proxy), README e `docs/`. Depende de todos os outros changes.

**Premissas:** o repositório é público, então nenhum segredo, senha, IP ou nome de host interno vai para ele; segredos ficam nos secrets do GitHub e no `.env` da VPS. A VPS de aplicações e o PostgreSQL de desenvolvimento do autor já existem; o deploy usa um usuário de publicação restrito e o proxy compartilhado. A senha do demo é enviada aos avaliadores em privado e não aparece no README.

**Não objetivos:** alta disponibilidade, múltiplos ambientes, monitoramento com Prometheus/Grafana, certificados gerenciados fora do proxy.

## Documentação

ADRs previstos: 0018 Pipeline CI/CD com GHCR e SSH restrito; 0019 Demo público com proteção no proxy e banco dedicado. Runbook `docs/deploy.md`.
