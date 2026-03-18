package com.knowledge.agent.service.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {

    private Long id;

    private String kbId;

    private Long docId;

    private String chunkId;

    private String fileName;

    private String content;

    private Map<String, Object> metadata;

    private Double score;
}
