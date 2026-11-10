package com.franchise.domain.model.gateway;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface TransactionalPort {

    <T> Mono<T> executeInTransaction(Mono<T> publisher);

    static TransactionalPort passThrough() {
        return new TransactionalPort() {
            @Override
            public <T> Mono<T> executeInTransaction(Mono<T> publisher) {
                return publisher;
            }
        };
    }
}
