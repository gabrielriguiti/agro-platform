package br.com.gabrielriguiti.agro.application.integration;

/**
 * Lote com contagens de mensagens por status e estado derivado.
 *
 * @param batch dados do lote
 * @param total mensagens do lote
 * @param pending mensagens pendentes
 * @param processing mensagens em processamento
 * @param done mensagens concluídas
 * @param dead mensagens na DLQ
 */
public record BatchSummary(BatchInfo batch, long total, long pending, long processing, long done, long dead) {

    /** Estado derivado das contagens. */
    public enum State {
        /** Ainda há mensagens pendentes ou em processamento. */
        EM_ANDAMENTO,
        /** Todas concluídas. */
        CONCLUIDO,
        /** Terminou, mas ao menos uma mensagem está na DLQ. */
        CONCLUIDO_COM_FALHAS
    }

    /** @return o estado do lote */
    public State state() {
        if (pending + processing > 0) {
            return State.EM_ANDAMENTO;
        }
        return dead > 0 ? State.CONCLUIDO_COM_FALHAS : State.CONCLUIDO;
    }
}
