package com.knowledge.agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private final String serverUrl;

    public OpenApiConfig(@Value("${app.openapi.server-url}") String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Bean
    public OpenAPI knowledgeAgentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Knowledge Base Agent API")
                        .version("v1")
                        .description("RAG API for document indexing and question answering"))
                .servers(List.of(new Server()
                        .url(normalizeServerUrl(serverUrl))
                        .description("Default API server")));
    }

    private String normalizeServerUrl(String rawServerUrl) {
        if (rawServerUrl == null) {
            return "http://localhost:8080";
        }
        String trimmed = rawServerUrl.trim();
        if (trimmed.isEmpty()) {
            return "http://localhost:8080";
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
