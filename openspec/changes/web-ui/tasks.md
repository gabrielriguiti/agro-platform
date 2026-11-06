## 1. Base

- [ ] 1.1 Configurar `packages/api-client` com geração de tipos do OpenAPI; verificar que alterar o contrato muda os tipos
- [ ] 1.2 Configurar roteamento, layout, cliente HTTP e tratamento de `ProblemDetail`; verificar teste do tratamento de erro

## 2. Produtores

- [ ] 2.1 Implementar o grid com paginação, estados e exclusão com confirmação; verificar testes dos cenários
- [ ] 2.2 Implementar o formulário com máscara, validação de CPF/CNPJ e período; verificar testes de validação
- [ ] 2.3 Implementar o multi-select de propriedades e o select de laboratório; verificar o cenário de edição de conjunto
- [ ] 2.4 Exibir erros 400/409 junto aos campos; verificar o cenário de propriedade já vinculada

## 3. Dashboard

- [ ] 3.1 Implementar filtros, validação da quantidade mínima e a consulta; verificar os cenários
- [ ] 3.2 Implementar tabela e gráfico de barras SVG, com estados vazio e erro; verificar a ordem igual à da API

## 4. Monitor de integração

- [ ] 4.1 Implementar a lista de lotes com atualização a cada 5 s; verificar o cenário em andamento
- [ ] 4.2 Implementar a lista de mensagens com filtro e o detalhe com erro e payload; verificar o cenário da DLQ
- [ ] 4.3 Implementar reprocessamento e o envio de lote de exemplo; verificar o fluxo completo contra a API de teste

## 5. Documentação

- [ ] 5.1 Registrar o ADR 0017; verificar o índice
- [ ] 5.2 Capturas de tela das quatro telas no README; verificar os links
