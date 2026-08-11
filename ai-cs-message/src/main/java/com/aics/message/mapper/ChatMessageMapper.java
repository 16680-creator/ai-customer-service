package com.aics.message.mapper;

import com.aics.message.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 按会话标识 sessionKey 查询消息（按创建时间升序）
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