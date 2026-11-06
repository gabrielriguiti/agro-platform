# tax-id Specification

## Purpose

Garante que toda inscrição fiscal (CPF ou CNPJ) seja válida, normalizada e única.

## Requirements

### Requirement: Validação de CPF
Um CPF DEVE ter 11 dígitos, com os dois dígitos verificadores corretos, e NÃO DEVE ser uma sequência de dígitos repetidos.

#### Scenario: CPF válido com máscara
- **WHEN** a inscrição `529.982.247-25` é informada
- **THEN** ela é aceita e normalizada para `52998224725`

#### Scenario: CPF com dígito verificador errado
- **WHEN** a inscrição `123.456.789-00` é informada
- **THEN** ela é rejeitada com a mensagem "CPF/CNPJ inválido"

#### Scenario: CPF de dígitos repetidos
- **WHEN** a inscrição `111.111.111-11` é informada
- **THEN** ela é rejeitada

### Requirement: Validação de CNPJ
Um CNPJ DEVE ter 14 dígitos, com os dois dígitos verificadores corretos, e NÃO DEVE ser uma sequência de dígitos repetidos.

#### Scenario: CNPJ válido
- **WHEN** a inscrição `11.222.333/0001-81` é informada
- **THEN** ela é aceita e normalizada para `11222333000181`

#### Scenario: CNPJ inválido
- **WHEN** a inscrição `11.222.333/0001-44` é informada
- **THEN** ela é rejeitada

### Requirement: Entrada tolerante, armazenamento normalizado
A entrada PODE conter máscara (pontos, traço, barra e espaços); o armazenamento DEVE guardar apenas dígitos e a saída da API DEVE devolver a inscrição formatada.

#### Scenario: Consulta devolve máscara
- **WHEN** um laboratório com CNPJ `11222333000181` é consultado
- **THEN** a resposta traz `11.222.333/0001-81`

#### Scenario: Caracteres inválidos
- **WHEN** a inscrição contém letras
- **THEN** ela é rejeitada com erro de validação

### Requirement: Unicidade por tipo de entidade
A inscrição fiscal DEVE ser única entre produtores, entre propriedades e entre laboratórios, garantida por constraint do banco.

#### Scenario: Duplicidade
- **WHEN** um segundo laboratório é cadastrado com um CNPJ já existente
- **THEN** a API responde 409 e nenhum registro novo é criado
