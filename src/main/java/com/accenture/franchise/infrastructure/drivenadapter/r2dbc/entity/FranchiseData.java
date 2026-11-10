package com.accenture.franchise.infrastructure.drivenadapter.r2dbc.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("franchises")
public class FranchiseData {

    @Id
    @Column("id")
    private Long id;

    @Column("name")
    private String name;

    public FranchiseData() {
    }

    public FranchiseData(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
