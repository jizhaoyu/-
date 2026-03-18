package com.knowledge.agent.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.common.ErrorCodeEnum;
import com.knowledge.agent.config.MilvusProperties;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.service.VectorStoreService;
import com.knowledge.agent.service.model.ChunkVector;
import com.knowledge.agent.service.model.RetrievedChunk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.milvus.enabled", havingValue = "true", matchIfMissing = true)
public class MilvusVectorStoreService implements VectorStoreService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final MilvusProperties milvusProperties;
    private final ObjectMapper objectMapper;

    private RestClient restClient;
    private volatile boolean collectionReady;

    @jakarta.annotation.PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);
        this.restClient = RestClient.builder()
                .baseUrl(milvusProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void upsertChunks(List<ChunkVector> chunkVectors) {
        if (CollectionUtil.isEmpty(chunkVectors)) {
            return;
        }
        ensureCollection();

        Long docId = chunkVectors.get(0).getDocId();

        List<Map<String, Object>> rows = new ArrayList<>(chunkVectors.size());
        for (ChunkVector chunk : chunkVectors) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", chunk.getId());
            row.put("kb_id", chunk.getKbId());
            row.put("doc_id", chunk.getDocId());
            row.put("chunk_id", chunk.getChunkId());
            row.put("file_name", chunk.getFileName());
            row.put("content", chunk.getContent());
            row.put("metadata", toJson(chunk.getMetadata()));
            row.put("vector", chunk.getVector());
            rows.add(row);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("collectionName", milvusProperties.getCollectionName());
        body.put("data", rows);

        deleteByDocument(docId);
        JsonNode response = post("/v2/vectordb/entities/insert", body);
        validateSuccess(response, "insert milvus chunks");
    }

    @Override
    public void deleteByDocument(Long documentId) {
        ensureCollection();

        Map<String, Object> body = new HashMap<>();
        body.put("collectionName", milvusProperties.getCollectionName());
        body.put("filter", "doc_id == " + documentId);
        JsonNode response = post("/v2/vectordb/entities/delete", body);
        validateSuccess(response, "delete milvus doc chunks");
    }

    @Override
    public List<RetrievedChunk> search(String kbId, List<Float> queryVector, int topK, Map<String, String> metadataFilters) {
        ensureCollection();

        String filter = buildFilter(kbId, metadataFilters);
        Map<String, Object> body = new HashMap<>();
        body.put("collectionName", milvusProperties.getCollectionName());
        body.put("data", List.of(queryVector));
        body.put("limit", topK);
        body.put("annsField", "vector");
        body.put("outputFields", List.of("id", "kb_id", "doc_id", "chunk_id", "file_name", "content", "metadata"));
        body.put("filter", filter);

        JsonNode response = post("/v2/vectordb/entities/search", body);
        validateSuccess(response, "search milvus chunks");

        return parseSearch(response.path("data"));
    }

    private synchronized void ensureCollection() {
        if (collectionReady) {
            return;
        }

        Map<String, Object> hasBody = Map.of("collectionName", milvusProperties.getCollectionName());
        JsonNode hasResponse = post("/v2/vectordb/collections/has", hasBody);
        boolean exists = hasResponse.path("data").path("has").asBoolean(false);

        if (!exists) {
            Map<String, Object> createBody = new HashMap<>();
            createBody.put("collectionName", milvusProperties.getCollectionName());
            createBody.put("dimension", milvusProperties.getVectorDimension());
            createBody.put("metricType", "COSINE");
            createBody.put("primaryField", "id");
            createBody.put("idType", "Int64");
            createBody.put("vectorField", "vector");
            createBody.put("autoId", false);
            createBody.put("enableDynamicField", true);
            JsonNode createResp = post("/v2/vectordb/collections/create", createBody);
            validateSuccess(createResp, "create milvus collection");
        }

        Map<String, Object> loadBody = Map.of("collectionName", milvusProperties.getCollectionName());
        JsonNode loadResp = post("/v2/vectordb/collections/load", loadBody);
        validateSuccess(loadResp, "load milvus collection");

        collectionReady = true;
    }

    private String buildFilter(String kbId, Map<String, String> metadataFilters) {
        StringBuilder expr = new StringBuilder();
        expr.append("kb_id == \"").append(kbId).append("\"");

        if (metadataFilters != null && metadataFilters.containsKey("docId")) {
            expr.append(" && doc_id == ").append(metadataFilters.get("docId"));
        }
        return expr.toString();
    }

    private List<RetrievedChunk> parseSearch(JsonNode dataNode) {
        if (dataNode == null || !dataNode.isArray()) {
            return List.of();
        }

        List<RetrievedChunk> chunks = new ArrayList<>();
        for (JsonNode node : dataNode) {
            JsonNode entityNode = node.path("entity");
            if (entityNode.isMissingNode() || entityNode.isNull()) {
                entityNode = node;
            }

            Map<String, Object> metadata = fromJson(entityNode.path("metadata").asText("{}"));
            chunks.add(RetrievedChunk.builder()
                    .id(entityNode.path("id").asLong(0L))
                    .kbId(entityNode.path("kb_id").asText())
                    .docId(entityNode.path("doc_id").asLong())
                    .chunkId(entityNode.path("chunk_id").asText())
                    .fileName(entityNode.path("file_name").asText())
                    .content(entityNode.path("content").asText())
                    .metadata(metadata)
                    .score(node.path("distance").isMissingNode() ? node.path("score").asDouble() : node.path("distance").asDouble())
                    .build());
        }
        return chunks;
    }

    private JsonNode post(String uri, Object body) {
        try {
            return restClient.post()
                    .uri(uri)
                    .headers(headers -> {
                        if (milvusProperties.getToken() != null && !milvusProperties.getToken().isBlank()) {
                            headers.setBearerAuth(milvusProperties.getToken());
                        }
                    })
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception ex) {
            throw new ServiceException(ErrorCodeEnum.VECTOR_STORE_FAILED, "milvus request failed: " + ex.getMessage());
        }
    }

    private void validateSuccess(JsonNode response, String action) {
        if (response == null) {
            throw new ServiceException(ErrorCodeEnum.VECTOR_STORE_FAILED, action + " failed: empty response");
        }
        int code = response.path("code").asInt(0);
        if (code != 0 && code != 200) {
            String message = response.path("message").asText(response.path("msg").asText("unknown"));
            log.warn("milvus action failed, action={}, code={}, message={}", action, code, message);
            throw new ServiceException(ErrorCodeEnum.VECTOR_STORE_FAILED,
                    action + " failed, code=" + code + ", message=" + message);
        }
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Map<String, Object> fromJson(String raw) {
        try {
            return objectMapper.readValue(raw, MAP_TYPE);
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
