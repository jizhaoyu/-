package com.knowledge.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.knowledge.agent.config.AiProperties;
import com.knowledge.agent.config.MilvusProperties;
import com.knowledge.agent.config.RagProperties;
import com.knowledge.agent.config.StorageProperties;
import com.knowledge.agent.entity.vo.RuntimeComponentVO;
import com.knowledge.agent.entity.vo.RuntimeOverviewVO;
import com.knowledge.agent.service.RuntimeOverviewService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class RuntimeOverviewServiceImpl implements RuntimeOverviewService {

    private static final String DEFAULT_KB_ID = "default-kb";
    private static final List<String> SUPPORTED_FILE_TYPES = List.of("pdf", "md", "txt");

    private final AiProperties aiProperties;
    private final MilvusProperties milvusProperties;
    private final RagProperties ragProperties;
    private final StorageProperties storageProperties;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Override
    public RuntimeOverviewVO getOverview() {
        return RuntimeOverviewVO.builder()
                .defaultKbId(DEFAULT_KB_ID)
                .vectorStoreMode(milvusProperties.isEnabled() ? "MILVUS" : "IN_MEMORY")
                .embeddingModel(aiProperties.getEmbeddingModel())
                .chatModel(aiProperties.getChatModel())
                .milvusCollectionName(milvusProperties.getCollectionName())
                .vectorDimension(milvusProperties.getVectorDimension())
                .chunkSize(ragProperties.getChunkSize())
                .chunkOverlap(ragProperties.getChunkOverlap())
                .storagePath(resolveStoragePath())
                .supportedFileTypes(SUPPORTED_FILE_TYPES)
                .components(List.of(
                        checkDatabase(),
                        checkStorage(),
                        checkRedis(),
                        checkMilvus(),
                        checkAi()))
                .build();
    }

    private RuntimeComponentVO checkDatabase() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return component("mysql", "MySQL", "UP", "数据库连接正常",
                    "JDBC 探测成功，返回值: " + result);
        } catch (Exception ex) {
            return component("mysql", "MySQL", "DOWN", "数据库连接失败", simplify(ex));
        }
    }

    private RuntimeComponentVO checkStorage() {
        Path storagePath = Paths.get(storageProperties.getStoragePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storagePath);
            if (!Files.isWritable(storagePath)) {
                return component("storage", "本地存储", "DOWN", "存储目录不可写", storagePath.toString());
            }
            return component("storage", "本地存储", "UP", "存储目录可写", storagePath.toString());
        } catch (Exception ex) {
            return component("storage", "本地存储", "DOWN", "存储目录不可用",
                    storagePath + " | " + simplify(ex));
        }
    }

    private RuntimeComponentVO checkRedis() {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return component("redis", "Redis", "DISABLED", "Redis Bean 未创建", "缓存会自动跳过。");
        }

        try {
            String pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            return component("redis", "Redis", "UP", "缓存连接正常",
                    StrUtil.blankToDefault(pong, "PING 未返回内容"));
        } catch (Exception ex) {
            return component("redis", "Redis", "WARN", "缓存不可用，将自动降级", simplify(ex));
        }
    }

    private RuntimeComponentVO checkMilvus() {
        if (!milvusProperties.isEnabled()) {
            return component("milvus", "Milvus", "DISABLED", "当前使用内存向量库",
                    "将查询和索引保存在当前进程内存中，重启后丢失。");
        }

        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(3000);
            requestFactory.setReadTimeout(3000);
            RestClient restClient = RestClient.builder()
                    .baseUrl(milvusProperties.getBaseUrl())
                    .requestFactory(requestFactory)
                    .build();

            JsonNode response = restClient.post()
                    .uri("/v2/vectordb/collections/has")
                    .headers(headers -> {
                        if (StrUtil.isNotBlank(milvusProperties.getToken())) {
                            headers.setBearerAuth(milvusProperties.getToken());
                        }
                    })
                    .body(Map.of("collectionName", milvusProperties.getCollectionName()))
                    .retrieve()
                    .body(JsonNode.class);

            int code = response == null ? -1 : response.path("code").asInt(0);
            if (code != 0 && code != 200) {
                String message = response == null
                        ? "response is empty"
                        : response.path("message").asText(response.path("msg").asText("unknown"));
                return component("milvus", "Milvus", "DOWN", "Milvus 探测失败",
                        "code=" + code + ", message=" + message);
            }

            boolean exists = response.path("data").path("has").asBoolean(false);
            String summary = exists ? "Milvus 已连接，Collection 已存在" : "Milvus 已连接，Collection 将按需创建";
            String detail = milvusProperties.getBaseUrl() + " | collection=" + milvusProperties.getCollectionName();
            return component("milvus", "Milvus", "UP", summary, detail);
        } catch (Exception ex) {
            return component("milvus", "Milvus", "DOWN", "Milvus 不可达", simplify(ex));
        }
    }

    private RuntimeComponentVO checkAi() {
        List<String> missingItems = new ArrayList<>();
        if (StrUtil.isBlank(aiProperties.resolveChatBaseUrl())) {
            missingItems.add("chat-base-url");
        }
        if (StrUtil.isBlank(aiProperties.resolveChatApiKey())) {
            missingItems.add("chat-api-key");
        }
        if (StrUtil.isBlank(aiProperties.resolveEmbeddingBaseUrl())) {
            missingItems.add("embedding-base-url");
        }
        if (StrUtil.isBlank(aiProperties.resolveEmbeddingApiKey())) {
            missingItems.add("embedding-api-key");
        }

        String detail = "chat=" + aiProperties.getChatModel() + " | embedding=" + aiProperties.getEmbeddingModel();
        if (missingItems.isEmpty()) {
            return component("ai", "AI Provider", "UP", "聊天与向量模型配置已就绪（仅校验配置）",
                    detail + " | probe=config-only");
        }
        return component("ai", "AI Provider", "WARN", "AI 配置不完整，真实索引或问答可能失败",
                detail + " | missing=" + String.join(", ", missingItems));
    }

    private RuntimeComponentVO component(String key, String label, String status, String summary, String detail) {
        return RuntimeComponentVO.builder()
                .key(key)
                .label(label)
                .status(status)
                .summary(summary)
                .detail(detail)
                .build();
    }

    private String resolveStoragePath() {
        return Paths.get(storageProperties.getStoragePath()).toAbsolutePath().normalize().toString();
    }

    private String simplify(Exception ex) {
        return StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName());
    }
}
