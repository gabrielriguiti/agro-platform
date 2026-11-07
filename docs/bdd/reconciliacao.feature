# language: pt
Funcionalidade: Reconciliação de registros entre sistemas
  O mesmo registro vindo de sistemas diferentes é identificado e fundido conforme a estratégia do lote.

  # Verificado em: IntegrationProcessingIT.mesmoCnpjDeOutroSistemaReconciliaEGanhaSegundoVinculo
  Cenário: Mesmo CNPJ vindo de outro sistema
    Dado um laboratório cadastrado pelo sistema "A"
    Quando o sistema "B" envia um laboratório com o mesmo CNPJ
    Então o registro é reconciliado em vez de duplicado
    E passa a ter também o vínculo externo do sistema "B"

  # Verificado em: ReconciliationPolicyTest e IntegrationProcessingIT.mergePreservaOExistenteOverwriteSubstituiKeepExistingIgnora
  Esquema do Cenário: Estratégias de fusão
    Dado um laboratório existente com nome "Original" e endereço "Rua Original"
    Quando um lote com a estratégia "<estratégia>" traz nome "Alterado" e outro endereço
    Então o nome resultante é "<nome>"

    Exemplos:
      | estratégia    | nome      |
      | MERGE         | Original  |
      | OVERWRITE     | Alterado  |
      | KEEP_EXISTING | Original  |

  # Verificado em: IntegrationProcessingIT.conflitoDeIdentidadeVaiParaDlqSemAlterarNada
  Cenário: Conflito de identidade
    Dado que o id externo "L-1" já aponta para o laboratório X
    Quando chega um payload com "L-1" e o CNPJ do laboratório Y
    Então a mensagem vai para "DEAD" com o motivo "conflito de identidade"
    E os laboratórios X e Y permanecem intactos

  # Verificado em: IntegrationProcessingIT.produtorComLaboratorioEDuasPropriedadesNovasEmBancoVazio
  Cenário: Produtor com laboratório e duas propriedades novas
    Dado um banco vazio
    Quando um produtor com um laboratório e duas propriedades aninhadas é processado
    Então são criados 1 laboratório, 2 propriedades e 1 produtor, corretamente vinculados

  # Verificado em: IntegrationProcessingIT.reprocessarOMesmoProdutorNaoMudaContagensNemVinculos
  Cenário: Reprocessar o mesmo produtor
    Dado um produtor já sincronizado
    Quando o mesmo produtor é processado de novo
    Então a contagem de registros e de vínculos não muda
