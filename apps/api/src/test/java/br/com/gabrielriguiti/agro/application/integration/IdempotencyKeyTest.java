package br.com.gabrielriguiti.agro.application.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.domain.integration.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Chave de idempotência: JSON canônico + SHA-256 (spec idempotent-processing, task 1.3). */
class IdempotencyKeyTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private String key(String source, MessageType type, String externalId, String json) throws Exception {
        return IdempotencyKey.of(source, type, externalId, mapper.readTree(json));
    }

    @Test
    void ordemDosCamposNaoAlteraAChave() throws Exception {
        String a = key("erp", MessageType.PRODUCER, "P-1", "{\"nome\":\"A\",\"x\":{\"b\":1,\"a\":[1,2]}}");
        String b = key("erp", MessageType.PRODUCER, "P-1", "{\"x\":{\"a\":[1,2],\"b\":1},\"nome\":\"A\"}");
        assertThat(a).isEqualTo(b).hasSize(64);
    }

    @Test
    void ordemDosElementosDeListaImporta() throws Exception {
        assertThat(key("erp", MessageType.PRODUCER, "P-1", "{\"l\":[1,2]}"))
                .isNotEqualTo(key("erp", MessageType.PRODUCER, "P-1", "{\"l\":[2,1]}"));
    }

    @Test
    void origemTipoIdExternoEConteudoCompoemAChave() throws Exception {
        String base = key("erp", MessageType.PRODUCER, "P-1", "{\"n\":1}");
        assertThat(base)
                .isNotEqualTo(key("lims", MessageType.PRODUCER, "P-1", "{\"n\":1}"))
                .isNotEqualTo(key("erp", MessageType.PROPERTY, "P-1", "{\"n\":1}"))
                .isNotEqualTo(key("erp", MessageType.PRODUCER, "P-2", "{\"n\":1}"))
                .isNotEqualTo(key("erp", MessageType.PRODUCER, "P-1", "{\"n\":2}"));
    }
}
