package com.aics.message.mapper;

import com.aics.message.entity.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天会话 Mapper
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：提供 {@link ChatSession} 的数据库访问能力。
 * 当前仅依赖 {@link BaseMapper} 提供的通用 CRUD + 条件构造器（LambdaQueryWrapper）
 * 即可满足会话创建、按用户查询等需求，故无需额外自定义方法。
 * </p>
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
