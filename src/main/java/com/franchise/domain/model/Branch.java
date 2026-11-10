package com.franchise.domain.model;

import com.franchise.domain.exception.DomainValidationException;
import java.util.List;

public record Branch(
        Long id,
        Long franchiseId,
        String name,
        List<Product> products
) {
    public Branch {
        name = normalizeName(name);
        products = products != null ? List.copyOf(products) : List.of();
    }

    public static Branch createNew(Long franchiseId, String name) {
        if (franchiseId == null || franchiseId <= 0) {
            throw new DomainValidationException("Franchise identifier must be a positive number");
        }
        return new Branch(null, franchiseId, name, List.of());
    }

    public Branch withUpdatedName(String newName) {
        return new Branch(this.id, this.franchiseId, newName, this.products);
    }

    public Branch withProducts(List<Product> branchProducts) {
        return new Branch(this.id, this.franchiseId, this.name, branchProducts);
    }

    private static String normalizeName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new DomainValidationException("Branch name must not be blank");
        }
        String trimmed = rawName.trim();
        if (trimmed.length() > 120) {
            throw new DomainValidationException("Branch name must not exceed 120 characters");
        }
        return trimmed;
    }
}
