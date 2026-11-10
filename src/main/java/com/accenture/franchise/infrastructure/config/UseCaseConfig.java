package com.accenture.franchise.infrastructure.config;

import com.accenture.franchise.domain.model.gateway.BranchRepositoryPort;
import com.accenture.franchise.domain.model.gateway.FranchiseRepositoryPort;
import com.accenture.franchise.domain.model.gateway.ProductRepositoryPort;
import com.accenture.franchise.domain.usecase.FranchiseUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public FranchiseUseCase franchiseUseCase(
            FranchiseRepositoryPort franchiseRepositoryPort,
            BranchRepositoryPort branchRepositoryPort,
            ProductRepositoryPort productRepositoryPort
    ) {
        return new FranchiseUseCase(franchiseRepositoryPort, branchRepositoryPort, productRepositoryPort);
    }
}
