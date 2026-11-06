/**
 * Motor financeiro: uma calculadora (Strategy) por natureza de entidade, sem dependência de Spring.
 *
 * <p>As calculadoras recebem um {@link br.com.gabrielriguiti.agro.application.financial.FinancialSubject} (números
 * já resolvidos pelo caso de uso) e as taxas em {@link br.com.gabrielriguiti.agro.application.financial.FinancialRates};
 * não conhecem entidades nem persistência. Uma nova regra é uma nova implementação de
 * {@link br.com.gabrielriguiti.agro.application.financial.FinancialCalculator} (ADR-0010).
 */
package br.com.gabrielriguiti.agro.application.financial;
