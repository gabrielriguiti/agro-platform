## Purpose

Cadastro de propriedades, que existem sem produtor e podem ser vinculadas a no máximo um.

## ADDED Requirements

### Requirement: Existência autônoma
A propriedade DEVE poder ser cadastrada sem produtor, com nome, inscrição fiscal válida e endereço obrigatórios.

#### Scenario: Propriedade sem produtor
- **WHEN** uma propriedade é criada sem vínculo
- **THEN** ela é aceita e aparece como disponível

### Requirement: Listagem para seleção
`GET /api/properties` DEVE listar propriedades e aceitar o filtro `disponiveis=true` para devolver apenas as sem produtor (mais as do produtor em edição, quando `produtorId` é informado).

#### Scenario: Selecionar propriedades ao editar produtor
- **WHEN** o web lista propriedades para editar o produtor 7 com `disponiveis=true&produtorId=7`
- **THEN** a resposta traz as propriedades livres e as já vinculadas ao produtor 7, e nenhuma vinculada a outro

### Requirement: Vínculo exclusivo
Uma propriedade DEVE pertencer a no máximo um produtor.

#### Scenario: Propriedade de outro produtor
- **WHEN** um produtor tenta se vincular a uma propriedade que já pertence a outro
- **THEN** a API responde 409 e o vínculo existente não muda

### Requirement: Exclusão desvincula
A exclusão de uma propriedade DEVE removê-la do produtor sem afetar o restante.

#### Scenario: Excluir propriedade vinculada
- **WHEN** uma propriedade vinculada é excluída
- **THEN** o produtor continua existindo sem ela
