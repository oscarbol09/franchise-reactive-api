package com.franchise.domain.model.gateway;

import com.franchise.domain.model.Franchise;
import reactor.core.publisher.Mono;

public interface FranchiseRepositoryPort {

    Mono<Franchise> save(Franchise franchise);

    Mono<Franchise> findById(Long id);

    Mono<Boolean> existsByNameIgnoreCase(String name);

    Mono<Boolean> existsByNameIgnoreCaseAndIdNot(String name, Long excludedId);
}
