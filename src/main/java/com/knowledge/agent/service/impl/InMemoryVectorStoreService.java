package com.knowledge.agent.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.knowledge.agent.service.VectorStoreService;
import com.knowledge.agent.service.model.ChunkVector;
import com.knowledge.agent.service.model.RetrievedChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.milvus.enabled", havingValue = "false")
public class InMemoryVectorStoreService implements VectorStoreService {

    private final ConcurrentHashMap<Long, List<ChunkVector>> storage = new ConcurrentHashMap<>();

    @Override
    public void upsertChunks(List<ChunkVector> chunkVectors) {
        if (CollectionUtil.isEmpty(chunkVectors)) {
            return;
        }
        Long docId = chunkVectors.get(0).getDocId();
        storage.put(docId, new ArrayList<>(chunkVectors));
    }

    @Override
    public void deleteByDocument(Long documentId) {
        storage.remove(documentId);
    }

    @Override
    public List<RetrievedChunk> search(String kbId, List<Float> queryVector, int topK, Map<String, String> metadataFilters) {
        final Long filterDocId = metadataFilters != null && metadataFilters.containsKey("docId")
                ? Long.parseLong(metadataFilters.get("docId"))
                : null;

        return storage.values().stream()
                .flatMap(List::stream)
                .filter(chunk -> kbId.equals(chunk.getKbId()))
                .filter(chunk -> filterDocId == null || filterDocId.equals(chunk.getDocId()))
                .map(chunk -> RetrievedChunk.builder()
                        .id(chunk.getId())
                        .kbId(chunk.getKbId())
                        .docId(chunk.getDocId())
                        .chunkId(chunk.getChunkId())
                        .fileName(chunk.getFileName())
                        .content(chunk.getContent())
                        .metadata(chunk.getMetadata())
                        .score(cosineSimilarity(queryVector, chunk.getVector()))
                        .build())
                .sorted(Comparator.comparing(RetrievedChunk::getScore).reversed())
                .limit(topK)
                .toList();
    }

    private double cosineSimilarity(List<Float> left, List<Float> right) {
        if (left == null || right == null || left.size() != right.size() || left.isEmpty()) {
            return 0D;
        }

        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int i = 0; i < left.size(); i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
