## 1. Base

- [x] 1.1 Configurar `packages/api-client` com geração de tipos do OpenAPI; verificar que alterar o contrato muda os tipos
- [x] 1.2 Configurar roteamento, layout, cliente HTTP e tratamento de `ProblemDetail`; verificar teste do tratamento de erro
- [x] 1.3 Criar os tokens de CSS e os componentes reaproveitáveis em `apps/web/src/components/` (Button, Field, Select, MultiSelect, Badge, Notice, Table, Pagination, Modal, Toast, StateMessage, BarChart); verificar testes de MultiSelect, Modal e Field

## 2. Produtores

- [x] 2.1 Implementar o grid com paginação, estados e exclusão com confirmação; verificar testes dos cenários
- [x] 2.2 Implementar o formulário com máscara, validação de CPF/CNPJ e período; verificar testes de validação
- [x] 2.3 Implementar o multi-select de propriedades e o select de laboratório; verificar o cenário de edição de conjunto
- [x] 2.4 Exibir erros 400/409 junto aos campos; verificar o cenário de propriedade já vinculada
- [x] 2.5 Orientar o formulário quando não há laboratório ou propriedade disponível; verificar o teste do banco vazio (o link passa a apontar para as telas de cadastro: tarefa 6.3)

## 3. Dashboard

- [x] 3.1 Implementar filtros, validação da quantidade mínima e a consulta; verificar os cenários
- [x] 3.2 Implementar tabela e gráfico de barras SVG, com estados vazio e erro; verificar a ordem igual à da API

## 4. Monitor de integração

- [x] 4.1 Implementar a lista de lotes com atualização a cada 5 s; verificar o cenário em andamento
- [x] 4.2 Implementar a lista de mensagens com filtro e o detalhe com erro e payload; verificar o cenário da DLQ
- [x] 4.3 Implementar reprocessamento e o envio de lote de exemplo; verificar o fluxo completo contra a API de teste

## 5. Documentação

- [x] 5.1 Registrar o ADR 0017; verificar o índice
- [x] 5.2 Capturas de tela das telas no README (produtores, formulário, dashboard, monitor, lote com DLQ, laboratórios e propriedades); verificar os links
- [x] 5.3 Documentar a decisão sobre laboratórios e propriedades (revista na seção 6); verificar os links

## 6. Laboratórios e propriedades

- [x] 6.1 Implementar a listagem e o formulário de laboratório (CNPJ obrigatório, editar, excluir com confirmação, 400/409); verificar os testes dos cenários
- [x] 6.2 Implementar a listagem e o formulário de propriedade (CPF ou CNPJ, situação, excluir desvincula, 400/409); verificar os testes dos cenários
- [x] 6.3 Acrescentar as rotas e os itens de navegação e apontar o aviso do formulário de produtor para as novas telas; verificar o teste de navegação
- [x] 6.4 Atualizar o ADR 0017 e o README (decisão revista, telas e capturas); verificar os links
