CREATE TABLE IF NOT EXISTS franchises (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_franchises_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS branches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    franchise_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_branches_franchise FOREIGN KEY (franchise_id) REFERENCES franchises (id) ON DELETE CASCADE,
    CONSTRAINT uk_branches_franchise_name UNIQUE (franchise_id, name)
);

CREATE INDEX idx_branches_franchise_id ON branches (franchise_id);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    stock INT NOT NULL CHECK (stock >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE,
    CONSTRAINT uk_products_branch_name UNIQUE (branch_id, name)
);

CREATE INDEX idx_products_branch_stock ON products (branch_id, stock DESC);
