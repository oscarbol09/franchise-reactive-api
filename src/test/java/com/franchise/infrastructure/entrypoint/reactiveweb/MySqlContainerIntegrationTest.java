package com.franchise.infrastructure.entrypoint.reactiveweb;

import com.franchise.config.TestBlockHoundIntegration;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.BranchResponse;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.BranchTopProductResponse;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.CreateProductPayload;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.FranchiseResponse;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.NamePayload;
import com.franchise.infrastructure.entrypoint.reactiveweb.dto.FranchiseApiDtos.ProductResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.blockhound.BlockHound;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers(disabledWithoutDocker = true)
class MySqlContainerIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.8")
            .withDatabaseName("franchise_db")
            .withUsername("franchise_user")
            .withPassword("franchise_test_pwd")
            .withStartupTimeoutSeconds(240);

    @DynamicPropertySource
    static void registerMySqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format(
                "r2dbc:mysql://%s:%d/%s?sslMode=DISABLED&allowPublicKeyRetrieval=true",
                MYSQL.getHost(),
                MYSQL.getMappedPort(3306),
                MYSQL.getDatabaseName()
        ));
        registry.add("spring.r2dbc.username", MYSQL::getUsername);
        registry.add("spring.r2dbc.password", MYSQL::getPassword);
        registry.add("spring.flyway.url", () -> String.format(
                "jdbc:mysql://%s:%d/%s?sslMode=DISABLED&allowPublicKeyRetrieval=true",
                MYSQL.getHost(),
                MYSQL.getMappedPort(3306),
                MYSQL.getDatabaseName()
        ));
        registry.add("spring.flyway.user", MYSQL::getUsername);
        registry.add("spring.flyway.password", MYSQL::getPassword);
    }

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void setupBlockHound() {
        BlockHound.install(new TestBlockHoundIntegration());
    }

    @Test
    @DisplayName("MySQL 8.4 Testcontainers: validates Flyway DDL, utf8mb4_0900_ai_ci uniqueness, and ROW_NUMBER() window query")
    void shouldValidateRealMySql84PersistenceAndWindowQuery() {
        WebTestClient client = webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(20))
                .build();

        FranchiseResponse franchise = client.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Franquicia MySQL Real"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(franchise).isNotNull();

        // Case-insensitive uniqueness under MySQL 8.4 utf8mb4_0900_ai_ci collation
        client.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("franquicia mysql real"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("ERR-409-FRANCHISE");

        BranchResponse branch = client.post()
                .uri("/api/v1/franchises/{franchiseId}/branches", franchise.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new NamePayload("Sucursal Principal"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BranchResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(branch).isNotNull();

        ProductResponse firstProduct = client.post()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products", franchise.id(), branch.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductPayload("Café Geisha", 300))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();

        client.post()
                .uri("/api/v1/franchises/{franchiseId}/branches/{branchId}/products", franchise.id(), branch.id())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateProductPayload("Café Bourbon", 300))
                .exchange()
                .expectStatus().isCreated();

        assertThat(firstProduct).isNotNull();

        List<BranchTopProductResponse> topProducts = client.get()
                .uri("/api/v1/franchises/{franchiseId}/top-stock-products", franchise.id())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BranchTopProductResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(topProducts).hasSize(1);
        assertThat(topProducts.get(0).productId()).isEqualTo(firstProduct.id());
        assertThat(topProducts.get(0).productName()).isEqualTo("Café Geisha");
        assertThat(topProducts.get(0).stock()).isEqualTo(300);
    }
}
