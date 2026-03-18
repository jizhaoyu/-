package com.knowledge.agent.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document detail")
public class DocumentDetailVO {

    @Schema(description = "Document id")
    private String documentId;

    @Schema(description = "Knowledge base id")
    private String kbId;

    @Schema(description = "File name")
    private String fileName;

    @Schema(description = "Source type")
    private String sourceType;

    @Schema(description = "Status")
    private String status;

    @Schema(description = "Tags")
    private List<String> tags;

    @Schema(description = "Error message")
    private String errorMessage;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at")
    private LocalDateTime updatedAt;

    @Schema(description = "Indexed at")
    private LocalDateTime indexedAt;
}
