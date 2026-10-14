package com.frigus.coreapi.integration;

import com.frigus.coreapi.client.FrigusAiClient;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.security.TokenProvider;
import com.frigus.coreapi.service.RefreshTokenService;
import com.frigus.coreapi.service.SubscriptionBillingJobService;
import com.frigus.coreapi.service.TransactionQueueProducer;
import com.frigus.coreapi.service.TransactionWorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.open-in-view=false",
        "aws.s3.enabled=false", "app.swagger.open-on-startup=false", "logging.level.root=WARN"
})
class MoneySavingHttpIntegrationTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("money_http_test")
            .withStartupTimeout(java.time.Duration.ofMinutes(3))
            .withCopyFileToContainer(MountableFile.forHostPath("db/script.sql"), "/docker-entrypoint-initdb.d/001-schema.sql");
    @Container static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.url", () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired TokenProvider tokens;
    @Autowired ObjectMapper json;
    @MockitoBean FrigusAiClient aiClient;
    @MockitoBean RefreshTokenService refreshTokens;
    @MockitoBean TransactionQueueProducer queue;
    @MockitoBean TransactionWorkerService worker;
    @MockitoBean SubscriptionBillingJobService billing;

    MockMvc mvc;
    User owner;
    User outsider;
    UUID groupId;
    int stockId;
    int productId;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.execute("TRUNCATE users, products, recipes RESTART IDENTITY CASCADE");
        owner = user("owner-money@test.local");
        outsider = user("outsider-money@test.local");
        groupId = jdbc.queryForObject("INSERT INTO groups(owner_id,name) VALUES (?, 'Home') RETURNING id", UUID.class, owner.getId());
        jdbc.update("INSERT INTO user_groups(user_id,group_id) VALUES (?,?)", owner.getId(), groupId);
        stockId = jdbc.queryForObject("INSERT INTO stocks(group_id,name) VALUES (?, 'Fridge') RETURNING id", Integer.class, groupId);
        productId = jdbc.queryForObject("""
                INSERT INTO products(name,category,storage_place,unit_price,unit_of_measure)
                VALUES ('Milk','DAIRY','FRIDGE',99.00,'UNIT') RETURNING id
                """, Integer.class);
    }

    @Test
    void purchaseConsumptionAndReportsAreScopedToGroup() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        String purchaseBody = """
                {"purchaseDate":"%s","source":"USER_CONFIRMED","lines":[
                  {"stockId":%d,"productId":%d,"expireDate":"2099-01-01","quantity":2,"unitPrice":4.00}]}
                """.formatted(today, stockId, productId);
        String prefix = "/groups/" + groupId;
        JsonNode purchase = body(as(post(prefix + "/food-purchases"), owner)
                .header("Idempotency-Key", "http-receipt").contentType("application/json").content(purchaseBody), 201);
        int itemId = purchase.get("lines").get(0).get("stockProductId").asInt();
        body(as(post("/stock-products/" + itemId + "/movements"), owner).contentType("application/json")
                .content("{\"movementType\":\"OUT\",\"quantity\":1,\"reasonCode\":\"CONSUMED\"}"), 201);
        String dates = "?from=" + today + "&to=" + today.plusDays(1);
        JsonNode summary = body(as(get(prefix + "/money-saving/summary" + dates), owner), 200);
        assertThat(summary.get("spending").get("amount").asDouble()).isEqualTo(8.0);
        assertThat(summary.get("saving").get("amount").asDouble()).isEqualTo(4.0);
        JsonNode events = body(as(get(prefix + "/money-saving/events" + dates + "&kind=CONSUMED"), owner), 200);
        assertThat(events.get("totalElements").asInt()).isEqualTo(1);
        mvc.perform(as(get(prefix + "/money-saving/summary" + dates), outsider)).andExpect(status().isForbidden());
        mvc.perform(as(get(prefix + "/food-purchases/" + purchase.get("id").asString()), outsider))
                .andExpect(status().isForbidden());
    }

    private User user(String email) {
        UUID id = jdbc.queryForObject("""
                INSERT INTO users(name,email,hash_password,birth_date,role,account_type)
                VALUES ('Test',?,'hash','1990-01-01','USER','DOMESTIC') RETURNING id
                """, UUID.class, email);
        return users.findById(id).orElseThrow();
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, User user) {
        return request.header("Authorization", "Bearer " + tokens.generateAccessToken(user));
    }

    private JsonNode body(MockHttpServletRequestBuilder request, int expected) throws Exception {
        return json.readTree(mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString());
    }
}
