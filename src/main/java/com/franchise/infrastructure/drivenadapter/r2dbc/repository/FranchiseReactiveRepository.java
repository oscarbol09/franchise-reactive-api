package com.franchise.infrastructure.drivenadapter.r2dbc.repository;

import com.franchise.infrastructure.drivenadapter.r2dbc.entity.FranchiseData;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface FranchiseReactiveRepository extends ReactiveCrudRepository<FranchiseData, Long> {

    @Query("""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM franchises WHERE name = :name
            ) THEN 1 ELSE 0 END
            """)
    Mono<Long> existsByNameIgnoreCase(String name);

    @Query("""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM franchises WHERE name = :name AND id <> :excludedId
            ) THEN 1 ELSE 0 END
            """)
    Mono<Long> existsByNameIgnoreCaseAndIdNot(String name, Long excludedId);
}
