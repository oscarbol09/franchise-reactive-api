package com.franchise.infrastructure.entrypoint.reactiveweb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.franchise.config.TestBlockHoundIntegration;
import com.franchise.domain.exception.DomainValidationException;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.BranchResponse;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.BranchTopProductResponse;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.CreateProductPayload;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.FranchiseResponse;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.NamePayload;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.ProductResponse;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.UpdateStockPayload;
import com.franchise.infrastructure.entrypoint.reactiveweb.error.GlobalErrorWebExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.blockhound.BlockHound;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FranchiseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void setupBlockHound() {
        BlockHound.install(new TestBlockHoundIntegration());
    }

    @Test
    @Order(1)
    @DisplayName("Full lifecycle: creates franchise, branches, products, updates names/stock, deletes product, and queries top stock per branch")
    void shouldExecuteCompleteFranchiseLifecycleReactively() {
        // 1. Create Franchise
        FranchiseResponse franchise = webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Juan Valdez Holding"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(FranchiseResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(franchise).isNotNull();
        Long franchiseId = franchise.id();
        assertThat(franchise.name()).isEqualTo("Juan Valdez Holding");

        // 2. Bonus: Update Franchise Name
        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}", franchiseId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Juan Valdez Global"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(FranchiseResponse.class)
                .value(updated -> assertThat(updated.name()).isEqualTo("Juan Valdez Global"));

        // 3. Add two branches to the franchise
        BranchResponse branchMedellin = webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches", franchiseId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Sucursal Medellín"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BranchResponse.class)
                .returnResult()
                .getResponseBody();

        BranchResponse branchBogota = webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches", franchiseId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Sucursal Bogotá"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BranchResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(branchMedellin).isNotNull();
        assertThat(branchBogota).isNotNull();

        // 4. Bonus: Update Branch Name
        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}", franchiseId, branchMedellin.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Sucursal El Poblado"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(BranchResponse.class)
                .value(updated -> assertThat(updated.name()).isEqualTo("Sucursal El Poblado"));

        // 5. Add products to both branches
        ProductResponse prod1 = webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products", franchiseId, branchMedellin.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductPayload("Café Volcán", 120))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();

        ProductResponse prod2 = webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products", franchiseId, branchMedellin.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductPayload("Café Cumbre", 90))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();

        ProductResponse prodToDelete = webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products", franchiseId, branchMedellin.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductPayload("Producto Temporal", 999))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();

        ProductResponse prodBogota = webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products", franchiseId, branchBogota.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductPayload("Nevado Arequipe", 310))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(prod1).isNotNull();
        assertThat(prod2).isNotNull();
        assertThat(prodToDelete).isNotNull();
        assertThat(prodBogota).isNotNull();

        // 6. Delete temporary product
        webTestClient.delete()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}",
                        franchiseId, branchMedellin.id(), prodToDelete.id())
                .exchange()
                .expectStatus().isNoContent();

        // 7. Update stock of prod2 so it overtakes prod1 in Medellín branch
        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock",
                        franchiseId, branchMedellin.id(), prod2.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateStockPayload(450))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponse.class)
                .value(updated -> assertThat(updated.stock()).isEqualTo(450));

        // 8. Bonus: Update product name of prod2
        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}",
                        franchiseId, branchMedellin.id(), prod2.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Café Cumbre Reserva"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponse.class)
                .value(updated -> assertThat(updated.name()).isEqualTo("Café Cumbre Reserva"));

        // 9. Query top stock product per branch for this franchise
        List<BranchTopProductResponse> topProducts = webTestClient.get()
                .uri("/api/v1/franchises/{franchiseId}/top-stock-products", franchiseId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BranchTopProductResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(topProducts).hasSize(2);
        assertThat(topProducts.get(0).branchName()).isEqualTo("Sucursal El Poblado");
        assertThat(topProducts.get(0).productName()).isEqualTo("Café Cumbre Reserva");
        assertThat(topProducts.get(0).stock()).isEqualTo(450);

        assertThat(topProducts.get(1).branchName()).isEqualTo("Sucursal Bogotá");
        assertThat(topProducts.get(1).productName()).isEqualTo("Nevado Arequipe");
        assertThat(topProducts.get(1).stock()).isEqualTo(310);

        // 10. Query complete franchise detail
        webTestClient.get()
                .uri("/api/v1/franchises/{franchiseId}", franchiseId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FranchiseResponse.class)
                .value(detail -> {
                    assertThat(detail.name()).isEqualTo("Juan Valdez Global");
                    assertThat(detail.branches()).hasSize(2);
                });
    }

    @Test
    @Order(2)
    @DisplayName("Returns RFC 9457 ProblemDetail on 404 Not Found, 409 Conflict, and 400 Bad Request")
    void shouldReturnStandardProblemDetailsOnErrors() {
        // 404 Not Found on non-existent franchise
        webTestClient.get()
                .uri("/api/v1/franchises/99999/top-stock-products")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("ERR-404-FRANCHISE");

        // 409 Conflict on duplicate franchise name
        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Juan Valdez Global"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.errorCode").isEqualTo("ERR-409-FRANCHISE");

        // 400 Bad Request on blank name
        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("   "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("ERR-400");

        // 400 Bad Request on non-numeric path variable
        webTestClient.get()
                .uri("/api/v1/franchises/abc")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);

        // 400 Bad Request on zero or negative path variable
        webTestClient.get()
                .uri("/api/v1/franchises/0")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("ERR-400");

        // 400 Bad Request on empty request body
        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Request payload must not be empty");

        // 400 Bad Request on malformed JSON payload
        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\": ")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Malformed or unreadable JSON request payload");

        // 201 Created when name is 120 chars padded with whitespace (trimmed before @Size validation)
        String padded120 = "  " + "A".repeat(120) + "  ";
        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload(padded120))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseResponse.class)
                .value(created -> assertThat(created.name()).hasSize(120));

        // OpenAPI specification includes 400/409 responses and int64 path schemas
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['/api/v1/franchises/{franchiseId}/branches/{branchId}/products'].post.responses['409']").exists()
                .jsonPath("$.paths['/api/v1/franchises/{franchiseId}'].get.responses['400']").exists();
    }

    @Test
    @Order(3)
    @DisplayName("GlobalErrorWebExceptionHandler safely encodes invalid URI characters and maps DataIntegrity, ResponseStatus, and 500 errors")
    void shouldHandleInvalidUriCharactersAndFallbackExceptionTypes() {
        GlobalErrorWebExceptionHandler errorHandler = new GlobalErrorWebExceptionHandler(new ObjectMapper());

        // Path with unencoded curly braces '{' and '}' must not throw synchronous IllegalArgumentException
        MockServerWebExchange baseExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/franchises/invalid").build()
        );
        ServerWebExchange braceExchange = baseExchange.mutate()
                .request(new ServerHttpRequestDecorator(baseExchange.getRequest()) {
                    @Override
                    public RequestPath getPath() {
                        return RequestPath.parse("/api/v1/franchises/{franchiseId}", null);
                    }
                })
                .build();
        StepVerifier.create(errorHandler.handle(braceExchange,
                        new DomainValidationException("franchiseId must be a valid numeric identifier")))
                .verifyComplete();
        assertThat(baseExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(baseExchange.getResponse().getBodyAsString().block())
                .contains("%7BfranchiseId%7D")
                .contains("ERR-400");

        // DataIntegrityViolationException -> 409 Conflict with ERR-409-INTEGRITY
        MockServerWebExchange integrityExchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/franchises").build()
        );
        StepVerifier.create(errorHandler.handle(integrityExchange,
                        new DataIntegrityViolationException("Unique constraint violation")))
                .verifyComplete();
        assertThat(integrityExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(integrityExchange.getResponse().getBodyAsString().block())
                .contains("ERR-409-INTEGRITY");

        // ResponseStatusException -> preserves status code and populates errorCode
        MockServerWebExchange statusExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/unknown").build()
        );
        StepVerifier.create(errorHandler.handle(statusExchange,
                        new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Method not supported")))
                .verifyComplete();
        assertThat(statusExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(statusExchange.getResponse().getBodyAsString().block())
                .contains("ERR-405-HTTP");

        // Unexpected RuntimeException -> 500 Internal Server Error
        MockServerWebExchange fallbackExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/franchises/1").build()
        );
        StepVerifier.create(errorHandler.handle(fallbackExchange, new IllegalStateException("Unexpected failure")))
                .verifyComplete();
        assertThat(fallbackExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(fallbackExchange.getResponse().getBodyAsString().block())
                .contains("ERR-500-INTERNAL");
    }
}
