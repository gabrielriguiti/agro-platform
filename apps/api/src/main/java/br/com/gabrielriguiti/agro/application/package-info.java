/**
 * Casos de uso e portas (interfaces) que orquestram o {@code domain}.
 *
 * <p>Declara as portas de entrada e de saída; depende de {@code domain} e nunca de {@code infrastructure}, que
 * implementa as portas (ADR-0001). O motor financeiro vive aqui, sem Spring, para ser testado sem contexto.
 */
package br.com.gabrielriguiti.agro.application;
