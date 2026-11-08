## Purpose

Interagir com o endpoint analítico de laboratórios e visualizar o resultado.

## ADDED Requirements

### Requirement: Filtros
O painel DEVE ter campos para o intervalo da data inicial e o intervalo da data final do produtor (de e até, quatro campos), busca de texto nas observações e quantidade mínima de produtores (obrigatória, padrão 1), e só consultar com os filtros válidos (inclusive intervalos não invertidos).

#### Scenario: Quantidade mínima vazia
- **WHEN** o usuário limpa a quantidade mínima
- **THEN** o botão Consultar fica desabilitado e o campo indica a obrigatoriedade

### Requirement: Resultado
O painel DEVE exibir uma tabela com nome, total de produtores e valor financeiro (em R$) na ordem devolvida pela API, e um gráfico de barras do total por laboratório.

#### Scenario: Consulta com resultado
- **WHEN** o usuário consulta com a quantidade mínima 2
- **THEN** a tabela e o gráfico mostram os laboratórios retornados, na mesma ordem

### Requirement: Estados
O painel DEVE tratar carregamento, resultado vazio ("Nenhum laboratório atende aos filtros") e erro da API.

#### Scenario: Nenhum resultado
- **WHEN** a consulta não retorna laboratórios
- **THEN** a mensagem de vazio é exibida
