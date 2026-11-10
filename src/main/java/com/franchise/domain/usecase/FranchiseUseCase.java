package com.franchise.domain.usecase;

import com.franchise.domain.exception.DomainValidationException;
import com.franchise.domain.exception.DuplicateResourceException;
import com.franchise.domain.exception.ErrorCode;
import com.franchise.domain.exception.ResourceNotFoundException;
import com.franchise.domain.model.Branch;
import com.franchise.domain.model.BranchTopProduct;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.domain.model.gateway.BranchRepositoryPort;
import com.franchise.domain.model.gateway.FranchiseRepositoryPort;
import com.franchise.domain.model.gateway.ProductRepositoryPort;
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FranchiseUseCase {

    private final FranchiseRepositoryPort franchiseRepository;
    private final BranchRepositoryPort branchRepository;
    private final ProductRepositoryPort productRepository;

    public FranchiseUseCase(
            FranchiseRepositoryPort franchiseRepository,
            BranchRepositoryPort branchRepository,
            ProductRepositoryPort productRepository
    ) {
        this.franchiseRepository = Objects.requireNonNull(franchiseRepository);
        this.branchRepository = Objects.requireNonNull(branchRepository);
        this.productRepository = Objects.requireNonNull(productRepository);
    }

    public Mono<Franchise> createFranchise(String name) {
        return Mono.fromCallable(() -> Franchise.createNew(name))
                .flatMap(candidate -> franchiseRepository.existsByNameIgnoreCase(candidate.name())
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Mono.error(new DuplicateResourceException(
                                        ErrorCode.DUPLICATE_FRANCHISE_NAME,
                                        "Franchise with name '" + candidate.name() + "' already exists"))
                                : franchiseRepository.save(candidate)));
    }

    public Mono<Franchise> updateFranchiseName(Long franchiseId, String newName) {
        return validatePositiveId(franchiseId, "franchiseId")
                .then(Mono.defer(() -> findExistingFranchise(franchiseId)))
                .map(existing -> existing.withUpdatedName(newName))
                .flatMap(updated -> franchiseRepository.existsByNameIgnoreCaseAndIdNot(updated.name(), franchiseId)
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Mono.error(new DuplicateResourceException(
                                        ErrorCode.DUPLICATE_FRANCHISE_NAME,
                                        "Franchise with name '" + updated.name() + "' already exists"))
                                : franchiseRepository.save(updated)));
    }

    public Mono<Franchise> getFranchiseDetail(Long franchiseId) {
        return validatePositiveId(franchiseId, "franchiseId")
                .then(Mono.defer(() -> findExistingFranchise(franchiseId)))
                .flatMap(franchise -> branchRepository.findAllByFranchiseId(franchise.id())
                        .flatMap(branch -> productRepository.findAllByBranchId(branch.id())
                                .collectList()
                                .map(branch::withProducts))
                        .collectList()
                        .map(franchise::withBranches));
    }

    public Mono<Branch> addBranchToFranchise(Long franchiseId, String branchName) {
        return validatePositiveId(franchiseId, "franchiseId")
                .then(Mono.fromCallable(() -> Branch.createNew(franchiseId, branchName)))
                .flatMap(candidate -> findExistingFranchise(franchiseId)
                        .then(branchRepository.existsByFranchiseIdAndNameIgnoreCase(franchiseId, candidate.name()))
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Mono.error(new DuplicateResourceException(
                                        ErrorCode.DUPLICATE_BRANCH_NAME,
                                        "Branch '" + candidate.name() + "' already exists in franchise " + franchiseId))
                                : branchRepository.save(candidate)));
    }

    public Mono<Branch> updateBranchName(Long franchiseId, Long branchId, String newName) {
        return validatePositiveId(franchiseId, "franchiseId")
                .then(validatePositiveId(branchId, "branchId"))
                .then(Mono.defer(() -> findExistingBranch(franchiseId, branchId)))
                .map(existing -> existing.withUpdatedName(newName))
                .flatMap(updated -> branchRepository.existsByFranchiseIdAndNameIgnoreCaseAndIdNot(
                                franchiseId, updated.name(), branchId)
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Mono.error(new DuplicateResourceException(
                                        ErrorCode.DUPLICATE_BRANCH_NAME,
                                        "Branch '" + updated.name() + "' already exists in franchise " + franchiseId))
                                : branchRepository.save(updated)));
    }

    public Mono<Product> addProductToBranch(Long franchiseId, Long branchId, String productName, int stock) {
        return validatePositiveId(franchiseId, "franchiseId")
                .then(validatePositiveId(branchId, "branchId"))
                .then(Mono.fromCallable(() -> Product.createNew(branchId, productName, stock)))
                .flatMap(candidate -> findExistingBranch(franchiseId, branchId)
                        .then(productRepository.existsByBranchIdAndNameIgnoreCase(branchId, candidate.name()))
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Mono.error(new DuplicateResourceException(
                                        ErrorCode.DUPLICATE_PRODUCT_NAME,
                                        "Product '" + candidate.name() + "' already exists in branch " + branchId))
                                : productRepository.save(candidate)));
    }

    public Mono<Void> removeProductFromBranch(Long franchiseId, Long branchId, Long productId) {
        return validatePositiveId(franchiseId, "franchiseId")
                .then(validatePositiveId(branchId, "branchId"))
                .then(validatePositiveId(productId, "productId"))
                .then(Mono.defer(() -> findExistingBranch(franchiseId, branchId)))
                .then(Mono.defer(() -> findExistingProduct(branchId, productId)))
                .flatMap(product -> productRepository.deleteById(product.id()));
    }

    public Mono<Product> updateProductStock(Long franchiseId, Long branchId, Long productId, int newStock) {
        return validatePositiveId(franchiseId, "franchiseId")
                .then(validatePositiveId(branchId, "branchId"))
                .then(validatePositiveId(productId, "productId"))
                .then(Mono.defer(() -> findExistingBranch(franchiseId, branchId)))
                .then(Mono.defer(() -> findExistingProduct(branchId, productId)))
                .map(existing -> existing.withUpdatedStock(newStock))
                .flatMap(productRepository::save);
    }

    public Mono<Product> updateProductName(Long franchiseId, Long branchId, Long productId, String newName) {
        return validatePositiveId(franchiseId, "franchiseId")
                .then(validatePositiveId(branchId, "branchId"))
                .then(validatePositiveId(productId, "productId"))
                .then(Mono.defer(() -> findExistingBranch(franchiseId, branchId)))
                .then(Mono.defer(() -> findExistingProduct(branchId, productId)))
                .map(existing -> existing.withUpdatedName(newName))
                .flatMap(updated -> productRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(
                                branchId, updated.name(), productId)
                        .flatMap(exists -> Boolean.TRUE.equals(exists)
                                ? Mono.error(new DuplicateResourceException(
                                        ErrorCode.DUPLICATE_PRODUCT_NAME,
                                        "Product '" + updated.name() + "' already exists in branch " + branchId))
                                : productRepository.save(updated)));
    }

    public Flux<BranchTopProduct> getTopStockProductsByFranchise(Long franchiseId) {
        return validatePositiveId(franchiseId, "franchiseId")
                .then(Mono.defer(() -> findExistingFranchise(franchiseId)))
                .flatMapMany(franchise -> productRepository.findTopStockProductPerBranchByFranchiseId(franchise.id()));
    }

    private Mono<Franchise> findExistingFranchise(Long franchiseId) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        ErrorCode.FRANCHISE_NOT_FOUND,
                        "Franchise with id " + franchiseId + " was not found")));
    }

    private Mono<Branch> findExistingBranch(Long franchiseId, Long branchId) {
        return findExistingFranchise(franchiseId)
                .then(branchRepository.findByIdAndFranchiseId(branchId, franchiseId))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        ErrorCode.BRANCH_NOT_FOUND,
                        "Branch with id " + branchId + " was not found in franchise " + franchiseId)));
    }

    private Mono<Product> findExistingProduct(Long branchId, Long productId) {
        return productRepository.findByIdAndBranchId(productId, branchId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Product with id " + productId + " was not found in branch " + branchId)));
    }

    private static Mono<Void> validatePositiveId(Long id, String paramName) {
        if (id == null || id <= 0) {
            return Mono.error(new DomainValidationException(paramName + " must be a positive number"));
        }
        return Mono.empty();
    }
}
