package com.accenture.franchise.domain.exception;

public final class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
