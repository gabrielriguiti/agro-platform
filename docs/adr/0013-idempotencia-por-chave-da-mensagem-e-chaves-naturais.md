# 0013. Idempotência por chave da mensagem e chaves naturais

- Status: aceito
- Data: 2026-11-06
- Decisores: Gabriel
- Change relacionado: `openspec/changes/integration-pipeline`

## Contexto

Os parceiros reenviam lotes, corrigem payloads e enviam o mesmo registro por sistemas diferentes. Repetir, reordenar ou paralelizar não pode duplicar registros nem corromper vínculos. A entrega é ao-menos-uma-vez ([ADR-0011](0011-outbox-como-inbox-em-postgresql.md)).

## Opções consideradas

1. **Verificar em código antes de gravar:** sujeito a corrida entre instâncias.
2. **Constraints únicas do banco como última palavra, em duas camadas (escolhida).**

## Decisão

Duas camadas, ambas garantidas pelo banco:

1. **Chave da mensagem:** SHA-256 de `source`, `type`, `externalId` e do payload em JSON canônico (chaves ordenadas), em `outbox_message.idempotency_key` (`UNIQUE`). A ingestão usa `INSERT ... ON CONFLICT DO NOTHING`; o reenvio idêntico é contado em `duplicates`. A estratégia de fusão não entra na chave: o mesmo payload reenviado com outra estratégia é duplicata.
2. **Chaves naturais:** o registro é identificado por (`source`, `externalId`) e, na ausência, pela inscrição fiscal, ambos únicos no banco. Os vínculos externos ficam na tabela `external_reference` (`UNIQUE (entity_type, source, external_id)`), criada na migration V2 em substituição ao par `external_source`/`external_id` do V1, que comportava só um parceiro por registro. Assim um mesmo CNPJ pode ser conhecido por vários sistemas. Se dois workers criarem o mesmo CNPJ ao mesmo tempo, o segundo recebe a violação, é reagendado ([ADR-0014](0014-classificacao-de-falhas-e-backoff-com-jitter.md)) e na tentativa seguinte reconcilia com o registro criado.

Cada mensagem é processada em uma transação própria: ou todas as suas entidades são gravadas, ou nenhuma. O reenvio de um produtor nunca remove propriedades já vinculadas.

## Consequências

- Positivas: reenvio, corrida e reprocessamento convergem para o mesmo estado; sem duplicatas.
- Negativas e riscos: `external_reference` não tem chave estrangeira para o registro (a tabela depende do tipo); um vínculo órfão de um registro excluído é ignorado na busca e substituído no próximo vínculo.
- O que precisa mudar na documentação ou nas specs: o V2 documenta a nova tabela com `COMMENT ON`.
