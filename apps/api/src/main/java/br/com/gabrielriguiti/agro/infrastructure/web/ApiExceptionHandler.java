package br.com.gabrielriguiti.agro.infrastructure.web;

import br.com.gabrielriguiti.agro.application.integration.BatchTooLargeException;
import br.com.gabrielriguiti.agro.application.integration.InvalidBatchException;
import br.com.gabrielriguiti.agro.application.registry.ConflictException;
import br.com.gabrielriguiti.agro.application.registry.ResourceNotFoundException;
import br.com.gabrielriguiti.agro.domain.registry.DomainValidationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduz exceções em {@code application/problem+json} (RFC 9457) com {@code title}, {@code status}, {@code detail} e,
 * em validação, a propriedade {@code errors} com os campos inválidos.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Campo inválido listado em {@code errors}. */
    public record FieldProblem(String field, String message) {
    }

    @ExceptionHandler(DomainValidationException.class)
    ResponseEntity<Object> domainValidation(DomainValidationException e) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage(),
                List.of(new FieldProblem(e.field(), e.getMessage())));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Object> notFound(ResourceNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", e.getMessage(), null);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Object> conflict(ConflictException e) {
        return problem(HttpStatus.CONFLICT, "Conflito", e.getMessage(), null);
    }

    @ExceptionHandler(InvalidBatchException.class)
    ResponseEntity<Object> invalidBatch(InvalidBatchException e) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage(),
                List.of(new FieldProblem(e.field(), e.getMessage())));
    }

    @ExceptionHandler(BatchTooLargeException.class)
    ResponseEntity<Object> batchTooLarge(BatchTooLargeException e) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Lote grande demais", e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception e) {
        LOG.error("Erro inesperado", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Erro interno do servidor", null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldProblem> errors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(new FieldProblem(error.getField(), error.getDefaultMessage()));
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.add(new FieldProblem(error.getObjectName(), error.getDefaultMessage())));
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", "Há campos inválidos na requisição", errors);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldProblem> errors = new ArrayList<>();
        ex.getAllValidationResults().forEach(result -> result.getResolvableErrors().forEach(error -> errors.add(
                new FieldProblem(result.getMethodParameter().getParameterName(), error.getDefaultMessage()))));
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", "Há parâmetros inválidos na requisição",
                errors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "Corpo da requisição ausente, malformado ou com valor inválido (datas em ISO-8601)", null);
    }

    private static ResponseEntity<Object> problem(
            HttpStatus status, String title, String detail, List<FieldProblem> errors) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        if (errors != null) {
            body.setProperty("errors", errors);
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
