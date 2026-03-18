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
@Schema(description = "Runtime overview")
public class RuntimeOverviewVO {

    @Schema(description = "Default knowledge base id")
    private String defaultKbId;

    @Schema(description = "Vector store mode", allowableValues = {"IN_MEMORY", "MILVUS"})
    private String vectorStoreMode;

    @Schema(description = "Embedding model")
    private String embeddingModel;

    @Schema(description = "Chat model")
    private String chatModel;

    @Schema(description = "Milvus collection name")
    private String milvusCollectionName;

    @Schema(description = "Vector dimension")
    private Integer vectorDimension;

    @Schema(description = "Chunk size")
    private Integer chunkSize;

    @Schema(description = "Chunk overlap")
    private Integer chunkOverlap;

    @Schema(description = "Resolved storage path")
    private String storagePath;

    @Schema(description = "Supported file types")
    private List<String> supportedFileTypes;

    @Schema(description = "Component status list")
    private List<RuntimeComponentVO> components;
}
