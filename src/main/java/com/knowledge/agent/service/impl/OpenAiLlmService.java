package com.knowledge.agent.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.knowledge.agent.common.ErrorCodeEnum;
import com.knowledge.agent.config.AiProperties;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.service.LlmService;
import com.knowledge.agent.service.model.RetrievedChunk;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OpenAiLlmService implements LlmService {

    private static final String RAG_SYSTEM_PROMPT =
            "You are an enterprise knowledge assistant. Answer only with provided context. "
                    + "If context is insufficient, say you do not know.";
    private static final String GENERAL_CHAT_SYSTEM_PROMPT =
            "You are a helpful enterprise AI assistant. Provide a direct and concise answer. "
                    + "If you are unsure, clearly say so.";

    private final RestClient restClient;
    private final AiProperties aiProperties;

    public OpenAiLlmService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.toIntExact(Duration.ofSeconds(aiProperties.getTimeoutSeconds()).toMillis());
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.resolveChatBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String generateAnswer(String question, List<RetrievedChunk> contextChunks) {
        String context = buildContext(contextChunks);
        String userPrompt = "Question:\n" + question + "\n\nContext:\n" + context;
        return requestChatCompletion(RAG_SYSTEM_PROMPT, userPrompt);
    }

    @Override
    public String generateGeneralAnswer(String question) {
        return requestChatCompletion(GENERAL_CHAT_SYSTEM_PROMPT, question);
    }

    private String requestChatCompletion(String systemPrompt, String userPrompt) {
        String apiKey = aiProperties.resolveChatApiKey();
        if (StrUtil.isBlank(apiKey)) {
            throw new ServiceException(ErrorCodeEnum.LLM_FAILED, "chat API key is missing");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getChatModel());
        body.put("messages", messages);
        body.put("temperature", 1);

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .body(body)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || CollectionUtil.isEmpty(response.getChoices())) {
                throw new ServiceException(ErrorCodeEnum.LLM_FAILED, "empty llm response");
            }
            String answer = response.getChoices().get(0).getMessage().getContent();
            if (StrUtil.isBlank(answer)) {
                throw new ServiceException(ErrorCodeEnum.LLM_FAILED, "llm returned blank answer");
            }
            return answer;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(ErrorCodeEnum.LLM_FAILED, "llm request failed: " + ex.getMessage());
        }
    }

    private String buildContext(List<RetrievedChunk> chunks) {
        if (CollectionUtil.isEmpty(chunks)) {
            return "No context available";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (RetrievedChunk chunk : chunks) {
            builder.append("[").append(index++).append("] ")
                    .append(chunk.getContent())
                    .append("\n---\n");
        }
        return builder.toString();
    }

    @Data
    static class ChatCompletionResponse {
        private List<Choice> choices;
    }

    @Data
    static class Choice {
        private Message message;
    }

    @Data
    static class Message {
        private String content;
    }
}
