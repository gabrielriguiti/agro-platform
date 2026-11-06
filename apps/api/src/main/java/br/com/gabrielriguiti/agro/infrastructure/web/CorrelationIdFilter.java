package br.com.gabrielriguiti.agro.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Associa um identificador de correlação a cada requisição HTTP.
 *
 * <p>Reaproveita o cabeçalho {@value #HEADER} quando presente e, caso contrário, gera um UUID. O valor vai
 * para o MDC (chave {@value #MDC_KEY}), aparece em todo evento de log da requisição e é devolvido na resposta.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdFilter.class);

    /** Cabeçalho HTTP de entrada e saída. */
    public static final String HEADER = "X-Correlation-Id";

    /** Chave no MDC do SLF4J. */
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String id = (incoming == null || incoming.isBlank() || incoming.length() > 100)
                ? UUID.randomUUID().toString()
                : incoming;
        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
            LOG.info("{} {} -> {}", request.getMethod(), request.getRequestURI(), response.getStatus());
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
