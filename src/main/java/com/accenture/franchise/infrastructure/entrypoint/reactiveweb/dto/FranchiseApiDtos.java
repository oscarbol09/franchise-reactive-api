package com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto;

import com.accenture.franchise.domain.model.Branch;
import com.accenture.franchise.domain.model.BranchTopProduct;
import com.accenture.franchise.domain.model.Franchise;
import com.accenture.franchise.domain.model.Product;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class FranchiseApiDtos {

    private FranchiseApiDtos() {
    }

    public record NamePayload(
            @NotBlank(message = "name must not be blank")
            @Size(max = 120, message = "name must not exceed 120 characters")
            String name
    ) {
    }

    public record CreateProductPayload(
            @NotBlank(message = "name must not be blank")
            @Size(max = 120, message = "name must not exceed 120 characters")
            String name,
            @NotNull(message = "stock is required")
            @Min(value = 0, message = "stock must be greater than or equal to 0")
            Integer stock
    ) {
    }

    public record UpdateStockPayload(
            @NotNull(message = "stock is required")
            @Min(value = 0, message = "stock must be greater than or equal to 0")
            Integer stock
    ) {
    }

    public record ProductResponse(
            Long id,
            Long branchId,
            String name,
            int stock
    ) {
        public static ProductResponse fromDomain(Product product) {
            return new ProductResponse(
                    product.id(),
                    product.branchId(),
                    product.name(),
                    product.stock()
            );
        }
    }

    public record BranchResponse(
            Long id,
            Long franchiseId,
            String name,
            List<ProductResponse> products
    ) {
        public static BranchResponse fromDomain(Branch branch) {
            List<ProductResponse> productItems = branch.products()
                    .stream()
                    .map(ProductResponse::fromDomain)
                    .toList();
            return new BranchResponse(
                    branch.id(),
                    branch.franchiseId(),
                    branch.name(),
                    productItems
            );
        }
    }

    public record FranchiseResponse(
            Long id,
            String name,
            List<BranchResponse> branches
    ) {
        public static FranchiseResponse fromDomain(Franchise franchise) {
            List<BranchResponse> branchItems = franchise.branches()
                    .stream()
                    .map(BranchResponse::fromDomain)
                    .toList();
            return new FranchiseResponse(
                    franchise.id(),
                    franchise.name(),
                    branchItems
            );
        }
    }

    public record BranchTopProductResponse(
            Long branchId,
            String branchName,
            Long productId,
            String productName,
            int stock
    ) {
        public static BranchTopProductResponse fromDomain(BranchTopProduct item) {
            return new BranchTopProductResponse(
                    item.branchId(),
                    item.branchName(),
                    item.product().id(),
                    item.product().name(),
                    item.product().stock()
            );
        }
    }
}
