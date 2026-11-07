# 0014. Classificação de falhas e backoff com jitter

- Status: aceito
- Data: 2026-11-06
- Decisores: Gabriel
- Change relacionado: `openspec/changes/integration-pipeline`

## Contexto

Nem toda falha merece nova tentativa: um CPF inválido nunca vai passar, mas uma indisponibilidade do banco passa sozinha. Repetir tudo desperdiça recursos; desistir de tudo perde dados. Retentativas simultâneas também podem sobrecarregar um serviço que acabou de se recuperar.

## Opções consideradas

1. **Retry fixo para qualquer erro:** simples, mas repete o que não tem conserto.
2. **Classificação por tipo de exceção com backoff exponencial e jitter (escolhida).**

## Decisão

- **Permanentes** (`PermanentIntegrationException` e `DomainValidationException`: JSON malformado, campo obrigatório ausente, CPF/CNPJ inválido, conflito de identidade) vão direto para `DEAD`.
- **Todas as demais** são transitórias, inclusive violação de unicidade por corrida e erro desconhecido; reagendam até o máximo de tentativas (padrão 5) e então vão para `DEAD`. Classificar o desconhecido como transitório evita perder dado por um erro que talvez passe.
- **Backoff** em `BackoffPolicy` (domínio): `min(max, base × 2^(n-1)) × (1 + jitter × r)`, com `r` em [0,1) injetado para o cálculo ser determinístico. Padrões: base 5 s, teto 10 min, jitter 0,1, o que dá 5, 10, 20, 40 e 80 s sem jitter.
- A DLQ é o status `DEAD`: preserva `attempts`, `lastError` e o payload. O reprocessamento manual (`POST /api/integration/messages/{id}/reprocess` e `.../batches/{id}/reprocess`) volta a mensagem para `PENDING` com zero tentativas; só vale para `DEAD` (409 nos demais casos).

## Consequências

- Positivas: falhas sem conserto aparecem logo na DLQ; falhas passageiras se resolvem sozinhas; o jitter espalha as retentativas.
- Negativas e riscos: uma falha permanente mal classificada como transitória gasta tentativas antes da DLQ (aceitável); mensagens `DEAD` esquecidas são mitigadas pelo painel de monitoramento.
- O que precisa mudar na documentação ou nas specs: parâmetros em `agro.integration.*`, listados no README.
