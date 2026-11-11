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
import com.franchise.infrastructure.entrypoint.reactiveweb.security.SecurityHeadersWebFilter;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class FranchiseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeAll
    static void setupBlockHound() {
        BlockHound.install(new TestBlockHoundIntegration());
    }

    @BeforeEach
    void cleanDatabase() {
        databaseClient.sql("DELETE FROM products").fetch().rowsUpdated()
                .then(databaseClient.sql("DELETE FROM branches").fetch().rowsUpdated())
                .then(databaseClient.sql("DELETE FROM franchises").fetch().rowsUpdated())
                .block();
    }

    @Test
    @DisplayName("Full lifecycle: creates franchise, branches, products, updates names/stock, deletes product, and queries top stock per branch")
    void shouldExecuteCompleteFranchiseLifecycleReactively() {
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

        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}", franchiseId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Juan Valdez Global"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(FranchiseResponse.class)
                .value(updated -> assertThat(updated.name()).isEqualTo("Juan Valdez Global"));

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

        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}", franchiseId, branchMedellin.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Sucursal El Poblado"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(BranchResponse.class)
                .value(updated -> assertThat(updated.name()).isEqualTo("Sucursal El Poblado"));

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

        webTestClient.delete()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}",
                        franchiseId, branchMedellin.id(), prodToDelete.id())
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock",
                        franchiseId, branchMedellin.id(), prod2.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateStockPayload(450))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponse.class)
                .value(updated -> assertThat(updated.stock()).isEqualTo(450));

        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}",
                        franchiseId, branchMedellin.id(), prod2.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Café Cumbre Reserva"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponse.class)
                .value(updated -> assertThat(updated.name()).isEqualTo("Café Cumbre Reserva"));

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
    @DisplayName("Top-stock query breaks ties deterministically by lowest product ID and handles empty franchises/branches")
    void shouldBreakTiesDeterministicallyAndHandleEmptyBranchesInTopStockQuery() {
        FranchiseResponse franchise = webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Franquicia Desempate"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(franchise).isNotNull();
        Long franchiseId = franchise.id();

        // 1. Franchise with 0 branches returns empty top-stock list and empty branches detail
        webTestClient.get()
                .uri("/api/v1/franchises/{franchiseId}/top-stock-products", franchiseId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BranchTopProductResponse.class)
                .hasSize(0);

        webTestClient.get()
                .uri("/api/v1/franchises/{franchiseId}", franchiseId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FranchiseResponse.class)
                .value(detail -> assertThat(detail.branches()).isEmpty());

        // 2. Branch with 0 products returns empty top-stock list
        BranchResponse branch = webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches", franchiseId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Sucursal Norte"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BranchResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(branch).isNotNull();

        webTestClient.get()
                .uri("/api/v1/franchises/{franchiseId}/top-stock-products", franchiseId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BranchTopProductResponse.class)
                .hasSize(0);

        // 3. Two products with identical max stock (500): tie-breaker must select the first created (lowest ID)
        ProductResponse firstTied = webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products", franchiseId, branch.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductPayload("Producto Empate Primero", 500))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();

        webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products", franchiseId, branch.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductPayload("Producto Empate Segundo", 500))
                .exchange()
                .expectStatus().isCreated();

        assertThat(firstTied).isNotNull();

        List<BranchTopProductResponse> topProducts = webTestClient.get()
                .uri("/api/v1/franchises/{franchiseId}/top-stock-products", franchiseId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BranchTopProductResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(topProducts).hasSize(1);
        assertThat(topProducts.get(0).productId()).isEqualTo(firstTied.id());
        assertThat(topProducts.get(0).productName()).isEqualTo("Producto Empate Primero");
        assertThat(topProducts.get(0).stock()).isEqualTo(500);
    }

    @Test
    @DisplayName("Enforces hierarchical isolation across franchises/branches, duplicate conflicts, and idempotent self-renames")
    void shouldEnforceHierarchicalIsolationAndIdempotentRenames() {
        FranchiseResponse franchise1 = webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Franquicia Uno"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseResponse.class)
                .returnResult()
                .getResponseBody();

        FranchiseResponse franchise2 = webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Franquicia Dos"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(franchise1).isNotNull();
        assertThat(franchise2).isNotNull();

        BranchResponse branch1 = webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches", franchise1.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Sucursal A"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BranchResponse.class)
                .returnResult()
                .getResponseBody();

        BranchResponse branch2 = webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches", franchise2.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Sucursal B"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BranchResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(branch1).isNotNull();
        assertThat(branch2).isNotNull();

        ProductResponse product1 = webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products", franchise1.id(), branch1.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductPayload("Producto Exclusivo", 50))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(product1).isNotNull();

        // Cross-franchise branch access (IDOR protection) -> 404 ERR-404-BRANCH
        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}", franchise1.id(), branch2.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Intento Cruzado"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("ERR-404-BRANCH");

        // Cross-branch product access (IDOR protection) -> 404 ERR-404-PRODUCT
        webTestClient.delete()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}",
                        franchise2.id(), branch2.id(), product1.id())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("ERR-404-PRODUCT");

        // Duplicate branch name within the same franchise -> 409 ERR-409-BRANCH
        webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches", franchise1.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("sucursal a"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("ERR-409-BRANCH");

        // Duplicate product name within the same branch -> 409 ERR-409-PRODUCT
        webTestClient.post()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products", franchise1.id(), branch1.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductPayload("producto exclusivo", 10))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("ERR-409-PRODUCT");

        // Idempotent self-renames (excluded by id <> :excludedId) -> 200 OK
        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}", franchise1.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Franquicia Uno"))
                .exchange()
                .expectStatus().isOk();

        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}", franchise1.id(), branch1.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Sucursal A"))
                .exchange()
                .expectStatus().isOk();

        webTestClient.patch()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}",
                        franchise1.id(), branch1.id(), product1.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Producto Exclusivo"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Returns RFC 9457 ProblemDetail on 404 Not Found, 409 Conflict, and 400 Bad Request")
    void shouldReturnStandardProblemDetailsOnErrors() {
        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Juan Valdez Global"))
                .exchange()
                .expectStatus().isCreated();

        // 404 Not Found on non-existent franchise
        webTestClient.get()
                .uri("/api/v1/franchises/99999/top-stock-products")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.errorCode").isEqualTo("ERR-404-FRANCHISE");

        // 409 Conflict on duplicate franchise name (exact and case-insensitive)
        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("juan valdez global"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY")
                .expectHeader().valueEquals("Referrer-Policy", "no-referrer")
                .expectHeader().valueEquals("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.errorCode").isEqualTo("ERR-409-FRANCHISE");

        // Actuator health returns UP without leaking internal database/disk topology details
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.components").doesNotExist();

        // Actuator metrics endpoint is not exposed publicly
        webTestClient.get()
                .uri("/actuator/metrics")
                .exchange()
                .expectStatus().isNotFound();

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

        // 400 Bad Request on name exceeding 120 characters
        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("A".repeat(121)))
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

        webTestClient.get()
                .uri("/api/v1/franchises/-10")
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
    @DisplayName("GlobalErrorWebExceptionHandler safely encodes invalid URI characters and maps DataIntegrity, ResponseStatus, committed, and 500 errors")
    void shouldHandleInvalidUriCharactersAndFallbackExceptionTypes() {
        assertThat(new NamePayload(null).name()).isNull();
        assertThat(new CreateProductPayload(null, 5).name()).isNull();

        GlobalErrorWebExceptionHandler errorHandler = new GlobalErrorWebExceptionHandler(new ObjectMapper());

        // Committed response propagates error directly
        MockServerWebExchange committedExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/franchises/1").build()
        );
        committedExchange.getResponse().setComplete().block();
        StepVerifier.create(errorHandler.handle(committedExchange, new IllegalStateException("Already committed")))
                .expectError(IllegalStateException.class)
                .verify();

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

        // ResponseStatusException with non-null reason and with null reason
        MockServerWebExchange statusExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/unknown").build()
        );
        StepVerifier.create(errorHandler.handle(statusExchange,
                        new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Method not supported")))
                .verifyComplete();
        assertThat(statusExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(statusExchange.getResponse().getBodyAsString().block())
                .contains("ERR-405-HTTP");

        MockServerWebExchange nullReasonStatusExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/unknown").build()
        );
        StepVerifier.create(errorHandler.handle(nullReasonStatusExchange,
                        new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE)))
                .verifyComplete();
        assertThat(nullReasonStatusExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);

        // Security headers omit restrictive CSP for Swagger UI & WebJars assets
        MockServerWebExchange swaggerExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/swagger-ui/index.html").build()
        );
        SecurityHeadersWebFilter.applySecurityHeaders(swaggerExchange);
        assertThat(swaggerExchange.getResponse().getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(swaggerExchange.getResponse().getHeaders().getFirst("Content-Security-Policy")).isNull();

        MockServerWebExchange webjarsExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/webjars/swagger-ui/index.css").build()
        );
        SecurityHeadersWebFilter.applySecurityHeaders(webjarsExchange);
        assertThat(webjarsExchange.getResponse().getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(webjarsExchange.getResponse().getHeaders().getFirst("Content-Security-Policy")).isNull();

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
