# Deploy na VPS (runbook)

Publica `agro.gabrielriguiti.com.br` atrás do proxy Caddy compartilhado. Decisões: [ADR-0020](adr/0020-pipeline-ci-cd-com-ghcr-e-ssh-restrito.md) e [ADR-0021](adr/0021-demo-publico-com-protecao-no-proxy-e-banco-dedicado.md). Nada sensível vai para o repositório: segredos ficam nos secrets do GitHub e no `.env` da VPS.

Os passos 1 a 6 são feitos **uma vez**; depois, todo push em `main` com CI verde implanta sozinho.

## 1. DNS

Crie o registro `A` (e `AAAA`, se houver IPv6) de `agro.gabrielriguiti.com.br` apontando para a VPS.

## 2. Banco dedicado

No PostgreSQL de desenvolvimento, como administrador:

```sql
CREATE ROLE agro_demo LOGIN PASSWORD '<senha-forte>' NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE DATABASE agro_demo OWNER agro_demo;
REVOKE CONNECT ON DATABASE agro_demo FROM PUBLIC;
GRANT CONNECT ON DATABASE agro_demo TO agro_demo;
```

O role só alcança o `agro_demo`. Confira: `psql -U agro_demo -d outro_banco` deve ser negado. Revogue também o `CONNECT` de `PUBLIC` nos demais bancos (`REVOKE CONNECT ON DATABASE appdb, postgres, template1 FROM PUBLIC`), senão o role ainda alcança bancos que herdam o privilégio padrão. Com o Postgres em container na mesma VPS, a conexão é interna pela rede Docker (`sslmode=disable`, porta publicada só em `127.0.0.1`); se o banco ficar fora da VPS, use TLS verificado (`sslmode=verify-full`, com o certificado raiz do servidor disponível no host da aplicação).

## 3. Diretório e `.env` na VPS

```bash
sudo install -d -o <usuario-de-publicacao> /opt/agro
cp infra/vps/docker-compose.prod.yml infra/vps/.env.example /opt/agro/   # via scp, uma vez
cd /opt/agro && mv .env.example .env && chmod 600 .env                    # preencha com os valores reais
```

`GHCR_OWNER` é o usuário do GitHub em minúsculas; `PROXY_NETWORK` é o nome da rede Docker do proxy; `DB_NETWORK` é a rede Docker do container Postgres (o `agro-api` entra nela para resolver o host `postgres`); `DB_*` vêm do passo 2. Os pacotes `agro-api` e `agro-web` do GHCR devem ser **públicos**; se preferir privados, faça `docker login ghcr.io` na VPS com um token de leitura (`read:packages`).

## 4. Chave de publicação (exclusiva do projeto)

Na sua máquina:

```bash
ssh-keygen -t ed25519 -f agro-deploy-key -C "agro-deploy" -N ""
```

Na VPS, instale o script e prenda a chave a ele (uma linha no `~/.ssh/authorized_keys` do usuário de publicação):

```bash
sudo install -m 755 infra/vps/agro-deploy /usr/local/bin/agro-deploy
# authorized_keys:
command="/usr/local/bin/agro-deploy",no-pty,no-port-forwarding,no-agent-forwarding,no-X11-forwarding ssh-ed25519 AAAA... agro-deploy
```

O usuário precisa poder executar `docker`. Teste: qualquer comando diferente de `deploy <sha>` é recusado (`ssh -i agro-deploy-key usuario@host "ls"` responde "Comando recusado").

## 5. Registro no proxy

**Sem proxy na VPS:** suba o Caddy de `infra/vps/proxy/` (cria a rede `proxy`, que vira o `PROXY_NETWORK`). Libere TCP 80 e 443 (e UDP 443) no firewall do provedor e no host.

```bash
sudo install -d -o <usuario-de-publicacao> /opt/edge
scp -r infra/vps/proxy/. <usuario>@<host>:/opt/edge/
cp infra/vps/agro.caddy /opt/edge/sites/                        # na VPS
cd /opt/edge && cp .env.example .env && chmod 600 .env         # preencha usuário e hash
docker compose up -d && docker exec caddy caddy validate --config /etc/caddy/Caddyfile
```

**Com proxy já existente:** siga os passos abaixo.

Gere o hash da senha do demo e registre o bloco com o script de borda (que valida antes do reload):

```bash
caddy hash-password          # informe a senha; copie o hash
```

Defina `AGRO_DEMO_USER` e `AGRO_DEMO_HASH` no ambiente do Caddy (o hash vai entre aspas simples por causa dos `$`), copie `infra/vps/agro.caddy` para o diretório de sites do proxy e recarregue com o script de borda. Envie a senha aos avaliadores em privado.

## 6. Secrets do GitHub

Em Settings → Secrets and variables → Actions:

| Secret | Conteúdo |
|---|---|
| `DEPLOY_SSH_KEY` | conteúdo de `agro-deploy-key` (chave privada) |
| `DEPLOY_KNOWN_HOSTS` | saída de `ssh-keyscan -t ed25519 <host>`, conferida com a impressão digital do servidor |
| `DEPLOY_HOST` | endereço da VPS |
| `DEPLOY_USER` | usuário de publicação |

Apague `agro-deploy-key` da sua máquina depois de cadastrar.

## 7. Primeiro deploy

Faça um push em `main` (ou rode **Deploy** em Actions com uma tag). O workflow publica as imagens, chama `deploy <sha>` e confere `/actuator/health` e `/actuator/info` (que deve conter o SHA). Verificações manuais:

```bash
curl -s https://agro.gabrielriguiti.com.br/actuator/health
curl -s https://agro.gabrielriguiti.com.br/actuator/info          # contém o SHA
curl -s -o /dev/null -w "%{http_code}\n" -X POST https://agro.gabrielriguiti.com.br/api/producers   # 401
```

## Reverter

Em Actions, **Deploy → Run workflow**, informe a tag anterior (o SHA da versão boa; a VPS guarda a atual em `/opt/agro/.current-tag` e a anterior em `.previous-tag`). Nenhuma imagem é reconstruída. Se os serviços de uma versão nova não subirem, ou a API não ficar saudável em 90 s depois de subir (o `docker compose up` espera a API antes, por até cerca de 170 s), o `agro-deploy` já restaura a anterior sozinho e o job falha.

## Diagnóstico

`docker logs agro-api`, `docker ps --filter name=agro-` e o log do job de deploy (sem segredos). Rodar o script direto na VPS: `SSH_ORIGINAL_COMMAND="deploy <sha>" /usr/local/bin/agro-deploy`.
