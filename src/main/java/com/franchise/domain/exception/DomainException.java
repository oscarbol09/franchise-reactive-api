package com.franchise.domain.exception;

import java.util.Objects;

public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    public DomainException(ErrorCode errorCode, String detail) {
        super(Objects.requireNonNull(detail, "Error detail must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "ErrorCode must not be null");
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
