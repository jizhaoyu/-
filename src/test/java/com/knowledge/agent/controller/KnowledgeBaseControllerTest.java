package com.knowledge.agent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.common.ErrorCodeEnum;
import com.knowledge.agent.common.PageResult;
import com.knowledge.agent.entity.dto.DocumentUploadDTO;
import com.knowledge.agent.entity.dto.QueryRequestDTO;
import com.knowledge.agent.entity.vo.DocumentContentVO;
import com.knowledge.agent.entity.vo.DocumentDetailVO;
import com.knowledge.agent.entity.vo.DocumentUploadVO;
import com.knowledge.agent.entity.vo.QueryResponseVO;
import com.knowledge.agent.entity.vo.RuntimeComponentVO;
import com.knowledge.agent.entity.vo.RuntimeOverviewVO;
import com.knowledge.agent.exception.GlobalExceptionHandler;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.service.DocumentService;
import com.knowledge.agent.service.KnowledgeQueryService;
import com.knowledge.agent.service.RuntimeOverviewService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private LocalValidatorFactoryBean validator;

    @Mock
    private DocumentService documentService;

    @Mock
    private KnowledgeQueryService queryService;

    @Mock
    private RuntimeOverviewService runtimeOverviewService;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        KnowledgeBaseController controller = new KnowledgeBaseController(documentService, queryService, runtimeOverviewService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("runtime overview endpoint should return runtime summary")
    void runtimeOverview_shouldReturnRuntimeSummary() throws Exception {
        when(runtimeOverviewService.getOverview()).thenReturn(RuntimeOverviewVO.builder()
                .defaultKbId("default-kb")
                .vectorStoreMode("MILVUS")
                .chatModel("kimi-k2.5")
                .embeddingModel("text-embedding-v2")
                .components(List.of(RuntimeComponentVO.builder()
                        .key("mysql")
                        .label("MySQL")
                        .status("UP")
                        .summary("数据库连接正常")
                        .detail("SELECT 1")
                        .build()))
                .build());

        mockMvc.perform(get("/api/kb/runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.defaultKbId").value("default-kb"))
                .andExpect(jsonPath("$.data.vectorStoreMode").value("MILVUS"))
                .andExpect(jsonPath("$.data.components[0].status").value("UP"));
    }

    @Test
    @DisplayName("upload document endpoint should return success result")
    void uploadDocument_shouldReturnSuccess() throws Exception {
        when(documentService.uploadDocument(any(DocumentUploadDTO.class))).thenReturn(DocumentUploadVO.builder()
                .documentId("1001")
                .status("PROCESSING")
                .traceId("trace123")
                .build());

        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", "hello world".getBytes());

        mockMvc.perform(multipart("/api/kb/documents")
                        .file(file)
                        .param("kbId", "default-kb")
                        .param("tags", "demo,test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.documentId").value("1001"));
    }

    @Test
    @DisplayName("query endpoint should return answer")
    void query_shouldReturnAnswer() throws Exception {
        when(queryService.query(any(QueryRequestDTO.class))).thenReturn(QueryResponseVO.builder()
                .answer("answer")
                .citations(List.of())
                .latencyMs(100L)
                .traceId("trace456")
                .build());

        QueryRequestDTO request = QueryRequestDTO.builder()
                .kbId("default-kb")
                .question("what is rag")
                .topK(5)
                .build();

        mockMvc.perform(post("/api/kb/query")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.answer").value("answer"));
    }

    @Test
    @DisplayName("get document endpoint should return details")
    void getDocument_shouldReturnDetails() throws Exception {
        when(documentService.getDocument(1001L)).thenReturn(DocumentDetailVO.builder()
                .documentId("1001")
                .kbId("default-kb")
                .fileName("demo.txt")
                .status("READY")
                .build());

        mockMvc.perform(get("/api/kb/documents/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("demo.txt"));
    }

    @Test
    @DisplayName("get document content endpoint should return content")
    void getDocumentContent_shouldReturnContent() throws Exception {
        when(documentService.getDocumentContent(1001L)).thenReturn(DocumentContentVO.builder()
                .documentId("1001")
                .content("hello world")
                .build());

        mockMvc.perform(get("/api/kb/documents/1001/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.documentId").value("1001"))
                .andExpect(jsonPath("$.data.content").value("hello world"));
    }

    @Test
    @DisplayName("get document content endpoint should reject non-positive id")
    void getDocumentContent_shouldRejectNonPositiveId() throws Exception {
        mockMvc.perform(get("/api/kb/documents/0/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("id must be greater than 0"));
    }

    @Test
    @DisplayName("list documents endpoint should return page result")
    void listDocuments_shouldReturnPageResult() throws Exception {
        when(documentService.listDocuments(any())).thenReturn(PageResult.<DocumentDetailVO>builder()
                .records(List.of(DocumentDetailVO.builder()
                        .documentId("1001")
                        .kbId("default-kb")
                        .fileName("demo.txt")
                        .status("READY")
                        .build()))
                .total(1L)
                .pageNum(1L)
                .pageSize(10L)
                .build());

        mockMvc.perform(get("/api/kb/documents")
                        .param("kbId", "default-kb")
                        .param("status", "READY")
                        .param("fileName", "demo")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].fileName").value("demo.txt"));
    }

    @Test
    @DisplayName("list documents endpoint should validate query params")
    void listDocuments_shouldValidateQueryParams() throws Exception {
        mockMvc.perform(get("/api/kb/documents")
                        .param("kbId", "default-kb")
                        .param("status", "INVALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("status must be one of PROCESSING, READY, FAILED"));
    }

    @Test
    @DisplayName("upload document endpoint should reject invalid kbId")
    void uploadDocument_shouldRejectInvalidKbId() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", "hello world".getBytes());

        mockMvc.perform(multipart("/api/kb/documents")
                        .file(file)
                        .param("kbId", "../default-kb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(
                        "kbId must start with a letter or digit and contain only letters, digits, dot, underscore or dash"));
    }

    @Test
    @DisplayName("get document endpoint should reject non-positive id")
    void getDocument_shouldRejectNonPositiveId() throws Exception {
        mockMvc.perform(get("/api/kb/documents/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("id must be greater than 0"));
    }

    @Test
    @DisplayName("delete document endpoint should return success")
    void deleteDocument_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/kb/documents/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(documentService).deleteDocument(1001L);
    }

    @Test
    @DisplayName("delete document endpoint should return business error when document missing")
    void deleteDocument_shouldReturnBusinessErrorWhenMissing() throws Exception {
        doThrow(new ServiceException(ErrorCodeEnum.DOCUMENT_NOT_FOUND, "document not found: 9999"))
                .when(documentService)
                .deleteDocument(eq(9999L));

        mockMvc.perform(delete("/api/kb/documents/9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4101))
                .andExpect(jsonPath("$.message").value("document not found: 9999"));
    }
}
