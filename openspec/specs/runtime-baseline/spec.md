# runtime-baseline Specification

## Purpose

Garante uma aplicação que sobe de forma reprodutível, com banco migrado, saúde, contrato de API e logs estruturados.

## Requirements

### Requirement: Inicialização com migrations
A aplicação DEVE aplicar as migrations do Flyway ao subir e DEVE falhar se o mapeamento JPA divergir do schema (`ddl-auto: validate`).

#### Scenario: Banco vazio
- **WHEN** a aplicação sobe contra um banco PostgreSQL vazio
- **THEN** as migrations são aplicadas e a aplicação fica saudável

### Requirement: Saúde da aplicação
A aplicação DEVE expor `GET /actuator/health` com status `UP` quando o banco está acessível.

#### Scenario: Banco inacessível
- **WHEN** o banco está fora do ar
- **THEN** `/actuator/health` responde `DOWN`

### Requirement: Contrato OpenAPI
A aplicação DEVE publicar o contrato OpenAPI 3 em `/v3/api-docs` e o Swagger UI em `/swagger-ui.html`.

#### Scenario: Consulta ao contrato
- **WHEN** um cliente faz `GET /v3/api-docs`
- **THEN** recebe um documento OpenAPI 3 válido com os endpoints expostos

### Requirement: Logs estruturados
A aplicação DEVE escrever logs em JSON, um evento por linha, fora do profile `local`, e NÃO DEVE registrar segredos.

#### Scenario: Requisição atendida
- **WHEN** uma requisição HTTP é atendida
- **THEN** o log contém um evento JSON com nível, mensagem e identificador de correlação
