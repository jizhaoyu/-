package com.knowledge.agent.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Citation fragment")
public class CitationVO {

    @Schema(description = "Document id")
    private String docId;

    @Schema(description = "Chunk id")
    private String chunkId;

    @Schema(description = "Snippet")
    private String snippet;

    @Schema(description = "Score")
    private Double score;

    @Schema(description = "Source file")
    private String fileName;
}
