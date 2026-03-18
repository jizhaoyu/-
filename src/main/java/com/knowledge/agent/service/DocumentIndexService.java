package com.knowledge.agent.service;

public interface DocumentIndexService {

    void indexAsync(Long documentId, String traceId);
}
