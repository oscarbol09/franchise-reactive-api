package com.franchise.infrastructure.drivenadapter.r2dbc.adapter;

import com.franchise.domain.exception.DuplicateResourceException;
import com.franchise.domain.exception.ErrorCode;
import com.franchise.domain.model.Franchise;
import com.franchise.domain.model.gateway.FranchiseRepositoryPort;
import com.franchise.infrastructure.drivenadapter.r2dbc.entity.FranchiseData;
import com.franchise.infrastructure.drivenadapter.r2dbc.repository.FranchiseReactiveRepository;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class FranchiseR2dbcAdapter implements FranchiseRepositoryPort {

    private final FranchiseReactiveRepository repository;

    public FranchiseR2dbcAdapter(FranchiseReactiveRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        FranchiseData entity = new FranchiseData(franchise.id(), franchise.name());
        return repository.save(entity)
                .map(saved -> new Franchise(saved.getId(), saved.getName(), franchise.branches()))
                .onErrorMap(DuplicateKeyException.class, ex -> new DuplicateResourceException(
                        ErrorCode.DUPLICATE_FRANCHISE_NAME,
                        "Franchise with name '" + franchise.name() + "' already exists"
                ));
    }

    @Override
    public Mono<Franchise> findById(Long id) {
        return repository.findById(id)
                .map(data -> new Franchise(data.getId(), data.getName(), List.of()));
    }

    @Override
    public Mono<Boolean> existsByNameIgnoreCase(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public Mono<Boolean> existsByNameIgnoreCaseAndIdNot(String name, Long excludedId) {
        return repository.existsByNameIgnoreCaseAndIdNot(name, excludedId);
    }
}
