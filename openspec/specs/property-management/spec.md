# property-management Specification

## Purpose

Cadastrar e manter propriedades pela interface, para que o formulário de produtor tenha propriedades a vincular.

## Requirements

### Requirement: Listagem
A tela DEVE listar todas as propriedades com nome, inscrição fiscal, endereço e a situação (disponível ou vinculada a produtor), e tratar carregando, vazio ("Nenhuma propriedade cadastrada") e erro com "Tentar de novo".

A lista DEVE ser paginada no cliente, com 10 propriedades por página (a API devolve a lista inteira).

#### Scenario: Situação da propriedade
- **WHEN** uma propriedade tem produtor
- **THEN** a linha a mostra como vinculada; sem produtor, como disponível

#### Scenario: Mais propriedades que o tamanho da página
- **WHEN** existem 23 propriedades
- **THEN** a tela mostra 10 por página e a navegação chega à terceira página com 3 linhas

### Requirement: Formulário
O formulário DEVE ter nome, inscrição fiscal (CPF ou CNPJ) e endereço, todos obrigatórios, com máscara e validação dos dígitos verificadores no cliente e mensagem junto ao campo. A propriedade nasce disponível; o vínculo com o produtor é feito no formulário de produtor.

#### Scenario: Criar propriedade
- **WHEN** o usuário informa nome, CPF ou CNPJ válido e endereço e salva
- **THEN** a propriedade é criada, volta à lista com a confirmação e fica disponível para seleção no formulário de produtor

Ao salvar com erros, o formulário DEVE mostrar no topo um resumo ("Corrija N campos para salvar") além do erro junto a cada campo. Na edição, o formulário DEVE mostrar a seção "Vínculo com produtor": a quem a propriedade está vinculada (ou que está disponível) e que o vínculo é alterado no formulário de produtor.

#### Scenario: Inscrição fiscal inválida
- **WHEN** o usuário digita um CPF com dígito verificador errado
- **THEN** o campo mostra o erro e o envio é bloqueado

#### Scenario: Campos vazios
- **WHEN** o usuário salva sem preencher nada
- **THEN** o topo mostra "Corrija 3 campos para salvar" e cada campo mostra "Informe ..."

### Requirement: Erros do servidor
Erros 400/409 DEVEM ser exibidos junto aos campos ou em um aviso do formulário, sem perder o que foi digitado.

#### Scenario: Inscrição de outra propriedade
- **WHEN** a API responde 409 por inscrição fiscal já cadastrada
- **THEN** o aviso (com "HTTP 409") aparece no topo, o erro aparece no campo e o formulário mantém os dados

### Requirement: Editar e excluir
Cada linha DEVE ter Editar e Excluir (com confirmação). A confirmação DEVE informar que excluir uma propriedade vinculada a desvincula do produtor, que continua existindo, e mostrar a situação e o produtor vinculado.

#### Scenario: Excluir propriedade vinculada
- **WHEN** o usuário confirma a exclusão de uma propriedade vinculada
- **THEN** a propriedade some da lista e o produtor permanece
