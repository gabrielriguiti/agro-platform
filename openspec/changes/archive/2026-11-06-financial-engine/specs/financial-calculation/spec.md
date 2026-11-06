## Purpose

Calcula valores financeiros conforme a natureza da entidade, de forma extensível e desacoplada.

## ADDED Requirements

### Requirement: Cálculo por natureza da entidade
O motor DEVE escolher a calculadora pela natureza da entidade: comissão de produção para produtor, taxas operacional e territorial para propriedade e para laboratório.

#### Scenario: Produtor
- **WHEN** o valor de um produtor com 5 dias de vigência e 2 propriedades é calculado com os padrões (120,00 por dia e 50,00 por propriedade)
- **THEN** o resultado é 700,00

#### Scenario: Propriedade
- **WHEN** o valor de uma propriedade é calculado com os padrões (300,00 operacional e 150,00 territorial)
- **THEN** o resultado é 450,00

#### Scenario: Laboratório
- **WHEN** o valor de um laboratório com 4 produtores é calculado com os padrões (500,00 operacional e 25,00 por produtor)
- **THEN** o resultado é 600,00

### Requirement: Taxas configuráveis
As taxas DEVEM vir de configuração (`agro.financial.*`), com valores padrão, sem exigir recompilação.

#### Scenario: Alterar a comissão diária
- **WHEN** `agro.financial.producer.daily-commission` é alterada para 100,00
- **THEN** o mesmo produtor de 5 dias e 2 propriedades passa a valer 600,00

### Requirement: Dias de vigência
Os dias de vigência do produtor DEVEM ser o intervalo entre `dataInicial` e `dataFinal` arredondado para cima em dias inteiros, com mínimo de 1.

#### Scenario: Intervalo fracionado
- **WHEN** o período vai de 11/06 12:00 a 15/06 18:00 (4,25 dias)
- **THEN** a vigência considerada é de 5 dias

### Requirement: Arredondamento monetário
Todo valor DEVE ter duas casas decimais, com arredondamento HALF_EVEN.

#### Scenario: Escala
- **WHEN** qualquer valor é calculado
- **THEN** o resultado tem exatamente duas casas decimais

### Requirement: Extensibilidade
Uma nova regra ou natureza DEVE ser adicionada com uma nova implementação de `FinancialCalculator`, sem alterar o motor nem as existentes.

#### Scenario: Natureza sem calculadora
- **WHEN** o motor recebe uma natureza sem calculadora registrada
- **THEN** ele falha com erro de domínio explícito

### Requirement: Endpoint de consulta
`GET /api/financial/{producers|properties|laboratories}/{id}` DEVE devolver o valor calculado, a natureza e os componentes usados; 404 se a entidade não existir.

#### Scenario: Consultar produtor
- **WHEN** `GET /api/financial/producers/10` é chamado para um produtor existente
- **THEN** a resposta traz `valor`, `natureza` e os componentes do cálculo
