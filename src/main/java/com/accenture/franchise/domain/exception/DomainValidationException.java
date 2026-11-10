package com.accenture.franchise.domain.exception;

public final class DomainValidationException extends DomainException {

    public DomainValidationException(String detail) {
        super(ErrorCode.INVALID_ARGUMENT, detail);
    }
}
