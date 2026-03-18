package com.knowledge.agent.common;

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
@Schema(description = "Page result")
public class PageResult<T> {

    @Schema(description = "Current page records")
    private List<T> records;

    @Schema(description = "Total records")
    private long total;

    @Schema(description = "Current page number")
    private long pageNum;

    @Schema(description = "Page size")
    private long pageSize;
}
