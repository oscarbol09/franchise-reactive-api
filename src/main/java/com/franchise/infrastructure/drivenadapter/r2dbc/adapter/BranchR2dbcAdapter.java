package com.franchise.infrastructure.drivenadapter.r2dbc.adapter;

import com.franchise.domain.exception.DuplicateResourceException;
import com.franchise.domain.exception.ErrorCode;
import com.franchise.domain.model.Branch;
import com.franchise.domain.model.gateway.BranchRepositoryPort;
import com.franchise.infrastructure.drivenadapter.r2dbc.entity.BranchData;
import com.franchise.infrastructure.drivenadapter.r2dbc.repository.BranchReactiveRepository;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class BranchR2dbcAdapter implements BranchRepositoryPort {

    private final BranchReactiveRepository repository;

    public BranchR2dbcAdapter(BranchReactiveRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Branch> save(Branch branch) {
        BranchData entity = new BranchData(branch.id(), branch.franchiseId(), branch.name());
        return repository.save(entity)
                .map(saved -> new Branch(saved.getId(), saved.getFranchiseId(), saved.getName(), branch.products()))
                .onErrorMap(DuplicateKeyException.class, ex -> new DuplicateResourceException(
                        ErrorCode.DUPLICATE_BRANCH_NAME,
                        "Branch '" + branch.name() + "' already exists in franchise " + branch.franchiseId()
                ));
    }

    @Override
    public Mono<Branch> findByIdAndFranchiseId(Long id, Long franchiseId) {
        return repository.findByIdAndFranchiseId(id, franchiseId)
                .map(data -> new Branch(data.getId(), data.getFranchiseId(), data.getName(), List.of()));
    }

    @Override
    public Flux<Branch> findAllByFranchiseId(Long franchiseId) {
        return repository.findAllByFranchiseIdOrderByIdAsc(franchiseId)
                .map(data -> new Branch(data.getId(), data.getFranchiseId(), data.getName(), List.of()));
    }

    @Override
    public Mono<Boolean> existsByFranchiseIdAndNameIgnoreCase(Long franchiseId, String name) {
        return repository.existsByFranchiseIdAndNameIgnoreCase(franchiseId, name)
                .map(flag -> flag > 0L);
    }

    @Override
    public Mono<Boolean> existsByFranchiseIdAndNameIgnoreCaseAndIdNot(Long franchiseId, String name, Long excludedId) {
        return repository.existsByFranchiseIdAndNameIgnoreCaseAndIdNot(franchiseId, name, excludedId)
                .map(flag -> flag > 0L);
    }
}
