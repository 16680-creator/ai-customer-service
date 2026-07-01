package com.aics.knowledge.mapper;

import com.aics.knowledge.entity.KnowledgeDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识文档 Mapper
 */
@Mapper
public interface KnowledgeMapper extends BaseMapper<KnowledgeDocument> {
}
