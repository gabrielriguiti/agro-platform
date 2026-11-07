# language: pt
Funcionalidade: Retry com backoff e Dead Letter Queue
  Falhas passageiras são retentadas com atraso crescente; falhas sem conserto vão para a DLQ.

  # Verificado em: BackoffPolicyTest.progressaoDe5A80SegundosSemJitter e nuncaPassaDoTetoDeDezMinutos
  Cenário: Progressão do atraso
    Dado a mesma mensagem falhando de forma transitória em 5 tentativas, com jitter zero
    Então os atrasos são 5 s, 10 s, 20 s, 40 s e 80 s
    E nunca passam de 10 minutos

  # Verificado em: IntegrationProcessingIT.falhaTransitoriaReagendaComBackoffEEsgotaNaDlq
  Cenário: Tentativas esgotadas
    Dado uma mensagem que falha de forma transitória a cada tentativa
    Quando a quinta tentativa falha
    Então a mensagem fica "DEAD" com "attempts" 5, o último erro e o payload original

  # Verificado em: IntegrationProcessingIT.falhaPermanenteVaiDiretoParaDlqNaPrimeiraTentativa
  Cenário: Falha permanente vai direto para a DLQ
    Dado uma mensagem com inscrição fiscal inválida
    Quando ela é processada
    Então ela fica "DEAD" já na primeira tentativa, com o motivo registrado

  # Verificado em: IntegrationMonitoringApiIT.reprocessarLoteReenfileiraTodasAsMortasEAposACorrecaoConclui
  Cenário: Reprocessar após corrigir a causa
    Dado uma mensagem "DEAD" cuja causa foi corrigida
    Quando o lote é reprocessado
    Então a mensagem volta a "PENDING" com zero tentativas e conclui como "DONE" sem duplicatas

  # Verificado em: IntegrationMonitoringApiIT.reprocessarMensagemForaDeDeadResponde409ENaoMudaNada
  Cenário: Reprocessar mensagem que não está em DEAD
    Dado uma mensagem "DONE"
    Quando o reprocessamento é pedido
    Então a API responde 409 e nada muda

  # Verificado em: OutboxPortIT.leaseVencidoTornaAMensagemElegivelDeNovoComATentativaContada
  Cenário: Worker que morreu
    Dado uma mensagem "PROCESSING" cujo lease venceu sem conclusão
    Quando um worker reserva mensagens
    Então a mensagem é reservada de novo e a tentativa é contada
