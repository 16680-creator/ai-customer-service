package com.aics.knowledge.mapper;

import com.aics.knowledge.entity.KnowledgeFaq;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * FAQ 条目 Mapper。
 */
@Mapper
public interface KnowledgeFaqMapper extends BaseMapper<KnowledgeFaq> {
}