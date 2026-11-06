/**
 * Núcleo do negócio: entidades, objetos de valor e regras (invariantes) de cadastro, integração, financeiro e análise.
 *
 * <p>Subpacotes por contexto: {@code registry}, {@code integration}, {@code financial} e {@code analytics}.
 * Só pode depender de Java padrão e de si mesmo: sem Spring, JPA ou qualquer framework (ADR-0001, ADR-0005).
 */
package br.com.gabrielriguiti.agro.domain;
