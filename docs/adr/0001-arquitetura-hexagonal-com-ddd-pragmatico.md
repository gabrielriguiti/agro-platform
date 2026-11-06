# 0001. Arquitetura hexagonal com DDD pragmático

- Status: aceito
- Data: 2026-11-05
- Decisores: Gabriel
- Change relacionado: `openspec/changes/foundation`

## Contexto

O sistema tem três contextos com ritmos diferentes: cadastro (CRUD com regras), integração (assíncrona e tolerante a falhas) e financeiro/analítico (cálculo e leitura). A regra de negócio precisa ser testável sem banco e sem Spring, e a infraestrutura (JPA, JDBC, HTTP) precisa ser trocável sem mexer no domínio.

## Opções consideradas

1. **Camadas tradicionais (controller, service, repository):** simples, mas a regra de negócio acaba acoplada ao framework e ao JPA.
2. **Hexagonal completo em tudo (entidade de domínio, entidade JPA e mapper para cada tabela):** isolamento máximo, com muita cerimônia em CRUD simples.
3. **Hexagonal pragmático (escolhida):** domínio e casos de uso puros onde há regra (identidade fiscal, integração, financeiro); adaptadores finos nas bordas; leituras analíticas direto no banco por uma porta de consulta.

## Decisão

Pacotes `domain` (modelo e invariantes, sem framework), `application` (casos de uso e portas de entrada e saída) e `infrastructure` (web, persistência, integração), com dependência apontando sempre para dentro. Agregados se referenciam por id. Value Objects (`TaxId`, `Identity`) concentram a validação. A consulta analítica é uma leitura separada (CQRS leve), que não passa pelo domínio.

## Consequências

- Positivas: regras testáveis em milissegundos; trocar persistência ou fila não afeta o domínio; fronteiras claras para defender na apresentação.
- Negativas e riscos: mais classes e mapeamento do que um CRUD direto; exige disciplina, por isso as regras são verificadas por teste ([ADR-0005](0005-fronteiras-de-arquitetura-verificadas-com-archunit.md)).
- Documentação: diagrama de arquitetura no README.
