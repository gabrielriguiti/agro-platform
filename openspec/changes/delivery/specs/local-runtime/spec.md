## Purpose

Qualquer pessoa clona o repositório e roda o sistema completo com um comando.

## ADDED Requirements

### Requirement: Subir tudo com um comando
`docker compose up --build` na raiz DEVE subir PostgreSQL, API e web, com a API aguardando o banco saudável e aplicando as migrations.

#### Scenario: Máquina limpa
- **WHEN** o desenvolvedor clona o repositório e roda `docker compose up --build`
- **THEN** o web responde em `http://localhost:8081` e o Swagger UI em `http://localhost:8080/swagger-ui.html`

### Requirement: Credenciais locais descartáveis
O Compose DEVE usar credenciais locais fixas (`agro`/`agro`) definidas apenas para desenvolvimento, e nenhuma credencial real DEVE existir no repositório.

#### Scenario: Varredura de segredos
- **WHEN** o repositório é inspecionado em busca de senhas, chaves ou hosts internos
- **THEN** nada além das credenciais locais é encontrado

### Requirement: Seed fictício opcional
Com o profile `demo`, a aplicação DEVE carregar dados fictícios idempotentes (laboratórios, propriedades, produtores e um lote de integração com um item inválido), usando inscrições fiscais válidas e inexistentes na vida real.

#### Scenario: Seed repetido
- **WHEN** a aplicação sobe duas vezes com o profile `demo`
- **THEN** não há registros duplicados

### Requirement: Execução sem Docker para a API
A API DEVE rodar direto com `mvn spring-boot:run` apontando para um PostgreSQL por variáveis `DB_URL`, `DB_USER` e `DB_PASSWORD`.

#### Scenario: Banco externo
- **WHEN** as variáveis de banco são definidas e a API sobe
- **THEN** ela conecta, migra e fica saudável
