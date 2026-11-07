## 1. Consulta

- [x] 1.1 Implementar a montagem do SQL dinâmico e o adaptador JDBC; verificar testes `*IT` por combinação de filtros
- [x] 1.2 Implementar escape de `LIKE` e a busca literal; verificar os cenários `integração` e `100%`
- [x] 1.3 Implementar a ordenação condicional com desempate por id; verificar os cenários com e sem filtro de data inicial
- [x] 1.4 Criar os índices (migration) e comentar o plano de execução; verificar `EXPLAIN` documentado

## 2. Caso de uso e API

- [x] 2.1 Implementar o caso de uso (nome em caixa alta pt-BR e valor via motor financeiro); verificar teste de unidade
- [x] 2.2 Implementar o endpoint com validação (`qtdMinimaProdutores` ≥ 1, intervalos); verificar 400 e 200
- [x] 2.3 Teste que conta as consultas executadas; verificar exatamente 1

## 3. Documentação

- [x] 3.1 Registrar o ADR 0016; verificar o índice
- [x] 3.2 Exemplos de chamada e semântica do total no README; verificar contra os testes
