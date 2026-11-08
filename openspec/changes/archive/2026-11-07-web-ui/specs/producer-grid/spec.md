## Purpose

Listagem operacional de produtores com ações de edição e exclusão.

## ADDED Requirements

### Requirement: Listagem paginada
O grid DEVE mostrar nome, inscrição fiscal, laboratório, quantidade de propriedades e período, com paginação.

#### Scenario: Muitas linhas
- **WHEN** existem mais produtores que o tamanho da página
- **THEN** o usuário navega entre as páginas sem recarregar tudo

### Requirement: Editar e excluir
Cada linha DEVE ter as ações Editar (abre o formulário preenchido) e Excluir (com confirmação).

#### Scenario: Excluir com confirmação
- **WHEN** o usuário aciona Excluir e confirma
- **THEN** o produtor é removido e a linha some

#### Scenario: Cancelar exclusão
- **WHEN** o usuário cancela a confirmação
- **THEN** nada é removido

### Requirement: Estados da tela
O grid DEVE mostrar estados de carregando, vazio ("Nenhum produtor cadastrado") e erro com opção de tentar de novo.

#### Scenario: Falha de rede
- **WHEN** a listagem falha
- **THEN** a tela mostra o erro e um botão "Tentar de novo"
