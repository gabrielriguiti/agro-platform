package br.com.gabrielriguiti.agro.application.integration;

import br.com.gabrielriguiti.agro.domain.integration.MessageType;

/**
 * Mensagem a enfileirar.
 *
 * @param idempotencyKey chave de idempotência
 * @param type tipo da entidade
 * @param externalId id do item no parceiro
 * @param payload JSON do item
 */
public record NewMessage(String idempotencyKey, MessageType type, String externalId, String payload) {
}
