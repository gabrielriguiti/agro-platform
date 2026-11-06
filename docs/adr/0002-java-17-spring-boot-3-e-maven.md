# 0002. Java 17, Spring Boot 3 e Maven

- Status: aceito
- Data: 2026-11-05
- Decisores: Gabriel
- Change relacionado: `openspec/changes/foundation`

## Contexto

A stack-alvo do back-end é Java 17+ com Spring Boot 3.x. O projeto é um monorepo com back-end e front-end, construído no GitHub Actions e empacotado em Docker.

## Opções consideradas

1. **Maven:** padrão do ecossistema Spring, `spring-boot-starter-parent` fixa as versões, plugins de teste (Surefire e Failsafe) conhecidos.
2. **Gradle:** mais flexível, com mais configuração e curva maior para quem lê o projeto.

## Decisão

Java 17 (LTS), Spring Boot 3.3.x e Maven. Testes unitários (`*Test`) rodam no Surefire e testes de integração com Testcontainers (`*IT`) no Failsafe, para separar o que precisa de Docker.

## Consequências

- Positivas: build previsível; separação clara entre testes rápidos e testes com banco.
- Negativas e riscos: o Java 17 é o piso; recursos de versões mais novas ficam de fora.
- Documentação: comandos de build e teste no README.
