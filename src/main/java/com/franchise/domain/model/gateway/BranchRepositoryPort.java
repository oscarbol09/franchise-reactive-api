package com.franchise.domain.model.gateway;

import com.franchise.domain.model.Branch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BranchRepositoryPort {

    Mono<Branch> save(Branch branch);

    Mono<Branch> findByIdAndFranchiseId(Long id, Long franchiseId);

    Flux<Branch> findAllByFranchiseId(Long franchiseId);

    Mono<Boolean> existsByFranchiseIdAndNameIgnoreCase(Long franchiseId, String name);

    Mono<Boolean> existsByFranchiseIdAndNameIgnoreCaseAndIdNot(Long franchiseId, String name, Long excludedId);
}
