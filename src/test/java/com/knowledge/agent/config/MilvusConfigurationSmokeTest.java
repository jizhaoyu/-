package com.knowledge.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MilvusConfigurationSmokeTest {

    @Test
    @DisplayName("application config should point Milvus to the REST API port")
    void applicationConfig_shouldPointToRestPort() throws IOException {
        String applicationConfig = readClasspathResource("application.yml");

        assertThat(applicationConfig).contains("http://localhost:19530");
        assertThat(applicationConfig).doesNotContain("http://localhost:9091");
    }

    @Test
    @DisplayName("local profile should keep the same Milvus REST API port")
    void localProfile_shouldUseSameRestPort() throws IOException {
        String localConfig = readClasspathResource("application-local.yml");

        assertThat(localConfig).contains("http://localhost:19530");
    }

    private String readClasspathResource(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
