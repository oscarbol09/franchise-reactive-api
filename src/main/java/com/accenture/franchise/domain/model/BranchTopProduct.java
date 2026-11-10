package com.accenture.franchise.domain.model;

import java.util.Objects;

public record BranchTopProduct(
        Long branchId,
        String branchName,
        Product product
) {
    public BranchTopProduct {
        Objects.requireNonNull(branchId, "branchId must not be null");
        Objects.requireNonNull(branchName, "branchName must not be null");
        Objects.requireNonNull(product, "product must not be null");
    }
}
