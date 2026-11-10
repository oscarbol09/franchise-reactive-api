package com.franchise.infrastructure.entrypoint.reactiveweb;

import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.BranchResponse;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.BranchTopProductResponse;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.CreateProductPayload;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.FranchiseResponse;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.NamePayload;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.ProductResponse;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.UpdateStockPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class FranchiseRouterRest {

    private static final String BASE_PATH = "/api/v1/franchises";
    private static final String FRANCHISE_PATH = BASE_PATH + "/{franchiseId}";
    private static final String BRANCHES_PATH = FRANCHISE_PATH + "/branches";
    private static final String BRANCH_PATH = BRANCHES_PATH + "/{branchId}";
    private static final String PRODUCTS_PATH = BRANCH_PATH + "/products";
    private static final String PRODUCT_PATH = PRODUCTS_PATH + "/{productId}";
    private static final String PRODUCT_STOCK_PATH = PRODUCT_PATH + "/stock";
    private static final String TOP_STOCK_PATH = FRANCHISE_PATH + "/top-stock-products";

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = BASE_PATH,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
                    method = RequestMethod.POST,
                    beanClass = FranchiseHandler.class,
                    beanMethod = "createFranchise",
                    operation = @Operation(
                            operationId = "createFranchise",
                            summary = "Create a new franchise",
                            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = NamePayload.class))),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Franchise created", content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "409", description = "Duplicate franchise name", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = FRANCHISE_PATH,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
                    method = RequestMethod.GET,
                    beanClass = FranchiseHandler.class,
                    beanMethod = "getFranchiseDetail",
                    operation = @Operation(
                            operationId = "getFranchiseDetail",
                            summary = "Get franchise with all branches and products",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Franchise detail", content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "404", description = "Franchise not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = FRANCHISE_PATH,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
                    method = RequestMethod.PATCH,
                    beanClass = FranchiseHandler.class,
                    beanMethod = "updateFranchiseName",
                    operation = @Operation(
                            operationId = "updateFranchiseName",
                            summary = "Update franchise name",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                            },
                            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = NamePayload.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Franchise updated", content = @Content(schema = @Schema(implementation = FranchiseResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "404", description = "Franchise not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "409", description = "Duplicate franchise name", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = BRANCHES_PATH,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
                    method = RequestMethod.POST,
                    beanClass = FranchiseHandler.class,
                    beanMethod = "addBranch",
                    operation = @Operation(
                            operationId = "addBranch",
                            summary = "Add a new branch to an existing franchise",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                            },
                            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = NamePayload.class))),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Branch created", content = @Content(schema = @Schema(implementation = BranchResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "404", description = "Franchise not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "409", description = "Duplicate branch name in franchise", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = BRANCH_PATH,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
                    method = RequestMethod.PATCH,
                    beanClass = FranchiseHandler.class,
                    beanMethod = "updateBranchName",
                    operation = @Operation(
                            operationId = "updateBranchName",
                            summary = "Update branch name",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1")),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                            },
                            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = NamePayload.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Branch updated", content = @Content(schema = @Schema(implementation = BranchResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "404", description = "Franchise or branch not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "409", description = "Duplicate branch name in franchise", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = PRODUCTS_PATH,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
                    method = RequestMethod.POST,
                    beanClass = FranchiseHandler.class,
                    beanMethod = "addProduct",
                    operation = @Operation(
                            operationId = "addProduct",
                            summary = "Add a new product to a branch",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1")),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                            },
                            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CreateProductPayload.class))),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Product created", content = @Content(schema = @Schema(implementation = ProductResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "404", description = "Franchise or branch not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "409", description = "Duplicate product name in branch", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = PRODUCT_PATH,
                    produces = {MediaType.APPLICATION_PROBLEM_JSON_VALUE},
                    method = RequestMethod.DELETE,
                    beanClass = FranchiseHandler.class,
                    beanMethod = "removeProduct",
                    operation = @Operation(
                            operationId = "removeProduct",
                            summary = "Remove a product from a branch",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1")),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1")),
                                    @Parameter(in = ParameterIn.PATH, name = "productId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                            },
                            responses = {
                                    @ApiResponse(responseCode = "204", description = "Product deleted"),
                                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "404", description = "Product, branch, or franchise not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = PRODUCT_STOCK_PATH,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
                    method = RequestMethod.PATCH,
                    beanClass = FranchiseHandler.class,
                    beanMethod = "updateProductStock",
                    operation = @Operation(
                            operationId = "updateProductStock",
                            summary = "Modify stock quantity of a product",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1")),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1")),
                                    @Parameter(in = ParameterIn.PATH, name = "productId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                            },
                            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = UpdateStockPayload.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Stock updated", content = @Content(schema = @Schema(implementation = ProductResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "404", description = "Product, branch, or franchise not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = PRODUCT_PATH,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
                    method = RequestMethod.PATCH,
                    beanClass = FranchiseHandler.class,
                    beanMethod = "updateProductName",
                    operation = @Operation(
                            operationId = "updateProductName",
                            summary = "Update product name",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1")),
                                    @Parameter(in = ParameterIn.PATH, name = "branchId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1")),
                                    @Parameter(in = ParameterIn.PATH, name = "productId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                            },
                            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = NamePayload.class))),
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Product name updated", content = @Content(schema = @Schema(implementation = ProductResponse.class))),
                                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "404", description = "Product, branch, or franchise not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "409", description = "Duplicate product name in branch", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
                            }
                    )
            ),
            @RouterOperation(
                    path = TOP_STOCK_PATH,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE},
                    method = RequestMethod.GET,
                    beanClass = FranchiseHandler.class,
                    beanMethod = "getTopStockProducts",
                    operation = @Operation(
                            operationId = "getTopStockProducts",
                            summary = "Get the product with highest stock per branch for a franchise",
                            parameters = {
                                    @Parameter(in = ParameterIn.PATH, name = "franchiseId", required = true, schema = @Schema(type = "integer", format = "int64", minimum = "1"))
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "List of highest stock products indicating their branch",
                                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BranchTopProductResponse.class)))
                                    ),
                                    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
                                    @ApiResponse(responseCode = "404", description = "Franchise not found", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> franchiseRoutes(FranchiseHandler handler) {
        return RouterFunctions.route()
                .POST(BASE_PATH, handler::createFranchise)
                .GET(FRANCHISE_PATH, handler::getFranchiseDetail)
                .PATCH(FRANCHISE_PATH, handler::updateFranchiseName)
                .POST(BRANCHES_PATH, handler::addBranch)
                .PATCH(BRANCH_PATH, handler::updateBranchName)
                .POST(PRODUCTS_PATH, handler::addProduct)
                .DELETE(PRODUCT_PATH, handler::removeProduct)
                .PATCH(PRODUCT_STOCK_PATH, handler::updateProductStock)
                .PATCH(PRODUCT_PATH, handler::updateProductName)
                .GET(TOP_STOCK_PATH, handler::getTopStockProducts)
                .build();
    }
}
