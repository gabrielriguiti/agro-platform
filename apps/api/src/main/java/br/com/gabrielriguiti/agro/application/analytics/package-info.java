/**
 * Consulta analítica de laboratórios (leitura separada da escrita, ADR-0016): a porta
 * {@link br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsPort} entrega a agregação feita no banco e o
 * caso de uso {@link br.com.gabrielriguiti.agro.application.analytics.LabAnalyticsService} apresenta o nome e
 * calcula o valor financeiro com o motor, sem consultas por laboratório.
 */
package br.com.gabrielriguiti.agro.application.analytics;
