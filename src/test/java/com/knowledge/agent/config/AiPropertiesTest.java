package com.knowledge.agent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiPropertiesTest {

    @Test
    @DisplayName("embedding and chat share provider should reuse generic api key")
    void resolveEmbeddingApiKey_shouldReuseGenericKeyWhenProviderIsShared() {
        AiProperties properties = new AiProperties();
        properties.setBaseUrl("https://api.moonshot.cn/v1");
        properties.setApiKey("shared-key");

        assertEquals("shared-key", properties.resolveEmbeddingApiKey());
    }

    @Test
    @DisplayName("embedding with dedicated provider should require dedicated api key")
    void resolveEmbeddingApiKey_shouldNotReuseGenericKeyWhenProviderIsSeparate() {
        AiProperties properties = new AiProperties();
        properties.setBaseUrl("https://api.moonshot.cn/v1");
        properties.setApiKey("chat-key");
        properties.setEmbeddingBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");

        assertNull(properties.resolveEmbeddingApiKey());
    }

    @Test
    @DisplayName("embedding with dedicated provider should use dedicated api key")
    void resolveEmbeddingApiKey_shouldUseDedicatedEmbeddingKey() {
        AiProperties properties = new AiProperties();
        properties.setBaseUrl("https://api.moonshot.cn/v1");
        properties.setApiKey("chat-key");
        properties.setEmbeddingBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        properties.setEmbeddingApiKey("embedding-key");

        assertEquals("embedding-key", properties.resolveEmbeddingApiKey());
    }
}
