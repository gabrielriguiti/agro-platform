package br.com.gabrielriguiti.agro.domain.integration;

/** Estratégia de fusão entre o registro existente e o recebido de um parceiro (ADR-0015). */
public enum MergeStrategy {
    /** Preserva o existente e preenche apenas os campos vazios. */
    MERGE,
    /** O recebido substitui o existente. */
    OVERWRITE,
    /** Ignora o recebido quando o registro já existe. */
    KEEP_EXISTING
}
