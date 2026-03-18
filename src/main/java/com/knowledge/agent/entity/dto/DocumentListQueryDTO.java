package com.knowledge.agent.entity.dto;

import com.knowledge.agent.common.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document list query request")
public class DocumentListQueryDTO {

    @Schema(description = "Knowledge base id", example = "default-kb", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "kbId cannot be blank")
    @Pattern(regexp = ValidationConstants.KB_ID_REGEX, message = ValidationConstants.KB_ID_MESSAGE)
    private String kbId;

    @Schema(description = "Document status", allowableValues = {"PROCESSING", "READY", "FAILED"})
    @Pattern(regexp = "PROCESSING|READY|FAILED", message = "status must be one of PROCESSING, READY, FAILED")
    private String status;

    @Schema(description = "File name fuzzy match", example = "demo")
    private String fileName;

    @Schema(description = "Page number", example = "1")
    @Min(value = 1, message = "pageNum must be >= 1")
    @Builder.Default
    private Integer pageNum = 1;

    @Schema(description = "Page size", example = "10")
    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 50, message = "pageSize must be <= 50")
    @Builder.Default
    private Integer pageSize = 10;
}
