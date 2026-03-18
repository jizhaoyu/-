package com.knowledge.agent.entity.dto;

import com.knowledge.agent.common.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Knowledge base query request")
public class QueryRequestDTO {

    @Schema(description = "Knowledge base id", example = "default-kb")
    @NotBlank(message = "kbId cannot be blank")
    @Pattern(regexp = ValidationConstants.KB_ID_REGEX, message = ValidationConstants.KB_ID_MESSAGE)
    private String kbId;

    @Schema(description = "User question", example = "How to start Milvus?", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "question cannot be blank")
    private String question;

    @Schema(description = "Query mode", example = "KB", allowableValues = {"KB", "CHAT"})
    @Pattern(regexp = "KB|CHAT", message = "mode must be KB or CHAT")
    @Builder.Default
    private String mode = "KB";

    @Schema(description = "Top K retrieval size", example = "5")
    @Min(value = 1, message = "topK must be >= 1")
    @Max(value = 20, message = "topK must be <= 20")
    @Builder.Default
    private Integer topK = 5;

    @Schema(description = "Metadata filters", example = "{\"docId\":\"123\"}")
    @Builder.Default
    private Map<String, String> metadataFilters = new HashMap<>();
}
