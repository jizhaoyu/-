package com.knowledge.agent.service;

import java.util.List;

public interface EmbeddingService {

    List<Float> embed(String content);

    List<List<Float>> embedBatch(List<String> contentList);
}
