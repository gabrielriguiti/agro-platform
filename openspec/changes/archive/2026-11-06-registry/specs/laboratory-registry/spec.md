## Purpose

Cadastro estrutural de laboratórios, com existência autônoma.

## ADDED Requirements

### Requirement: Cadastro com atributos globais
O laboratório DEVE ter nome, inscrição fiscal válida e endereço, todos obrigatórios, e a criação (`POST /api/laboratories`) DEVE responder 201 com o cabeçalho `Location`.

#### Scenario: Criação válida
- **WHEN** um laboratório é criado com nome, CNPJ válido e endereço
- **THEN** a API responde 201 com o id gerado

#### Scenario: Campo obrigatório ausente
- **WHEN** o endereço não é informado
- **THEN** a API responde 400 com o campo `endereco` apontado no erro

### Requirement: Consulta e listagem
A API DEVE permitir consultar um laboratório por id (404 se não existir) e listar todos, ordenados por nome.

#### Scenario: Laboratório inexistente
- **WHEN** `GET /api/laboratories/999999` é chamado
- **THEN** a API responde 404

### Requirement: Atualização e exclusão
A API DEVE permitir atualizar os dados do laboratório e exclui-lo apenas quando nenhum produtor estiver vinculado a ele.

#### Scenario: Exclusão com produtores vinculados
- **WHEN** um laboratório com 3 produtores é excluído
- **THEN** a API responde 409 e o laboratório permanece
