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
@Schema(description = "Runtime component status")
public class RuntimeComponentVO {

    @Schema(description = "Component key")
    private String key;

    @Schema(description = "Component label")
    private String label;

    @Schema(description = "Status", allowableValues = {"UP", "WARN", "DOWN", "DISABLED"})
    private String status;

    @Schema(description = "Short summary")
    private String summary;

    @Schema(description = "Detailed message")
    private String detail;
}
