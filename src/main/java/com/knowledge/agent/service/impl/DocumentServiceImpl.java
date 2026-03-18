package com.knowledge.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.knowledge.agent.service.DocumentIndexService;
import com.knowledge.agent.service.DocumentParserService;
import com.knowledge.agent.service.DocumentService;
import com.knowledge.agent.service.FileStorageService;
import com.knowledge.agent.service.VectorStoreService;
import com.knowledge.agent.util.TagUtil;
import com.knowledge.agent.util.TraceIdUtil;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final KnowledgeDocumentMapper documentMapper;
    private final FileStorageService fileStorageService;
    private final DocumentIndexService documentIndexService;
    private final VectorStoreService vectorStoreService;
    private final DocumentParserService documentParserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentUploadVO uploadDocument(DocumentUploadDTO dto) {
        if (dto.getFile().isEmpty()) {
            throw new ServiceException(ErrorCodeEnum.BAD_REQUEST, "file cannot be empty");
        }
        Path storedPath = fileStorageService.store(dto.getFile(), dto.getKbId());
        String sourceType = fileStorageService.detectSourceType(dto.getFile().getOriginalFilename());
        LocalDateTime now = LocalDateTime.now();

        KnowledgeDocument document = KnowledgeDocument.builder()
                .kbId(dto.getKbId())
                .fileName(Optional.ofNullable(dto.getFile().getOriginalFilename()).orElse(storedPath.getFileName().toString()))
                .sourceType(sourceType)
                .storagePath(storedPath.toString())
                .status(DocumentStatus.PROCESSING.name())
                .tags(TagUtil.toStorageValue(TagUtil.parseTags(dto.getTags())))
                .createdAt(now)
                .updatedAt(now)
                .build();

        documentMapper.insert(document);
        String traceId = TraceIdUtil.newTraceId();
        triggerIndexAfterCommit(document.getId(), traceId);

        return DocumentUploadVO.builder()
                .documentId(String.valueOf(document.getId()))
                .status(document.getStatus())
                .traceId(traceId)
                .build();
    }

    @Override
    public PageResult<DocumentDetailVO> listDocuments(DocumentListQueryDTO queryDTO) {
        Page<KnowledgeDocument> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<KnowledgeDocument> queryWrapper = new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getKbId, queryDTO.getKbId())
                .eq(StrUtil.isNotBlank(queryDTO.getStatus()), KnowledgeDocument::getStatus, queryDTO.getStatus())
                .like(StrUtil.isNotBlank(queryDTO.getFileName()), KnowledgeDocument::getFileName, queryDTO.getFileName())
                .orderByDesc(KnowledgeDocument::getCreatedAt);

        Page<KnowledgeDocument> resultPage = documentMapper.selectPage(page, queryWrapper);
        List<DocumentDetailVO> records = resultPage.getRecords().stream()
                .map(this::toDetailVO)
                .toList();

        return PageResult.<DocumentDetailVO>builder()
                .records(records)
                .total(resultPage.getTotal())
                .pageNum(resultPage.getCurrent())
                .pageSize(resultPage.getSize())
                .build();
    }

    @Override
    public DocumentDetailVO getDocument(Long documentId) {
        KnowledgeDocument document = getDocumentOrThrow(documentId);
        return toDetailVO(document);
    }

    @Override
    public DocumentContentVO getDocumentContent(Long documentId) {
        KnowledgeDocument document = getDocumentOrThrow(documentId);
        String content = documentParserService.parse(Path.of(document.getStoragePath()), document.getSourceType());
        return DocumentContentVO.builder()
                .documentId(String.valueOf(document.getId()))
                .content(content)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        KnowledgeDocument document = getDocumentOrThrow(documentId);
        vectorStoreService.deleteByDocument(documentId);
        fileStorageService.delete(document.getStoragePath());

        int deleted = documentMapper.deleteById(documentId);
        if (deleted != 1) {
            throw new ServiceException(ErrorCodeEnum.SYSTEM_ERROR, "delete document failed: " + documentId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentUploadVO reindexDocument(Long documentId) {
        KnowledgeDocument document = getDocumentOrThrow(documentId);
        document.setStatus(DocumentStatus.PROCESSING.name());
        document.setErrorMessage(null);
        document.setIndexedAt(null);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);

        String traceId = TraceIdUtil.newTraceId();
        triggerIndexAfterCommit(documentId, traceId);

        return DocumentUploadVO.builder()
                .documentId(String.valueOf(documentId))
                .status(document.getStatus())
                .traceId(traceId)
                .build();
    }

    private DocumentDetailVO toDetailVO(KnowledgeDocument document) {
        return DocumentDetailVO.builder()
                .documentId(String.valueOf(document.getId()))
                .kbId(document.getKbId())
                .fileName(document.getFileName())
                .sourceType(document.getSourceType())
                .status(document.getStatus())
                .tags(TagUtil.parseTags(document.getTags()))
                .errorMessage(document.getErrorMessage())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .indexedAt(document.getIndexedAt())
                .build();
    }

    private KnowledgeDocument getDocumentOrThrow(Long documentId) {
        KnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new ServiceException(ErrorCodeEnum.DOCUMENT_NOT_FOUND, "document not found: " + documentId);
        }
        return document;
    }

    private void triggerIndexAfterCommit(Long documentId, String traceId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            documentIndexService.indexAsync(documentId, traceId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                documentIndexService.indexAsync(documentId, traceId);
            }
        });
    }
}
