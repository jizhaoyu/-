package com.knowledge.agent.service;

import com.knowledge.agent.service.model.ChunkVector;
import com.knowledge.agent.service.model.RetrievedChunk;
import java.util.List;
import java.util.Map;

public interface VectorStoreService {

    void upsertChunks(List<ChunkVector> chunkVectors);

    void deleteByDocument(Long documentId);

    List<RetrievedChunk> search(String kbId, List<Float> queryVector, int topK, Map<String, String> metadataFilters);
}
