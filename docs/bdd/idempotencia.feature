# language: pt
Funcionalidade: Idempotência do processamento
  Repetir, reordenar ou paralelizar o envio não duplica registros nem quebra vínculos.

  # Verificado em: BatchIngestionApiIT.reenvioIdenticoContaDuplicatasENaoCriaMensagens
  Cenário: Reenvio idêntico de um lote
    Dado que um lote de 2 itens foi aceito
    Quando o mesmo lote é enviado de novo
    Então a resposta tem "accepted" 0 e "duplicates" 2
    E nenhuma mensagem nova existe na outbox

  # Verificado em: IdempotencyKeyTest.ordemDosCamposNaoAlteraAChave e BatchIngestionApiIT.mesmoPayloadComCamposEmOutraOrdemEDuplicata
  Cenário: Mesmo payload com os campos em outra ordem
    Dado que um item foi aceito
    Quando o mesmo item chega com os campos do payload em outra ordem
    Então a chave de idempotência é a mesma
    E o item é tratado como duplicata

  # Verificado em: IntegrationProcessingIT.duasMensagensComOMesmoCnpjTerminamComUmUnicoRegistro
  Cenário: Corrida entre dois workers pelo mesmo CNPJ
    Dado duas mensagens com ids externos diferentes e o mesmo CNPJ
    Quando dois workers as processam ao mesmo tempo
    Então um cria o registro e o outro recebe a violação de unicidade e é reagendado
    E ao final existe um único registro e as duas mensagens estão "DONE"

  # Verificado em: IntegrationProcessingIT.falhaNaSegundaPropriedadeNaoDeixaNadaGravado
  Cenário: Atomicidade por mensagem
    Dado um produtor com laboratório e duas propriedades aninhadas, a segunda com CPF inválido
    Quando a mensagem é processada
    Então nem o produtor, nem o laboratório, nem a primeira propriedade ficam gravados

  # Verificado em: IntegrationProcessingIT.reenvioParcialNaoRemoveNemDuplicaPropriedadesJaVinculadas
  Cenário: Reenvio parcial preserva os vínculos
    Dado um produtor vinculado às propriedades A e B
    Quando ele é reenviado com apenas a propriedade A
    Então A e B continuam vinculadas, sem duplicata
