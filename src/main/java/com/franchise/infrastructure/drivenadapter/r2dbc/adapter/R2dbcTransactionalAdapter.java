package com.franchise.infrastructure.drivenadapter.r2dbc.adapter;

import com.franchise.domain.model.gateway.TransactionalPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Component
public class R2dbcTransactionalAdapter implements TransactionalPort {

    private final TransactionalOperator transactionalOperator;

    public R2dbcTransactionalAdapter(TransactionalOperator transactionalOperator) {
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public <T> Mono<T> executeInTransaction(Mono<T> publisher) {
        return publisher.as(transactionalOperator::transactional);
    }
}
