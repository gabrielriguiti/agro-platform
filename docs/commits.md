# Padrão de commits

O histórico do repositório conta a história do projeto **change por change**. Cada change do OpenSpec aparece em dois commits: a proposta (as specs) e a entrega (código, testes e documentação). Decisão registrada no [ADR-0004](adr/0004-commits-por-change-sem-coautoria-de-ia.md).

## Formato

Conventional Commits, com o tipo e o escopo (nome do change) em inglês e o resumo e o corpo em português:

```
<type>(<change>): <resumo curto, no presente>

<corpo: o que o change exigia e como foi atendido, em poucas linhas>

Change: <change>
ADR: 0008, 0009
```

- `type`: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`.
- `change`: nome do change em `openspec/changes/`, por exemplo `registry`, `integration-pipeline`, `lab-analytics`.
- Os trailers `Change:` e `ADR:` ligam o commit ao change e às decisões. O `ADR:` só aparece quando o commit cria ou altera um ADR.

## Regras

1. **Dois commits por change.** A proposta entra como `docs(<change>): propõe o change <change>` (proposal, specs, design e tasks). A entrega entra como `feat(<change>): ...`, com código, testes, migrations, documentação e ADRs de todas as specs do change. Um change grande pode ter commits intermediários de entrega, mas cada commit deixa o CI verde.
2. **Specs antes do código.** A proposta de um change vem antes da sua entrega.
3. **Correções** posteriores a uma entrega usam `fix(<change>): ...`, também com o nome do change como escopo.
4. **Sem coautoria de IA.** Nenhum commit nem descrição de PR leva `Co-Authored-By: Claude` (ou de qualquer IA) nem linhas como "Generated with". O autor do commit é o dono do repositório, que revisou e responde pelo código. O uso de IA é declarado no README.
5. **CI verde em cada commit.** Não se commita código que quebra build ou testes.
6. **Sem segredos.** Nenhuma senha, chave ou connection string real em commit.

## Exemplos

```
docs(registry): propõe o change registry
feat(registry): entrega o cadastro de produtor, propriedade e laboratório com validação fiscal
feat(integration-pipeline): entrega a ingestão idempotente com retry, DLQ e reconciliação
fix(lab-analytics): aplica a ordenação pela menor data inicial só quando há filtro de data
```
