package com.knowledge.agent.service;

import com.knowledge.agent.entity.dto.QueryRequestDTO;
import com.knowledge.agent.entity.vo.QueryResponseVO;

public interface KnowledgeQueryService {

    QueryResponseVO query(QueryRequestDTO requestDTO);
}
