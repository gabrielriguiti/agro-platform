package br.com.gabrielriguiti.agro.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.gabrielriguiti.agro.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** O seed do profile {@code demo} envia um lote ao pipeline e não duplica nada ao rodar de novo. */
@ActiveProfiles("demo")
class DemoSeedIT extends AbstractIntegrationTest {

    @Autowired
    DemoSeed seed;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void rodarDuasVezesGravaUmUnicoLoteComTodasAsMensagens() {
        seed.run(null);
        seed.run(null);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM integration_batch WHERE source = ?", Integer.class,
                DemoSeed.SOURCE)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM outbox_message m JOIN integration_batch b ON b.id = m.batch_id WHERE b.source = ?",
                Integer.class, DemoSeed.SOURCE)).isEqualTo(7);
    }
}
