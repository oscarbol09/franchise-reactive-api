package com.franchise.infrastructure.drivenadapter.r2dbc.repository;

import com.franchise.infrastructure.drivenadapter.r2dbc.entity.BranchData;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BranchReactiveRepository extends ReactiveCrudRepository<BranchData, Long> {

    @Query("""
            SELECT id, franchise_id, name
            FROM branches
            WHERE id = :id AND franchise_id = :franchiseId
            FOR UPDATE
            """)
    Mono<BranchData> findByIdAndFranchiseId(Long id, Long franchiseId);

    Flux<BranchData> findAllByFranchiseIdOrderByIdAsc(Long franchiseId);

    @Query("""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM branches WHERE franchise_id = :franchiseId AND name = :name
            ) THEN TRUE ELSE FALSE END
            """)
    Mono<Boolean> existsByFranchiseIdAndNameIgnoreCase(Long franchiseId, String name);

    @Query("""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM branches WHERE franchise_id = :franchiseId AND name = :name AND id <> :excludedId
            ) THEN TRUE ELSE FALSE END
            """)
    Mono<Boolean> existsByFranchiseIdAndNameIgnoreCaseAndIdNot(Long franchiseId, String name, Long excludedId);
}
