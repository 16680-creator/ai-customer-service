package com.aics.knowledge.mapper;

import com.aics.knowledge.entity.KnowledgeDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识文档 Mapper 接口
 *
 * <p>职责：提供对 kb_document 表的基础 CRUD 访问能力。</p>
 *
 * <p>技术要点：继承 MyBatis-Plus 的 BaseMapper，自动获得
 * insert / deleteById / updateById / selectById / selectPage 等方法，
 * 无需手写 XML；分页由 {@link com.aics.knowledge.config.MybatisPlusConfig} 的分页插件支持。</p>
 */
@Mapper
public interface KnowledgeMapper extends BaseMapper<KnowledgeDocument> {
}
