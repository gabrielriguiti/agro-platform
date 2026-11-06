package br.com.gabrielriguiti.agro.domain.registry;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Produtor rural, vinculado a exatamente um laboratório e a várias propriedades (ADR-0008).
 *
 * <p>Referencia o laboratório e as propriedades por id; a existência e a exclusividade são verificadas pelo caso
 * de uso.
 *
 * @param id identificador gerado pelo banco; {@code null} antes de persistir
 * @param identity nome, inscrição fiscal (CPF ou CNPJ) e endereço
 * @param dataInicial início do período de relacionamento
 * @param dataFinal fim do período, nunca anterior a {@code dataInicial}
 * @param observacoes texto livre opcional
 * @param laboratoryId laboratório do produtor (obrigatório)
 * @param propertyIds ids das propriedades do produtor (pode ser vazio)
 */
public record Producer(
        Long id,
        Identity identity,
        Instant dataInicial,
        Instant dataFinal,
        String observacoes,
        Long laboratoryId,
        Set<Long> propertyIds) {

    /**
     * @throws DomainValidationException se faltar identidade, período ou laboratório, ou se o período for invertido
     */
    public Producer {
        if (identity == null) {
            throw new DomainValidationException("nome", "identidade é obrigatória");
        }
        if (dataInicial == null) {
            throw new DomainValidationException("dataInicial", "dataInicial é obrigatória");
        }
        if (dataFinal == null) {
            throw new DomainValidationException("dataFinal", "dataFinal é obrigatória");
        }
        if (dataFinal.isBefore(dataInicial)) {
            throw new DomainValidationException("dataFinal", "dataFinal não pode ser anterior a dataInicial");
        }
        if (laboratoryId == null) {
            throw new DomainValidationException("laboratorioId", "laboratorioId é obrigatório");
        }
        propertyIds = propertyIds == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(propertyIds));
    }

    /** @return cópia com os campos editáveis substituídos, preservando o id */
    public Producer update(
            Identity newIdentity,
            Instant newDataInicial,
            Instant newDataFinal,
            String newObservacoes,
            Long newLaboratoryId,
            Set<Long> newPropertyIds) {
        return new Producer(id, newIdentity, newDataInicial, newDataFinal, newObservacoes, newLaboratoryId,
                newPropertyIds);
    }
}
