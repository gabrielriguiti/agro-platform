# 0020. Pipeline CI/CD com GHCR e SSH restrito

- Status: aceito
- Data: 2026-11-07
- Decisores: Gabriel
- Change relacionado: `openspec/changes/delivery`

## Contexto

O projeto precisa de verificação a cada push e de publicação automática em uma VPS própria e compartilhada com outras aplicações. O repositório é público: nenhum segredo, IP ou nome de host interno pode ir para ele. O deploy tem de ser reversível e não pode dar à automação mais acesso do que ela precisa.

## Opções consideradas

1. **Build na própria VPS** (`git pull` + `docker compose build`): simples, mas consome CPU e memória de uma máquina compartilhada e não deixa artefato versionado para reverter.
2. **Imagens no GHCR + SSH com shell completo**: reversível, mas uma chave vazada dá shell na VPS.
3. **Imagens no GHCR + SSH com comando fixo**: reversível e com superfície mínima, ao custo de um script a manter na VPS.

## Decisão

Opção 3. O CI (`pnpm verify`, Testcontainers e verificação da documentação) roda em todo push e pull request. Em `main` com o CI verde, o workflow de deploy constrói as imagens da API e do web, publica no GHCR com a **tag igual ao SHA do commit** e chama a VPS por SSH. A chave de publicação é exclusiva do projeto e, no `authorized_keys`, fica presa ao script `agro-deploy` (`command=`, sem pty nem redirecionamento de portas): ela só executa `deploy <tag>`, com a tag validada por expressão regular. A chave do servidor é fixada em um secret (sem confiança no primeiro uso). O script usa `flock` para impedir deploys simultâneos, sobe os serviços e decide o sucesso pelo `/actuator/health` em até 90 s; se falhar, restaura a versão anterior. Reverter é o `workflow_dispatch` com uma tag já publicada, sem reconstruir imagens.

## Consequências

- Positivas: o SHA no `/actuator/info` liga o que está no ar a um commit; a reversão é rápida; vazar a chave não dá shell; nada sensível no repositório.
- Negativas e riscos: o primeiro deploy exige passos manuais na VPS (chave, DNS, banco, registro no proxy), descritos em [docs/deploy.md](../deploy.md); os pacotes do GHCR precisam ser públicos ou a VPS precisa de um token de leitura.
- Documentação: runbook `docs/deploy.md`; scripts em `infra/vps/`.
