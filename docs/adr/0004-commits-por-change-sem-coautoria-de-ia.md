# 0004. Commits por change e sem coautoria de IA

- Status: aceito
- Data: 2026-11-05
- Decisores: Gabriel
- Change relacionado: `openspec/changes/foundation`

## Contexto

O desenvolvimento é guiado por specs (OpenSpec) e assistido por IA. O repositório é público e faz parte de um portfólio: o histórico precisa mostrar o raciocínio do projeto e a autoria é do dono, que revisa e responde pelo código.

## Opções consideradas

1. **Commits livres:** rápido, mas o histórico não conta a história do projeto.
2. **Um commit por spec:** granularidade fina, porém muitos commits pequenos que só fazem sentido em conjunto, já que as specs de um change são entregues juntas.
3. **Commits por change, em Conventional Commits, com o nome do change como escopo (escolhida):** o histórico vira um índice dos changes entregues, na ordem planejada.
4. **Manter `Co-Authored-By` de IA nos commits:** descartada por decisão do autor; o uso de IA é declarado no README, de forma explícita e em um só lugar.

## Decisão

Cada change tem um commit de proposta (`docs(<change>): ...`, com proposal, specs, design e tasks) e um commit de entrega (`feat(<change>): ...`, com código, testes, migrations, documentação e ADRs de todas as specs do change), no formato de `docs/commits.md`, com os trailers `Change:` e `ADR:`. Nenhum commit ou PR contém `Co-Authored-By` de IA nem linhas "Generated with". A regra também está no `openspec/config.yaml`, para valer em todo change.

## Consequências

- Positivas: histórico curto e legível, uma linha por change; rastreável até a spec pelo escopo e pelo trailer; releases e changelog podem ser gerados por Conventional Commits.
- Negativas e riscos: um change grande gera um commit de entrega grande, então pode ter commits intermediários, cada um com o CI verde; ferramentas que acrescentam trailers automáticos precisam ser configuradas para não fazê-lo.
- Documentação: `docs/commits.md`; seção "Uso de IA" no README.
