# producer-registry Specification

## Purpose

Cadastro de produtores, vinculados a exatamente um laboratório e a várias propriedades.

## Requirements

### Requirement: Cadastro de produtor
O produtor DEVE ter nome, inscrição fiscal, endereço, `dataInicial`, `dataFinal`, `observacoes` (opcional), `laboratorioId` obrigatório e `propriedadeIds` opcional; a criação (`POST /api/producers`) DEVE responder 201 com `Location`.

#### Scenario: Criação completa
- **WHEN** um produtor é criado com laboratório existente e duas propriedades livres
- **THEN** a API responde 201 e a consulta devolve o produtor com o laboratório e as duas propriedades

### Requirement: Exatamente um laboratório
O produtor DEVE estar vinculado a exatamente um laboratório existente.

#### Scenario: Sem laboratório
- **WHEN** `laboratorioId` não é informado
- **THEN** a API responde 400

#### Scenario: Laboratório inexistente
- **WHEN** `laboratorioId` não corresponde a nenhum laboratório
- **THEN** a API responde 404 e nada é gravado

### Requirement: Período válido
`dataFinal` NÃO DEVE ser anterior a `dataInicial`; ambas DEVEM ser instantes ISO-8601.

#### Scenario: Período invertido
- **WHEN** `dataFinal` é anterior a `dataInicial`
- **THEN** a API responde 400 com mensagem clara

### Requirement: Propriedades do produtor
Ao criar ou atualizar, as propriedades informadas DEVEM existir e não pertencer a outro produtor; na atualização, o conjunto informado substitui o anterior e as propriedades removidas ficam livres.

#### Scenario: Atualizar conjunto de propriedades
- **WHEN** um produtor com as propriedades A e B é atualizado com apenas B e C
- **THEN** A fica livre, B permanece e C é vinculada

#### Scenario: Propriedade de outro produtor
- **WHEN** a atualização inclui uma propriedade que pertence a outro produtor
- **THEN** a API responde 409 e nenhuma alteração é aplicada

### Requirement: Contrato de consulta
A consulta de um produtor DEVE devolver o payload do enunciado: dados do produtor, `propriedades` (id, nome, inscrição fiscal formatada, endereço) e `laboratorio` embutidos.

#### Scenario: Consulta por id
- **WHEN** `GET /api/producers/{id}` é chamado
- **THEN** a resposta contém `laboratorio` e a lista `propriedades`

### Requirement: Listagem paginada sem N+1
`GET /api/producers` DEVE ser paginada (`page`, `size`) e carregar laboratórios e propriedades da página em número constante de consultas, independente do tamanho da página.

#### Scenario: Página de 50 produtores
- **WHEN** uma página com 50 produtores é listada
- **THEN** a listagem executa no máximo 3 consultas ao banco

### Requirement: Exclusão
A exclusão de um produtor DEVE remover o produtor e liberar suas propriedades, que continuam existindo.

#### Scenario: Excluir produtor
- **WHEN** um produtor com 2 propriedades é excluído
- **THEN** as 2 propriedades permanecem cadastradas e livres

### Requirement: Erros padronizados
Erros DEVEM ser devolvidos em `application/problem+json` com `title`, `status`, `detail` e, em validação, a lista de campos inválidos.

#### Scenario: Validação com vários campos
- **WHEN** o corpo tem nome vazio e CPF inválido
- **THEN** o erro 400 lista os dois campos
