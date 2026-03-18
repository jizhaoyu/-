package com.knowledge.agent.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledge.agent.common.DocumentStatus;
import com.knowledge.agent.entity.po.KnowledgeDocument;
import com.knowledge.agent.mapper.KnowledgeDocumentMapper;
import com.knowledge.agent.service.DocumentParserService;
import com.knowledge.agent.service.EmbeddingService;
import com.knowledge.agent.service.TextChunkService;
import com.knowledge.agent.service.VectorStoreService;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentIndexServiceImplTest {

    @Mock
    private KnowledgeDocumentMapper documentMapper;

    @Mock
    private DocumentParserService documentParserService;

    @Mock
    private TextChunkService textChunkService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorStoreService vectorStoreService;

    private DocumentIndexServiceImpl documentIndexService;

    @BeforeEach
    void setUp() {
        documentIndexService = new DocumentIndexServiceImpl(
                documentMapper,
                documentParserService,
                textChunkService,
                embeddingService,
                vectorStoreService);
    }

    @Test
    @DisplayName("indexAsync should not delete vectors when parse fails")
    void indexAsync_shouldNotDeleteVectorsWhenParseFails() {
        KnowledgeDocument document = sampleDocument();
        when(documentMapper.selectById(1001L)).thenReturn(document);
        when(documentParserService.parse(Path.of(document.getStoragePath()), document.getSourceType()))
                .thenThrow(new RuntimeException("parse failed"));

        documentIndexService.indexAsync(1001L, "trace-1");

        verify(vectorStoreService, never()).deleteByDocument(any());
        verify(vectorStoreService, never()).upsertChunks(any());
        verify(documentMapper).updateById(document);
    }

    @Test
    @DisplayName("indexAsync should not delete vectors when embedding fails")
    void indexAsync_shouldNotDeleteVectorsWhenEmbeddingFails() {
        KnowledgeDocument document = sampleDocument();
        when(documentMapper.selectById(1001L)).thenReturn(document);
        when(documentParserService.parse(Path.of(document.getStoragePath()), document.getSourceType()))
                .thenReturn("chunk source");
        when(textChunkService.split("chunk source")).thenReturn(List.of("chunk-1"));
        when(embeddingService.embedBatch(List.of("chunk-1"))).thenThrow(new RuntimeException("embedding failed"));

        documentIndexService.indexAsync(1001L, "trace-2");

        verify(vectorStoreService, never()).deleteByDocument(any());
        verify(vectorStoreService, never()).upsertChunks(any());
        verify(documentMapper).updateById(document);
    }

    @Test
    @DisplayName("indexAsync should mark document failed when upsert fails without pre-delete")
    void indexAsync_shouldMarkDocumentFailedWhenUpsertFailsWithoutPreDelete() {
        KnowledgeDocument document = sampleDocument();
        when(documentMapper.selectById(1001L)).thenReturn(document);
        when(documentParserService.parse(Path.of(document.getStoragePath()), document.getSourceType()))
                .thenReturn("chunk source");
        when(textChunkService.split("chunk source")).thenReturn(List.of("chunk-1"));
        when(embeddingService.embedBatch(List.of("chunk-1"))).thenReturn(List.of(List.of(0.1F, 0.2F)));
        doThrow(new RuntimeException("milvus insert failed"))
                .when(vectorStoreService)
                .upsertChunks(any());

        documentIndexService.indexAsync(1001L, "trace-3");

        verify(vectorStoreService, never()).deleteByDocument(any());
        verify(vectorStoreService).upsertChunks(any());

        ArgumentCaptor<KnowledgeDocument> documentCaptor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(documentMapper).updateById(documentCaptor.capture());
        KnowledgeDocument updated = documentCaptor.getValue();
        assertThat(updated.getStatus()).isEqualTo(DocumentStatus.FAILED.name());
        assertThat(updated.getErrorMessage()).contains("milvus insert failed");
    }

    private KnowledgeDocument sampleDocument() {
        return KnowledgeDocument.builder()
                .id(1001L)
                .kbId("default-kb")
                .fileName("demo.md")
                .sourceType("md")
                .storagePath("D:/data/storage/default-kb/demo.md")
                .status(DocumentStatus.PROCESSING.name())
                .build();
    }
}
