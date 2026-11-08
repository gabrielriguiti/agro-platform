# Cenários BDD

Cenários em Gherkin (em português) dos fluxos críticos: pipeline de integração e consulta analítica. Não são executados por um runner: cada cenário aponta para o teste automatizado que o verifica, e o texto serve de documentação viva do comportamento esperado.

| Feature | Testes que a verificam |
|---|---|
| [idempotencia.feature](idempotencia.feature) | `BatchIngestionApiIT`, `IdempotencyKeyTest`, `IntegrationProcessingIT` |
| [retry-e-dlq.feature](retry-e-dlq.feature) | `BackoffPolicyTest`, `IntegrationProcessingIT`, `IntegrationMonitoringApiIT` |
| [reconciliacao.feature](reconciliacao.feature) | `ReconciliationPolicyTest`, `IntegrationProcessingIT` |
| [analise-de-laboratorios.feature](analise-de-laboratorios.feature) | `LabAnalyticsApiIT`, `LabAnalyticsQueryIT` |

Os testes ficam em `apps/api/src/test/java/br/com/gabrielriguiti/agro/`.
