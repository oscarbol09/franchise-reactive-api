package com.franchise.infrastructure.drivenadapter.r2dbc.repository;

import com.franchise.infrastructure.drivenadapter.r2dbc.entity.ProductData;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductReactiveRepository extends ReactiveCrudRepository<ProductData, Long> {

    Mono<ProductData> findByIdAndBranchId(Long id, Long branchId);

    Flux<ProductData> findAllByBranchIdOrderByIdAsc(Long branchId);

    @Query("""
            SELECT CASE WHEN COUNT(1) > 0 THEN TRUE ELSE FALSE END
            FROM products
            WHERE branch_id = :branchId AND LOWER(name) = LOWER(:name)
            """)
    Mono<Boolean> existsByBranchIdAndNameIgnoreCase(Long branchId, String name);

    @Query("""
            SELECT CASE WHEN COUNT(1) > 0 THEN TRUE ELSE FALSE END
            FROM products
            WHERE branch_id = :branchId AND LOWER(name) = LOWER(:name) AND id <> :excludedId
            """)
    Mono<Boolean> existsByBranchIdAndNameIgnoreCaseAndIdNot(Long branchId, String name, Long excludedId);
}
