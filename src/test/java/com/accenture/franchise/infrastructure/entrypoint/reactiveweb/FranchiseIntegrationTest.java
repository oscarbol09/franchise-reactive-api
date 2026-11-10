package com.accenture.franchise.infrastructure.entrypoint.reactiveweb;

import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.BranchResponse;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.BranchTopProductResponse;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.CreateProductPayload;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.FranchiseResponse;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.NamePayload;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.ProductResponse;
import com.accenture.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.UpdateStockPayload;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.blockhound.BlockHound;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FranchiseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void setupBlockHound() {
        BlockHound.install(builder -> builder
                .allowBlockingCallsInside("org.springframework.util.JdkIdGenerator", "generateId")
                .allowBlockingCallsInside("java.util.UUID", "randomUUID")
                .allowBlockingCallsInside(
                        "org.springframework.core.type.classreading.SimpleMetadataReaderFactory",
                        "getMetadataReader"
                ));
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
    }
}
