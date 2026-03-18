package com.knowledge.agent.service;

import com.knowledge.agent.common.PageResult;
import com.knowledge.agent.entity.dto.DocumentListQueryDTO;
import com.knowledge.agent.entity.dto.DocumentUploadDTO;
import com.knowledge.agent.entity.vo.DocumentContentVO;
import com.knowledge.agent.entity.vo.DocumentDetailVO;
import com.knowledge.agent.entity.vo.DocumentUploadVO;

public interface DocumentService {

    DocumentUploadVO uploadDocument(DocumentUploadDTO dto);

    PageResult<DocumentDetailVO> listDocuments(DocumentListQueryDTO queryDTO);

    DocumentDetailVO getDocument(Long documentId);

    DocumentContentVO getDocumentContent(Long documentId);

    void deleteDocument(Long documentId);

    DocumentUploadVO reindexDocument(Long documentId);
}
