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
@Schema(description = "Document content")
public class DocumentContentVO {

    @Schema(description = "Document id")
    private String documentId;

    @Schema(description = "Document content")
    private String content;
}
