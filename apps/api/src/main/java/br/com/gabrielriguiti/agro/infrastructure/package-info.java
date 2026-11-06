/**
 * Adaptadores: controllers REST, persistência (JPA e JDBC), configuração, filtros e agendadores.
 *
 * <p>Implementa as portas declaradas em {@code application} e é a única camada que conhece frameworks. Controllers
 * chamam casos de uso, nunca repositórios diretamente (ADR-0001, ADR-0005).
 */
package br.com.gabrielriguiti.agro.infrastructure;
