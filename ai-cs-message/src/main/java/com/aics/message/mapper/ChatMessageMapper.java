package com.aics.message.mapper;

import com.aics.message.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天消息 Mapper
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：提供 {@link ChatMessage} 的数据库访问能力。
 * 继承 {@link BaseMapper} 已具备标准 CRUD（insert/selectById/update/delete 等），
 * 此处仅补充按 sessionKey 查询的自定义 SQL，因为 sessionKey 为跨服务字符串标识，
 * 需要按业务侧约定的升序返回历史消息。
 * </p>
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 按会话标识 sessionKey 查询消息（按创建时间升序）
     * <p>
     * 升序返回以保证历史消息按对话顺序排列；通过 limit 控制返回条数，
     * 避免长会话一次性加载过多数据。
     * </p>
     *
     * @param sessionKey 会话标识
     * @param limit      返回条数上限
     * @return 消息列表
     */
    @Select("SELECT * FROM chat_message WHERE session_key = #{sessionKey} " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<ChatMessage> selectBySessionKey(@Param("sessionKey") String sessionKey,
                                         @Param("limit") int limit);
}