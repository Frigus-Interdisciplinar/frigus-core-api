package com.frigus.coreapi.integration;

import com.frigus.coreapi.model.User;
import com.frigus.coreapi.client.FrigusAiClient;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.security.TokenProvider;
import com.frigus.coreapi.service.RefreshTokenService;
import com.frigus.coreapi.service.SubscriptionBillingJobService;
import com.frigus.coreapi.service.TransactionQueueProducer;
import com.frigus.coreapi.service.TransactionWorkerService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real filters, controllers, services, repositories and production SQL; only Redis boundaries are mocked. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.address=127.0.0.1",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.open-in-view=false", "app.swagger.open-on-startup=false",
        "aws.s3.enabled=false", "logging.level.root=WARN"
})
class ApiIntegrationTest {
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("frigus_test")
            .withStartupTimeout(java.time.Duration.ofMinutes(3))
            .withCopyFileToContainer(MountableFile.forHostPath("db/script.sql"),
                    "/docker-entrypoint-initdb.d/001-schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("db/migrations/003_allow_free_transactions.sql"),
                    "/test/003_allow_free_transactions.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.data.redis.url", () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired TokenProvider tokens;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired FrigusAiClient aiClient;
    @LocalServerPort int port;
    @MockitoBean RefreshTokenService refreshTokens;
    @MockitoBean TransactionQueueProducer queue;
    // Remove scheduled consumers so no background task touches Redis or mutates test data.
    @MockitoBean TransactionWorkerService worker;
    @MockitoBean SubscriptionBillingJobService billing;

    MockMvc mvc;
    User owner;
    User outsider;
    User admin;
    UUID groupId;
    UUID otherGroupId;
    int stockId;
    int itemId;
    int productId;

    @BeforeEach
    void arrangeIsolatedDatabase() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.execute("TRUNCATE users, products, recipes RESTART IDENTITY CASCADE");
        owner = user("owner@example.test", "USER");
        outsider = user("outsider@example.test", "USER");
        admin = user("admin@example.test", "ADMIN");
        groupId = group(owner, "Owner group");
        otherGroupId = group(outsider, "Other group");
        stockId = jdbc.queryForObject("INSERT INTO stocks(group_id,name) VALUES (?, 'Fridge') RETURNING id", Integer.class, groupId);
        productId = jdbc.queryForObject("""
                INSERT INTO products(name,category,storage_place,unit_price,unit_of_measure)
                VALUES ('Milk','DAIRY','FRIDGE',8.50,'LITER') RETURNING id
                """, Integer.class);
        itemId = jdbc.queryForObject("""
                INSERT INTO stock_products(product_id,stock_id,quantity,minimal_quantity,expire_date,category)
                VALUES (?, ?, 5, 1, '2099-01-01', 'DAIRY') RETURNING id
                """, Integer.class, productId, stockId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/profile", "/user", "/groups", "/conversations", "/transactions", "/subscriptions/me", "/stocks", "/stock-products", "/discard", "/ingredients", "/ai/recipes/suggestions/stock/1"})
    void rejectsProtectedRoutesWithoutCredentials(String route) throws Exception {
        mvc.perform(get(route)).andExpect(status().isForbidden());
    }

    @Test
    void permitsAnonymousCatalogReadsWithPaginationAndNumericFields() throws Exception {
        mvc.perform(get("/products").param("page", "0").param("size", "1").param("sort", "id,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(productId))
                .andExpect(jsonPath("$.content[0].unitPrice").value(8.50))
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.size").value(1));
        mvc.perform(get("/plans/active")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planCode").isString());
    }

    @Test
    void authenticatesRealBearerAndCookieTokensAndUsesPersistedRole() throws Exception {
        String token = tokens.generateAccessToken(owner);
        mvc.perform(get("/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(owner.getId().toString()))
                .andExpect(jsonPath("$.hashPassword").doesNotExist());
        mvc.perform(get("/profile").cookie(new Cookie("accessToken", token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(owner.getEmail()));
        jdbc.update("UPDATE users SET role='ADMIN' WHERE id=?", owner.getId());
        mvc.perform(get("/user").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidTokenEvenWhenCookieContainsValidToken() throws Exception {
        mvc.perform(get("/profile").header("Authorization", "Bearer invalid")
                        .cookie(new Cookie("accessToken", tokens.generateAccessToken(owner))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsTokenAfterUserIsDeleted() throws Exception {
        String token = tokens.generateAccessToken(admin);
        jdbc.update("DELETE FROM users WHERE id=?", admin.getId());
        mvc.perform(get("/profile").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/user", "/plans/admin/all", "/subscriptions"})
    void forbidsRegularUserFromAdministrativeReads(String route) throws Exception {
        mvc.perform(as(get(route), owner)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/products", "/plans"})
    void forbidsRegularUserFromCatalogCreation(String route) throws Exception {
        String body = route.equals("/products") ? productBody("Bread") : """
                {"planCode":"CUSTOM","name":"Custom","price":9.99,"billingInterval":"MONTHLY"}
                """;
        mvc.perform(as(post(route), owner).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        assertThat(count("products")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plans WHERE plan_code='CUSTOM'", Integer.class)).isZero();
    }

    @Test
    void adminCreatesUpdatesAndDeletesCatalogProduct() throws Exception {
        JsonNode created = body(as(post("/products"), admin).contentType("application/json").content(productBody(" Bread ")), 201);
        int id = created.get("id").asInt();
        assertThat(created.get("name").asString()).isEqualTo("Bread");
        mvc.perform(as(put("/products/" + id), admin).contentType("application/json").content(productBody("Rice")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Rice"));
        mvc.perform(as(delete("/products/" + id), admin)).andExpect(status().isNoContent());
        mvc.perform(get("/products/" + id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void rejectsDuplicateCatalogNameAndDeletingReferencedProductWithoutChangingData() throws Exception {
        mvc.perform(as(post("/products"), admin).contentType("application/json").content(productBody(" milk ")))
                .andExpect(status().isConflict());
        mvc.perform(as(delete("/products/" + productId), admin)).andExpect(status().isConflict());
        assertThat(count("products")).isEqualTo(1);
        assertThat(count("stock_products")).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"\"}", "{\"name\":\"   \"}", "{\"name\":\"Bread\",\"category\":\"INVALID\"}", "{"})
    void invalidCatalogPayloadReturnsClientErrorWithoutPersistenceOrInternalDetails(String payload) throws Exception {
        var response = mvc.perform(as(post("/products"), admin).contentType("application/json").content(payload))
                .andExpect(status().isBadRequest()).andReturn().getResponse();
        assertThat(response.getContentAsString()).doesNotContain("Exception", "stackTrace", "jdbc:", "hashPassword");
        assertThat(count("products")).isEqualTo(1);
    }

    @Test
    void registersHashesPasswordAndLogsInWithHttpOnlyCookies() throws Exception {
        String registration = """
                {"name":"New User","email":"new@example.test","birthDate":"01/01/1990","rawPassword":"Strong123!"}
                """;
        mvc.perform(post("/auth/register").contentType("application/json").content(registration))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new@example.test"))
                .andExpect(jsonPath("$.accountType").value("DOMESTIC"))
                .andExpect(jsonPath("$.rawPassword").doesNotExist()).andExpect(jsonPath("$.hashPassword").doesNotExist());
        User registered = users.findByEmail("new@example.test").orElseThrow();
        assertThat(registered.getHashPassword()).isNotEqualTo("Strong123!");
        assertThat(passwords.matches("Strong123!", registered.getHashPassword())).isTrue();
        when(refreshTokens.createRefreshToken(registered.getId())).thenReturn("test-refresh");
        var login = mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"new@example.test\",\"rawPassword\":\"Strong123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString()).andReturn().getResponse();
        assertThat(login.getHeaders("Set-Cookie")).hasSize(2).allSatisfy(cookie ->
                assertThat(cookie).contains("HttpOnly", "Path=/", "SameSite=Lax"));
        mvc.perform(post("/auth/register").contentType("application/json").content(registration)).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email='new@example.test'", Integer.class)).isEqualTo(1);
    }

    @Test
    void missingRefreshTokenReturnsUnauthorizedWithoutSettingCookies() throws Exception {
        mvc.perform(post("/auth/refresh")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED")).andExpect(header().doesNotExist("Set-Cookie"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/stocks/1", "/stock-products/1", "/stock-products/1/movements"})
    void deniesOutsiderReadingAnotherGroupsResources(String route) throws Exception {
        mvc.perform(as(get(route), outsider)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void scopesStockListingAndAllowsAdminAccess() throws Exception {
        mvc.perform(as(get("/stocks").param("groupId", groupId.toString()), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(stockId))
                .andExpect(jsonPath("$.content[0].groupId").value(groupId.toString()));
        mvc.perform(as(get("/stocks").param("groupId", groupId.toString()), outsider)).andExpect(status().isForbidden());
        mvc.perform(as(get("/stocks/" + stockId), admin)).andExpect(status().isOk());
    }

    @Test
    void createsUpdatesAndDeletesStockWithPersistedNormalizedName() throws Exception {
        String payload = "{\"groupId\":\"" + groupId + "\",\"name\":\" Pantry \"}";
        int id = body(as(post("/stocks"), owner).contentType("application/json").content(payload), 201).get("id").asInt();
        mvc.perform(as(put("/stocks/" + id), owner).contentType("application/json").content(payload.replace("Pantry", "Shelf")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Shelf"));
        assertThat(jdbc.queryForObject("SELECT name FROM stocks WHERE id=?", String.class, id)).isEqualTo("Shelf");
        mvc.perform(as(delete("/stocks/" + id), owner)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stocks WHERE id=?", Integer.class, id)).isZero();
    }

    @Test
    void rejectsMovingStockIntoUnauthorizedGroupAndOutsiderDeletion() throws Exception {
        mvc.perform(as(put("/stocks/" + stockId), owner).contentType("application/json")
                        .content("{\"groupId\":\"" + otherGroupId + "\",\"name\":\"Stolen\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(as(delete("/stocks/" + stockId), outsider)).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT group_id FROM stocks WHERE id=?", UUID.class, stockId)).isEqualTo(groupId);
        assertThat(jdbc.queryForObject("SELECT name FROM stocks WHERE id=?", String.class, stockId)).isEqualTo("Fridge");
    }

    @Test
    void createsUpdatesAndDeletesStockProductAndRejectsDuplicateBatch() throws Exception {
        String payload = "{\"stockId\":" + stockId + ",\"productId\":" + productId +
                ",\"quantity\":2,\"minimalQuantity\":0,\"expireDate\":\"2099-02-01\"}";
        int id = body(as(post("/stock-products"), owner).contentType("application/json").content(payload), 201).get("id").asInt();
        mvc.perform(as(post("/stock-products"), owner).contentType("application/json").content(payload)).andExpect(status().isConflict());
        mvc.perform(as(put("/stock-products/" + id), owner).contentType("application/json")
                        .content("{\"minimalQuantity\":1,\"expireDate\":\"2099-03-01\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(2)).andExpect(jsonPath("$.productStatus").value("FRESH"));
        mvc.perform(as(get("/stock-products").param("stockId", String.valueOf(stockId)).param("size", "1"), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(as(delete("/stock-products/" + id), outsider)).andExpect(status().isForbidden());
        mvc.perform(as(delete("/stock-products/" + id), owner)).andExpect(status().isOk());
        assertThat(count("stock_products")).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"IN,3,8", "OUT,2,3", "OUT,5,0", "ADJUSTMENT,0,0", "ADJUSTMENT,2,2"})
    void movementAppliesExactlyOnceAndPersistsAudit(String type, int quantity, int expected) throws Exception {
        JsonNode response = body(as(post("/stock-products/" + itemId + "/movements"), owner)
                .contentType("application/json").content(movement(type, quantity)), 201);
        assertThat(response.get("balanceAfter").asInt()).isEqualTo(expected);
        assertThat(response.get("userId").asString()).isEqualTo(owner.getId().toString());
        assertThat(Instant.parse(response.get("date").asString())).isNotNull();
        assertThat(balance()).isEqualTo(expected);
        assertThat(count("stock_movements")).isEqualTo(1);
        mvc.perform(as(get("/stock-products/" + itemId + "/movements"), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].quantity").value(quantity))
                .andExpect(jsonPath("$.content[0].movementType").value(type));
    }

    @ParameterizedTest
    @CsvSource({"OUT,6", "IN,0", "OUT,0", "IN,-1", "ADJUSTMENT,-1", "IN,2147483647"})
    void rejectsInvalidMovementsWithoutBalanceOrAuditChanges(String type, int quantity) throws Exception {
        mvc.perform(as(post("/stock-products/" + itemId + "/movements"), owner)
                        .contentType("application/json").content(movement(type, quantity))).andExpect(status().isBadRequest());
        assertThat(balance()).isEqualTo(5);
        assertThat(count("stock_movements")).isZero();
    }

    @Test
    void rollsBackBalanceWhenAuditPersistenceFails() throws Exception {
        // A real database constraint fails after the service changes the balance.
        jdbc.execute("ALTER TABLE stock_movements ADD CONSTRAINT test_reject_audit CHECK (quantity < 2)");
        try {
            var result = mvc.perform(as(post("/stock-products/" + itemId + "/movements"), owner)
                            .contentType("application/json").content(movement("IN", 3)))
                    .andExpect(status().isInternalServerError()).andReturn();
            assertThat(result.getResolvedException()).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            var failure = (org.springframework.dao.DataIntegrityViolationException) result.getResolvedException();
            assertThat(failure.getMostSpecificCause().getMessage()).contains("test_reject_audit");
            assertThat(result.getResponse().getContentAsString()).doesNotContain("test_reject_audit", "SQLException", "stackTrace");
            assertThat(balance()).isEqualTo(5);
            assertThat(count("stock_movements")).isZero();
        } finally {
            jdbc.execute("ALTER TABLE stock_movements DROP CONSTRAINT test_reject_audit");
        }
    }

    @Test
    void deniesOutsiderMovementWithoutWritingAnything() throws Exception {
        mvc.perform(as(post("/stock-products/" + itemId + "/movements"), outsider)
                        .contentType("application/json").content(movement("IN", 3))).andExpect(status().isForbidden());
        assertThat(balance()).isEqualTo(5);
        assertThat(count("stock_movements")).isZero();
    }

    @Test
    void paidCheckoutIsIdempotentAndQueuesOnlyAfterCommittedPersistence() throws Exception {
        String payload = "{\"planCode\":\"PLUS\",\"paymentMethod\":\"CREDIT_CARD\",\"fakeCardLast4\":\"1234\"}";
        doAnswer(invocation -> {
            // A separate connection can see the transaction only after its commit.
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT count(*) FROM transactions")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
            return null;
        }).when(queue).enqueue(any());
        JsonNode first = body(as(post("/transactions/checkout"), owner).header("Idempotency-Key", "checkout-test")
                .contentType("application/json").content(payload), 200);
        JsonNode repeated = body(as(post("/transactions/checkout"), owner).header("Idempotency-Key", "checkout-test")
                .contentType("application/json").content(payload), 200);
        assertThat(repeated.get("id")).isEqualTo(first.get("id"));
        assertThat(first.get("status").asString()).isEqualTo("PENDING");
        assertThat(count("transactions")).isEqualTo(1);
        verify(queue, times(1)).enqueue(any());
        mvc.perform(as(get("/transactions/" + first.get("id").asString()), outsider)).andExpect(status().isForbidden());
        mvc.perform(as(post("/transactions/" + first.get("id").asString() + "/cancel"), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELED"));
        mvc.perform(as(post("/transactions/" + first.get("id").asString() + "/cancel"), owner)).andExpect(status().isBadRequest());
    }

    @Test
    void checkoutCannotReuseAnotherUsersIdempotencyKey() throws Exception {
        String payload = "{\"planCode\":\"PLUS\",\"paymentMethod\":\"CREDIT_CARD\",\"fakeCardLast4\":\"1234\"}";
        body(as(post("/transactions/checkout"), owner).header("Idempotency-Key", "shared-key")
                .contentType("application/json").content(payload), 200);
        mvc.perform(as(post("/transactions/checkout"), outsider).header("Idempotency-Key", "shared-key")
                        .contentType("application/json").content(payload)).andExpect(status().isForbidden());
        assertThat(count("transactions")).isEqualTo(1);
        verify(queue, times(1)).enqueue(any());
    }

    @Test
    void freeCheckoutActivatesSubscriptionWithoutQueueAndPersistsZeroAmount() throws Exception {
        JsonNode response = body(as(post("/transactions/checkout"), owner).header("Idempotency-Key", "free-test")
                .contentType("application/json").content("{\"planCode\":\"FREE\",\"paymentMethod\":\"PIX\"}"), 200);
        assertThat(response.get("status").asString()).isEqualTo("APPROVED");
        assertThat(response.get("amount").asDouble()).isZero();
        assertThat(count("subscriptions")).isEqualTo(1);
        mvc.perform(as(get("/subscriptions/me"), owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.plan.planCode").value("FREE"));
        verifyNoInteractions(queue);
    }

    @ParameterizedTest
    @CsvSource({"unitPrice,0", "unitPrice,-1", "unitPrice,1.001", "unitPrice,100000000", "category,\"BAD\""})
    void rejectsInvalidProductPriceLimitsAndCategory(String field, String value) throws Exception {
        var payload = json.readTree(productBody("Bread"));
        ((tools.jackson.databind.node.ObjectNode) payload).set(field, json.readTree(value));
        mvc.perform(as(post("/products"), admin).contentType("application/json").content(json.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        assertThat(count("products")).isEqualTo(1);
    }

    @Test
    void rejectsOversizedStockNameAndMissingOrMalformedRequiredFilter() throws Exception {
        String payload = "{\"groupId\":\"" + groupId + "\",\"name\":\"" + "a".repeat(2001) + "\"}";
        mvc.perform(as(post("/stocks"), owner).contentType("application/json").content(payload)).andExpect(status().isBadRequest());
        mvc.perform(as(get("/stocks"), owner)).andExpect(status().isBadRequest());
        mvc.perform(as(get("/stocks").param("groupId", "invalid"), owner)).andExpect(status().isBadRequest());
        mvc.perform(get("/products/not-an-integer")).andExpect(status().isBadRequest());
        assertThat(count("stocks")).isEqualTo(1);
    }

    @Test
    void missingStockAndProductReferencesReturnNotFoundWithoutWrites() throws Exception {
        mvc.perform(as(get("/stocks/2147483647"), owner)).andExpect(status().isNotFound());
        mvc.perform(as(post("/stock-products"), owner).contentType("application/json")
                        .content("{\"stockId\":" + stockId + ",\"productId\":2147483647,\"quantity\":1,\"expireDate\":\"2099-01-01\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(as(post("/stock-products/2147483647/movements"), owner).contentType("application/json")
                        .content(movement("IN", 1))).andExpect(status().isNotFound());
        assertThat(count("stock_products")).isEqualTo(1);
        assertThat(count("stock_movements")).isZero();
    }

    @Test
    void databaseEnforcesBatchUniquenessAndProductForeignKey() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO stock_products(product_id,stock_id,quantity,expire_date,category)
                VALUES (?, ?, 1, '2099-01-01', 'DAIRY')
                """, productId, stockId)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("DELETE FROM products WHERE id=?", productId))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(count("stock_products")).isEqualTo(1);
        assertThat(count("products")).isEqualTo(1);
    }

    @Test
    void freeTransactionMigrationUpgradesLegacyConstraintAndStillRejectsNegativeAmounts() throws Exception {
        jdbc.execute("ALTER TABLE transactions DROP CONSTRAINT transactions_amount_check");
        jdbc.execute("ALTER TABLE transactions ADD CONSTRAINT transactions_amount_check CHECK (amount > 0)");
        var migration = POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
                "-v", "ON_ERROR_STOP=1", "-f", "/test/003_allow_free_transactions.sql");
        assertThat(migration.getExitCode()).as(migration.getStderr()).isZero();
        // Reapplying the migration is safe and uses the same constraint name.
        assertThat(POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
                "-v", "ON_ERROR_STOP=1", "-f", "/test/003_allow_free_transactions.sql").getExitCode()).isZero();
        jdbc.update("""
                INSERT INTO transactions(idempotency_key,user_id,plan_id,amount,payment_method)
                VALUES ('zero-amount',?,(SELECT id FROM plans WHERE plan_code='FREE'),0,'PIX')
                """, owner.getId());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO transactions(idempotency_key,user_id,plan_id,amount,payment_method)
                VALUES ('negative-amount',?,(SELECT id FROM plans WHERE plan_code='FREE'),-1,'PIX')
                """, owner.getId())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(count("transactions")).isEqualTo(1);
    }

    @Test
    void embeddedHttpServerServesCatalogAndRequiresAuthenticationForProfile() throws Exception {
        var client = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
        var catalog = client.send(java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://127.0.0.1:" + port + "/products"))
                        .timeout(java.time.Duration.ofSeconds(10)).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(catalog.statusCode()).isEqualTo(200);
        assertThat(json.readTree(catalog.body()).get("content").get(0).get("id").asInt()).isEqualTo(productId);

        var profileUri = java.net.URI.create("http://127.0.0.1:" + port + "/profile");
        assertThat(client.send(java.net.http.HttpRequest.newBuilder(profileUri)
                        .timeout(java.time.Duration.ofSeconds(10)).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
        var profile = client.send(java.net.http.HttpRequest.newBuilder(profileUri)
                        .timeout(java.time.Duration.ofSeconds(10))
                        .header("Authorization", "Bearer " + tokens.generateAccessToken(owner)).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(profile.statusCode()).isEqualTo(200);
        assertThat(json.readTree(profile.body()).get("id").asText()).isEqualTo(owner.getId().toString());
    }

    @Test
    void recipeCrudPersistsChangesAndDeleteOnlyDeactivatesRecipe() throws Exception {
        var created = body(as(post("/recipes"), owner).contentType("application/json")
                .content("{\"name\":\"Soup\",\"description\":\"Dinner\",\"instructions\":\"Cook\"}"), 201);
        int recipeId = created.get("id").asInt();
        assertThat(created.get("domesticOnly").asBoolean()).isTrue();
        assertThat(created.get("active").asBoolean()).isTrue();
        assertThat(Instant.parse(created.get("createdAt").asText())).isNotNull();
        var persisted = body(as(get("/recipes/{id}", recipeId), owner), 200);
        assertThat(persisted.get("name").asText()).isEqualTo("Soup");
        assertThat(persisted.get("instructions").asText()).isEqualTo("Cook");
        mvc.perform(as(put("/recipes/{id}", recipeId), owner).contentType("application/json")
                        .content("{\"name\":\"Salad\",\"description\":\"Lunch\",\"instructions\":\"Mix\",\"domesticOnly\":false,\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Salad"))
                .andExpect(jsonPath("$.domesticOnly").value(false)).andExpect(jsonPath("$.createdAt").value(persisted.get("createdAt").asText()));
        mvc.perform(as(delete("/recipes/{id}", recipeId), owner)).andExpect(status().isOk());
        assertThat(count("recipes")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT active FROM recipes WHERE id=?", Boolean.class, recipeId)).isFalse();
        mvc.perform(as(get("/recipes/{id}", recipeId), owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false)).andExpect(jsonPath("$.name").value("Salad"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"   \"}"})
    void invalidRecipeNameReturnsBadRequestWithoutPersisting(String payload) throws Exception {
        mvc.perform(as(post("/recipes"), owner).contentType("application/json").content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        assertThat(count("recipes")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void missingRecipeReturnsNotFound(String method) throws Exception {
        var request = request(org.springframework.http.HttpMethod.valueOf(method), "/recipes/999999");
        if (method.equals("PUT")) {
            request.contentType("application/json").content("{\"name\":\"Missing\"}");
        }
        mvc.perform(as(request, owner)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        assertThat(count("recipes")).isZero();
    }

    @Test
    void anonymousRecipeCreationIsForbiddenWithoutPersisting() throws Exception {
        mvc.perform(post("/recipes").contentType("application/json").content("{\"name\":\"Soup\"}"))
                .andExpect(status().isForbidden());
        assertThat(count("recipes")).isZero();
    }

    @Test
    void unavailableAiReturnsSafeHttp503WithoutPersistingRecipeOrSuggestion() throws Exception {
        var original = (org.springframework.web.client.RestClient)
                org.springframework.test.util.ReflectionTestUtils.getField(aiClient, "restClient");
        var builder = original.mutate().baseUrl("http://ai.example.test");
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://ai.example.test/chats/existing/messages"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        org.springframework.test.util.ReflectionTestUtils.setField(aiClient, "restClient", builder.build());
        try {
            var response = mvc.perform(as(post("/ai/recipes/chat"), owner).contentType("application/json")
                            .content("{\"message\":\"Dinner?\",\"stockId\":" + stockId + ",\"sessionId\":\"existing\"}"))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(response).doesNotContain("ai.example.test", "stackTrace", "RestClient", "X-API-Key");
            assertThat(count("recipes")).isZero();
            assertThat(count("recipe_suggestions")).isZero();
            server.verify();
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(aiClient, "restClient", original);
        }
    }

    private User user(String email, String role) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO users(name,email,hash_password,birth_date,role,account_type)
                VALUES ('Test User',?,'test-hash','1990-01-01',?::user_role_enum,'DOMESTIC') RETURNING id
                """, UUID.class, email, role);
        return users.findById(id).orElseThrow();
    }

    private UUID group(User user, String name) {
        UUID id = jdbc.queryForObject("INSERT INTO groups(owner_id,name) VALUES (?,?) RETURNING id", UUID.class, user.getId(), name);
        jdbc.update("INSERT INTO user_groups(user_id,group_id) VALUES (?,?)", user.getId(), id);
        return id;
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, User user) {
        return request.header("Authorization", "Bearer " + tokens.generateAccessToken(user));
    }

    private JsonNode body(MockHttpServletRequestBuilder request, int status) throws Exception {
        return json.readTree(mvc.perform(request).andExpect(status().is(status)).andReturn().getResponse().getContentAsString());
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private int balance() {
        return jdbc.queryForObject("SELECT quantity FROM stock_products WHERE id=?", Integer.class, itemId);
    }

    private String movement(String type, int quantity) {
        return "{\"movementType\":\"" + type + "\",\"quantity\":" + quantity + "}";
    }

    private String productBody(String name) {
        return "{\"name\":\"" + name + "\",\"category\":\"GRAIN\",\"storagePlace\":\"PANTRY\",\"unitPrice\":1.25,\"unitOfMeasure\":\"UNIT\"}";
    }
}
