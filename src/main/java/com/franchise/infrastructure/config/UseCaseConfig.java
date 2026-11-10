package com.franchise.infrastructure.config;

import com.franchise.domain.model.gateway.BranchRepositoryPort;
import com.franchise.domain.model.gateway.FranchiseRepositoryPort;
import com.franchise.domain.model.gateway.ProductRepositoryPort;
import com.franchise.domain.model.gateway.TransactionalPort;
import com.franchise.domain.usecase.FranchiseUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public FranchiseUseCase franchiseUseCase(
            FranchiseRepositoryPort franchiseRepositoryPort,
            BranchRepositoryPort branchRepositoryPort,
            ProductRepositoryPort productRepositoryPort,
            TransactionalPort transactionalPort
    ) {
        return new FranchiseUseCase(
                franchiseRepositoryPort,
                branchRepositoryPort,
                productRepositoryPort,
                transactionalPort
        );
    }
}
