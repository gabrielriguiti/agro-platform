## Purpose

Garante, por teste automatizado, que a arquitetura hexagonal não se degrada com o tempo.

## ADDED Requirements

### Requirement: Domínio sem dependência de framework
Classes do pacote `domain` DEVEM depender apenas de Java padrão e de outras classes do próprio `domain`.

#### Scenario: Domínio importa Spring
- **WHEN** uma classe de `domain` importa um tipo de `org.springframework` ou `jakarta.persistence`
- **THEN** o teste de arquitetura falha apontando a classe e o import

### Requirement: Aplicação sem dependência de infraestrutura
Classes de `application` DEVEM depender de `domain` e de interfaces (portas) declaradas em `application`, nunca de `infrastructure`.

#### Scenario: Caso de uso importa adaptador
- **WHEN** uma classe de `application` importa uma classe de `infrastructure`
- **THEN** o teste de arquitetura falha

### Requirement: Entrada web passa pelos casos de uso
Controllers DEVEM chamar casos de uso de `application` e NÃO DEVEM acessar repositórios ou adaptadores de persistência diretamente.

#### Scenario: Controller usa repositório
- **WHEN** um controller depende de um tipo de persistência
- **THEN** o teste de arquitetura falha

### Requirement: Motor financeiro puro
As classes de cálculo financeiro DEVEM ser testáveis sem contexto do Spring e NÃO DEVEM importar `org.springframework`.

#### Scenario: Calculadora com anotação do Spring
- **WHEN** uma calculadora financeira importa `org.springframework`
- **THEN** o teste de arquitetura falha
