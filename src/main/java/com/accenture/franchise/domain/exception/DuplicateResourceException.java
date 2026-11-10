package com.accenture.franchise.domain.exception;

public final class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
