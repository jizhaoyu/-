package com.knowledge.agent.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledge.agent.config.RagProperties;
import com.knowledge.agent.service.impl.TextChunkServiceImpl;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextChunkServiceImplTest {

    @Test
    @DisplayName("split should respect chunk size and overlap")
    void split_shouldRespectChunkSizeAndOverlap() {
        RagProperties properties = new RagProperties();
        properties.setChunkSize(10);
        properties.setChunkOverlap(2);
        TextChunkServiceImpl chunkService = new TextChunkServiceImpl(properties);

        List<String> chunks = chunkService.split("abcdefghijklmnopqrstuvwxyz");

        assertThat(chunks).containsExactly("abcdefghij", "ijklmnopqr", "qrstuvwxyz");
    }

    @Test
    @DisplayName("split should return empty when content is blank")
    void split_shouldReturnEmptyWhenBlank() {
        RagProperties properties = new RagProperties();
        TextChunkServiceImpl chunkService = new TextChunkServiceImpl(properties);

        assertThat(chunkService.split("  ")).isEmpty();
    }
}
