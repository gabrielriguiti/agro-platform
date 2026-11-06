# 0006. Logs estruturados e contrato OpenAPI

- Status: aceito
- Data: 2026-11-05
- Decisores: Gabriel
- Change relacionado: `openspec/changes/foundation`

## Contexto

O enunciado do desafio pede observabilidade (logs estruturados) e documentação de API funcional. O front-end consome a API apenas pelo contrato.

## Opções consideradas

1. **Logs em texto livre:** legíveis, difíceis de consultar.
2. **Logs em JSON com `logstash-logback-encoder` (escolhida):** um evento por linha, com campos consultáveis; o profile `local` mantém texto legível.
3. **OpenAPI gerado por springdoc (escolhida):** contrato derivado do código e Swagger UI.

## Decisão

Logs em JSON por padrão (texto no profile `local`), com identificador de correlação por requisição e de mensagem nas operações do integrador. OpenAPI 3 publicado em `/v3/api-docs` e Swagger UI em `/swagger-ui.html`.

## Consequências

- Positivas: logs consultáveis; contrato sempre sincronizado com o código.
- Negativas e riscos: é preciso nunca registrar segredos nem payloads completos com dados pessoais.
- Documentação: exemplos de chamadas no README.
