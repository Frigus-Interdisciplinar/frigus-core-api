package com.frigus.coreapi.client;

import com.frigus.coreapi.dto.ai.FrigusAiPromptPayload;
import com.frigus.coreapi.dto.ai.FrigusAiRecipeResponse;
import com.frigus.coreapi.exception.ServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(OutputCaptureExtension.class)
class FrigusAiClientTest {
    private static final String SERVICE_KEY = "test-service-key-with-at-least-32-characters";
    private MockRestServiceServer server;
    private FrigusAiClient client;

    @BeforeEach
    void setUp() {
        client = new FrigusAiClient("http://ai.example.test", SERVICE_KEY, true);
        RestClient.Builder builder = ((RestClient) ReflectionTestUtils.getField(client, "restClient")).mutate();
        server = MockRestServiceServer.bindTo(builder).build();
        // Bind the existing client to an in-memory HTTP transport; no real network requests.
        ReflectionTestUtils.setField(client, "restClient", builder.build());
    }

    @AfterEach
    void verifiesEveryExpectedRequestWasSent() {
        server.verify();
    }

    @Test
    void sendsAuthenticatedUserAndCurrentStockInOneRequest() {
        expectMessage(null).andRespond(withSuccess(
                "{\"sessionId\":\"new-session\",\"chatMessage\":\"Suggestion\",\"recipeIds\":[42]}", MediaType.APPLICATION_JSON));

        var response = client.requestRecipeFromAi(payload(null));

        assertThat(response.getSessionId()).isEqualTo("new-session");
        assertThat(response.getChatMessage()).isEqualTo("Suggestion");
        assertThat(response.getRecipeName()).isNull();
        assertThat(response.getRecipeIds()).containsExactly(42);
    }

    @Test
    void reusesExistingSessionWithTheSameUserAndStock() {
        expectMessage("existing").andRespond(withSuccess("{\"sessionId\":\"existing\",\"chatMessage\":\"Reply\",\"recipeIds\":[]}", MediaType.APPLICATION_JSON));
        var response = client.requestRecipeFromAi(payload("existing"));
        assertThat(response.getSessionId()).isEqualTo("existing");
        assertThat(response.getChatMessage()).isEqualTo("Reply");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"chatMessage\":null}", "not-json",
            "{\"sessionId\":\"existing\",\"chatMessage\":\"Reply\"}",
            "{\"sessionId\":\"existing\",\"chatMessage\":\"Reply\",\"recipeIds\":[-1]}"})
    void invalidProviderResponseProducesServiceUnavailableError(String body) {
        expectMessage("existing").andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertUnavailable("existing");
    }

    @Test
    void unavailableProviderProducesServiceUnavailableError() {
        expectMessage("existing").andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertUnavailable("existing");
    }

    @Test
    void socketTimeoutProducesServiceUnavailableErrorWithoutWaitingOrRealNetwork(CapturedOutput output) {
        expectMessage("existing").andRespond(withException(new SocketTimeoutException("simulated timeout")));
        assertUnavailable("existing");
        assertThat(output.getAll()).contains("stockId=7", "java.net.SocketTimeoutException", "elapsedMs=");
    }

    @Test
    void providerStatusIsDiagnosableWithoutLeakingItsBodyOrCredentials(CapturedOutput output) {
        expectMessage("existing").andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON).body("{\"detail\":\"private-provider-body\"}"));
        assertUnavailable("existing");
        assertThat(output.getAll()).contains("upstreamStatus=422", "stockId=7", "HttpClientErrorException")
                .doesNotContain("private-provider-body", SERVICE_KEY);
    }

    @Test
    void failedChatCreationProducesServiceUnavailableErrorWithoutSendingMessage() {
        expectMessage(null).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertUnavailable(null);
    }

    @Test
    void requiredApiKeyFailsBeforeAnyRequest() {
        assertThatThrownBy(() -> new FrigusAiClient("http://ai.example.test", "", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("test-api-key");
    }

    private org.springframework.test.web.client.ResponseActions expectMessage(String session) {
        return server.expect(requestTo("http://ai.example.test/internal/core/chat"))
                .andExpect(header("X-Core-Service-Key", SERVICE_KEY))
                .andExpect(method(HttpMethod.POST)).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"userId\":\"123e4567-e89b-12d3-a456-426614174000\",\"message\":\"Dinner?\",\"stockId\":7,\"foodPreferences\":[\"VEGAN\"],\"sessionId\":" +
                        (session == null ? "null" : "\"" + session + "\"") + "}"));
    }

    private FrigusAiPromptPayload payload(String session) {
        return FrigusAiPromptPayload.builder().sessionId(session).stockId(7).message("Dinner?")
                .userId("123e4567-e89b-12d3-a456-426614174000").foodPreferences(List.of("VEGAN"))
                .availableItems(List.of(item(1, "Rice"), item(2, "Beans"), item(3, "Carrot"), item(4, "Onion"))).build();
    }

    private FrigusAiPromptPayload.StockItemPayload item(int id, String name) {
        return FrigusAiPromptPayload.StockItemPayload.builder().productId(id).productName(name).build();
    }

    private void assertUnavailable(String session) {
        assertThatThrownBy(() -> client.requestRecipeFromAi(payload(session)))
                .isInstanceOfSatisfying(ServiceUnavailableException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(503);
                    assertThat(error.getCode()).isEqualTo("SERVICE_UNAVAILABLE");
                    assertThat(error.getDisplayMessage()).isNotBlank().doesNotContain("test-api-key", "ai.example.test");
                });
    }
}
