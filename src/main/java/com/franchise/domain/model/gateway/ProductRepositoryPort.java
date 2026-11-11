package com.franchise.domain.model.gateway;

import com.franchise.domain.model.BranchTopProduct;
import com.franchise.domain.model.Product;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductRepositoryPort {

    Mono<Product> save(Product product);

    Mono<Product> findByIdAndBranchId(Long id, Long branchId);

    Flux<Product> findAllByBranchId(Long branchId);

    Flux<Product> findAllByBranchIds(List<Long> branchIds);

    Mono<Void> deleteById(Long id);

    Mono<Boolean> existsByBranchIdAndNameIgnoreCase(Long branchId, String name);

    Mono<Boolean> existsByBranchIdAndNameIgnoreCaseAndIdNot(Long branchId, String name, Long excludedId);

    Flux<BranchTopProduct> findTopStockProductPerBranchByFranchiseId(Long franchiseId);
}
