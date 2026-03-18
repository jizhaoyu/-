package com.knowledge.agent.service;

import com.knowledge.agent.service.model.RetrievedChunk;
import java.util.List;

public interface LlmService {

    String generateAnswer(String question, List<RetrievedChunk> contextChunks);

    default String generateGeneralAnswer(String question) {
        throw new UnsupportedOperationException("general chat is not implemented");
    }
}
