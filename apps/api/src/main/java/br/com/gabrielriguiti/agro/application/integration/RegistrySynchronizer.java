package br.com.gabrielriguiti.agro.application.integration;

import br.com.gabrielriguiti.agro.application.registry.ConflictException;
import br.com.gabrielriguiti.agro.application.registry.LaboratoryRepository;
import br.com.gabrielriguiti.agro.application.registry.ProducerRepository;
import br.com.gabrielriguiti.agro.application.registry.PropertyRepository;
import br.com.gabrielriguiti.agro.domain.integration.MergeStrategy;
import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import br.com.gabrielriguiti.agro.domain.integration.PermanentIntegrationException;
import br.com.gabrielriguiti.agro.domain.integration.ReconciliationPolicy;
import br.com.gabrielriguiti.agro.domain.registry.Identity;
import br.com.gabrielriguiti.agro.domain.registry.Laboratory;
import br.com.gabrielriguiti.agro.domain.registry.Producer;
import br.com.gabrielriguiti.agro.domain.registry.Property;
import br.com.gabrielriguiti.agro.domain.registry.TaxId;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Sincroniza as entidades de uma mensagem com o cadastro: resolve a identidade (primeiro por origem+id externo e,
 * na ausência, pela inscrição fiscal), reconcilia pela estratégia do lote e vincula o id externo (ADR-0013 e 0015).
 *
 * <p>Não abre transação: o chamador ({@link MessageProcessor}) executa tudo em uma transação por mensagem, de
 * modo que ou todas as entidades da mensagem são gravadas, ou nenhuma.
 *
 * <p>Formato do payload (nomes iguais aos da API de cadastro): laboratório e propriedade têm {@code nome},
 * {@code inscricaoFiscal} e {@code endereco}; o produtor soma {@code dataInicial}, {@code dataFinal},
 * {@code observacoes}, {@code laboratorio} (objeto com {@code id}) e {@code propriedades} (lista de objetos com
 * {@code id}).
 */
@Component
public class RegistrySynchronizer {

    private final LaboratoryRepository laboratories;
    private final PropertyRepository properties;
    private final ProducerRepository producers;
    private final ExternalReferencePort references;

    public RegistrySynchronizer(
            LaboratoryRepository laboratories,
            PropertyRepository properties,
            ProducerRepository producers,
            ExternalReferencePort references) {
        this.laboratories = laboratories;
        this.properties = properties;
        this.producers = producers;
        this.references = references;
    }

    /**
     * Sincroniza o item da mensagem.
     *
     * @param type tipo da entidade
     * @param source sistema parceiro
     * @param externalId id do item no parceiro
     * @param data payload já interpretado
     * @param strategy estratégia de fusão do lote
     * @throws PermanentIntegrationException se o payload for malformado ou houver conflito de identidade
     * @throws br.com.gabrielriguiti.agro.domain.registry.DomainValidationException se um dado violar o domínio
     */
    public void sync(MessageType type, String source, String externalId, JsonNode data, MergeStrategy strategy) {
        switch (type) {
            case LABORATORY -> syncLaboratory(source, externalId, data, strategy);
            case PROPERTY -> syncProperty(source, externalId, data, strategy);
            case PRODUCER -> syncProducer(source, externalId, data, strategy);
        }
    }

    private long syncLaboratory(String source, String externalId, JsonNode data, MergeStrategy strategy) {
        requireObject(data, "laboratório");
        Laboratory received = Laboratory.create(identity(data));
        Optional<Long> existingId = resolve(MessageType.LABORATORY, source, externalId, received.identity().taxId(),
                laboratories.findIdByTaxId(received.identity().taxId().digits()));
        if (existingId.isEmpty()) {
            long id = laboratories.save(received).id();
            references.link(MessageType.LABORATORY, id, source, externalId);
            return id;
        }
        long id = existingId.get();
        Laboratory existing = laboratories.findById(id).orElseThrow();
        linkIfMissing(MessageType.LABORATORY, id, source, externalId);
        Identity merged = ReconciliationPolicy.identity(existing.identity(), received.identity(), strategy);
        if (!merged.equals(existing.identity())) {
            laboratories.save(existing.withIdentity(merged));
        }
        return id;
    }

    private long syncProperty(String source, String externalId, JsonNode data, MergeStrategy strategy) {
        requireObject(data, "propriedade");
        Property received = Property.create(identity(data));
        Optional<Long> existingId = resolve(MessageType.PROPERTY, source, externalId, received.identity().taxId(),
                properties.findIdByTaxId(received.identity().taxId().digits()));
        if (existingId.isEmpty()) {
            long id = properties.save(received).id();
            references.link(MessageType.PROPERTY, id, source, externalId);
            return id;
        }
        long id = existingId.get();
        Property existing = properties.findById(id).orElseThrow();
        linkIfMissing(MessageType.PROPERTY, id, source, externalId);
        Identity merged = ReconciliationPolicy.identity(existing.identity(), received.identity(), strategy);
        if (!merged.equals(existing.identity())) {
            properties.save(existing.withIdentity(merged));
        }
        return id;
    }

