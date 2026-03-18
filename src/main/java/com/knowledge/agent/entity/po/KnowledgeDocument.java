package com.knowledge.agent.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("kb_document")
public class KnowledgeDocument {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("kb_id")
    private String kbId;

    @TableField("file_name")
    private String fileName;

    @TableField("source_type")
    private String sourceType;

    @TableField("storage_path")
    private String storagePath;

    @TableField("status")
    private String status;

    @TableField("tags")
    private String tags;

    @TableField("error_message")
    private String errorMessage;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("indexed_at")
    private LocalDateTime indexedAt;
}
