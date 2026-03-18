package com.knowledge.agent.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.common.ErrorCodeEnum;
import com.knowledge.agent.config.RagProperties;
import com.knowledge.agent.entity.dto.QueryRequestDTO;
import com.knowledge.agent.entity.vo.CitationVO;
import com.knowledge.agent.entity.vo.QueryResponseVO;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.service.EmbeddingService;
import com.knowledge.agent.service.KnowledgeQueryService;
import com.knowledge.agent.service.LlmService;
import com.knowledge.agent.service.VectorStoreService;
import com.knowledge.agent.service.model.RetrievedChunk;
import com.knowledge.agent.util.TraceIdUtil;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeQueryServiceImpl implements KnowledgeQueryService {

    private static final double LOW_CONFIDENCE_SCORE_THRESHOLD = 0.35D;
    private static final List<String> ENTITY_QUESTION_MARKERS = List.of("是谁", "什么", "哪位", "哪个");
    private static final String LOW_CONFIDENCE_ANSWER = "未找到足够可信的知识片段，请核对文档后重试。以下引用仅供人工核对。";
    private static final String DEGRADED_ANSWER = "模型服务暂时不可用，未生成最终答案；以下引用片段仅供人工核对。";

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final LlmService llmService;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Override
    public QueryResponseVO query(QueryRequestDTO requestDTO) {
        long start = System.currentTimeMillis();
        String traceId = TraceIdUtil.newTraceId();
        Map<String, String> metadataFilters = normalizeMetadataFilters(requestDTO.getMetadataFilters());
        String cacheKey = buildCacheKey(requestDTO.getKbId(), requestDTO.getQuestion(), requestDTO.getTopK(), metadataFilters);

        QueryResponseVO cached = queryFromCache(cacheKey);
        if (cached != null) {
            cached.setTraceId(traceId);
            return cached;
        }

        List<Float> queryVector = embeddingService.embed(requestDTO.getQuestion());
        List<RetrievedChunk> retrievedChunks = vectorStoreService.search(
                requestDTO.getKbId(),
                queryVector,
                requestDTO.getTopK(),
                metadataFilters);

        List<RetrievedChunk> contextChunks = selectContextChunks(requestDTO.getQuestion(), retrievedChunks);

        AnswerResult answerResult = generateAnswerWithFallback(requestDTO.getQuestion(), contextChunks);

        QueryResponseVO response = QueryResponseVO.builder()
                .answer(answerResult.answer())
                .citations(toCitations(contextChunks))
                .latencyMs(System.currentTimeMillis() - start)
                .traceId(traceId)
                .build();

        if (answerResult.cacheable()) {
            putCache(cacheKey, response);
        }
        return response;
    }

    private AnswerResult generateAnswerWithFallback(String question, List<RetrievedChunk> contextChunks) {
        if (CollectionUtil.isEmpty(contextChunks)) {
            try {
                return AnswerResult.nonCacheable(llmService.generateGeneralAnswer(question));
            } catch (Exception ex) {
                log.warn("general chat unavailable, return no-knowledge answer: {}", ex.getMessage());
                return AnswerResult.nonCacheable("No relevant knowledge found. Please upload related documents and retry.");
            }
        }

        List<String> keywords = extractKeywords(question);
        if (shouldRefuseLowConfidence(contextChunks, keywords)) {
            log.info("query refused due to low-confidence retrieval, question={}", question);
            return AnswerResult.nonCacheable(LOW_CONFIDENCE_ANSWER);
        }

        try {
            return AnswerResult.cacheable(llmService.generateAnswer(question, contextChunks));
        } catch (Exception ex) {
            log.warn("rag llm unavailable, fallback to general chat: {}", ex.getMessage());
            try {
                return AnswerResult.nonCacheable(llmService.generateGeneralAnswer(question));
            } catch (Exception generalChatEx) {
                log.warn("general chat unavailable, return degraded reference-only answer: {}", generalChatEx.getMessage());
                return AnswerResult.nonCacheable(DEGRADED_ANSWER);
            }
        }
    }

    private List<RetrievedChunk> selectContextChunks(String question, List<RetrievedChunk> retrievedChunks) {
        if (CollectionUtil.isEmpty(retrievedChunks)) {
            return List.of();
        }

        List<String> keywords = extractKeywords(question);
        Comparator<RetrievedChunk> byScoreDesc = Comparator.comparing(
                chunk -> chunk.getScore() == null ? Double.NEGATIVE_INFINITY : chunk.getScore(),
                Comparator.reverseOrder());

        List<RetrievedChunk> literalMatches = retrievedChunks.stream()
                .filter(chunk -> matchesAnyKeyword(chunk, keywords))
                .sorted(byScoreDesc)
                .toList();

        if (CollectionUtil.isNotEmpty(literalMatches)) {
            LinkedHashSet<RetrievedChunk> ordered = new LinkedHashSet<>(literalMatches);
            retrievedChunks.stream()
                    .sorted(byScoreDesc)
                    .forEach(ordered::add);
            return ordered.stream().limit(ragProperties.getMaxContextChunks()).toList();
        }

        return retrievedChunks.stream()
                .limit(ragProperties.getMaxContextChunks())
                .toList();
    }

    private boolean shouldRefuseLowConfidence(List<RetrievedChunk> contextChunks, List<String> keywords) {
        if (CollectionUtil.isEmpty(contextChunks)) {
            return false;
        }
        if (contextChunks.stream().anyMatch(chunk -> matchesAnyKeyword(chunk, keywords))) {
            return false;
        }
        Double topScore = contextChunks.get(0).getScore();
        return topScore == null || topScore < LOW_CONFIDENCE_SCORE_THRESHOLD;
    }

    private boolean matchesAnyKeyword(RetrievedChunk chunk, List<String> keywords) {
        if (CollectionUtil.isEmpty(keywords) || chunk == null) {
            return false;
        }
        String haystack = normalizeForMatch(StrUtil.blankToDefault(chunk.getContent(), "") + " "
                + StrUtil.blankToDefault(chunk.getFileName(), ""));
        return keywords.stream().anyMatch(haystack::contains);
    }

    private List<String> extractKeywords(String question) {
        String normalizedQuestion = normalizeForMatch(question);
        if (StrUtil.isBlank(normalizedQuestion)) {
            return List.of();
        }

        String stripped = normalizedQuestion;
        for (String marker : ENTITY_QUESTION_MARKERS) {
            stripped = stripped.replace(marker, " ");
        }
        for (String stopWord : List.of("是", "的", "了", "吗", "呢")) {
            stripped = stripped.replace(stopWord, " ");
        }

        List<String> tokens = StrUtil.splitTrim(stripped, ' ');
        if (CollectionUtil.isNotEmpty(tokens)) {
            List<String> keywords = tokens.stream()
                    .map(String::trim)
                    .filter(token -> token.length() >= 2)
                    .distinct()
                    .toList();
            if (CollectionUtil.isNotEmpty(keywords)) {
                return keywords;
            }
        }
        return List.of(normalizedQuestion.replace(" ", ""));
    }

    private String normalizeForMatch(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String normalized = StrUtil.cleanBlank(text).toLowerCase();
        normalized = normalized.replaceAll("[，。！？；：,.!?;:\\\"'()（）\\[\\]【】{}<>《》/\\\\\\-_=+]", " ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private List<CitationVO> toCitations(List<RetrievedChunk> chunks) {
        if (CollectionUtil.isEmpty(chunks)) {
            return List.of();
        }
        return chunks.stream()
                .map(chunk -> CitationVO.builder()
                        .docId(String.valueOf(chunk.getDocId()))
                        .chunkId(chunk.getChunkId())
                        .fileName(chunk.getFileName())
                        .score(chunk.getScore())
                        .snippet(StrUtil.maxLength(chunk.getContent(), 300))
                        .build())
                .toList();
    }

    static String buildCacheKey(QueryRequestDTO requestDTO) {
        return buildCacheKey(
                requestDTO.getKbId(),
                requestDTO.getQuestion(),
                requestDTO.getTopK(),
                normalizeMetadataFilters(requestDTO.getMetadataFilters()));
    }

    private static String buildCacheKey(String kbId, String question, Integer topK, Map<String, String> metadataFilters) {
        TreeMap<String, String> normalizedFilters = new TreeMap<>(metadataFilters);
        String fingerprint = kbId
                + "|"
                + question
                + "|"
                + topK
                + "|"
                + normalizedFilters;
        return "kb:query:" + DigestUtils.md5DigestAsHex(fingerprint.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, String> normalizeMetadataFilters(Map<String, String> metadataFilters) {
        if (metadataFilters == null || metadataFilters.isEmpty()) {
            return Map.of();
        }

        String rawDocId = metadataFilters.get("docId");
        if (StrUtil.isBlank(rawDocId)) {
            return Map.of();
        }

        String normalizedDocId = normalizePositiveLong(rawDocId, "metadataFilters.docId");
        return Map.of("docId", normalizedDocId);
    }

    private static String normalizePositiveLong(String rawValue, String fieldName) {
        try {
            long value = Long.parseLong(StrUtil.trim(rawValue));
            if (value <= 0) {
                throw new NumberFormatException("value must be positive");
            }
            return String.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new ServiceException(ErrorCodeEnum.BAD_REQUEST, fieldName + " must be a positive integer");
        }
    }

    private QueryResponseVO queryFromCache(String cacheKey) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return null;
        }
        try {
            String cacheValue = redisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isBlank(cacheValue)) {
                return null;
            }
            return objectMapper.readValue(cacheValue, QueryResponseVO.class);
        } catch (Exception ex) {
            log.debug("query cache failed: {}", ex.getMessage());
            return null;
        }
    }

    private void putCache(String cacheKey, QueryResponseVO response) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofMinutes(10));
        } catch (JsonProcessingException ex) {
            log.debug("serialize cache failed: {}", ex.getMessage());
        } catch (Exception ex) {
            log.debug("write cache failed: {}", ex.getMessage());
        }
    }

    private record AnswerResult(String answer, boolean cacheable) {

        private static AnswerResult cacheable(String answer) {
            return new AnswerResult(answer, true);
        }

        private static AnswerResult nonCacheable(String answer) {
            return new AnswerResult(answer, false);
        }
    }
}
