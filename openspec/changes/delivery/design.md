## Context

Adota um padrão de entrega consolidado: imagens no GHCR, usuário de publicação restrito que só executa `deploy <tag>`, proxy de borda (Caddy) da própria VPS e verificação de saúde.

## Goals / Non-Goals

**Goals:**
- Um comando local; um pipeline verde publica na VPS; reversão simples.
- Nada sensível no repositório público.

**Non-Goals:**
- Alta disponibilidade, múltiplos ambientes, observabilidade com métricas.

## Decisions

1. **Compose com PostgreSQL, API e web** localmente; na VPS, o Compose usa o PostgreSQL da própria VPS, alcançado pela rede Docker externa (`DB_NETWORK`) e configurado via `.env`.
2. **CI único (`pnpm verify`)** mais Testcontainers no runner do GitHub, que tem Docker.
3. **CD em dois estágios:** build e push das imagens (tag = SHA) e deploy por SSH com comando fixo; `flock` evita deploy simultâneo; health check decide sucesso.
4. **Hostname único** (`agro.gabrielriguiti.com.br`): o nginx do web repassa `/api` para a API, então sem CORS.
5. **Proxy:** Caddy versionado em `infra/vps/proxy/` (um `sites/*.caddy` por projeto, TLS automático); `basicauth` só para métodos de escrita.
6. **Banco:** database `agro_demo` e role próprio, sem privilégios elevados, no PostgreSQL em container da VPS; conexão interna pela rede Docker (`sslmode=disable`, porta publicada só em `127.0.0.1`); `CONNECT` revogado de `PUBLIC` nos demais bancos; TLS `verify-full` apenas se o banco for externo à VPS; connection string apenas em `.env` da VPS.
7. **Chave de publicação própria** do projeto, presa ao comando `agro-deploy`, exclusiva deste projeto.
8. **Limites de memória** nos containers (JVM com `-Xmx` baixo) por ser VPS compartilhada.

ADRs a gerar: 0020 Pipeline CI/CD com GHCR e SSH restrito; 0021 Demo público com proteção no proxy e banco dedicado.

## Risks / Trade-offs

- [Demo público acessível a qualquer um] → escrita protegida no proxy, dados fictícios, banco isolado, senha enviada em privado.
- [Primeiro deploy exige passos manuais na VPS (chave, DNS, arquivo do proxy, banco)] → runbook `docs/deploy.md` com cada passo.
- [Pacotes do GHCR privados] → tornar públicos ou fornecer token de leitura à VPS.
- [Domínio atrás do Cloudflare] → SSL em Full (strict) para evitar loop de redirecionamento.
