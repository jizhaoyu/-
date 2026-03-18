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
@Schema(description = "Document upload response")
public class DocumentUploadVO {

    @Schema(description = "Document id")
    private String documentId;

    @Schema(description = "Current status")
    private String status;

    @Schema(description = "Trace id")
    private String traceId;
}
