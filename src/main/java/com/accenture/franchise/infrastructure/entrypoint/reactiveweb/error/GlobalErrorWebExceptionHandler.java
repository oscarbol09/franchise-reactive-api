package com.accenture.franchise.infrastructure.entrypoint.reactiveweb.error;

import com.accenture.franchise.domain.exception.DomainException;
import com.accenture.franchise.domain.exception.DomainValidationException;
import com.accenture.franchise.domain.exception.DuplicateResourceException;
import com.accenture.franchise.domain.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        ProblemDetail problem = buildProblemDetail(exchange, ex);
        HttpStatus status = HttpStatus.resolve(problem.getStatus());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(problem))
                .map(bytes -> {
                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                    return buffer;
                })
                .flatMap(buffer -> exchange.getResponse().writeWith(Mono.just(buffer)));
    }

    private ProblemDetail buildProblemDetail(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();

        if (ex instanceof ResourceNotFoundException notFound) {
            return createProblem(HttpStatus.NOT_FOUND, notFound, path);
        }
        if (ex instanceof DuplicateResourceException duplicate) {
            return createProblem(HttpStatus.CONFLICT, duplicate, path);
        }
        if (ex instanceof DomainValidationException validation) {
            return createProblem(HttpStatus.BAD_REQUEST, validation, path);
        }
        if (ex instanceof DataIntegrityViolationException) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Database integrity constraint violated for the requested operation"
            );
            problem.setTitle("Data Integrity Conflict");
            problem.setInstance(URI.create(path));
            problem.setProperty("errorCode", "ERR-409-INTEGRITY");
            problem.setProperty("timestamp", Instant.now().toString());
            return problem;
        }
        if (ex instanceof ResponseStatusException statusEx) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    statusEx.getStatusCode(),
                    statusEx.getReason() != null ? statusEx.getReason() : statusEx.getMessage()
            );
            problem.setTitle("HTTP Request Error");
            problem.setInstance(URI.create(path));
            problem.setProperty("timestamp", Instant.now().toString());
            return problem;
        }

        log.error("Unhandled exception processing request [{} {}]",
                exchange.getRequest().getMethod(), path, ex);

        ProblemDetail fallback = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred"
        );
        fallback.setTitle("Internal Server Error");
        fallback.setInstance(URI.create(path));
        fallback.setProperty("errorCode", "ERR-500-INTERNAL");
        fallback.setProperty("timestamp", Instant.now().toString());
        return fallback;
    }

    private ProblemDetail createProblem(HttpStatus status, DomainException ex, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(ex.errorCode().defaultTitle());
        problem.setType(URI.create("urn:problem-type:" + ex.errorCode().code().toLowerCase()));
        problem.setInstance(URI.create(path));
        problem.setProperty("errorCode", ex.errorCode().code());
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
