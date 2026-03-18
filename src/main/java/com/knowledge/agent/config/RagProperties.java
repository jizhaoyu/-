package com.knowledge.agent.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    @Min(200)
    private int chunkSize = 800;

    @Min(0)
    private int chunkOverlap = 120;

    @Min(1)
    private int maxContextChunks = 6;
}
