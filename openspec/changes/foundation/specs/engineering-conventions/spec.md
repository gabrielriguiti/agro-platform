## Purpose

Define como o trabalho é conduzido e registrado no repositório: specs, decisões e histórico.

## ADDED Requirements

### Requirement: Specs antes do código
Todo change DEVE ter `proposal.md`, specs com requisitos e cenários, `design.md` e `tasks.md` antes da implementação, e cada task DEVE ter critério de pronto verificável.

#### Scenario: Change sem tasks verificáveis
- **WHEN** uma task não indica como verificar que está pronta
- **THEN** o change não está pronto para implementação

### Requirement: Decisões registradas em ADR
Toda decisão técnica relevante DEVE gerar um ADR no formato de `docs/adr/0000-template.md`, e ADR aceito NÃO DEVE ser editado, apenas substituído.

#### Scenario: Mudança de decisão
- **WHEN** uma decisão aceita precisa mudar
- **THEN** um novo ADR a substitui e o status do antigo aponta para ele

### Requirement: Commits por change
Cada change DEVE ter um commit de proposta e um commit de entrega em Conventional Commits, com o nome do change como escopo e os trailers `Change:` e `ADR:` descritos em `docs/commits.md`.

#### Scenario: Entrega de um change
- **WHEN** o change `registry` é entregue
- **THEN** existe um commit `feat(registry): ...` com código, testes e documentação de todas as specs do change

### Requirement: Sem coautoria de IA nos commits
Commits e descrições de PR NÃO DEVEM conter `Co-Authored-By` de IA nem linhas "Generated with"; o uso de IA DEVE ser declarado na seção "Uso de IA" do README.

#### Scenario: Ferramenta sugere trailer automático
- **WHEN** uma ferramenta propõe acrescentar um trailer de coautoria de IA
- **THEN** o trailer é removido antes do commit

### Requirement: Verificação única na raiz
O repositório DEVE ser um monorepo pnpm + Turborepo e `pnpm verify` na raiz DEVE compilar, testar e verificar a API, o web e os pacotes.

#### Scenario: Verificar tudo
- **WHEN** o desenvolvedor roda `pnpm verify` na raiz
- **THEN** a API (Maven), o web (typecheck, testes e build) e os pacotes são verificados, e o comando falha se qualquer um falhar

### Requirement: Repositório sem segredos
Segredos, senhas reais, chaves e dados reais NÃO DEVEM ser commitados; apenas credenciais de desenvolvimento local (`agro`/`agro`) são permitidas.

#### Scenario: Arquivo de chave no diretório
- **WHEN** existe um arquivo `*.key` ou `.env` no diretório de trabalho
- **THEN** ele é ignorado pelo git
