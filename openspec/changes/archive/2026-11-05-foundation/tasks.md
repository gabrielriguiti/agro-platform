## 1. Convenções e documentação

- [x] 1.1 Criar `openspec/config.yaml`, `docs/commits.md`, template e índice de ADRs; verificar que os links entre arquivos resolvem
- [x] 1.2 Registrar os ADRs 0001 a 0006; verificar o índice em `docs/adr/README.md`

## 2. Monorepo e esqueleto da API

- [x] 2.0 Criar `package.json`, `pnpm-workspace.yaml` e `turbo.json` na raiz e um `apps/api/package.json` cujos scripts chamam o Maven (`build`, `test`, `verify`); verificar `pnpm turbo build test` com a API compilando e testando

- [x] 2.1 Criar `apps/api/pom.xml` (Spring Boot 3.3, Java 17, Surefire e Failsafe) e a classe `AgroApplication`; verificar `mvn -q -DskipTests package` no CI
- [x] 2.2 Criar a migration `V1__schema.sql` do schema completo com `COMMENT ON`; verificar a aplicação contra Postgres vazio em teste de integração
- [x] 2.3 Configurar `application.yml`, logs JSON/`local` e correlação por requisição; verificar saída JSON no teste de integração
- [x] 2.4 Expor `/actuator/health`, `/v3/api-docs` e `/swagger-ui.html`; verificar por teste `*IT` que os três respondem

## 3. Arquitetura

- [x] 3.1 Criar os pacotes `domain`, `application` e `infrastructure` com `package-info.java` documentando o papel de cada um; verificar o Javadoc
- [x] 3.2 Implementar `ArchitectureTest` com as quatro regras da spec `architecture-boundaries`; verificar que uma classe de violação criada de propósito faz o teste falhar

## 4. Esqueleto do web e dos pacotes

- [x] 4.1 Criar `apps/web` (Vite, React, TypeScript) com roteamento e as quatro rotas vazias; verificar `pnpm --filter web build` e `typecheck`
- [x] 4.2 Criar `packages/api-client` (tipos gerados do OpenAPI com `openapi-typescript`) e usá-lo no web; verificar que alterar o contrato muda os tipos e quebra o typecheck

## 5. Entrega

- [x] 5.1 Criar o workflow de CI (`pnpm verify`: API e web); verificar execução verde no GitHub Actions
- [x] 5.2 Criar o README com visão geral, como rodar, diagrama de arquitetura inicial e a seção "Uso de IA"; verificar os comandos em uma máquina limpa
