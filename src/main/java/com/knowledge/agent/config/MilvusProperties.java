package com.knowledge.agent.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.milvus")
public class MilvusProperties {

    private boolean enabled = false;

    @NotBlank
    private String baseUrl;

    private String token;

    @NotBlank
    private String collectionName;

    @Min(64)
    private int vectorDimension = 1536;
}
