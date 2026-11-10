package com.franchise.infrastructure.drivenadapter.r2dbc.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("branches")
public class BranchData {

    @Id
    @Column("id")
    private Long id;

    @Column("franchise_id")
    private Long franchiseId;

    @Column("name")
    private String name;

    public BranchData() {
    }

    public BranchData(Long id, Long franchiseId, String name) {
        this.id = id;
        this.franchiseId = franchiseId;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFranchiseId() {
        return franchiseId;
    }

    public void setFranchiseId(Long franchiseId) {
        this.franchiseId = franchiseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
