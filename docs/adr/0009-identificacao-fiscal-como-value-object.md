# 0009. Identificação fiscal como Value Object

- Status: aceito
- Data: 2026-11-05
- Decisores: Gabriel
- Change relacionado: `openspec/changes/registry`

## Contexto

Produtor, propriedade e laboratório têm inscrição fiscal (CPF ou CNPJ) obrigatória, única por tipo de entidade. A entrada chega com ou sem máscara, o armazenamento deve ser comparável (a mesma inscrição com e sem máscara é o mesmo registro) e a validação dos dígitos verificadores precisa valer no domínio, não só na borda REST. Os valores de exemplo do enunciado (`123.456.789-00`) não passam nessa validação.

## Opções consideradas

1. **`String` validada com anotação de Bean Validation no DTO:** simples, mas o domínio aceita valores inválidos e cada ponto de entrada (REST, integrador) precisa repetir a regra.
2. **Serviço de validação chamado pelos casos de uso:** centraliza a regra, mas não impede construir uma entidade inválida.
3. **Value Object `TaxId` imutável (escolhida):** só existe se for válido; normaliza na criação e formata na saída.

## Decisão

`TaxId` é um `record` do domínio, sem framework, que guarda apenas os dígitos (11 de CPF ou 14 de CNPJ) e rejeita tamanho errado, caracteres não numéricos, sequências repetidas e dígitos verificadores incorretos. `TaxId.of` tolera pontos, traço, barra e espaços; `formatted()` devolve a máscara usada nas respostas da API. `Identity` agrupa nome, `TaxId` e endereço, obrigatórios em toda entidade. A API reaproveita a mesma regra por uma constraint de validação (`@TaxIdFormat`), para listar todos os campos inválidos de uma vez, e o domínio revalida ao construir. O banco guarda só dígitos, com `UNIQUE` em `tax_id` por tabela e `CHECK` de formato; a violação vira 409. O laboratório exige CNPJ.

## Consequências

- Positivas: uma única regra testada sem Spring; impossível persistir inscrição inválida; unicidade independente da máscara.
- Negativas e riscos: os exemplos do enunciado não servem como dados de teste ou de seed (testes e seed usam valores válidos); a regra de dígitos é nacional e não cobre documentos estrangeiros.
- O que precisa mudar na documentação ou nas specs: o README deve avisar que os exemplos do enunciado são rejeitados.
