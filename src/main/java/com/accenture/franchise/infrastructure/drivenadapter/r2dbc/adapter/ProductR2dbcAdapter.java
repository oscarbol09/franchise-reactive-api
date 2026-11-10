package com.accenture.franchise.infrastructure.drivenadapter.r2dbc.adapter;

import com.accenture.franchise.domain.model.BranchTopProduct;
import com.accenture.franchise.domain.model.Product;
import com.accenture.franchise.domain.model.gateway.ProductRepositoryPort;
import com.accenture.franchise.infrastructure.drivenadapter.r2dbc.entity.ProductData;
import com.accenture.franchise.infrastructure.drivenadapter.r2dbc.repository.ProductReactiveRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class ProductR2dbcAdapter implements ProductRepositoryPort {

    private static final String TOP_STOCK_BY_FRANCHISE_SQL = """
            SELECT ranked.branch_id,
                   ranked.branch_name,
                   ranked.product_id,
                   ranked.product_name,
                   ranked.stock
            FROM (
                SELECT b.id AS branch_id,
                       b.name AS branch_name,
                       p.id AS product_id,
                       p.name AS product_name,
                       p.stock AS stock,
                       ROW_NUMBER() OVER (PARTITION BY b.id ORDER BY p.stock DESC, p.id ASC) AS rn
                FROM branches b
                INNER JOIN products p ON p.branch_id = b.id
                WHERE b.franchise_id = :franchiseId
            ) ranked
            WHERE ranked.rn = 1
            ORDER BY ranked.branch_id ASC
            """;

    private final ProductReactiveRepository repository;
    private final DatabaseClient databaseClient;

    public ProductR2dbcAdapter(ProductReactiveRepository repository, DatabaseClient databaseClient) {
        this.repository = repository;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Product> save(Product product) {
        ProductData entity = new ProductData(product.id(), product.branchId(), product.name(), product.stock());
        return repository.save(entity)
                .map(saved -> new Product(saved.getId(), saved.getBranchId(), saved.getName(), saved.getStock()));
    }

    @Override
    public Mono<Product> findByIdAndBranchId(Long id, Long branchId) {
        return repository.findByIdAndBranchId(id, branchId)
                .map(data -> new Product(data.getId(), data.getBranchId(), data.getName(), data.getStock()));
    }

    @Override
    public Flux<Product> findAllByBranchId(Long branchId) {
        return repository.findAllByBranchIdOrderByIdAsc(branchId)
                .map(data -> new Product(data.getId(), data.getBranchId(), data.getName(), data.getStock()));
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<Boolean> existsByBranchIdAndNameIgnoreCase(Long branchId, String name) {
        return repository.existsByBranchIdAndNameIgnoreCase(branchId, name);
    }

    @Override
    public Mono<Boolean> existsByBranchIdAndNameIgnoreCaseAndIdNot(Long branchId, String name, Long excludedId) {
        return repository.existsByBranchIdAndNameIgnoreCaseAndIdNot(branchId, name, excludedId);
    }

    @Override
    public Flux<BranchTopProduct> findTopStockProductPerBranchByFranchiseId(Long franchiseId) {
        return databaseClient.sql(TOP_STOCK_BY_FRANCHISE_SQL)
                .bind("franchiseId", franchiseId)
                .map((row, metadata) -> {
                    Long branchId = row.get("branch_id", Long.class);
                    String branchName = row.get("branch_name", String.class);
                    Long productId = row.get("product_id", Long.class);
                    String productName = row.get("product_name", String.class);
                    Integer stock = row.get("stock", Integer.class);
                    return new BranchTopProduct(
                            branchId,
                            branchName,
                            new Product(productId, branchId, productName, stock != null ? stock : 0)
                    );
                })
                .all();
    }
}
