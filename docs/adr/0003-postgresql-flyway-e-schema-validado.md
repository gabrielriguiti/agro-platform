# 0003. PostgreSQL, Flyway e schema validado

- Status: aceito
- Data: 2026-11-05
- Decisores: Gabriel
- Change relacionado: `openspec/changes/foundation`

## Contexto

A integração precisa de unicidade garantida pelo banco (idempotência), `SELECT ... FOR UPDATE SKIP LOCKED` (fila em tabela) e consultas agregadas eficientes. O ambiente de demonstração usa o Postgres de desenvolvimento do autor, com database e role dedicados.

## Opções consideradas

1. **H2 em memória:** simples, mas não tem `SKIP LOCKED` com a mesma semântica e esconde diferenças de dialeto.
2. **PostgreSQL com Flyway (escolhida):** mesmo banco em testes (Testcontainers), local (Compose) e demo.

## Decisão

PostgreSQL com migrations versionadas em SQL pelo Flyway. O Hibernate roda em `ddl-auto: validate`: o schema pertence às migrations e a aplicação falha ao subir se o mapeamento divergir. Todo recurso do banco tem `COMMENT ON` nas tabelas principais.

## Consequências

- Positivas: schema revisável como SQL; divergência detectada no boot; testes contra o banco real.
- Negativas e riscos: testes de integração exigem Docker; migrations aplicadas não são editadas (correção vira migration nova).
- Documentação: diagrama ER no README.
