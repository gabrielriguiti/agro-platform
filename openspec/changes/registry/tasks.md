## 1. Domínio

- [ ] 1.1 Implementar `TaxId` e `Identity` com testes de unidade (CPF/CNPJ válidos, dígitos errados, repetidos, máscara); verificar os cenários da spec `tax-id`
- [ ] 1.2 Implementar `Producer`, `Property` e `Laboratory` com invariantes (período, laboratório obrigatório); verificar testes de unidade

## 2. Persistência

- [ ] 2.1 Completar a migration com `COMMENT ON` e índices; verificar a migration em teste de integração
- [ ] 2.2 Implementar os adaptadores JPA e os mappers; verificar teste de integração de cada repositório
- [ ] 2.3 Implementar a listagem em lote (página, laboratórios, propriedades); verificar teste que conta as consultas (no máximo 3)

## 3. Casos de uso e API

- [ ] 3.1 Implementar os casos de uso de laboratório e propriedade; verificar os cenários das specs
- [ ] 3.2 Implementar os casos de uso de produtor (criar, atualizar conjunto de propriedades, excluir); verificar os cenários, incluindo o 409 de propriedade alheia
- [ ] 3.3 Implementar os controllers, DTOs e o `ProblemDetail` global; verificar testes `*IT` dos endpoints
- [ ] 3.4 Teste concorrente de vínculo da mesma propriedade por dois produtores; verificar que só um vence

## 4. Documentação

- [ ] 4.1 Registrar os ADRs 0008 e 0009; verificar o índice
- [ ] 4.2 Javadoc dos tipos públicos e anotações OpenAPI dos endpoints; verificar o Swagger UI
