# 0015. Estratégias de reconciliação

- Status: aceito
- Data: 2026-11-06
- Decisores: Gabriel
- Change relacionado: `openspec/changes/integration-pipeline`

## Contexto

O mesmo registro pode chegar de sistemas diferentes e já existir no banco. É preciso identificá-lo e decidir o que fazer com os campos que divergem; a escolha depende do parceiro e do momento, por isso é do lote.

## Opções consideradas

1. **Uma regra fixa (por exemplo, o último vence):** simples, mas errada para parte dos casos.
2. **Estratégia por lote, em função pura (escolhida).**

## Decisão

Cada lote informa `mergeStrategy`. A fusão é a função pura `ReconciliationPolicy` no domínio, testada sem banco:

| Estratégia | Efeito |
|---|---|
| `MERGE` | preserva o existente e preenche só os campos vazios |
| `OVERWRITE` | o recebido substitui o existente (campo recebido vazio não apaga o existente) |
| `KEEP_EXISTING` | ignora o recebido; a mensagem conclui como `DONE` e nada muda |

A **resolução de identidade** localiza primeiro por (`source`, `externalId`) e, na ausência, pela inscrição fiscal normalizada. Se o id externo apontar para um registro e a inscrição fiscal para outro, é **conflito de identidade**: falha permanente, sem alterar nenhum dos dois. Em produtor, `laboratorio` e `propriedades` aninhados são reconciliados pelas mesmas regras, com o `id` aninhado como id externo, e o conjunto de propriedades do produtor nunca é reduzido.

## Consequências

- Positivas: a regra é explícita, testável e trocável por lote; sem duplicatas entre sistemas.
- Negativas e riscos: não há ordenação global entre mensagens; com `OVERWRITE`, vale a ordem de chegada. Estrita ordenação está fora de escopo.
- O que precisa mudar na documentação ou nas specs: os cenários BDD de reconciliação ficam em `docs/bdd/`.
