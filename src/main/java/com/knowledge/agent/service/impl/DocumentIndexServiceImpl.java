package com.knowledge.agent.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.knowledge.agent.common.DocumentStatus;
import com.knowledge.agent.common.ErrorCodeEnum;
import com.knowledge.agent.entity.po.KnowledgeDocument;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.mapper.KnowledgeDocumentMapper;
import com.knowledge.agent.service.DocumentIndexService;
import com.knowledge.agent.service.DocumentParserService;
import com.knowledge.agent.service.EmbeddingService;
import com.knowledge.agent.service.TextChunkService;
import com.knowledge.agent.service.VectorStoreService;
import com.knowledge.agent.service.model.ChunkVector;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexServiceImpl implements DocumentIndexService {

    private final KnowledgeDocumentMapper documentMapper;
    private final DocumentParserService documentParserService;
    private final TextChunkService textChunkService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    @Override
    @Async("indexExecutor")
    public void indexAsync(Long documentId, String traceId) {
        KnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            log.warn("index skipped, document not found, documentId={}, traceId={}", documentId, traceId);
            return;
        }

        try {
            String content = documentParserService.parse(Path.of(document.getStoragePath()), document.getSourceType());
            List<String> chunks = textChunkService.split(content);
            if (CollectionUtil.isEmpty(chunks)) {
                throw new ServiceException(ErrorCodeEnum.DOCUMENT_INDEX_FAILED, "document chunk result is empty");
            }

            List<List<Float>> vectors = embeddingService.embedBatch(chunks);
            if (vectors.size() != chunks.size()) {
                throw new ServiceException(ErrorCodeEnum.DOCUMENT_INDEX_FAILED, "chunk and embedding size mismatch");
            }

            vectorStoreService.upsertChunks(buildChunkVectors(document, chunks, vectors));

            markSuccess(document);
            log.info("index success, documentId={}, chunks={}, traceId={}", documentId, chunks.size(), traceId);
        } catch (Exception ex) {
            markFailed(document, ex.getMessage());
            log.error("index failed, documentId={}, traceId={}", documentId, traceId, ex);
        }
    }

    private List<ChunkVector> buildChunkVectors(KnowledgeDocument document, List<String> chunks, List<List<Float>> vectors) {
        return java.util.stream.IntStream.range(0, chunks.size())
                .mapToObj(index -> {
                    HashMap<String, Object> metadata = new HashMap<>();
                    metadata.put("sourceType", document.getSourceType());
                    metadata.put("fileName", document.getFileName());
                    metadata.put("chunkIndex", index);
                    metadata.put("tags", document.getTags());

                    return ChunkVector.builder()
                            .id(IdUtil.getSnowflakeNextId())
                            .kbId(document.getKbId())
                            .docId(document.getId())
                            .chunkId(document.getId() + "-" + index)
                            .fileName(document.getFileName())
                            .content(chunks.get(index))
                            .metadata(metadata)
                            .vector(vectors.get(index))
                            .build();
                })
                .toList();
    }

    private void markSuccess(KnowledgeDocument document) {
        document.setStatus(DocumentStatus.READY.name());
        document.setErrorMessage(null);
        document.setIndexedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
    }

    private void markFailed(KnowledgeDocument document, String message) {
        document.setStatus(DocumentStatus.FAILED.name());
        document.setErrorMessage(StrUtil.maxLength(message, 1000));
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
    }
}
