## 1. Domínio e regras puras

- [x] 1.1 Implementar `BackoffPolicy`, `OutboxMessage` e transições de estado; verificar testes de unidade (progressão 5/10/20/40/80 s, teto de 10 min)
- [x] 1.2 Implementar `ReconciliationPolicy` (MERGE, OVERWRITE, KEEP_EXISTING); verificar testes de unidade por estratégia
- [x] 1.3 Implementar a chave de idempotência (JSON canônico + SHA-256); verificar que ordem de campos não altera a chave

## 2. Persistência da fila

- [x] 2.1 Implementar o adaptador JDBC do `OutboxPort` (criar lote, enfileirar com `ON CONFLICT DO NOTHING`, reservar com `SKIP LOCKED` e lease, atualizar, reenfileirar); verificar testes `*IT`
- [x] 2.2 Teste concorrente: dois workers reservando ao mesmo tempo; verificar conjuntos disjuntos
- [x] 2.3 Teste do lease vencido; verificar que a mensagem volta a ser elegível com a tentativa contada

## 3. Ingestão

- [x] 3.1 Implementar o endpoint `POST /api/integration/batches` com a validação do envelope e o limite de itens; verificar 202, 400 e 413
- [x] 3.2 Teste de atomicidade do lote (falha no meio não grava nada); verificar
- [x] 3.3 Teste de reenvio idêntico (`duplicates`); verificar

## 4. Processamento

- [x] 4.1 Implementar o sincronizador de laboratório e de propriedade com resolução de identidade; verificar cenários de reconciliação
- [x] 4.2 Implementar o sincronizador de produtor com entidades aninhadas e vínculos preservados; verificar o cenário do payload do enunciado e o reprocessamento sem duplicatas
- [x] 4.3 Implementar o processador (worker) com classificação de falhas, backoff e DLQ; verificar testes com falhas injetadas (transitória até esgotar, permanente direta)
- [x] 4.4 Teste de corrida por CNPJ entre duas mensagens; verificar que termina com um único registro
- [x] 4.5 Teste de atomicidade por mensagem (falha na segunda propriedade não deixa nada); verificar

## 5. Monitoramento e reprocesso

- [x] 5.1 Implementar `GET` de lotes, detalhe e mensagens filtráveis; verificar contagens e estado derivado
- [x] 5.2 Implementar reprocessamento de mensagem e de lote (409 fora de `DEAD`); verificar
- [x] 5.3 Implementar a execução manual do worker configurável; verificar 404 quando desabilitada

## 6. Documentação

- [x] 6.1 Registrar os ADRs 0011 a 0015; verificar o índice
- [x] 6.2 Diagramas de sequência (ingestão e processamento) e de estados da mensagem em Mermaid; verificar a renderização no GitHub
- [x] 6.3 Cenários BDD (Gherkin) de idempotência, retry até DLQ e reconciliação em `docs/bdd/`; verificar o mapeamento para os testes
