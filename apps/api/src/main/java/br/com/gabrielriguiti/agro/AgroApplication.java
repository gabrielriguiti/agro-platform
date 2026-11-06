package br.com.gabrielriguiti.agro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da API da plataforma agro.
 *
 * <p>A estrutura de pacotes segue a arquitetura hexagonal (ADR-0001):
 * {@code domain}, {@code application} e {@code infrastructure}.
 */
@SpringBootApplication
public class AgroApplication {

    /**
     * Inicia a aplicação Spring Boot.
     *
     * @param args argumentos de linha de comando
     */
    public static void main(String[] args) {
        SpringApplication.run(AgroApplication.class, args);
    }
}
