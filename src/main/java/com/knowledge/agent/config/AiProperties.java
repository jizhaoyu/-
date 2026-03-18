package com.knowledge.agent.config;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String baseUrl;

    private String apiKey;

    private String embeddingBaseUrl;

    private String embeddingApiKey;

    @NotBlank
    private String embeddingModel;

    private String chatBaseUrl;

    private String chatApiKey;

    @NotBlank
    private String chatModel;

    @Min(5)
    private int timeoutSeconds = 30;

    public String resolveChatBaseUrl() {
        return StrUtil.blankToDefault(chatBaseUrl, baseUrl);
    }

    public String resolveChatApiKey() {
        return StrUtil.blankToDefault(chatApiKey, apiKey);
    }

    public String resolveEmbeddingBaseUrl() {
        return StrUtil.blankToDefault(embeddingBaseUrl, baseUrl);
    }

    public String resolveEmbeddingApiKey() {
        if (StrUtil.isNotBlank(embeddingApiKey)) {
            return embeddingApiKey;
        }

        String resolvedEmbeddingBaseUrl = resolveEmbeddingBaseUrl();
        String resolvedChatBaseUrl = resolveChatBaseUrl();
        if (StrUtil.isBlank(resolvedEmbeddingBaseUrl) || StrUtil.equals(resolvedEmbeddingBaseUrl, resolvedChatBaseUrl)) {
            return StrUtil.blankToDefault(apiKey, chatApiKey);
        }
        return null;
    }
}
