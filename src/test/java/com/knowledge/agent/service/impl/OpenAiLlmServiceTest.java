package com.knowledge.agent.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.config.AiProperties;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.service.model.RetrievedChunk;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenAiLlmServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("generate answer should parse json body even when content type is octet-stream")
    void generateAnswer_shouldParseJsonBodyEvenWhenContentTypeIsOctetStream() throws Exception {
        AtomicReference<String> requestBodyRef = new AtomicReference<>();
        AtomicReference<String> acceptHeaderRef = new AtomicReference<>();
        AtomicReference<String> contentTypeHeaderRef = new AtomicReference<>();
        startServer(exchange -> {
            requestBodyRef.set(readRequestBody(exchange));
            acceptHeaderRef.set(exchange.getRequestHeaders().getFirst("Accept"));
            contentTypeHeaderRef.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            writeResponse(exchange,
                    200,
                    "application/octet-stream",
                    "{\"choices\":[{\"message\":{\"content\":\"Hello from knowledge assistant.\"}}]}");
        });

        OpenAiLlmService service = buildService();

        String answer = service.generateAnswer("hello", List.of(sampleChunk("hello from context")));

        assertThat(answer).isEqualTo("Hello from knowledge assistant.");
        assertThat(requestBodyRef.get()).contains("\"model\":\"test-model\"");
        assertThat(requestBodyRef.get()).contains("\"Question:\\nhello");
        assertThat(acceptHeaderRef.get()).contains("application/json");
        assertThat(contentTypeHeaderRef.get()).contains("application/json");
    }

    @Test
    @DisplayName("generate general answer should expose provider error in json body")
    void generateGeneralAnswer_shouldExposeProviderErrorInJsonBody() throws Exception {
        startServer(exchange -> writeResponse(exchange,
                200,
                "application/octet-stream",
                "{\"error\":{\"message\":\"The engine is currently overloaded, please try again later\",\"type\":\"engine_overloaded_error\"}}"));

        OpenAiLlmService service = buildService();

        assertThatThrownBy(() -> service.generateGeneralAnswer("hello"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("engine_overloaded_error")
                .hasMessageContaining("currently overloaded");
    }

    private OpenAiLlmService buildService() {
        AiProperties properties = new AiProperties();
        properties.setChatBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setChatApiKey("test-key");
        properties.setChatModel("test-model");
        properties.setTimeoutSeconds(5);
        return new OpenAiLlmService(properties, new ObjectMapper());
    }

    private RetrievedChunk sampleChunk(String content) {
        return RetrievedChunk.builder()
                .id(1L)
                .kbId("default-kb")
                .docId(1L)
                .chunkId("1-0")
                .fileName("demo.txt")
                .content(content)
                .score(0.9D)
                .build();
    }

    private void startServer(ServerResponseHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void writeResponse(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, payload.length);
        exchange.getResponseBody().write(payload);
    }

    @FunctionalInterface
    private interface ServerResponseHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}