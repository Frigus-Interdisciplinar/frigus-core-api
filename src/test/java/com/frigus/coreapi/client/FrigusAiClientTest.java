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

import java.net.SocketTimeoutException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class FrigusAiClientTest {
    private MockRestServiceServer server;
    private FrigusAiClient client;

    @BeforeEach
    void setUp() {
        client = new FrigusAiClient("http://ai.example.test", "test-api-key");
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
    void createsChatThenSendsMessageAndStockIdUsingProviderSession() {
        server.expect(requestTo("http://ai.example.test/chats")).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"chat_id\":\"new-session\",\"stock_id\":7}", MediaType.APPLICATION_JSON));
        expectMessage("new-session").andRespond(withSuccess(
                "{\"chat_id\":\"new-session\",\"content\":\"Suggestion\"}", MediaType.APPLICATION_JSON));

        var response = client.requestRecipeFromAi(payload(null));

        assertThat(response.getSessionId()).isEqualTo("new-session");
        assertThat(response.getChatMessage()).isEqualTo("Suggestion");
        assertThat(response.getRecipeName()).isNull();
    }

    @Test
    void reusesExistingSessionAndRetainsItWhenProviderOmitsChatId() {
        expectMessage("existing").andRespond(withSuccess("{\"content\":\"Reply\"}", MediaType.APPLICATION_JSON));
        var response = client.requestRecipeFromAi(payload("existing"));
        assertThat(response.getSessionId()).isEqualTo("existing");
        assertThat(response.getChatMessage()).isEqualTo("Reply");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"content\":null}", "not-json"})
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
    void socketTimeoutProducesServiceUnavailableErrorWithoutWaitingOrRealNetwork() {
        expectMessage("existing").andRespond(withException(new SocketTimeoutException("simulated timeout")));
        assertUnavailable("existing");
    }

    @Test
    void failedChatCreationProducesServiceUnavailableErrorWithoutSendingMessage() {
        server.expect(requestTo("http://ai.example.test/chats")).andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "test-api-key"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertUnavailable(null);
    }

    private org.springframework.test.web.client.ResponseActions expectMessage(String session) {
        return server.expect(requestTo("http://ai.example.test/chats/" + session + "/messages"))
                .andExpect(header("X-API-Key", "test-api-key"))
                .andExpect(method(HttpMethod.POST)).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"content\":\"Dinner?\",\"stock_id\":7}"));
    }

    private FrigusAiPromptPayload payload(String session) {
        return FrigusAiPromptPayload.builder().sessionId(session).stockId(7).message("Dinner?")
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
