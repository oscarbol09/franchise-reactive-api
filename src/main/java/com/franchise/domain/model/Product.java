package com.franchise.domain.model;

import com.franchise.domain.exception.DomainValidationException;

public record Product(
        Long id,
        Long branchId,
        String name,
        int stock
) {
    public Product {
        if (branchId == null || branchId <= 0) {
            throw new DomainValidationException("Branch identifier must be a positive number");
        }
        name = normalizeName(name);
        if (stock < 0) {
            throw new DomainValidationException("Product stock cannot be negative: " + stock);
        }
    }

    public static Product createNew(Long branchId, String name, int stock) {
        return new Product(null, branchId, name, stock);
    }

    public Product withUpdatedStock(int newStock) {
        return new Product(this.id, this.branchId, this.name, newStock);
    }

    public Product withUpdatedName(String newName) {
        return new Product(this.id, this.branchId, newName, this.stock);
    }

    private static String normalizeName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new DomainValidationException("Product name must not be blank");
        }
        String trimmed = rawName.trim();
        if (trimmed.length() > 120) {
            throw new DomainValidationException("Product name must not exceed 120 characters");
        }
        return trimmed;
    }
}
