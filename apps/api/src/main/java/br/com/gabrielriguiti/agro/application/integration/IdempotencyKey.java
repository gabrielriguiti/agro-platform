package br.com.gabrielriguiti.agro.application.integration;

import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Chave de idempotência da mensagem: SHA-256 de {@code source}, {@code type}, {@code externalId} e do payload em JSON
 * canônico (chaves ordenadas), de modo que a ordem dos campos não altera a chave (ADR-0013).
 */
public final class IdempotencyKey {

    private static final ObjectMapper CANONICAL = JsonMapper.builder()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private IdempotencyKey() {
    }

    /**
     * @param source sistema parceiro
     * @param type tipo da entidade
     * @param externalId id do item no parceiro
     * @param data payload do item
     * @return hash hexadecimal de 64 caracteres
     */
    public static String of(String source, MessageType type, String externalId, JsonNode data) {
        String material = String.join("\u0000", source, type.name(), externalId, canonical(data));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /** @return o JSON com as chaves de todos os objetos em ordem alfabética */
    static String canonical(JsonNode data) {
        try {
            return CANONICAL.writeValueAsString(CANONICAL.treeToValue(data, Object.class));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Payload não serializável", e);
        }
    }
}
