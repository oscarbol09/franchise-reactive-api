package com.franchise.domain.exception;

import java.util.Objects;

public abstract sealed class DomainException extends RuntimeException
        permits DomainValidationException, DuplicateResourceException, ResourceNotFoundException {

    private final ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode, String detail) {
        super(Objects.requireNonNull(detail, "Error detail must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "ErrorCode must not be null");
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
