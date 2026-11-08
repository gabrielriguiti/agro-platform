# producer-form Specification

## Purpose

Criar e editar produtores com validação clara e seleção de laboratório e propriedades.

## Requirements

### Requirement: Campos do formulário
O formulário DEVE ter nome, inscrição fiscal (com máscara de CPF/CNPJ), endereço, data e hora inicial, data e hora final, observações, um select de laboratório (escolha única, obrigatório) e um multi-select de propriedades.

#### Scenario: Criar produtor
- **WHEN** o usuário preenche os campos, escolhe um laboratório e duas propriedades e salva
- **THEN** o produtor é criado e o usuário volta ao grid com a confirmação

### Requirement: Validação no cliente
O formulário DEVE validar obrigatoriedade, dígitos verificadores da inscrição fiscal e período (final não anterior ao inicial) antes de enviar, com mensagem junto ao campo.

#### Scenario: CPF inválido
- **WHEN** o usuário digita um CPF com dígito verificador errado
- **THEN** o campo mostra o erro e o envio é bloqueado

### Requirement: Multi-select de propriedades
O multi-select DEVE listar as propriedades disponíveis (e as do produtor em edição), permitir busca por nome, marcar e desmarcar e mostrar a quantidade selecionada.

#### Scenario: Editar conjunto
- **WHEN** o usuário desmarca uma propriedade e marca outra e salva
- **THEN** o produtor passa a ter o novo conjunto

### Requirement: Orientação com banco vazio
Quando não houver laboratório ou propriedade disponível, o formulário DEVE avisar junto ao campo correspondente e oferecer link para o cadastro (de laboratório ou de propriedade).

#### Scenario: Banco sem laboratório e sem propriedade
- **WHEN** o usuário abre o formulário e a API não devolve laboratórios nem propriedades
- **THEN** aparecem "Nenhum laboratório cadastrado" e "Nenhuma propriedade disponível", cada um com link para o cadastro correspondente

### Requirement: Erros do servidor
Erros 400/409 do servidor DEVEM ser exibidos junto aos campos correspondentes ou em um aviso do formulário, sem perder o que foi digitado.

#### Scenario: Propriedade já vinculada
- **WHEN** a API responde 409 por propriedade de outro produtor
- **THEN** o aviso aparece, o formulário mantém os dados e a lista de propriedades é recarregada
