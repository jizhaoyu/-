package com.knowledge.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.config.RagProperties;
import com.knowledge.agent.entity.dto.QueryRequestDTO;
import com.knowledge.agent.entity.vo.QueryResponseVO;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.service.impl.KnowledgeQueryServiceImpl;
import com.knowledge.agent.service.model.ChunkVector;
import com.knowledge.agent.service.model.RetrievedChunk;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class KnowledgeQueryServiceImplTest {

    private static final ObjectProvider<StringRedisTemplate> NO_REDIS_PROVIDER = new ObjectProvider<>() {
        @Override
        public StringRedisTemplate getObject(Object... args) {
            return null;
        }

        @Override
        public StringRedisTemplate getIfAvailable() {
            return null;
        }

        @Override
        public StringRedisTemplate getIfUnique() {
            return null;
        }

        @Override
        public StringRedisTemplate getObject() {
            return null;
        }

        @Override
        public Iterator<StringRedisTemplate> iterator() {
            return Collections.emptyIterator();
        }
    };

    @Test
    @DisplayName("query should return llm answer and citations")
    void query_shouldReturnAnswerAndCitations() {
        EmbeddingService embeddingService = new FixedEmbeddingService();
        VectorStoreService vectorStoreService = new FixedVectorStoreService(List.of(sampleChunk("RAG combines retrieval and generation")));
        LlmService llmService = (question, contextChunks) -> "RAG combines retrieval and generation";

        KnowledgeQueryService knowledgeQueryService = buildService(embeddingService, vectorStoreService, llmService);

        QueryResponseVO response = knowledgeQueryService.query(QueryRequestDTO.builder()
                .kbId("default-kb")
                .question("What is RAG")
                .topK(5)
                .metadataFilters(Map.of())
                .build());

        assertThat(response.getAnswer()).contains("RAG");
        assertThat(response.getCitations()).hasSize(1);
    }

    @Test
    @DisplayName("query should return reference-only degraded answer when all llm paths are unavailable")
    void query_shouldReturnReferenceOnlyDegradedAnswerWhenAllLlmPathsUnavailable() {
        EmbeddingService embeddingService = new FixedEmbeddingService();
        RetrievedChunk chunk = sampleChunk("Milvus is a vector database");
        VectorStoreService vectorStoreService = new FixedVectorStoreService(List.of(chunk));
        LlmService llmService = new LlmService() {
            @Override
            public String generateAnswer(String question, List<RetrievedChunk> contextChunks) {
                throw new RuntimeException("429 Too Many Requests");
            }

            @Override
            public String generateGeneralAnswer(String question) {
                throw new RuntimeException("overload");
            }
        };
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        KnowledgeQueryService knowledgeQueryService = buildService(
                embeddingService,
                vectorStoreService,
                llmService,
                providerOf(redisTemplate));

        QueryResponseVO response = knowledgeQueryService.query(QueryRequestDTO.builder()
                .kbId("default-kb")
                .question("What is Milvus")
                .topK(5)
                .metadataFilters(Map.of())
                .build());

        assertThat(response.getAnswer()).contains("模型服务暂时不可用");
        assertThat(response.getAnswer()).doesNotContain(chunk.getContent());
        assertThat(response.getCitations()).hasSize(1);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("query should fall back to general chat when rag llm is unavailable")
    void query_shouldFallbackToGeneralChatWhenRagLlmUnavailable() {
        EmbeddingService embeddingService = new FixedEmbeddingService();
        VectorStoreService vectorStoreService = new FixedVectorStoreService(List.of(sampleChunk("Milvus is a vector database")));
        LlmService llmService = new LlmService() {
            @Override
            public String generateAnswer(String question, List<RetrievedChunk> contextChunks) {
                throw new RuntimeException("timeout");
            }

            @Override
            public String generateGeneralAnswer(String question) {
                return "General chat fallback answer for: " + question;
            }
        };

        KnowledgeQueryService knowledgeQueryService = buildService(embeddingService, vectorStoreService, llmService);

        QueryResponseVO response = knowledgeQueryService.query(QueryRequestDTO.builder()
                .kbId("default-kb")
                .question("What is Milvus")
                .topK(5)
                .metadataFilters(Map.of())
                .build());

        assertThat(response.getAnswer()).contains("General chat fallback answer");
        assertThat(response.getCitations()).hasSize(1);
    }

    @Test
    @DisplayName("query should fall back to general chat when retrieval is empty")
    void query_shouldFallbackToGeneralChatWhenRetrievalIsEmpty() {
        EmbeddingService embeddingService = new FixedEmbeddingService();
        VectorStoreService vectorStoreService = new FixedVectorStoreService(List.of());
        LlmService llmService = new LlmService() {
            @Override
            public String generateAnswer(String question, List<RetrievedChunk> contextChunks) {
                return "unused";
            }

            @Override
            public String generateGeneralAnswer(String question) {
                return "General chat answer for: " + question;
            }
        };

        KnowledgeQueryService knowledgeQueryService = buildService(embeddingService, vectorStoreService, llmService);

        QueryResponseVO response = knowledgeQueryService.query(QueryRequestDTO.builder()
                .kbId("default-kb")
                .question("Unknown question")
                .topK(5)
                .metadataFilters(Map.of())
                .build());

        assertThat(response.getAnswer()).contains("General chat answer");
        assertThat(response.getCitations()).isEmpty();
    }

    @Test
    @DisplayName("query should return no-knowledge answer when retrieval is empty and general chat is unavailable")
    void query_shouldReturnNoKnowledgeAnswerWhenGeneralChatUnavailable() {
        EmbeddingService embeddingService = new FixedEmbeddingService();
        VectorStoreService vectorStoreService = new FixedVectorStoreService(List.of());
        LlmService llmService = (question, contextChunks) -> "unused";

        KnowledgeQueryService knowledgeQueryService = buildService(embeddingService, vectorStoreService, llmService);

        QueryResponseVO response = knowledgeQueryService.query(QueryRequestDTO.builder()
                .kbId("default-kb")
                .question("Unknown question")
                .topK(5)
                .metadataFilters(Map.of())
                .build());

        assertThat(response.getAnswer()).contains("No relevant knowledge found");
        assertThat(response.getCitations()).isEmpty();
    }

    @Test
    @DisplayName("query should reject invalid docId metadata filter")
    void query_shouldRejectInvalidDocIdMetadataFilter() {
        EmbeddingService embeddingService = new FixedEmbeddingService();
        VectorStoreService vectorStoreService = new FixedVectorStoreService(List.of());
        LlmService llmService = (question, contextChunks) -> "unused";

        KnowledgeQueryService knowledgeQueryService = buildService(embeddingService, vectorStoreService, llmService);

        assertThatThrownBy(() -> knowledgeQueryService.query(QueryRequestDTO.builder()
                .kbId("default-kb")
                .question("What is RAG")
                .topK(5)
                .metadataFilters(Map.of("docId", "abc"))
                .build()))
                .isInstanceOf(ServiceException.class)
                .hasMessage("metadataFilters.docId must be a positive integer");
    }

    @Test
    @DisplayName("query should refuse low-confidence retrieval and skip cache")
    void query_shouldRefuseLowConfidenceRetrievalAndSkipCache() {
        EmbeddingService embeddingService = new FixedEmbeddingService();
        VectorStoreService vectorStoreService = new FixedVectorStoreService(List.of(
                sampleChunk("这是一个无关片段", 0.2D),
                sampleChunk("另一个无关结果", 0.1D)));
        LlmService llmService = mock(LlmService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        KnowledgeQueryService knowledgeQueryService = buildService(
                embeddingService,
                vectorStoreService,
                llmService,
                providerOf(redisTemplate));

        QueryResponseVO response = knowledgeQueryService.query(QueryRequestDTO.builder()
                .kbId("default-kb")
                .question("前端工程师是谁")
                .topK(5)
                .metadataFilters(Map.of())
                .build());

        assertThat(response.getAnswer()).contains("未找到足够可信的知识片段");
        assertThat(response.getCitations()).hasSize(2);
        verifyNoInteractions(llmService);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("query should prioritize literal hit chunks before answer generation")
    void query_shouldPrioritizeLiteralHitChunksBeforeAnswerGeneration() {
        EmbeddingService embeddingService = new FixedEmbeddingService();
        RetrievedChunk highScoreMismatch = sampleChunk("后端工程师是李四", 0.95D, 1L, "backend.md");
        RetrievedChunk literalHit = sampleChunk("前端工程师是张三", 0.40D, 2L, "frontend-team.md");
        RetrievedChunk anotherChunk = sampleChunk("团队介绍", 0.30D, 3L, "team.md");
        VectorStoreService vectorStoreService = new FixedVectorStoreService(List.of(highScoreMismatch, literalHit, anotherChunk));
        LlmService llmService = mock(LlmService.class);
        when(llmService.generateAnswer(anyString(), anyList())).thenReturn("前端工程师是张三");

        KnowledgeQueryService knowledgeQueryService = buildService(embeddingService, vectorStoreService, llmService);

        QueryResponseVO response = knowledgeQueryService.query(QueryRequestDTO.builder()
                .kbId("default-kb")
                .question("前端工程师是谁")
                .topK(5)
                .metadataFilters(Map.of())
                .build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RetrievedChunk>> contextCaptor = ArgumentCaptor.forClass(List.class);
        verify(llmService).generateAnswer(eq("前端工程师是谁"), contextCaptor.capture());
        assertThat(contextCaptor.getValue()).isNotEmpty();
        assertThat(contextCaptor.getValue().get(0).getContent()).contains("前端工程师是张三");
        assertThat(response.getAnswer()).contains("张三");
    }

    private KnowledgeQueryService buildService(EmbeddingService embeddingService,
                                               VectorStoreService vectorStoreService,
                                               LlmService llmService) {
        return buildService(embeddingService, vectorStoreService, llmService, NO_REDIS_PROVIDER);
    }

    private KnowledgeQueryService buildService(EmbeddingService embeddingService,
                                               VectorStoreService vectorStoreService,
                                               LlmService llmService,
                                               ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        RagProperties ragProperties = new RagProperties();
        ragProperties.setMaxContextChunks(3);
        return new KnowledgeQueryServiceImpl(
                embeddingService,
                vectorStoreService,
                llmService,
                ragProperties,
                new ObjectMapper(),
                redisTemplateProvider);
    }

    private ObjectProvider<StringRedisTemplate> providerOf(StringRedisTemplate redisTemplate) {
        return new ObjectProvider<>() {
            @Override
            public StringRedisTemplate getObject(Object... args) {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getIfAvailable() {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getIfUnique() {
                return redisTemplate;
            }

            @Override
            public StringRedisTemplate getObject() {
                return redisTemplate;
            }

            @Override
            public Iterator<StringRedisTemplate> iterator() {
                return Collections.singleton(redisTemplate).iterator();
            }
        };
    }

    private RetrievedChunk sampleChunk(String content) {
        return sampleChunk(content, 0.9D);
    }

    private RetrievedChunk sampleChunk(String content, Double score) {
        return sampleChunk(content, score, 1L, "demo.md");
    }

    private RetrievedChunk sampleChunk(String content, Double score, Long docId, String fileName) {
        return RetrievedChunk.builder()
                .id(docId)
                .kbId("default-kb")
                .docId(docId)
                .chunkId(docId + "-0")
                .fileName(fileName)
                .content(content)
                .score(score)
                .build();
    }

    private static class FixedEmbeddingService implements EmbeddingService {
        @Override
        public List<Float> embed(String content) {
            return List.of(0.1F, 0.2F, 0.3F);
        }

        @Override
        public List<List<Float>> embedBatch(List<String> contentList) {
            return List.of();
        }
    }

    private static class FixedVectorStoreService implements VectorStoreService {
        private final List<RetrievedChunk> searchResult;

        private FixedVectorStoreService(List<RetrievedChunk> searchResult) {
            this.searchResult = searchResult;
        }

        @Override
        public void upsertChunks(List<ChunkVector> chunkVectors) {
        }

        @Override
        public void deleteByDocument(Long documentId) {
        }

        @Override
        public List<RetrievedChunk> search(String kbId, List<Float> queryVector, int topK, Map<String, String> metadataFilters) {
            return searchResult;
        }
    }
}
