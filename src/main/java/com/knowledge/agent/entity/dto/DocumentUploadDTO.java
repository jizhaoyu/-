package com.knowledge.agent.entity.dto;

import com.knowledge.agent.common.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document upload request")
public class DocumentUploadDTO {

    @Schema(description = "Knowledge base id", example = "default-kb")
    @NotBlank(message = "kbId cannot be blank")
    @Pattern(regexp = ValidationConstants.KB_ID_REGEX, message = ValidationConstants.KB_ID_MESSAGE)
    private String kbId;

    @Schema(description = "Upload file")
    @NotNull(message = "file cannot be null")
    private MultipartFile file;

    @Schema(description = "Comma separated tags", example = "spring,milvus,rag")
    private String tags;
}
