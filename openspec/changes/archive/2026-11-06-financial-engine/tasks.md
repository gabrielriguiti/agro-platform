## 1. Motor

- [x] 1.1 Implementar `FinancialSubject`, `FinancialRates`, `FinancialCalculator` e as três calculadoras; verificar testes de unidade com os exemplos da spec (700,00 / 450,00 / 600,00)
- [x] 1.2 Implementar `FinancialEngine` e o erro para natureza sem calculadora; verificar teste
- [x] 1.3 Implementar o cálculo de dias de vigência (arredondamento para cima, mínimo 1); verificar o cenário 4,25 dias → 5

## 2. Integração com a aplicação

- [x] 2.1 Configurar `agro.financial.*` e os beans; verificar teste que altera a taxa por propriedade
- [x] 2.2 Implementar o caso de uso e o endpoint `GET /api/financial/...`; verificar teste `*IT` (200 e 404)

## 3. Documentação

- [x] 3.1 Registrar o ADR 0010 com as fórmulas como premissa; verificar o índice
- [x] 3.2 Tabela de fórmulas, exemplo numérico e Javadoc; verificar a renderização no README
