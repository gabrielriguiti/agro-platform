## Purpose

Cadastrar e manter laboratórios pela interface, para que o formulário de produtor tenha laboratórios a escolher.

## ADDED Requirements

### Requirement: Listagem
A tela DEVE listar os laboratórios, na ordem da API (por nome), com nome, CNPJ e endereço, e tratar carregando, vazio ("Nenhum laboratório cadastrado") e erro com "Tentar de novo".

#### Scenario: Lista vazia
- **WHEN** não há laboratórios
- **THEN** a tela mostra "Nenhum laboratório cadastrado" e a ação de cadastrar o primeiro

### Requirement: Formulário
O formulário DEVE ter nome, inscrição fiscal e endereço, todos obrigatórios, com máscara e validação de CNPJ no cliente (laboratório não aceita CPF), e mensagem junto ao campo.

Ao salvar com erros, o formulário DEVE mostrar no topo um resumo ("Corrija N campos para salvar") além do erro junto a cada campo.

#### Scenario: Criar laboratório
- **WHEN** o usuário informa nome, CNPJ válido e endereço e salva
- **THEN** o laboratório é criado e o usuário volta à lista com a confirmação

#### Scenario: CPF no lugar do CNPJ
- **WHEN** o usuário digita um CPF válido
- **THEN** o campo indica que o laboratório exige CNPJ e o envio é bloqueado

### Requirement: Erros do servidor
Erros 400/409 DEVEM ser exibidos junto aos campos ou em um aviso do formulário, sem perder o que foi digitado.

#### Scenario: CNPJ de outro laboratório
- **WHEN** a API responde 409 por CNPJ já cadastrado
- **THEN** o aviso (com "HTTP 409") aparece no topo, o erro aparece no campo CNPJ e o formulário mantém os dados

### Requirement: Editar e excluir
Cada linha DEVE ter Editar (abre o formulário preenchido) e Excluir (com confirmação). A exclusão de laboratório com produtores vinculados (409) DEVE mostrar o motivo e manter o laboratório.

#### Scenario: Excluir laboratório com produtores
- **WHEN** o usuário confirma a exclusão de um laboratório com produtores vinculados
- **THEN** o aviso "Há produtores vinculados" aparece e o laboratório continua na lista
