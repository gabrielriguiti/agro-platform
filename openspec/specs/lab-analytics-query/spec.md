# lab-analytics-query Specification

## Purpose

Consulta agregada de laboratórios para análise operacional, com filtros, ordenação e desempenho definidos.

## Requirements

### Requirement: Contrato de resposta
Cada item DEVE trazer `id`, `nome` em CAIXA ALTA, `totalProdutores` e `valorFinanceiroConsolidado` (duas casas decimais).

#### Scenario: Nome em caixa alta
- **WHEN** o laboratório "Laboratório Modelo 1" aparece no resultado
- **THEN** o campo `nome` é "LABORATÓRIO MODELO 1", com acentos preservados

### Requirement: Quantidade mínima obrigatória
`qtdMinimaProdutores` DEVE ser obrigatório e maior ou igual a 1; apenas laboratórios com `totalProdutores` maior ou igual a ele DEVEM aparecer.

#### Scenario: Parâmetro ausente
- **WHEN** a consulta é feita sem `qtdMinimaProdutores`
- **THEN** a API responde 400 indicando o parâmetro

#### Scenario: Corte por quantidade
- **WHEN** existem laboratórios com 5, 3 e 1 produtores e `qtdMinimaProdutores=3`
- **THEN** só os de 5 e 3 produtores aparecem

### Requirement: Filtros de período
`dataInicialInicio`/`dataInicialFim` e `dataFinalInicio`/`dataFinalFim` DEVEM ser opcionais e inclusivos, aplicados às datas `dataInicial` e `dataFinal` dos produtores; em cada par, o início NÃO DEVE ser posterior ao fim (400).

#### Scenario: Filtro por data inicial
- **WHEN** a consulta informa `dataInicialInicio` e `dataInicialFim`
- **THEN** só produtores com `dataInicial` nesse intervalo são considerados na contagem

#### Scenario: Intervalo invertido
- **WHEN** `dataInicialInicio` é posterior a `dataInicialFim`
- **THEN** a API responde 400

### Requirement: Semântica do total
`totalProdutores` DEVE contar apenas os produtores que atendem a todos os filtros informados.

#### Scenario: Total filtrado
- **WHEN** um laboratório tem 4 produtores e apenas 2 atendem ao filtro de texto
- **THEN** `totalProdutores` é 2

### Requirement: Busca textual
`buscaTexto` DEVE fazer correspondência parcial, sem distinção de maiúsculas e minúsculas, sobre `observacoes` do produtor, tratando `%`, `_` e `\` como caracteres literais.

#### Scenario: Correspondência parcial
- **WHEN** `buscaTexto=integração` e um produtor tem "Iniciando testes de INTEGRAÇÃO"
- **THEN** esse produtor é considerado

#### Scenario: Caractere curinga literal
- **WHEN** `buscaTexto=100%` é informado
- **THEN** só observações que contêm literalmente "100%" correspondem

### Requirement: Ordenação
O resultado DEVE ser ordenado por `totalProdutores` decrescente; quando algum filtro de `dataInicial` for informado, o segundo critério DEVE ser a menor `dataInicial` entre os produtores considerados, crescente; o desempate final DEVE ser por `id` crescente.

#### Scenario: Sem filtro de data inicial
- **WHEN** dois laboratórios têm o mesmo total e nenhum filtro de `dataInicial` foi informado
- **THEN** a ordem entre eles é por `id`

#### Scenario: Com filtro de data inicial
- **WHEN** dois laboratórios têm o mesmo total e há filtro de `dataInicial`
- **THEN** vem primeiro o laboratório cujo produtor considerado mais antigo tem a menor `dataInicial`

### Requirement: Valor financeiro consolidado
`valorFinanceiroConsolidado` DEVE ser o resultado do cálculo financeiro do laboratório, calculado a partir do `totalProdutores` do próprio resultado.

#### Scenario: Valor coerente com o motor
- **WHEN** um laboratório aparece com `totalProdutores` 4 e os valores padrão
- **THEN** `valorFinanceiroConsolidado` é 600,00

### Requirement: Desempenho
A consulta DEVE ser executada em uma única instrução SQL com agregação no banco, sem consultas por laboratório, apoiada por índices em `laboratory_id` e nas datas.

#### Scenario: Contagem de consultas
- **WHEN** a consulta retorna 50 laboratórios
- **THEN** é executada exatamente 1 consulta ao banco para os dados agregados
