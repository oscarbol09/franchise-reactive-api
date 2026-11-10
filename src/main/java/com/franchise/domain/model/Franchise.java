package com.franchise.domain.model;

import com.franchise.domain.exception.DomainValidationException;
import java.util.List;

public record Franchise(
        Long id,
        String name,
        List<Branch> branches
) {
    public Franchise {
        name = normalizeName(name);
        branches = branches != null ? List.copyOf(branches) : List.of();
    }

    public static Franchise createNew(String name) {
        return new Franchise(null, name, List.of());
    }

    public Franchise withUpdatedName(String newName) {
        return new Franchise(this.id, newName, this.branches);
    }

    public Franchise withBranches(List<Branch> franchiseBranches) {
        return new Franchise(this.id, this.name, franchiseBranches);
    }

    private static String normalizeName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new DomainValidationException("Franchise name must not be blank");
        }
        String trimmed = rawName.trim();
        if (trimmed.length() > 120) {
            throw new DomainValidationException("Franchise name must not exceed 120 characters");
        }
        return trimmed;
    }
}
