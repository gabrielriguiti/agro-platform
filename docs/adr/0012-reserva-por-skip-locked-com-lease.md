# 0012. Reserva por SKIP LOCKED com lease

- Status: aceito
- Data: 2026-11-06
- Decisores: Gabriel
- Change relacionado: `openspec/changes/integration-pipeline`

## Contexto

Várias instâncias do worker podem rodar ao mesmo tempo. Duas não podem processar a mesma mensagem simultaneamente, e um worker que morre no meio não pode deixar a mensagem presa em `PROCESSING` para sempre nem fazer o sistema repeti-la sem contar tentativas.

## Opções consideradas

1. **`SELECT` e depois `UPDATE` sem trava:** duas instâncias leem a mesma mensagem; corrida.
2. **Trava de linha com `FOR UPDATE` (sem `SKIP LOCKED`):** correto, mas as instâncias se enfileiram atrás da trava.
3. **`FOR UPDATE SKIP LOCKED` com lease (`locked_until`) (escolhida):** cada instância pula o que outra já travou, e uma reserva vencida volta a ser elegível.

## Decisão

A reserva é um único comando `UPDATE ... FROM (SELECT ... FOR UPDATE SKIP LOCKED) RETURNING`, que marca a mensagem `PROCESSING`, define `locked_until` e **incrementa `attempts` na própria reserva**. São elegíveis as `PENDING` com `next_attempt_at` vencido e as `PROCESSING` com lease vencido. Contar a tentativa na reserva garante que um worker que morre ainda consome uma tentativa, e a mensagem acaba na DLQ em vez de repetir indefinidamente.

## Consequências

- Positivas: exclusão mútua sem coordenação externa; recuperação automática de worker morto; testado com dois workers concorrentes e com lease vencido.
- Negativas e riscos: se o tempo de processamento superar o lease, outra instância pode reservar a mensagem enquanto a primeira ainda trabalha; por isso o processamento é idempotente. O lease padrão é de 2 minutos (`agro.integration.lease`).
- O que precisa mudar na documentação ou nas specs: a configuração é listada no README.
