# language: pt
Funcionalidade: Consulta analítica de laboratórios
  Agrega produtores por laboratório, com filtros e total coerente com eles, em uma única consulta.

  # Verificado em: LabAnalyticsApiIT.respondeComNomeEmCaixaAltaTotalEValorFinanceiro
  Cenário: Nome em caixa alta
    Dado o laboratório "Laboratório Modelo 1" com produtores
    Quando a consulta é feita
    Então o campo "nome" é "LABORATÓRIO MODELO 1", com acentos preservados

  # Verificado em: LabAnalyticsApiIT.parametroObrigatorioAusenteOuInvalidoResponde400 e LabAnalyticsQueryIT.qtdMinimaCortaPeloTotalFiltradoELaboratorioSemProdutorNaoAparece
  Cenário: Quantidade mínima obrigatória e corte por quantidade
    Dado laboratórios com 5, 3 e 1 produtores
    Quando a consulta usa "qtdMinimaProdutores=3"
    Então só os laboratórios de 5 e 3 produtores aparecem
    E sem o parâmetro a API responde 400

  # Verificado em: LabAnalyticsQueryIT.totalFiltradoNaoEhTotalDeVinculos e totalCorrespondeAExatamenteOsProdutoresQueAtendemACadaCombinacaoDeFiltros
  Cenário: Total filtrado
    Dado um laboratório com 4 produtores, dos quais 2 atendem ao filtro de texto
    Quando a consulta é feita com esse filtro
    Então "totalProdutores" é 2

  # Verificado em: LabAnalyticsQueryIT.buscaTextualEhParcialSemDistincaoDeCaixaECuringasSaoLiterais
  Cenário: Busca textual com curinga literal
    Dado produtores cujas observações contêm "100%" e outros que não
    Quando a consulta usa "buscaTexto=100%"
    Então só as observações que contêm literalmente "100%" correspondem

  # Verificado em: LabAnalyticsApiIT.intervalosInvertidosEDatasMalFormadasResponde400
  Cenário: Intervalo de datas invertido
    Quando "dataInicialInicio" é posterior a "dataInicialFim"
    Então a API responde 400

  # Verificado em: LabAnalyticsQueryIT.totalDecrescenteTemPrecedenciaSobreAData e comFiltroDeDataInicialEmpateVaiParaQuemTemAMenorDataInicial
  Cenário: Ordenação
    Dado dois laboratórios com o mesmo total e filtro de "dataInicial"
    Quando a consulta é feita
    Então vem primeiro o laboratório com a menor "dataInicial" entre os produtores considerados

  # Verificado em: LabAnalyticsQueryIT.cinquentaLaboratoriosSaoLidosEmUmaUnicaConsulta
  Cenário: Uma única consulta ao banco
    Dado 50 laboratórios no resultado
    Quando a consulta é feita
    Então exatamente 1 consulta aos dados agregados é executada
