package com.knowledge.agent.controller;

import com.knowledge.agent.common.ErrorCodeEnum;
import com.knowledge.agent.common.PageResult;
import com.knowledge.agent.common.Result;
import com.knowledge.agent.entity.dto.DocumentListQueryDTO;
import com.knowledge.agent.entity.dto.DocumentUploadDTO;
import com.knowledge.agent.entity.dto.QueryRequestDTO;
import com.knowledge.agent.entity.vo.DocumentContentVO;
import com.knowledge.agent.entity.vo.DocumentDetailVO;
import com.knowledge.agent.entity.vo.DocumentUploadVO;
import com.knowledge.agent.entity.vo.QueryResponseVO;
import com.knowledge.agent.entity.vo.RuntimeOverviewVO;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.service.DocumentService;
import com.knowledge.agent.service.KnowledgeQueryService;
import com.knowledge.agent.service.RuntimeOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kb")
@Tag(name = "知识库管理")
public class KnowledgeBaseController {

    private final DocumentService documentService;
    private final KnowledgeQueryService queryService;
    private final RuntimeOverviewService runtimeOverviewService;

    @GetMapping("/runtime")
    @Operation(summary = "获取本地演示运行态摘要")
    public Result<RuntimeOverviewVO> runtimeOverview() {
        return Result.success(runtimeOverviewService.getOverview());
    }

    @PostMapping("/documents")
    @Operation(summary = "上传文档并触发索引")
    public Result<DocumentUploadVO> uploadDocument(@Valid @ModelAttribute DocumentUploadDTO request) {
        return Result.success(documentService.uploadDocument(request));
    }

    @GetMapping("/documents")
    @Operation(summary = "按条件分页查询文档")
    public Result<PageResult<DocumentDetailVO>> listDocuments(@Valid @ModelAttribute DocumentListQueryDTO request) {
        return Result.success(documentService.listDocuments(request));
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "查询文档索引状态")
    public Result<DocumentDetailVO> getDocument(@PathVariable("id") @Positive(message = "id must be greater than 0") Long id) {
        validateDocumentId(id);
        return Result.success(documentService.getDocument(id));
    }

    @GetMapping("/documents/{id}/content")
    @Operation(summary = "读取文档正文")
    public Result<DocumentContentVO> getDocumentContent(@PathVariable("id") @Positive(message = "id must be greater than 0") Long id) {
        validateDocumentId(id);
        return Result.success(documentService.getDocumentContent(id));
    }

    @DeleteMapping("/documents/{id}")
    @Operation(summary = "删除文档及其索引数据")
    public Result<Void> deleteDocument(@PathVariable("id") @Positive(message = "id must be greater than 0") Long id) {
        validateDocumentId(id);
        documentService.deleteDocument(id);
        return Result.success(null);
    }

    @PostMapping("/query")
    @Operation(summary = "统一问答入口（知识库问答 / 普通聊天）")
    public Result<QueryResponseVO> query(@Valid @RequestBody QueryRequestDTO request) {
        return Result.success(queryService.query(request));
    }

    @PostMapping("/reindex/{id}")
    @Operation(summary = "重建单个文档索引")
    public Result<DocumentUploadVO> reindex(@PathVariable("id") @Positive(message = "id must be greater than 0") Long id) {
        validateDocumentId(id);
        return Result.success(documentService.reindexDocument(id));
    }

    private void validateDocumentId(Long id) {
        if (id == null || id <= 0) {
            throw new ServiceException(ErrorCodeEnum.BAD_REQUEST, "id must be greater than 0");
        }
    }
}
