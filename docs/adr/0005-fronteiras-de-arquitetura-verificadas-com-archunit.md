# 0005. Fronteiras de arquitetura verificadas com ArchUnit

- Status: aceito
- Data: 2026-11-05
- Decisores: Gabriel
- Change relacionado: `openspec/changes/foundation`

## Contexto

A arquitetura hexagonal ([ADR-0001](0001-arquitetura-hexagonal-com-ddd-pragmatico.md)) só se sustenta se as regras de dependência forem respeitadas ao longo do tempo. Revisão humana não escala; uma regra que não quebra o build tende a ser violada.

## Opções consideradas

1. **Convenção e revisão:** frágil.
2. **Módulos Maven separados:** fronteira forte, porém mais estrutura de build do que o porte pede.
3. **ArchUnit rodando como teste unitário (escolhida):** regras declarativas, executadas em todo `mvn test`.

## Decisão

Um teste ArchUnit falha o build quando: `domain` importa Spring, JPA ou `application`/`infrastructure`; `application` importa `infrastructure`; os controllers acessam repositórios diretamente em vez dos casos de uso; o motor financeiro importa Spring.

## Consequências

- Positivas: violação vira falha de CI com mensagem clara.
- Negativas e riscos: as regras precisam evoluir junto com a estrutura de pacotes.
- Documentação: regras listadas na spec `architecture-boundaries`.
