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
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        BlockHound.install();
    }

    @BeforeEach
    void setUp() {
        useCase = new FranchiseUseCase(franchiseRepository, branchRepository, productRepository);
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

            StepVerifier.create(useCase.createFranchise("  Franquicia Café  "))
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

            StepVerifier.create(useCase.createFranchise("Franquicia Café"))
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

            verify(franchiseRepository, never()).existsByNameIgnoreCase(any());
        }

        @Test
        @DisplayName("Updates franchise name when franchise exists and new name is unique")
        void shouldUpdateFranchiseNameSuccessfully() {
            Franchise existing = new Franchise(10L, "Nombre Antiguo", List.of());
            Franchise updated = new Franchise(10L, "Nombre Nuevo", List.of());

            when(franchiseRepository.findById(10L)).thenReturn(Mono.just(existing));
            when(franchiseRepository.existsByNameIgnoreCaseAndIdNot("Nombre Nuevo", 10L)).thenReturn(Mono.just(false));
            when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.just(updated));

            StepVerifier.create(useCase.updateFranchiseName(10L, "Nombre Nuevo"))
                    .assertNext(result -> assertThat(result.name()).isEqualTo("Nombre Nuevo"))
                    .verifyComplete();
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
        @DisplayName("Assembles full franchise hierarchy reactively")
        void shouldAssembleFranchiseHierarchy() {
            Franchise franchise = new Franchise(1L, "Grupo Norte", List.of());
            Branch branch = new Branch(2L, 1L, "Sucursal Centro", List.of());
            Product product = new Product(3L, 2L, "Espresso", 45);

            when(franchiseRepository.findById(1L)).thenReturn(Mono.just(franchise));
            when(branchRepository.findAllByFranchiseId(1L)).thenReturn(Flux.just(branch));
            when(productRepository.findAllByBranchId(2L)).thenReturn(Flux.just(product));

            StepVerifier.create(useCase.getFranchiseDetail(1L))
                    .assertNext(detail -> {
                        assertThat(detail.branches()).hasSize(1);
                        assertThat(detail.branches().getFirst().name()).isEqualTo("Sucursal Centro");
                        assertThat(detail.branches().getFirst().products())
                                .extracting(Product::name)
                                .containsExactly("Espresso");
                    })
                    .verifyComplete();
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

            StepVerifier.create(useCase.addBranchToFranchise(1L, "Sucursal Poblado"))
                    .assertNext(branch -> {
                        assertThat(branch.id()).isEqualTo(5L);
                        assertThat(branch.franchiseId()).isEqualTo(1L);
                        assertThat(branch.name()).isEqualTo("Sucursal Poblado");
                    })
                    .verifyComplete();
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

            StepVerifier.create(useCase.updateBranchName(1L, 5L, "Sucursal Renovada"))
                    .assertNext(result -> assertThat(result.name()).isEqualTo("Sucursal Renovada"))
                    .verifyComplete();
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

            StepVerifier.create(useCase.addProductToBranch(1L, 5L, "Capuccino", 80))
                    .assertNext(product -> {
                        assertThat(product.id()).isEqualTo(100L);
                        assertThat(product.stock()).isEqualTo(80);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Rejects product creation when stock is negative")
        void shouldRejectNegativeStock() {
            StepVerifier.create(useCase.addProductToBranch(1L, 5L, "Capuccino", -5))
                    .expectError(DomainValidationException.class)
                    .verify();

            verify(productRepository, never()).save(any());
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

            StepVerifier.create(useCase.updateProductStock(1L, 5L, 100L, 250))
                    .assertNext(result -> assertThat(result.stock()).isEqualTo(250))
                    .verifyComplete();
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

            StepVerifier.create(useCase.updateProductName(1L, 5L, 100L, "Flat White"))
                    .assertNext(result -> assertThat(result.name()).isEqualTo("Flat White"))
                    .verifyComplete();
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

            StepVerifier.create(useCase.removeProductFromBranch(1L, 5L, 100L))
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

            StepVerifier.create(useCase.getTopStockProductsByFranchise(1L))
                    .expectNext(topBranch1)
                    .expectNext(topBranch2)
                    .verifyComplete();
        }
    }
}
