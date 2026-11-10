package com.franchise.infrastructure.drivenadapter.r2dbc.adapter;

import com.franchise.domain.exception.DuplicateResourceException;
import com.franchise.domain.exception.ErrorCode;
import com.franchise.domain.model.Branch;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.Product;
import com.franchise.infrastructure.drivenadapter.r2dbc.entity.ProductData;
import com.franchise.infrastructure.drivenadapter.r2dbc.repository.BranchReactiveRepository;
import com.franchise.infrastructure.drivenadapter.r2dbc.repository.FranchiseReactiveRepository;
import com.franchise.infrastructure.drivenadapter.r2dbc.repository.ProductReactiveRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class R2dbcAdaptersTest {

    @Mock
    private FranchiseReactiveRepository franchiseRepository;

    @Mock
    private BranchReactiveRepository branchRepository;

    @Mock
    private ProductReactiveRepository productRepository;

    @Mock
    private DatabaseClient databaseClient;

    @Test
    @DisplayName("Maps DuplicateKeyException to DuplicateResourceException across all R2DBC adapters")
    void shouldMapDuplicateKeyExceptionsToDomainExceptions() {
        FranchiseR2dbcAdapter franchiseAdapter = new FranchiseR2dbcAdapter(franchiseRepository);
        BranchR2dbcAdapter branchAdapter = new BranchR2dbcAdapter(branchRepository);
        ProductR2dbcAdapter productAdapter = new ProductR2dbcAdapter(productRepository, databaseClient);

        when(franchiseRepository.save(any())).thenReturn(Mono.error(new DuplicateKeyException("uk_franchises_name")));
        when(branchRepository.save(any())).thenReturn(Mono.error(new DuplicateKeyException("uk_branches_franchise_name")));
        when(productRepository.save(any())).thenReturn(Mono.error(new DuplicateKeyException("uk_products_branch_name")));

        StepVerifier.create(franchiseAdapter.save(Franchise.createNew("Franquicia Norte")))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DuplicateResourceException.class);
                    assertThat(((DuplicateResourceException) error).errorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_FRANCHISE_NAME);
                })
                .verify();

        StepVerifier.create(branchAdapter.save(Branch.createNew(1L, "Sucursal Centro")))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DuplicateResourceException.class);
                    assertThat(((DuplicateResourceException) error).errorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_BRANCH_NAME);
                })
                .verify();

        StepVerifier.create(productAdapter.save(Product.createNew(10L, "Café Especial", 25)))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DuplicateResourceException.class);
                    assertThat(((DuplicateResourceException) error).errorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_PRODUCT_NAME);
                })
                .verify();
    }

    @Test
    @DisplayName("Handles empty branchIds list and single-branch product lookups in ProductR2dbcAdapter")
    void shouldHandleEmptyBranchIdsAndSingleBranchQuery() {
        ProductR2dbcAdapter productAdapter = new ProductR2dbcAdapter(productRepository, databaseClient);

        StepVerifier.create(productAdapter.findAllByBranchIds(List.of()))
                .verifyComplete();

        StepVerifier.create(productAdapter.findAllByBranchIds(null))
                .verifyComplete();

        verifyNoInteractions(productRepository);

        when(productRepository.findAllByBranchIdOrderByIdAsc(10L))
                .thenReturn(Flux.just(new ProductData(1L, 10L, "Expreso", 50)));

        StepVerifier.create(productAdapter.findAllByBranchId(10L))
                .assertNext(product -> {
                    assertThat(product.id()).isEqualTo(1L);
                    assertThat(product.branchId()).isEqualTo(10L);
                    assertThat(product.name()).isEqualTo("Expreso");
                    assertThat(product.stock()).isEqualTo(50);
                })
                .verifyComplete();
    }
}
