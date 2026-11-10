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
import com.franchise.domain.model.gateway.TransactionalPort;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FranchiseUseCaseTest {

    @Mock
    private FranchiseRepositoryPort franchiseRepository;

    @Mock
    private BranchRepositoryPort branchRepository;

    @Mock
    private ProductRepositoryPort productRepository;

    private FranchiseUseCase useCase;

    @BeforeAll
    static void installBlockHound() {
        BlockHound.install(builder -> builder
                .allowBlockingCallsInside("org.springframework.util.JdkIdGenerator", "generateId")
                .allowBlockingCallsInside("java.util.UUID", "randomUUID")
                .allowBlockingCallsInside(
                        "org.springframework.core.type.classreading.SimpleMetadataReaderFactory",
                        "getMetadataReader"
                ));
    }

    @BeforeEach
    void setUp() {
        useCase = new FranchiseUseCase(
                franchiseRepository,
                branchRepository,
                productRepository,
                TransactionalPort.passThrough()
        );
    }

    @Nested
    @DisplayName("Domain Model Invariants")
    class DomainModelInvariants {

        @Test
        @DisplayName("Enforces length limit and null checks across Franchise, Branch, and Product records")
        void shouldValidateDomainRecordsDirectly() {
            String overLimit = "A".repeat(121);

            assertThatThrownBy(() -> Franchise.createNew(overLimit))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("120 characters");

            assertThatThrownBy(() -> new Branch(1L, 0L, "Sucursal", null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("positive number");

            assertThatThrownBy(() -> new Branch(1L, null, "Sucursal", null))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> new Branch(1L, 1L, "   ", null))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> new Branch(1L, 1L, overLimit, null))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> new Product(1L, 0L, "Café", 10))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("positive number");

            assertThatThrownBy(() -> new Product(1L, null, "Café", 10))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> new Product(1L, 1L, "  ", 10))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> new Product(1L, 1L, overLimit, 10))
                    .isInstanceOf(DomainValidationException.class);

            Franchise nullBranches = new Franchise(1L, "Valid", null);
            assertThat(nullBranches.branches()).isEmpty();

            Branch nullProducts = new Branch(1L, 1L, "Valid", null);
            assertThat(nullProducts.products()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Franchise Management")
    class FranchiseOperations {

        @Test
        @DisplayName("Creates a new franchise when name is valid and unique")
        void shouldCreateFranchiseSuccessfully() {
            Franchise saved = new Franchise(1L, "Franquicia Café", List.of());
            when(franchiseRepository.existsByNameIgnoreCase("Franquicia Café")).thenReturn(Mono.just(false));
            when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.just(saved));

            StepVerifier.create(useCase.createFranchise("  Franquicia Café  ").subscribeOn(Schedulers.parallel()))
                    .assertNext(result -> {
                        assertThat(result.id()).isEqualTo(1L);
                        assertThat(result.name()).isEqualTo("Franquicia Café");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Rejects franchise creation when name already exists")
        void shouldFailWhenFranchiseNameAlreadyExists() {
            when(franchiseRepository.existsByNameIgnoreCase("Franquicia Café")).thenReturn(Mono.just(true));

            StepVerifier.create(useCase.createFranchise("Franquicia Café").subscribeOn(Schedulers.parallel()))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(DuplicateResourceException.class);
                        assertThat(((DuplicateResourceException) error).errorCode())
                                .isEqualTo(ErrorCode.DUPLICATE_FRANCHISE_NAME);
                    })
                    .verify();

            verify(franchiseRepository, never()).save(any());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Rejects blank or null franchise names before touching persistence")
        void shouldRejectBlankFranchiseNames(String invalidName) {
            StepVerifier.create(useCase.createFranchise(invalidName))
                    .expectError(DomainValidationException.class)
                    .verify();

            verifyNoInteractions(franchiseRepository);
        }

        @Test
        @DisplayName("Updates franchise name when franchise exists and new name is unique")
        void shouldUpdateFranchiseNameSuccessfully() {
            Franchise existing = new Franchise(10L, "Nombre Antiguo", List.of());
            Franchise updated = new Franchise(10L, "Nombre Nuevo", List.of());

            when(franchiseRepository.findById(10L)).thenReturn(Mono.just(existing));
            when(franchiseRepository.existsByNameIgnoreCaseAndIdNot("Nombre Nuevo", 10L)).thenReturn(Mono.just(false));
            when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.just(updated));

            StepVerifier.create(useCase.updateFranchiseName(10L, "Nombre Nuevo").subscribeOn(Schedulers.parallel()))
                    .assertNext(result -> assertThat(result.name()).isEqualTo("Nombre Nuevo"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Rejects franchise rename when new name duplicates another franchise")
        void shouldFailUpdateWhenFranchiseNameIsDuplicated() {
            Franchise existing = new Franchise(10L, "Nombre Antiguo", List.of());

            when(franchiseRepository.findById(10L)).thenReturn(Mono.just(existing));
            when(franchiseRepository.existsByNameIgnoreCaseAndIdNot("Nombre Duplicado", 10L))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(useCase.updateFranchiseName(10L, "Nombre Duplicado"))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(DuplicateResourceException.class);
                        assertThat(((DuplicateResourceException) error).errorCode())
                                .isEqualTo(ErrorCode.DUPLICATE_FRANCHISE_NAME);
                    })
                    .verify();

            verify(franchiseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Fails fast before querying database when updating franchise with blank name or non-positive ID")
        void shouldFailFastOnInvalidFranchiseUpdateInputs() {
            StepVerifier.create(useCase.updateFranchiseName(10L, "   "))
                    .expectError(DomainValidationException.class)
                    .verify();

            StepVerifier.create(useCase.updateFranchiseName(0L, "Valido"))
                    .expectError(DomainValidationException.class)
                    .verify();

            StepVerifier.create(useCase.updateFranchiseName(null, "Valido"))
                    .expectError(DomainValidationException.class)
                    .verify();

            verifyNoInteractions(franchiseRepository);
        }

        @Test
        @DisplayName("Fails with ResourceNotFoundException when updating non-existent franchise")
        void shouldFailUpdateWhenFranchiseNotFound() {
            when(franchiseRepository.findById(99L)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.updateFranchiseName(99L, "Nombre Nuevo"))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
                        assertThat(((ResourceNotFoundException) error).errorCode())
                                .isEqualTo(ErrorCode.FRANCHISE_NOT_FOUND);
                    })
                    .verify();
        }

        @Test
        @DisplayName("Assembles full franchise hierarchy reactively in deterministic branch order")
        void shouldAssembleFranchiseHierarchyInOrder() {
            Franchise franchise = new Franchise(1L, "Grupo Norte", List.of());
            Branch branch1 = new Branch(2L, 1L, "Sucursal Centro", List.of());
            Branch branch2 = new Branch(3L, 1L, "Sucursal Sur", List.of());
            Product product1 = new Product(10L, 2L, "Espresso", 45);
            Product product2 = new Product(20L, 3L, "Latte", 30);

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findAllByFranchiseId(1L)).thenReturn(Flux.just(branch1, branch2));
            when(productRepository.findAllByBranchIds(List.of(2L, 3L))).thenReturn(Flux.just(product1, product2));

            StepVerifier.create(useCase.getFranchiseDetail(1L).subscribeOn(Schedulers.parallel()))
                    .assertNext(detail -> {
                        assertThat(detail.branches()).hasSize(2);
                        assertThat(detail.branches().get(0).name()).isEqualTo("Sucursal Centro");
                        assertThat(detail.branches().get(0).products())
                                .extracting(Product::name)
                                .containsExactly("Espresso");
                        assertThat(detail.branches().get(1).name()).isEqualTo("Sucursal Sur");
                        assertThat(detail.branches().get(1).products())
                                .extracting(Product::name)
                                .containsExactly("Latte");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Returns franchise with empty branches list when franchise has no branches")
        void shouldReturnFranchiseWithEmptyBranchesWhenNoneExist() {
            Franchise franchise = new Franchise(1L, "Grupo Norte", List.of());
            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findAllByFranchiseId(1L)).thenReturn(Flux.empty());

            StepVerifier.create(useCase.getFranchiseDetail(1L))
                    .assertNext(detail -> assertThat(detail.branches()).isEmpty())
                    .verifyComplete();

            verifyNoInteractions(productRepository);
        }
    }

    @Nested
    @DisplayName("Branch Management")
    class BranchOperations {

        @Test
        @DisplayName("Adds a new branch to an existing franchise")
        void shouldAddBranchSuccessfully() {
            Franchise existingFranchise = new Franchise(1L, "Franquicia Sur", List.of());
            Branch savedBranch = new Branch(5L, 1L, "Sucursal Poblado", List.of());

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(existingFranchise));
            when(branchRepository.existsByFranchiseIdAndNameIgnoreCase(1L, "Sucursal Poblado"))
                    .thenReturn(Mono.just(false));
            when(branchRepository.save(any(Branch.class))).thenReturn(Mono.just(savedBranch));

            StepVerifier.create(useCase.addBranchToFranchise(1L, "Sucursal Poblado").subscribeOn(Schedulers.parallel()))
                    .assertNext(branch -> {
                        assertThat(branch.id()).isEqualTo(5L);
                        assertThat(branch.franchiseId()).isEqualTo(1L);
                        assertThat(branch.name()).isEqualTo("Sucursal Poblado");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Does not invoke branchRepository when franchise does not exist during branch creation")
        void shouldNotInvokeBranchRepositoryWhenFranchiseNotFoundOnAddBranch() {
            when(franchiseRepository.findById(99L)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.addBranchToFranchise(99L, "Sucursal Poblado"))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
                        assertThat(((ResourceNotFoundException) error).errorCode())
                                .isEqualTo(ErrorCode.FRANCHISE_NOT_FOUND);
                    })
                    .verify();

            verifyNoInteractions(branchRepository);
        }

        @Test
        @DisplayName("Rejects branch creation when branch name already exists in franchise")
        void shouldFailWhenBranchNameAlreadyExists() {
            Franchise existingFranchise = new Franchise(1L, "Franquicia Sur", List.of());
            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(existingFranchise));
            when(branchRepository.existsByFranchiseIdAndNameIgnoreCase(1L, "Sucursal Poblado"))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(useCase.addBranchToFranchise(1L, "Sucursal Poblado"))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(DuplicateResourceException.class);
                        assertThat(((DuplicateResourceException) error).errorCode())
                                .isEqualTo(ErrorCode.DUPLICATE_BRANCH_NAME);
                    })
                    .verify();

            verify(branchRepository, never()).save(any());
        }

        @Test
        @DisplayName("Updates branch name when branch belongs to franchise and name is unique")
        void shouldUpdateBranchNameSuccessfully() {
            Franchise franchise = new Franchise(1L, "Franquicia Sur", List.of());
            Branch existingBranch = new Branch(5L, 1L, "Sucursal Vieja", List.of());
            Branch updatedBranch = new Branch(5L, 1L, "Sucursal Renovada", List.of());

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findByIdAndFranchiseId(5L, 1L)).thenReturn(Mono.just(existingBranch));
            when(branchRepository.existsByFranchiseIdAndNameIgnoreCaseAndIdNot(1L, "Sucursal Renovada", 5L))
                    .thenReturn(Mono.just(false));
            when(branchRepository.save(any(Branch.class))).thenReturn(Mono.just(updatedBranch));

            StepVerifier.create(useCase.updateBranchName(1L, 5L, "Sucursal Renovada").subscribeOn(Schedulers.parallel()))
                    .assertNext(result -> assertThat(result.name()).isEqualTo("Sucursal Renovada"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Rejects branch rename when new name duplicates another branch in same franchise")
        void shouldFailUpdateWhenBranchNameIsDuplicated() {
            Franchise franchise = new Franchise(1L, "Franquicia Sur", List.of());
            Branch existingBranch = new Branch(5L, 1L, "Sucursal Vieja", List.of());

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findByIdAndFranchiseId(5L, 1L)).thenReturn(Mono.just(existingBranch));
            when(branchRepository.existsByFranchiseIdAndNameIgnoreCaseAndIdNot(1L, "Sucursal Duplicada", 5L))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(useCase.updateBranchName(1L, 5L, "Sucursal Duplicada"))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(DuplicateResourceException.class);
                        assertThat(((DuplicateResourceException) error).errorCode())
                                .isEqualTo(ErrorCode.DUPLICATE_BRANCH_NAME);
                    })
                    .verify();

            verify(branchRepository, never()).save(any());
        }

        @Test
        @DisplayName("Fails with BRANCH_NOT_FOUND when branch does not belong to franchise")
        void shouldFailWhenBranchNotFoundInFranchise() {
            Franchise franchise = new Franchise(1L, "Franquicia Sur", List.of());
            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findByIdAndFranchiseId(99L, 1L)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.updateBranchName(1L, 99L, "Sucursal Renovada"))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
                        assertThat(((ResourceNotFoundException) error).errorCode())
                                .isEqualTo(ErrorCode.BRANCH_NOT_FOUND);
                    })
                    .verify();
        }
    }

    @Nested
    @DisplayName("Product & Stock Management")
    class ProductOperations {

        @Test
        @DisplayName("Adds a new product with valid stock to a branch")
        void shouldAddProductSuccessfully() {
            Franchise franchise = new Franchise(1L, "Franquicia Sur", List.of());
            Branch branch = new Branch(5L, 1L, "Sucursal Centro", List.of());
            Product savedProduct = new Product(100L, 5L, "Capuccino", 80);

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findByIdAndFranchiseId(5L, 1L)).thenReturn(Mono.just(branch));
            when(productRepository.existsByBranchIdAndNameIgnoreCase(5L, "Capuccino")).thenReturn(Mono.just(false));
            when(productRepository.save(any(Product.class))).thenReturn(Mono.just(savedProduct));

            StepVerifier.create(useCase.addProductToBranch(1L, 5L, "Capuccino", 80).subscribeOn(Schedulers.parallel()))
                    .assertNext(product -> {
                        assertThat(product.id()).isEqualTo(100L);
                        assertThat(product.stock()).isEqualTo(80);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Rejects product creation when product name already exists in branch")
        void shouldFailWhenProductNameAlreadyExistsInBranch() {
            Franchise franchise = new Franchise(1L, "Franquicia Sur", List.of());
            Branch branch = new Branch(5L, 1L, "Sucursal Centro", List.of());

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findByIdAndFranchiseId(5L, 1L)).thenReturn(Mono.just(branch));
            when(productRepository.existsByBranchIdAndNameIgnoreCase(5L, "Capuccino")).thenReturn(Mono.just(true));

            StepVerifier.create(useCase.addProductToBranch(1L, 5L, "Capuccino", 80))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(DuplicateResourceException.class);
                        assertThat(((DuplicateResourceException) error).errorCode())
                                .isEqualTo(ErrorCode.DUPLICATE_PRODUCT_NAME);
                    })
                    .verify();

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects product creation when stock is negative before querying database")
        void shouldRejectNegativeStock() {
            StepVerifier.create(useCase.addProductToBranch(1L, 5L, "Capuccino", -5))
                    .expectError(DomainValidationException.class)
                    .verify();

            verifyNoInteractions(franchiseRepository, branchRepository, productRepository);
        }

        @Test
        @DisplayName("Modifies product stock when hierarchy is valid")
        void shouldUpdateProductStockSuccessfully() {
            Franchise franchise = new Franchise(1L, "Franquicia Sur", List.of());
            Branch branch = new Branch(5L, 1L, "Sucursal Centro", List.of());
            Product existing = new Product(100L, 5L, "Capuccino", 80);
            Product updated = new Product(100L, 5L, "Capuccino", 250);

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findByIdAndFranchiseId(5L, 1L)).thenReturn(Mono.just(branch));
            when(productRepository.findByIdAndBranchId(100L, 5L)).thenReturn(Mono.just(existing));
            when(productRepository.save(any(Product.class))).thenReturn(Mono.just(updated));

            StepVerifier.create(useCase.updateProductStock(1L, 5L, 100L, 250).subscribeOn(Schedulers.parallel()))
                    .assertNext(result -> assertThat(result.stock()).isEqualTo(250))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Fails fast without querying database when updating stock with negative value")
        void shouldFailFastWhenUpdatingWithNegativeStock() {
            StepVerifier.create(useCase.updateProductStock(1L, 5L, 100L, -1))
                    .expectError(DomainValidationException.class)
                    .verify();

            verifyNoInteractions(franchiseRepository, branchRepository, productRepository);
        }

        @Test
        @DisplayName("Updates product name when new name is unique within branch")
        void shouldUpdateProductNameSuccessfully() {
            Franchise franchise = new Franchise(1L, "Franquicia Sur", List.of());
            Branch branch = new Branch(5L, 1L, "Sucursal Centro", List.of());
            Product existing = new Product(100L, 5L, "Capuccino", 80);
            Product updated = new Product(100L, 5L, "Flat White", 80);

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findByIdAndFranchiseId(5L, 1L)).thenReturn(Mono.just(branch));
            when(productRepository.findByIdAndBranchId(100L, 5L)).thenReturn(Mono.just(existing));
            when(productRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(5L, "Flat White", 100L))
                    .thenReturn(Mono.just(false));
            when(productRepository.save(any(Product.class))).thenReturn(Mono.just(updated));

            StepVerifier.create(useCase.updateProductName(1L, 5L, 100L, "Flat White").subscribeOn(Schedulers.parallel()))
                    .assertNext(result -> assertThat(result.name()).isEqualTo("Flat White"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Rejects product rename when new name duplicates another product in same branch")
        void shouldFailUpdateWhenProductNameIsDuplicated() {
            Franchise franchise = new Franchise(1L, "Franquicia Sur", List.of());
            Branch branch = new Branch(5L, 1L, "Sucursal Centro", List.of());
            Product existing = new Product(100L, 5L, "Capuccino", 80);

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findByIdAndFranchiseId(5L, 1L)).thenReturn(Mono.just(branch));
            when(productRepository.findByIdAndBranchId(100L, 5L)).thenReturn(Mono.just(existing));
            when(productRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(5L, "Expreso Doble", 100L))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(useCase.updateProductName(1L, 5L, 100L, "Expreso Doble"))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(DuplicateResourceException.class);
                        assertThat(((DuplicateResourceException) error).errorCode())
                                .isEqualTo(ErrorCode.DUPLICATE_PRODUCT_NAME);
                    })
                    .verify();

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Fails with PRODUCT_NOT_FOUND when product does not belong to branch")
        void shouldFailWhenProductNotFoundInBranch() {
            Franchise franchise = new Franchise(1L, "Franquicia Sur", List.of());
            Branch branch = new Branch(5L, 1L, "Sucursal Centro", List.of());

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findByIdAndFranchiseId(5L, 1L)).thenReturn(Mono.just(branch));
            when(productRepository.findByIdAndBranchId(999L, 5L)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.removeProductFromBranch(1L, 5L, 999L))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
                        assertThat(((ResourceNotFoundException) error).errorCode())
                                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
                    })
                    .verify();
        }

        @Test
        @DisplayName("Deletes product from branch when hierarchy matches")
        void shouldDeleteProductSuccessfully() {
            Franchise franchise = new Franchise(1L, "Franquicia Sur", List.of());
            Branch branch = new Branch(5L, 1L, "Sucursal Centro", List.of());
            Product existing = new Product(100L, 5L, "Capuccino", 80);

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findByIdAndFranchiseId(5L, 1L)).thenReturn(Mono.just(branch));
            when(productRepository.findByIdAndBranchId(100L, 5L)).thenReturn(Mono.just(existing));
            when(productRepository.deleteById(100L)).thenReturn(Mono.empty());

            StepVerifier.create(useCase.removeProductFromBranch(1L, 5L, 100L).subscribeOn(Schedulers.parallel()))
                    .verifyComplete();

            verify(productRepository).deleteById(100L);
        }

        @Test
        @DisplayName("Returns highest stock product per branch for a specific franchise")
        void shouldReturnTopStockProductPerBranch() {
            Franchise franchise = new Franchise(1L, "Franquicia Sur", List.of());
            BranchTopProduct topBranch1 = new BranchTopProduct(
                    10L, "Sucursal Norte", new Product(101L, 10L, "Cold Brew", 320));
            BranchTopProduct topBranch2 = new BranchTopProduct(
                    20L, "Sucursal Sur", new Product(205L, 20L, "Mocha", 190));

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(productRepository.findTopStockProductPerBranchByFranchiseId(1L))
                    .thenReturn(Flux.just(topBranch1, topBranch2));

            StepVerifier.create(useCase.getTopStockProductsByFranchise(1L).subscribeOn(Schedulers.parallel()))
                    .expectNext(topBranch1)
                    .expectNext(topBranch2)
                    .verifyComplete();
        }
    }
}
