package com.aics.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * FAQ 条目实体（映射 kb_faq 表）。
 */
@Data
@TableName("kb_faq")
public class KnowledgeFaq implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String question;

    private String answer;

    private String knowledgeBase;

    private String topicId;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}