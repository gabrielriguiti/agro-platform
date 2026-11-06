# 0008. Agregados referenciados por id e vínculo da propriedade ao produtor

- Status: aceito
- Data: 2026-11-05
- Decisores: Gabriel
- Change relacionado: `openspec/changes/registry`

## Contexto

Um produtor pertence a exatamente um laboratório e tem várias propriedades; cada propriedade existe sem produtor e pertence a no máximo um. A listagem do grid carrega produtor, laboratório e propriedades de uma só vez e não pode sofrer N+1. Duas requisições simultâneas podem tentar vincular a mesma propriedade a produtores diferentes.

## Opções consideradas

1. **Grafo de objetos com JPA (`@ManyToOne`/`@OneToMany`) e lazy loading:** natural, mas acopla o domínio ao JPA e esconde consultas N+1.
2. **Tabela associativa produtor-propriedade:** admite N:N, que o domínio não permite; exige mais uma junção e uma constraint extra para garantir "no máximo um dono".
3. **Agregados referenciados por id, com `producer_id` anulável em `properties` (escolhida):** o `Producer` guarda `laboratoryId` e `propertyIds`; o caso de uso verifica existência e exclusividade; a leitura monta a visão em lote.

## Decisão

`Producer`, `Property` e `Laboratory` são agregados independentes que se referenciam por id. O vínculo 1:N fica em `properties.producer_id` (`ON DELETE SET NULL`, de modo que excluir o produtor libera as propriedades). A exclusividade é garantida no banco por um `UPDATE ... WHERE id IN (...) AND (producer_id IS NULL OR producer_id = :produtor)` dentro da transação do caso de uso: se as linhas afetadas forem menos que as pedidas, há propriedade de outro produtor e a transação é desfeita com 409. A listagem executa no máximo 3 consultas: contagem, página unida ao laboratório e propriedades da página por `IN (...)`, montadas em memória.

## Consequências

- Positivas: domínio sem JPA; número de consultas constante e testado; a corrida entre dois produtores é resolvida pelo bloqueio de linha do banco (testada com concorrência real).
- Negativas e riscos: mapeamento extra entre entidades JPA e domínio; as regras de existência vivem nos casos de uso, não em relacionamentos do ORM.
- O que precisa mudar na documentação ou nas specs: o diagrama ER do README deve mostrar `properties.producer_id`.
