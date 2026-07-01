package com.aics.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档实体
 */
@Data
@TableName("t_knowledge_document")
public class KnowledgeDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 文档标题 */
    private String title;

    /** 文档内容 */
    private String content;

    /** 文档类型：pdf/docx/txt/markdown/html */
    private String docType;

    /** 文档来源URL */
    private String sourceUrl;

    /** 文档摘要 */
    private String summary;

    /** 标签（逗号分隔） */
    private String tags;

    /** 状态：0-待处理 1-已索引 2-索引失败 */
    private Integer status;

    /** 创建人ID */
    private Long createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
