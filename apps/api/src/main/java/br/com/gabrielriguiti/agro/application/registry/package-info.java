/**
 * Casos de uso e portas do contexto de cadastro ({@code registry}): laboratórios, propriedades e produtores.
 *
 * <p>Os casos de uso orquestram o domínio e verificam o que depende de outros agregados (existência e exclusividade
 * de vínculos, ADR-0008); a persistência entra apenas pelas portas {@code *Repository}.
 */
package br.com.gabrielriguiti.agro.application.registry;
