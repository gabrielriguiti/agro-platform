# 0019. Sem autenticação nesta entrega

- Status: aceito
- Data: 2026-11-07
- Decisores: Gabriel
- Change relacionado: `openspec/changes/web-ui` (decisão transversal, vale para a API e o front)

## Contexto

O enunciado não pede usuários, perfis, login nem proteção da API, e a entrega é um projeto de avaliação que roda localmente (`docker compose`), sem dados reais. Os endpoints de ingestão e de reprocessamento, porém, são operações sensíveis em produção: gravam dados de parceiros e reenfileiram mensagens da DLQ.

## Opções consideradas

1. **Autenticação própria (usuário e senha, sessão ou JWT emitido pela aplicação):** é o caminho mais caro e o pior: a aplicação passa a guardar credenciais, e nada disso é o problema de domínio que a entrega resolve.
2. **Provedor externo (OIDC, como Keycloak) com a API validando JWT:** é o caminho certo para produção, mas traz um serviço a mais no compose, perfis, escopos e telas de login sem requisito que os justifique.
3. **Sem autenticação, com a decisão registrada e o ponto de encaixe preparado (escolhida).**

## Decisão

A API e o front **não autenticam** nesta entrega. A API não deve ser exposta em rede pública no estado atual: o compose publica só a porta do front e o nginx repassa `/api` no mesmo host ([ADR-0017](0017-front-simples-com-vite-react-e-cliente-tipado.md)).

O plano para produção, que não foi implementado:

- OIDC com um provedor externo; a API como *resource server*, validando o JWT (Spring Security) e sem guardar senha.
- Escopos por papel: leitura (consulta e analítico), escrita (cadastro e ingestão) e operação (reprocessamento e execução manual do worker).
- O `source` da integração passa a vir da identidade do parceiro (client credentials), e não do corpo da mensagem.
- Limite de taxa nos endpoints de ingestão, e trilha de auditoria de quem reprocessou.

## Consequências

- Positivas: o esforço fica no domínio, na integração e nos testes; nenhum serviço extra no compose; a avaliação roda com um comando.
- Negativas e riscos: qualquer cliente que alcance a API lê e grava tudo, incluindo reprocesso e remoção; não há trilha de auditoria; `source` na integração é informado pelo próprio parceiro, sem verificação.
- Reversibilidade: adicionar autenticação não exige mexer no domínio. A hexagonal ([ADR-0001](0001-arquitetura-hexagonal-com-ddd-pragmatico.md)) mantém a segurança na borda: um filtro do Spring Security na camada web, mais `@PreAuthorize` nos casos de uso quando for preciso, e no front um interceptor no `openapi-fetch` com o token.
- O que precisa mudar na documentação ou nas specs: README (limitações conhecidas) e índice de ADRs.
