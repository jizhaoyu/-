package com.knowledge.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    @DisplayName("openapi spec should expose a concrete server url for api tools")
    void knowledgeAgentOpenApi_shouldExposeConcreteServerUrl() {
        OpenApiConfig config = new OpenApiConfig("http://localhost:8080/");

        assertThat(config.knowledgeAgentOpenApi().getServers())
                .extracting(Server::getUrl)
                .containsExactly("http://localhost:8080");
    }

    @Test
    @DisplayName("openapi spec should fallback to localhost when server url is blank")
    void knowledgeAgentOpenApi_shouldFallbackWhenServerUrlIsBlank() {
        OpenApiConfig config = new OpenApiConfig("   ");

        assertThat(config.knowledgeAgentOpenApi().getServers())
                .extracting(Server::getUrl)
                .containsExactly("http://localhost:8080");
    }
}
