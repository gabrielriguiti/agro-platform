## Context

Interface simples e desacoplada: só conhece a API pelo contrato OpenAPI ([ADR-0006](../../../docs/adr/0006-logs-estruturados-e-openapi.md)).

## Goals / Non-Goals

**Goals:**
- Telas funcionais, claras e testadas: produtores (grid e formulário), laboratórios, propriedades, dashboard e monitor de integração.
- Tipos da API gerados, não escritos à mão.

**Non-Goals:**
- Acabamento além do handoff em `ui-mockups/`, i18n, autenticação, temas.

## Decisions

1. **Vite + React + TypeScript**, `react-router` para as rotas das telas (produtores, laboratórios, propriedades, dashboard e integração). Alternativa: Next.js (descartada: sem necessidade de SSR).
2. **Cliente tipado em `packages/api-client`**, gerado com `openapi-typescript` a partir do contrato da API; uma função `fetch` fina por cima. Mudar o contrato quebra o typecheck.
3. **Estado do servidor com `@tanstack/react-query`**: cache, repetição, atualização periódica do monitor.
4. **CSS próprio, sem biblioteca de componentes**; gráfico de barras em SVG simples. Componentes reaproveitáveis próprios em `apps/web/src/components/` (Button, Field, Select, MultiSelect, Badge, Notice, Table, Pagination, Modal, Toast, StateMessage, BarChart), sobre tokens de CSS (cores, espaçamento, raios, tipografia) extraídos do design (handoff em `ui-mockups/`, que é a referência visual das telas). Semântica nativa primeiro (`<dialog>`, `<select>`, `<label>`, `aria-invalid`, `aria-describedby`, `role="status"`). O MultiSelect é uma lista de checkboxes com busca, chips e contador, sem combobox customizado. Alternativa: biblioteca headless como React Aria (descartada: poucos componentes, e o custo de dependência não compensa).
5. **Testes com Vitest e Testing Library** para validações, fluxo do formulário e estados das telas.
6. **Servido por nginx** na imagem Docker, com `/api` repassado ao back-end (mesmo host, sem CORS). Isso é entregue pelo change `delivery`; em desenvolvimento o proxy do Vite faz o mesmo.
7. **Telas de cadastro de laboratório e de propriedade.** Dois pares de listagem e formulário simples, sobre o mesmo esqueleto (`RegistryListPage` e `RegistryFormPage`, configurados por tipo), já que ambos têm nome, inscrição fiscal e endereço. Rotas `/laboratorios`, `/laboratorios/novo`, `/laboratorios/:id/editar` e `/propriedades`, `/propriedades/novo`, `/propriedades/:id/editar`. Laboratório exige CNPJ (regra do domínio); propriedade aceita CPF ou CNPJ. A lista de laboratórios não pagina; a de propriedades pagina no cliente (10 por página), porque a API devolve a lista inteira e o número de propriedades tende a ser maior. Excluir laboratório com produtores dá 409 e a tela mostra o motivo; excluir propriedade vinculada a desvincula, e a confirmação avisa. O vínculo propriedade-produtor continua só no formulário de produtor; a edição da propriedade apenas mostra a quem ela está vinculada, e a confirmação de exclusão mostra a situação e o produtor (consulta `GET /api/producers/{id}`). Os formulários seguem o handoff: resumo de erros no topo, aviso de 409 com "HTTP 409" e erro também no campo. Quando não há laboratório ou propriedade, o formulário de produtor aponta para essas telas. O histórico (cadastro só pela integração, depois revertido) está no proposal.
8. **Contrato com a resposta de sucesso declarada e verificada.** O springdoc omitia o `200` implícito das operações que declaravam só `400/404/409`, deixando o `data` do cliente sem tipo. Foram acrescentadas `@ApiResponse(responseCode = "200")` nas 7 operações usadas pelo web, e `packages/api-client/src/contract.check.ts` afirma em tempo de compilação os campos e as respostas de que o web depende: se a API mudar, o `typecheck` quebra. Os campos gerados saem opcionais (o springdoc não marca `required`) e `Item.data` do lote sai como objeto sem propriedades, então o web trata ausência e o editor de lote trabalha com JSON como texto.

ADR a gerar: 0017 Front simples com Vite, React e cliente tipado.

## Risks / Trade-offs

- [Contrato e tipos divergirem] → tipos gerados e verificados no `pnpm verify`.
- [Tela sem biblioteca de componentes fica crua] → o visual segue o handoff em `ui-mockups/`, com tokens e componentes próprios; o escopo continua sendo correção e clareza.
- [Campos opcionais e `Item.data` sem tipo no contrato gerado] → decisão 8: o web trata ausência e o lote é JSON em texto.
- [Banco vazio: o formulário de produtor não tem laboratório nem propriedade para escolher] → telas de cadastro (decisão 7) e aviso no formulário com link para elas.
