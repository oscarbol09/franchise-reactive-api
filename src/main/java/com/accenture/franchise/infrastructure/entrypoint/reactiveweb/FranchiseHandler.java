package com.accenture.franchise.infrastructure.entrypoint.reactiveweb;

import com.accenture.franchise.domain.usecase.FranchiseUseCase;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.BranchResponse;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.BranchTopProductResponse;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.CreateProductPayload;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.FranchiseResponse;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.NamePayload;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.ProductResponse;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.UpdateStockPayload;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.validation.RequestValidationHandler;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class FranchiseHandler {

    private static final String FRANCHISE_ID = "franchiseId";
    private static final String BRANCH_ID = "branchId";
    private static final String PRODUCT_ID = "productId";

    private final FranchiseUseCase franchiseUseCase;
    private final RequestValidationHandler validationHandler;

    public FranchiseHandler(FranchiseUseCase franchiseUseCase, RequestValidationHandler validationHandler) {
        this.franchiseUseCase = franchiseUseCase;
        this.validationHandler = validationHandler;
    }

    public Mono<ServerResponse> createFranchise(ServerRequest request) {
        return validationHandler.requireValidBody(request, NamePayload.class)
                .flatMap(payload -> franchiseUseCase.createFranchise(payload.name()))
                .map(FranchiseResponse::fromDomain)
                .flatMap(response -> ServerResponse
                        .created(URI.create("/api/v1/franchises/" + response.id()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> getFranchiseDetail(ServerRequest request) {
        return validationHandler.requirePositivePathVariable(request, FRANCHISE_ID)
                .flatMap(franchiseUseCase::getFranchiseDetail)
                .map(FranchiseResponse::fromDomain)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> updateFranchiseName(ServerRequest request) {
        return validationHandler.requirePositivePathVariable(request, FRANCHISE_ID)
                .zipWith(validationHandler.requireValidBody(request, NamePayload.class))
                .flatMap(tuple -> franchiseUseCase.updateFranchiseName(tuple.getT1(), tuple.getT2().name()))
                .map(FranchiseResponse::fromDomain)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> addBranch(ServerRequest request) {
        return validationHandler.requirePositivePathVariable(request, FRANCHISE_ID)
                .zipWith(validationHandler.requireValidBody(request, NamePayload.class))
                .flatMap(tuple -> franchiseUseCase.addBranchToFranchise(tuple.getT1(), tuple.getT2().name()))
                .map(BranchResponse::fromDomain)
                .flatMap(response -> ServerResponse
                        .created(URI.create("/api/v1/franchises/" + response.franchiseId() + "/branches/" + response.id()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> updateBranchName(ServerRequest request) {
        return Mono.zip(
                        validationHandler.requirePositivePathVariable(request, FRANCHISE_ID),
                        validationHandler.requirePositivePathVariable(request, BRANCH_ID),
                        validationHandler.requireValidBody(request, NamePayload.class)
                )
                .flatMap(tuple -> franchiseUseCase.updateBranchName(tuple.getT1(), tuple.getT2(), tuple.getT3().name()))
                .map(BranchResponse::fromDomain)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> addProduct(ServerRequest request) {
        return Mono.zip(
                        validationHandler.requirePositivePathVariable(request, FRANCHISE_ID),
                        validationHandler.requirePositivePathVariable(request, BRANCH_ID),
                        validationHandler.requireValidBody(request, CreateProductPayload.class)
                )
                .flatMap(tuple -> franchiseUseCase.addProductToBranch(
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3().name(),
                        tuple.getT3().stock()
                ))
                .map(ProductResponse::fromDomain)
                .flatMap(response -> ServerResponse
                        .created(URI.create("/api/v1/franchises/" + request.pathVariable(FRANCHISE_ID)
                                + "/branches/" + response.branchId()
                                + "/products/" + response.id()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> removeProduct(ServerRequest request) {
        return Mono.zip(
                        validationHandler.requirePositivePathVariable(request, FRANCHISE_ID),
                        validationHandler.requirePositivePathVariable(request, BRANCH_ID),
                        validationHandler.requirePositivePathVariable(request, PRODUCT_ID)
                )
                .flatMap(tuple -> franchiseUseCase.removeProductFromBranch(tuple.getT1(), tuple.getT2(), tuple.getT3()))
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> updateProductStock(ServerRequest request) {
        return Mono.zip(
                        validationHandler.requirePositivePathVariable(request, FRANCHISE_ID),
                        validationHandler.requirePositivePathVariable(request, BRANCH_ID),
                        validationHandler.requirePositivePathVariable(request, PRODUCT_ID),
                        validationHandler.requireValidBody(request, UpdateStockPayload.class)
                )
                .flatMap(tuple -> franchiseUseCase.updateProductStock(
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3(),
                        tuple.getT4().stock()
                ))
                .map(ProductResponse::fromDomain)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> updateProductName(ServerRequest request) {
        return Mono.zip(
                        validationHandler.requirePositivePathVariable(request, FRANCHISE_ID),
                        validationHandler.requirePositivePathVariable(request, BRANCH_ID),
                        validationHandler.requirePositivePathVariable(request, PRODUCT_ID),
                        validationHandler.requireValidBody(request, NamePayload.class)
                )
                .flatMap(tuple -> franchiseUseCase.updateProductName(
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3(),
                        tuple.getT4().name()
                ))
                .map(ProductResponse::fromDomain)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> getTopStockProducts(ServerRequest request) {
        return validationHandler.requirePositivePathVariable(request, FRANCHISE_ID)
                .flatMap(id -> franchiseUseCase.getTopStockProductsByFranchise(id)
                        .map(BranchTopProductResponse::fromDomain)
                        .collectList())
                .flatMap(list -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(list));
    }
}
