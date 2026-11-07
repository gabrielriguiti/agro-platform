## Purpose

Trata falhas de integração com retentativas inteligentes e desvia o que não tem conserto para uma fila de erro.

## ADDED Requirements

### Requirement: Classificação de falhas
Falhas DEVEM ser classificadas em permanentes (JSON malformado, campo obrigatório ausente, CPF/CNPJ inválido, conflito de identidade) e transitórias (indisponibilidade ou conflito do banco, violação de unicidade por corrida, timeout, erro desconhecido).

#### Scenario: CPF inválido
- **WHEN** uma mensagem traz uma inscrição fiscal inválida
- **THEN** ela vai direto para `DEAD` na primeira tentativa, com o motivo registrado

#### Scenario: Indisponibilidade do banco
- **WHEN** o processamento falha por erro transitório de banco
- **THEN** a mensagem volta para `PENDING` com `nextAttemptAt` no futuro

### Requirement: Backoff exponencial com jitter
Após a falha transitória da tentativa `n`, o próximo agendamento DEVE ser `min(max, base × 2^(n-1)) × (1 + jitter × r)`, com `r` aleatório em [0,1); padrões: base 5 s, teto 10 min, jitter 0,1.

#### Scenario: Progressão do atraso
- **WHEN** a mesma mensagem falha transitoriamente em 5 tentativas consecutivas, com jitter zero
- **THEN** os atrasos são 5 s, 10 s, 20 s, 40 s e 80 s, e nunca passam de 10 min

### Requirement: Dead Letter
Ao esgotar o máximo de tentativas (padrão 5) ou ao ocorrer falha permanente, a mensagem DEVE passar a `DEAD`, preservando `attempts`, `lastError` e o payload original.

#### Scenario: Tentativas esgotadas
- **WHEN** a quinta tentativa falha transitoriamente
- **THEN** a mensagem fica `DEAD` com o último erro registrado

### Requirement: Reprocessamento manual
A API DEVE permitir reenfileirar uma mensagem `DEAD` (`POST /api/integration/messages/{id}/reprocess`) ou todas as de um lote (`POST /api/integration/batches/{id}/reprocess`), voltando para `PENDING` com zero tentativas.

#### Scenario: Reprocessar após corrigir a causa
- **WHEN** uma mensagem `DEAD` é reprocessada e a causa já foi corrigida
- **THEN** ela é concluída como `DONE` sem criar duplicatas

#### Scenario: Reprocessar mensagem que não está em DEAD
- **WHEN** o reprocessamento é pedido para uma mensagem `DONE`
- **THEN** a API responde 409 e nada muda
