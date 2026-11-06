package br.com.gabrielriguiti.agro;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integração (*IT): sobe a aplicação contra um PostgreSQL vazio via Testcontainers.
 *
 * <p>O contêiner é um singleton compartilhado por todas as classes de teste (o contexto do Spring é reaproveitado
 * entre elas) e é encerrado pelo Ryuk ao fim da JVM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
