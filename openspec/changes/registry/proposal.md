## Why

Produtor, Propriedade e Laboratório são o núcleo do domínio e a base do integrador, do motor financeiro e da consulta analítica. Antes de tudo é preciso modelar a identidade fiscal (CPF/CNPJ), os vínculos e as invariantes, e expor o CRUD.

## What Changes

- Value Object `TaxId` (CPF/CNPJ com dígitos verificadores) e `Identity` (nome, inscrição fiscal, endereço), obrigatórios em toda entidade.
- CRUD de Laboratório e de Propriedade, ambos com existência autônoma.
- CRUD de Produtor com 1 laboratório obrigatório (N:1) e várias propriedades (1:N), no contrato de payload do enunciado.
- Listagens paginadas e sem consultas N+1, usadas pelo grid e pelos selects do web.
- Erros no formato `ProblemDetail` (RFC 9457).

## Capabilities

### New Capabilities
- `tax-id`: validação, normalização e unicidade da inscrição fiscal.
- `laboratory-registry`: cadastro de laboratórios.
- `property-registry`: cadastro de propriedades e vínculo exclusivo com um produtor.
- `producer-registry`: cadastro de produtores com laboratório e propriedades.

### Modified Capabilities

## Impact

Tabelas `laboratories`, `producers` e `properties` (migration `V1`), pacotes `domain`/`application`/`infrastructure` do contexto `registry`. Base dos changes `financial-engine`, `integration-pipeline` e `lab-analytics`. Depende de `foundation`.

**Premissas:** o payload de exemplo do enunciado não traz inscrição fiscal nem endereço do produtor, mas a regra de domínio exige os atributos globais em toda entidade; portanto o produtor DEVE informá-los. Os valores de exemplo do enunciado (`123.456.789-00` etc.) não passam na validação de dígitos; os testes e o seed usam valores válidos.

**Não objetivos:** autenticação de usuários, cadastro de propriedade/laboratório pelas telas do web (feitos pela API, pelo integrador e pelo seed).

## Documentação

ADRs previstos: "Agregados referenciados por id e vínculo da propriedade ao produtor"; "Identificação fiscal como Value Object". Javadoc, OpenAPI e `COMMENT ON` das tabelas.
