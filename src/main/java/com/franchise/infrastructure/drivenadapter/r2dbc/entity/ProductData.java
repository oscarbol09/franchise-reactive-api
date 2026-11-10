package com.franchise.infrastructure.drivenadapter.r2dbc.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("products")
public class ProductData {

    @Id
    @Column("id")
    private Long id;

    @Column("branch_id")
    private Long branchId;

    @Column("name")
    private String name;

    @Column("stock")
    private Integer stock;

    public ProductData(Long id, Long branchId, String name, Integer stock) {
        this.id = id;
        this.branchId = branchId;
        this.name = name;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public Long getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public Integer getStock() {
        return stock;
    }
}
