# ci-pipeline Specification

## Purpose

Verifica automaticamente cada mudança antes que ela chegue ao `main`.

## Requirements

### Requirement: Verificação completa a cada push em main e pull request
O workflow de CI DEVE rodar em todo push para `main` e em todo pull request, e ser reutilizável pelo workflow de deploy: build e testes da API (unitários e de integração com Testcontainers), typecheck, testes e build do web, e verificação de documentação.

#### Scenario: Teste quebrado
- **WHEN** um teste da API falha
- **THEN** o CI falha e o deploy não é disparado

#### Scenario: Pull request quebrado
- **WHEN** um teste falha num pull request
- **THEN** o CI fica vermelho e o sumário e os relatórios de teste ainda são publicados

### Requirement: Teste de arquitetura no CI
O teste de arquitetura (ArchUnit) DEVE fazer parte da verificação.

#### Scenario: Violação de camada
- **WHEN** um commit faz o domínio importar o Spring
- **THEN** o CI falha apontando a classe

### Requirement: Verificação de documentação
O CI DEVE verificar que os links internos da documentação resolvem e que todo ADR existente está no índice de `docs/adr/README.md`.

#### Scenario: ADR fora do índice
- **WHEN** um ADR novo não consta no índice
- **THEN** o CI falha

### Requirement: Relatório de testes
O CI DEVE publicar os relatórios de teste como artefato e resumir o resultado no sumário do job.

#### Scenario: Consulta ao resultado
- **WHEN** o CI termina
- **THEN** o sumário mostra quantos testes passaram e falharam
