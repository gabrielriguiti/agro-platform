## Context

Contexto `registry` do monólito hexagonal ([ADR-0001](../../../docs/adr/0001-arquitetura-hexagonal-com-ddd-pragmatico.md)). A regra de negócio está nas invariantes do domínio; as bordas (REST, JPA) são finas.

## Goals / Non-Goals

**Goals:**
- Invariantes no domínio (`TaxId`, `Identity`, `Producer`), testáveis sem Spring.
- Listagens com número constante de consultas.

**Non-Goals:**
- Telas de cadastro de propriedade/laboratório; autenticação.

## Decisions

1. **Agregados referenciados por id.** `Producer` guarda `laboratoryId` e `propertyIds`; o caso de uso verifica existência e exclusividade. Alternativa: grafo de objetos com lazy loading (descartada: N+1 e acoplamento).
2. **Vínculo 1:N na tabela `properties`** (`producer_id` anulável, `ON DELETE SET NULL`). Alternativa: tabela associativa (descartada: a relação é 1:N e a propriedade tem no máximo um dono).
3. **Carregamento em lote na listagem:** página de produtores, depois `IN (...)` para laboratórios e para propriedades; montagem em memória. Teste conta consultas.
4. **Persistência:** entidades JPA separadas do domínio, mapeadas no adaptador; `ddl-auto: validate`.
5. **Unicidade pelo banco** (`UNIQUE` em `tax_id`), com tradução da violação para 409.
6. **Concorrência:** a atualização do conjunto de propriedades roda em uma transação; a exclusividade é garantida por `UPDATE ... WHERE producer_id IS NULL OR producer_id = :id` conferindo as linhas afetadas.

ADRs a gerar: 0008 Agregados referenciados por id e vínculo da propriedade ao produtor; 0009 Identificação fiscal como Value Object.

## Risks / Trade-offs

- [Mapeamento extra entre domínio e JPA] → mapper único por agregado, coberto por teste de integração.
- [Corrida ao vincular a mesma propriedade a dois produtores] → condição no `UPDATE` e transação; teste concorrente.
