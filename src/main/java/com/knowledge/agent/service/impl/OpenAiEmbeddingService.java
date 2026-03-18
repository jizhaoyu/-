package com.knowledge.agent.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.knowledge.agent.common.ErrorCodeEnum;
import com.knowledge.agent.config.AiProperties;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.service.EmbeddingService;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

@Service
public class OpenAiEmbeddingService implements EmbeddingService {

    private final RestClient restClient;
    private final AiProperties aiProperties;

    public OpenAiEmbeddingService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.toIntExact(Duration.ofSeconds(aiProperties.getTimeoutSeconds()).toMillis());
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.resolveEmbeddingBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<Float> embed(String content) {
        List<List<Float>> vectors = embedBatch(List.of(content));
        return vectors.get(0);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> contentList) {
        if (CollectionUtil.isEmpty(contentList)) {
            return List.of();
        }
        String apiKey = aiProperties.resolveEmbeddingApiKey();
        if (StrUtil.isBlank(apiKey)) {
            throw new ServiceException(ErrorCodeEnum.EMBEDDING_FAILED, "embedding API key is missing");
        }

        Map<String, Object> body = Map.of(
                "model", aiProperties.getEmbeddingModel(),
                "input", contentList
        );

        try {
            EmbeddingResponse response = restClient.post()
                    .uri("/embeddings")
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .body(body)
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null || CollectionUtil.isEmpty(response.getData())) {
                throw new ServiceException(ErrorCodeEnum.EMBEDDING_FAILED, "empty embedding response");
            }

            return response.getData().stream()
                    .sorted(Comparator.comparing(EmbeddingData::getIndex))
                    .map(EmbeddingData::getEmbedding)
                    .toList();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 403
                    && StrUtil.containsIgnoreCase(ex.getResponseBodyAsString(), "The API you are accessing is not open")) {
                throw new ServiceException(
                        ErrorCodeEnum.EMBEDDING_FAILED,
                        "embedding endpoint is not available for the current provider, "
                                + "please configure app.ai.embedding-base-url / app.ai.embedding-api-key / "
                                + "app.ai.embedding-model separately");
            }
            throw new ServiceException(ErrorCodeEnum.EMBEDDING_FAILED, "embedding request failed: " + ex.getMessage());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(ErrorCodeEnum.EMBEDDING_FAILED, "embedding request failed: " + ex.getMessage());
        }
    }

    @Data
    static class EmbeddingResponse {
        private List<EmbeddingData> data;
    }

    @Data
    static class EmbeddingData {
        private Integer index;
        private List<Float> embedding;
    }
}
