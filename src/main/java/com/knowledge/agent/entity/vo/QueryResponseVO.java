package com.knowledge.agent.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Query response")
public class QueryResponseVO {

    @Schema(description = "Answer")
    private String answer;

    @Schema(description = "Citations")
    private List<CitationVO> citations;

    @Schema(description = "Latency in ms")
    private Long latencyMs;

    @Schema(description = "Trace id")
    private String traceId;
}
