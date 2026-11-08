# project-documentation Specification

## Purpose

Entrega a documentação que o enunciado exige e que torna o projeto compreensível e defensável.

## Requirements

### Requirement: README completo
O README DEVE conter visão geral, diagrama de arquitetura, diagrama ER, guia de execução (Docker e local), decisões de design (com links para os ADRs), instruções de teste e links úteis (Swagger UI e demo).

#### Scenario: Novo leitor
- **WHEN** alguém lê o README pela primeira vez
- **THEN** consegue subir o sistema, rodar os testes e entender a arquitetura sem outros arquivos

### Requirement: Diagramas em Mermaid
Os diagramas (arquitetura em camadas, ER, sequência da ingestão e do processamento, estados da mensagem) DEVEM estar em Mermaid, renderizados pelo GitHub.

#### Scenario: Renderização
- **WHEN** o README é aberto no GitHub
- **THEN** os diagramas aparecem renderizados

### Requirement: Cenários BDD
Os fluxos críticos (idempotência, retry até a DLQ, reconciliação, consulta analítica) DEVEM ter cenários em Gherkin em `docs/bdd/`, cada um indicando o teste automatizado que o cobre.

#### Scenario: Rastreabilidade
- **WHEN** um cenário Gherkin é lido
- **THEN** ele aponta para o teste que o verifica

### Requirement: Declaração de uso de IA
O README DEVE ter a seção "Uso de IA" declarando que o projeto foi desenvolvido com assistência de IA no fluxo spec-driven, que o autor revisou e responde pelo código, e como as specs e ADRs guiaram o trabalho.

#### Scenario: Transparência
- **WHEN** o avaliador procura como a IA foi usada
- **THEN** encontra a seção com o fluxo e o papel do autor

### Requirement: Premissas e limitações explícitas
O README DEVE listar as premissas assumidas (fórmulas financeiras, semântica do total filtrado, atributos fiscais do produtor, exemplos do enunciado com CPF/CNPJ inválidos) e as limitações conhecidas.

#### Scenario: Pergunta sobre uma fórmula
- **WHEN** o avaliador pergunta de onde vieram as fórmulas financeiras
- **THEN** o README diz que são premissas configuráveis e aponta o ADR
