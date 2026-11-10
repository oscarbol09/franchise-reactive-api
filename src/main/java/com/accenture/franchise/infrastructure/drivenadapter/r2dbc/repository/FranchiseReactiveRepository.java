package com.accenture.franchise.infrastructure.drivenadapter.r2dbc.repository;

import com.accenture.franchise.infrastructure.drivenadapter.r2dbc.entity.FranchiseData;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface FranchiseReactiveRepository extends ReactiveCrudRepository<FranchiseData, Long> {

    @Query("SELECT CASE WHEN COUNT(1) > 0 THEN TRUE ELSE FALSE END FROM franchises WHERE LOWER(name) = LOWER(:name)")
    Mono<Boolean> existsByNameIgnoreCase(String name);

    @Query("SELECT CASE WHEN COUNT(1) > 0 THEN TRUE ELSE FALSE END FROM franchises WHERE LOWER(name) = LOWER(:name) AND id <> :excludedId")
    Mono<Boolean> existsByNameIgnoreCaseAndIdNot(String name, Long excludedId);
}
