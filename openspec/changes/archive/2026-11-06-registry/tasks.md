## 1. Domínio

- [x] 1.1 Implementar `TaxId` e `Identity` com testes de unidade (CPF/CNPJ válidos, dígitos errados, repetidos, máscara); verificar os cenários da spec `tax-id`
- [x] 1.2 Implementar `Producer`, `Property` e `Laboratory` com invariantes (período, laboratório obrigatório); verificar testes de unidade

## 2. Persistência

- [x] 2.1 Completar a migration com `COMMENT ON` e índices; verificar a migration em teste de integração
- [x] 2.2 Implementar os adaptadores JPA e os mappers; verificar teste de integração de cada repositório
- [x] 2.3 Implementar a listagem em lote (página, laboratórios, propriedades); verificar teste que conta as consultas (no máximo 3)

## 3. Casos de uso e API

- [x] 3.1 Implementar os casos de uso de laboratório e propriedade; verificar os cenários das specs
- [x] 3.2 Implementar os casos de uso de produtor (criar, atualizar conjunto de propriedades, excluir); verificar os cenários, incluindo o 409 de propriedade alheia
- [x] 3.3 Implementar os controllers, DTOs e o `ProblemDetail` global; verificar testes `*IT` dos endpoints
- [x] 3.4 Teste concorrente de vínculo da mesma propriedade por dois produtores; verificar que só um vence

## 4. Documentação

- [x] 4.1 Registrar os ADRs 0008 e 0009; verificar o índice
- [x] 4.2 Javadoc dos tipos públicos e anotações OpenAPI dos endpoints; verificar o Swagger UI
