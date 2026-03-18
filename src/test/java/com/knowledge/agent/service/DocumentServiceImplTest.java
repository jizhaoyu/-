package com.knowledge.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.agent.common.DocumentStatus;
import com.knowledge.agent.common.ErrorCodeEnum;
import com.knowledge.agent.common.PageResult;
import com.knowledge.agent.entity.dto.DocumentListQueryDTO;
import com.knowledge.agent.entity.dto.DocumentUploadDTO;
import com.knowledge.agent.entity.po.KnowledgeDocument;
import com.knowledge.agent.entity.vo.DocumentContentVO;
import com.knowledge.agent.entity.vo.DocumentDetailVO;
import com.knowledge.agent.entity.vo.DocumentUploadVO;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.mapper.KnowledgeDocumentMapper;
import com.knowledge.agent.service.impl.DocumentServiceImpl;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private KnowledgeDocumentMapper documentMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentIndexService documentIndexService;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private DocumentParserService documentParserService;

    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(documentMapper, fileStorageService, documentIndexService, vectorStoreService, documentParserService);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), KnowledgeDocument.class);
    }

    @Test
    @DisplayName("list documents should apply filters and map page result")
    void listDocuments_shouldApplyFiltersAndMapPageResult() {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(1001L)
                .kbId("default-kb")
                .fileName("demo.txt")
                .sourceType("txt")
                .status(DocumentStatus.READY.name())
                .tags("spring,rag")
                .createdAt(now)
                .updatedAt(now)
                .indexedAt(now)
                .build();

        when(documentMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<KnowledgeDocument> page = invocation.getArgument(0);
            page.setTotal(1L);
            page.setRecords(List.of(document));
            return page;
        });

        PageResult<DocumentDetailVO> result = documentService.listDocuments(DocumentListQueryDTO.builder()
                .kbId("default-kb")
                .status(DocumentStatus.READY.name())
                .fileName("demo")
                .pageNum(2)
                .pageSize(5)
                .build());

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getPageNum()).isEqualTo(2L);
        assertThat(result.getPageSize()).isEqualTo(5L);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getFileName()).isEqualTo("demo.txt");
        assertThat(result.getRecords().get(0).getTags()).containsExactly("spring", "rag");

        ArgumentCaptor<Page<KnowledgeDocument>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<LambdaQueryWrapper<KnowledgeDocument>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);

        verify(documentMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(5L);
        assertThat(wrapperCaptor.getValue().getSqlSegment())
                .contains("kb_id")
                .contains("status")
                .contains("file_name")
                .contains("created_at");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values())
                .contains("default-kb", DocumentStatus.READY.name());
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values().stream()
                .map(String::valueOf)
                .anyMatch(value -> value.contains("demo")))
                .isTrue();
    }

    @Test
    @DisplayName("get document content should parse stored file")
    void getDocumentContent_shouldParseStoredFile() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(1001L)
                .storagePath("D:/data/storage/default-kb/demo.txt")
                .sourceType("txt")
                .build();
        when(documentMapper.selectById(1001L)).thenReturn(document);
        when(documentParserService.parse(Path.of("D:/data/storage/default-kb/demo.txt"), "txt")).thenReturn("hello world");

        DocumentContentVO result = documentService.getDocumentContent(1001L);

        assertThat(result.getDocumentId()).isEqualTo("1001");
        assertThat(result.getContent()).isEqualTo("hello world");
    }

    @Test
    @DisplayName("get document content should throw when document missing")
    void getDocumentContent_shouldThrowWhenDocumentMissing() {
        when(documentMapper.selectById(1001L)).thenReturn(null);

        assertThatThrownBy(() -> documentService.getDocumentContent(1001L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("document not found: 1001");

        verify(documentParserService, never()).parse(any(), anyString());
    }

    @Test
    @DisplayName("get document content should propagate parser failure")
    void getDocumentContent_shouldPropagateParserFailure() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(1001L)
                .storagePath("D:/data/storage/default-kb/demo.txt")
                .sourceType("txt")
                .build();
        when(documentMapper.selectById(1001L)).thenReturn(document);
        when(documentParserService.parse(Path.of("D:/data/storage/default-kb/demo.txt"), "txt"))
                .thenThrow(new ServiceException(ErrorCodeEnum.DOCUMENT_PARSE_FAILED, "parse file failed"));

        assertThatThrownBy(() -> documentService.getDocumentContent(1001L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("parse file failed");
    }

    @Test
    @DisplayName("delete document should remove vector data file and database record")
    void deleteDocument_shouldRemoveVectorDataFileAndDatabaseRecord() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(1001L)
                .storagePath("D:/data/storage/default-kb/demo.txt")
                .build();
        when(documentMapper.selectById(1001L)).thenReturn(document);
        when(documentMapper.deleteById(1001L)).thenReturn(1);

        documentService.deleteDocument(1001L);

        InOrder inOrder = inOrder(documentMapper, vectorStoreService, fileStorageService);
        inOrder.verify(documentMapper).selectById(1001L);
        inOrder.verify(vectorStoreService).deleteByDocument(1001L);
        inOrder.verify(fileStorageService).delete("D:/data/storage/default-kb/demo.txt");
        inOrder.verify(documentMapper).deleteById(1001L);
    }

    @Test
    @DisplayName("delete document should stop when vector store deletion fails")
    void deleteDocument_shouldStopWhenVectorStoreDeletionFails() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(1001L)
                .storagePath("D:/data/storage/default-kb/demo.txt")
                .build();
        when(documentMapper.selectById(1001L)).thenReturn(document);
        doThrow(new ServiceException(ErrorCodeEnum.VECTOR_STORE_FAILED, "delete vector failed"))
                .when(vectorStoreService)
                .deleteByDocument(1001L);

        assertThatThrownBy(() -> documentService.deleteDocument(1001L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("delete vector failed");

        verify(fileStorageService, never()).delete(anyString());
        verify(documentMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("upload document should trigger indexing only after transaction commit")
    void uploadDocument_shouldTriggerIndexingOnlyAfterTransactionCommit() {
        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", "hello world".getBytes());
        DocumentUploadDTO dto = DocumentUploadDTO.builder()
                .kbId("default-kb")
                .tags("demo,test")
                .file(file)
                .build();

        when(fileStorageService.store(any(), anyString())).thenReturn(Path.of("D:/data/storage/default-kb/demo.txt"));
        when(fileStorageService.detectSourceType("demo.txt")).thenReturn("txt");
        when(documentMapper.insert(any(KnowledgeDocument.class))).thenAnswer(invocation -> {
            KnowledgeDocument document = invocation.getArgument(0);
            document.setId(1001L);
            return 1;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            DocumentUploadVO result = documentService.uploadDocument(dto);

            assertThat(result.getDocumentId()).isEqualTo("1001");
            verify(documentIndexService, never()).indexAsync(anyLong(), anyString());

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            verify(documentIndexService).indexAsync(eq(1001L), anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("upload document should reject empty file")
    void uploadDocument_shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", new byte[0]);
        DocumentUploadDTO dto = DocumentUploadDTO.builder()
                .kbId("default-kb")
                .file(file)
                .build();

        assertThatThrownBy(() -> documentService.uploadDocument(dto))
                .isInstanceOf(ServiceException.class)
                .hasMessage("file cannot be empty");

        verify(fileStorageService, never()).store(any(), anyString());
    }

    @Test
    @DisplayName("reindex document should trigger indexing only after transaction commit")
    void reindexDocument_shouldTriggerIndexingOnlyAfterTransactionCommit() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(1001L)
                .status(DocumentStatus.FAILED.name())
                .errorMessage("embedding API key is missing")
                .build();
        when(documentMapper.selectById(1001L)).thenReturn(document);

        TransactionSynchronizationManager.initSynchronization();
        try {
            DocumentUploadVO result = documentService.reindexDocument(1001L);

            assertThat(result.getDocumentId()).isEqualTo("1001");
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSING.name());
            verify(documentMapper).updateById(document);
            verify(documentIndexService, never()).indexAsync(anyLong(), anyString());

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            verify(documentIndexService).indexAsync(eq(1001L), anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
