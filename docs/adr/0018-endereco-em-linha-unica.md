# 0018. Endereço em linha única, sem estrutura

- Status: aceito
- Data: 2026-11-07
- Decisores: Gabriel
- Change relacionado: `openspec/changes/web-ui` (decisão transversal, vale para todo o cadastro)

## Contexto

Produtor, propriedade e laboratório têm endereço, obrigatório em toda entidade (`Identity`, [ADR-0009](0009-identificacao-fiscal-como-value-object.md)). O enunciado não define o formato. Em propriedade rural o endereço raramente cabe em rua, número e CEP: aparece como "Linha 5, Sorriso/MT" ou "BR-163, km 12". Os dados chegam de parceiros (ERP, LIMS, órgãos de governo) pela integração, cada um com um formato próprio.

## Opções consideradas

1. **Estruturado (logradouro, número, complemento, bairro, cidade, UF, CEP):** permite filtrar por cidade ou UF e validar CEP, mas obriga o parceiro a decompor o que não decompõe, e endereço rural não tem essa forma. Gera campos vazios ou inventados, mais colunas, mais validação e mais motivos de rejeição na integração.
2. **Estruturado com campos opcionais:** reduz a rejeição, mas deixa o dado heterogêneo (parte estruturada, parte não) sem a vantagem de poder confiar nele.
3. **Linha única, texto livre (escolhida):** uma coluna `address VARCHAR(500) NOT NULL`, um `String` no domínio, nenhuma regra além de obrigatório e tamanho.

## Decisão

Endereço é uma **linha única de texto livre**, obrigatória e com até 500 caracteres, em todas as entidades. Nenhum requisito do enunciado usa o endereço para filtro, agrupamento ou cálculo: a consulta analítica ([ADR-0016](0016-consulta-analitica-em-sql-dinamico-como-leitura-separada.md)) trabalha com laboratório, datas e `observacoes`. Sem consumidor para a estrutura, o custo dela não se paga.

## Consequências

- Positivas: um campo só no formulário, no contrato, na migration e na integração; nenhum rejeite por formato de endereço; aceita endereço rural e de qualquer país.
- Negativas e riscos: sem filtro por cidade ou UF, sem validação de CEP, sem deduplicação por endereço e sem geocodificação; a qualidade do dado depende de quem envia.
- Reversibilidade: se surgir o requisito, a migração é aditiva. Criam-se as colunas estruturadas (nulas), o endereço atual continua como `address` e um passo de normalização (parser ou serviço de CEP) preenche o que for possível. O `Identity` esconde o campo atrás de um tipo, então a troca fica em `domain` e `infrastructure`; para o front, só o formulário muda.
- O que precisa mudar na documentação ou nas specs: README (premissas) e índice de ADRs.
