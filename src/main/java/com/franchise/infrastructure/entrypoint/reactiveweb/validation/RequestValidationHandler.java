package com.franchise.infrastructure.entrypoint.reactiveweb.validation;

import com.franchise.domain.exception.DomainValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@Component
public class RequestValidationHandler {

    private final Validator validator;

    public RequestValidationHandler(Validator validator) {
        this.validator = validator;
    }

    public <T> Mono<T> requireValidBody(ServerRequest request, Class<T> bodyClass) {
        return request.bodyToMono(bodyClass)
                .onErrorMap(ServerWebInputException.class,
                        ex -> new DomainValidationException("Malformed or unreadable JSON request payload"))
                .switchIfEmpty(Mono.error(() -> new DomainValidationException("Request payload must not be empty")))
                .flatMap(this::validateBean);
    }

    public Mono<Long> requirePositivePathVariable(ServerRequest request, String variableName) {
        return Mono.fromCallable(() -> {
            String rawValue = request.pathVariable(variableName);
            try {
                long parsed = Long.parseLong(rawValue);
                if (parsed <= 0) {
                    throw new DomainValidationException(variableName + " must be a positive integer");
                }
                return parsed;
            } catch (NumberFormatException ex) {
                throw new DomainValidationException(variableName + " must be a valid numeric identifier");
            }
        });
    }

    private <T> Mono<T> validateBean(T payload) {
        Set<ConstraintViolation<T>> violations = validator.validate(payload);
        if (violations.isEmpty()) {
            return Mono.just(payload);
        }
        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        return Mono.error(() -> new DomainValidationException(message));
    }
}