    private void syncProducer(String source, String externalId, JsonNode data, MergeStrategy strategy) {
        requireObject(data, "produtor");
        JsonNode labNode = data.get("laboratorio");
        if (labNode == null || labNode.isNull()) {
            throw new PermanentIntegrationException("laboratorio é obrigatório para o produtor");
        }
        long laboratoryId = syncLaboratory(source, nestedId(labNode, "laboratorio"), labNode, strategy);
        Set<Long> propertyIds = new LinkedHashSet<>();
        JsonNode propertyNodes = data.get("propriedades");
        if (propertyNodes != null && !propertyNodes.isNull()) {
            if (!propertyNodes.isArray()) {
                throw new PermanentIntegrationException("propriedades deve ser uma lista");
            }
            for (JsonNode node : propertyNodes) {
                propertyIds.add(syncProperty(source, nestedId(node, "propriedades"), node, strategy));
            }
        }
        Producer received = new Producer(null, identity(data), instant(data, "dataInicial"),
                instant(data, "dataFinal"), text(data, "observacoes"), laboratoryId, propertyIds);
        Optional<Long> existingId = resolve(MessageType.PRODUCER, source, externalId, received.identity().taxId(),
                producers.findIdByTaxId(received.identity().taxId().digits()));
        if (existingId.isEmpty()) {
            long id = producers.save(received).id();
            references.link(MessageType.PRODUCER, id, source, externalId);
            linkProperties(id, propertyIds);
            return;
        }
        long id = existingId.get();
        Producer existing = producers.findDetailsById(id).orElseThrow().producer();
        linkIfMissing(MessageType.PRODUCER, id, source, externalId);
        Producer merged = ReconciliationPolicy.producer(existing, received, strategy);
        if (!merged.equals(existing)) {
            producers.save(merged);
        }
        if (strategy != MergeStrategy.KEEP_EXISTING) {
            linkProperties(id, propertyIds);
        }
    }

    /**
     * Localiza o registro pela origem+id externo e, na ausência, pela inscrição fiscal. Se as duas buscas apontarem
     * para registros diferentes, é conflito de identidade (falha permanente, nada é alterado).
     */
    private Optional<Long> resolve(
            MessageType type, String source, String externalId, TaxId taxId, Optional<Long> byTaxId) {
        Optional<Long> byExternal = references.find(type, source, externalId);
        if (byExternal.isPresent() && byTaxId.isPresent() && !byExternal.get().equals(byTaxId.get())) {
            throw new PermanentIntegrationException("conflito de identidade: o id externo " + externalId + " de "
                    + source + " aponta para o registro " + byExternal.get() + ", mas a inscrição fiscal "
                    + taxId.formatted() + " pertence ao registro " + byTaxId.get());
        }
        return byExternal.isPresent() ? byExternal : byTaxId;
    }

    private void linkIfMissing(MessageType type, long id, String source, String externalId) {
        if (references.find(type, source, externalId).isEmpty()) {
            references.link(type, id, source, externalId);
        }
    }

    /** Vincula apenas propriedades livres ou já do produtor; nunca remove vínculos existentes. */
    private void linkProperties(long producerId, Set<Long> propertyIds) {
        if (!propertyIds.isEmpty() && properties.link(producerId, propertyIds) != propertyIds.size()) {
            throw new ConflictException("Alguma propriedade informada já pertence a outro produtor");
        }
    }

    private static void requireObject(JsonNode data, String what) {
        if (data == null || !data.isObject()) {
            throw new PermanentIntegrationException("payload de " + what + " malformado: esperado um objeto JSON");
        }
    }

    private static String nestedId(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            throw new PermanentIntegrationException(field + " deve conter objetos JSON");
        }
        String id = text(node, "id");
        if (id == null || id.isBlank()) {
            throw new PermanentIntegrationException("id é obrigatório em " + field);
        }
        return id.strip();
    }

    private static Identity identity(JsonNode data) {
        return new Identity(text(data, "nome"), TaxId.of(text(data, "inscricaoFiscal")), text(data, "endereco"));
    }

    private static String text(JsonNode data, String field) {
        JsonNode value = data.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Instant instant(JsonNode data, String field) {
        String value = text(data, field);
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new PermanentIntegrationException(field + " deve estar em ISO-8601", e);
        }
    }
}
