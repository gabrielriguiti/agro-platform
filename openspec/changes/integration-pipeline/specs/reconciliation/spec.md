## Purpose

Identifica o mesmo registro entre sistemas externos e o banco interno e aplica a estratégia de fusão escolhida.

## ADDED Requirements

### Requirement: Resolução de identidade
O processamento DEVE localizar o registro existente primeiro por (`source`, `externalId`) e, na ausência, pela inscrição fiscal normalizada.

#### Scenario: Mesmo CNPJ vindo de outro sistema
- **WHEN** o sistema B envia um laboratório cujo CNPJ já existe, cadastrado pelo sistema A
- **THEN** o registro existente é reconciliado em vez de duplicado, e passa a ter também o vínculo externo do sistema B

### Requirement: Estratégias de fusão
O lote DEVE informar `mergeStrategy`: `MERGE` (preserva o existente e preenche só campos vazios), `OVERWRITE` (o recebido substitui o existente) ou `KEEP_EXISTING` (ignora o recebido).

#### Scenario: MERGE
- **WHEN** o existente tem endereço e o recebido traz nome diferente e o mesmo endereço
- **THEN** nome e endereço do existente permanecem, e apenas campos vazios são preenchidos

#### Scenario: OVERWRITE
- **WHEN** o lote usa `OVERWRITE`
- **THEN** nome e endereço passam a ser os do payload

#### Scenario: KEEP_EXISTING
- **WHEN** o lote usa `KEEP_EXISTING` e o registro já existe
- **THEN** nada muda e a mensagem é concluída como `DONE`

### Requirement: Conflito de identidade
Se o id externo apontar para um registro e a inscrição fiscal recebida pertencer a outro, a mensagem DEVE falhar de forma permanente com o motivo "conflito de identidade", sem alterar nenhum registro.

#### Scenario: Duas identidades
- **WHEN** `externalId: L-1` já aponta para o laboratório X e o payload traz o CNPJ do laboratório Y
- **THEN** a mensagem vai para `DEAD` com "conflito de identidade" e X e Y permanecem intactos

### Requirement: Entidades aninhadas
Um produtor PODE trazer `laboratorio` e `propriedades` aninhados (contrato do enunciado); cada um DEVE ser reconciliado pelas mesmas regras, usando o `id` aninhado como id externo, e vinculado ao produtor na mesma transação.

#### Scenario: Produtor com laboratório e duas propriedades novas
- **WHEN** o payload do enunciado (com valores válidos) é processado em banco vazio
- **THEN** são criados 1 laboratório, 2 propriedades e 1 produtor, corretamente vinculados

#### Scenario: Reprocessar o mesmo produtor
- **WHEN** o mesmo payload é processado de novo
- **THEN** a contagem de registros e de vínculos não muda
