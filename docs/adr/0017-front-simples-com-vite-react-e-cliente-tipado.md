# 0017. Front simples com Vite, React e cliente tipado

- Status: aceito
- Data: 2026-11-06
- Decisores: Gabriel
- Change relacionado: `openspec/changes/web-ui`

## Contexto

A interface tem as telas de produtores (grid e formulário), laboratórios e propriedades (cadastro), o dashboard analítico e o monitor da integração e consome a API só pelo contrato OpenAPI ([ADR-0006](0006-logs-estruturados-e-openapi.md)). O escopo é correção e clareza, sem autenticação, i18n ou temas. O monorepo já tem `apps/web` e `packages/api-client` ([ADR-0007](0007-monorepo-pnpm-turborepo-com-api-java.md)), e a imagem de entrega (change `delivery`) deve servir o front e repassar `/api` ao back-end no mesmo host.

## Opções consideradas

1. **Next.js:** roteamento e SSR prontos, mas não há necessidade de SSR, e traz um servidor Node a mais para operar.
2. **Vite + React + TypeScript, com cliente gerado do OpenAPI e CSS próprio (escolhida):** build estático simples, tipos que acompanham o contrato e nenhuma dependência visual.
3. **Biblioteca de componentes (MUI, Ant) ou headless (React Aria, Radix):** acelera o formulário, mas impõe visual ou dependência para poucos componentes; o único item difícil, o multi-select com busca, cabe numa lista de checkboxes nativos.

## Decisão

- **Vite + React + TypeScript**, com `react-router` para as rotas. O build é estático: na entrega (change `delivery`) o nginx o serve e repassa `/api` ao back-end (mesmo host, sem CORS); em desenvolvimento o proxy do Vite faz o mesmo.
- **Cliente tipado em `packages/api-client`:** `openapi-typescript` gera os tipos de `openapi.json` e `openapi-fetch` faz a chamada. O pacote também tem um `contract.check.ts`, uma verificação em tempo de compilação: se a API mudar um campo ou uma resposta de que o web depende, o `typecheck` quebra. O `pnpm verify` roda isso.
- **Contrato com resposta de sucesso declarada:** o springdoc omite o `200` implícito de uma operação quando ela declara só `400/404/409`, o que deixava o `data` do cliente sem tipo. Passou a existir `@ApiResponse(responseCode = "200")` nas operações usadas pelo web (produtor por id e atualização, analítico, lote e mensagem por id, reprocessamentos), e o `openapi.json` foi regenerado.
- **Estado do servidor com `@tanstack/react-query`:** cache, nova tentativa e atualização periódica (lotes a cada 5 s; o detalhe do lote e as mensagens enquanto o lote está em andamento).
- **Erros da API (`ProblemDetail`, RFC 9457)** viram um `ApiError` único (`status`, `title`, `detail` e `errors` por campo); falha de rede vira `status` 0. As telas mostram erro de campo junto ao campo e o restante em aviso de formulário ou em toast.
- **CSS próprio, sem biblioteca de componentes:** tokens de design em `styles/tokens.css` e componentes reaproveitáveis em `apps/web/src/components/` (Button, Field, Select, MultiSelect, Badge, Table, Pagination, Modal, Toast, StateMessage, BarChart). Semântica nativa primeiro (`<dialog>`, `<select>`, `<label>`, `aria-invalid`, `aria-describedby`). O gráfico de barras é um SVG simples.
- **Telas de cadastro de laboratório e de propriedade.** Ao testar o formulário de produtor, notamos que num banco vazio ele não tem laboratório nem propriedade para escolher. Avaliamos deixar esses cadastros só pela integração (o lote de exemplo os cria) e **voltamos atrás**: o formulário precisa funcionar de ponta a ponta sem lote nem `curl`. São listagem e formulário simples para cada um, sobre um esqueleto comum (`RegistryListPage` e `RegistryFormPage`, configurados por tipo). Laboratório exige CNPJ (regra do domínio); propriedade aceita CPF ou CNPJ e mostra a situação (disponível ou vinculada); o vínculo com o produtor continua só no formulário de produtor. Excluir laboratório com produtores dá 409 e a tela mostra o motivo; excluir propriedade vinculada a desvincula, e a confirmação avisa. A integração segue como caminho de carga em volume e de demonstração da DLQ.
- **Testes** com Vitest e Testing Library, contra um `fetch` simulado: validações, fluxos do formulário, estados de cada tela e a atualização a cada 5 s.

## Consequências

- Positivas: tipos que não saem do contrato; poucas dependências; build estático barato de servir; o comportamento de cada cenário das specs tem teste.
- Negativas e riscos: há dois caminhos de cadastro para laboratório e propriedade (telas e integração), com a mesma validação na API, então o web só antecipa os erros e a API continua sendo a autoridade; sem biblioteca, a acessibilidade dos componentes depende de cuidado e de testes (o multi-select não é um combobox com navegação por setas); os campos do contrato saem opcionais porque o springdoc não marca `required`, então o front trata ausência; `Item.data` do lote sai como objeto sem propriedades (o conteúdo é livre), e o editor de lote trabalha com JSON como texto.
- O que precisa mudar na documentação ou nas specs: README com as telas e o fluxo de desenvolvimento; índice de ADRs.
