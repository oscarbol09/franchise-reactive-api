package com.accenture.franchise.infrastructure.drivenadapter.r2dbc.repository;

import com.accenture.franchise.infrastructure.drivenadapter.r2dbc.entity.BranchData;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BranchReactiveRepository extends ReactiveCrudRepository<BranchData, Long> {

    Mono<BranchData> findByIdAndFranchiseId(Long id, Long franchiseId);

    Flux<BranchData> findAllByFranchiseIdOrderByIdAsc(Long franchiseId);

    @Query("""
            SELECT CASE WHEN COUNT(1) > 0 THEN TRUE ELSE FALSE END
            FROM branches
            WHERE franchise_id = :franchiseId AND LOWER(name) = LOWER(:name)
            """)
    Mono<Boolean> existsByFranchiseIdAndNameIgnoreCase(Long franchiseId, String name);

    @Query("""
            SELECT CASE WHEN COUNT(1) > 0 THEN TRUE ELSE FALSE END
            FROM branches
            WHERE franchise_id = :franchiseId AND LOWER(name) = LOWER(:name) AND id <> :excludedId
            """)
    Mono<Boolean> existsByFranchiseIdAndNameIgnoreCaseAndIdNot(Long franchiseId, String name, Long excludedId);
}
