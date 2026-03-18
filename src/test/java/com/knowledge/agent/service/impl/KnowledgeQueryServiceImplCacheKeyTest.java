package com.knowledge.agent.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledge.agent.entity.dto.QueryRequestDTO;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KnowledgeQueryServiceImplCacheKeyTest {

    @Test
    @DisplayName("buildCacheKey should include metadata filters")
    void buildCacheKey_shouldIncludeMetadataFilters() {
        QueryRequestDTO baseRequest = QueryRequestDTO.builder()
                .kbId("default-kb")
                .question("What is Milvus?")
                .topK(5)
                .metadataFilters(Map.of("docId", "101"))
                .build();
        QueryRequestDTO anotherRequest = QueryRequestDTO.builder()
                .kbId("default-kb")
                .question("What is Milvus?")
                .topK(5)
                .metadataFilters(Map.of("docId", "202"))
                .build();

        String baseKey = KnowledgeQueryServiceImpl.buildCacheKey(baseRequest);
        String anotherKey = KnowledgeQueryServiceImpl.buildCacheKey(anotherRequest);

        assertThat(baseKey).isNotEqualTo(anotherKey);
    }
}
