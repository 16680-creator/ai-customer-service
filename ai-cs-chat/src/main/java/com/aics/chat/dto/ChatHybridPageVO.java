package com.aics.chat.dto;

import lombok.Data;

import java.util.List;

/**
 * 混合检索分页结果（chat 侧 DTO）。
 */
@Data
public class ChatHybridPageVO {

    private long total;
    private int page;
    private int size;
    private List<ChatHybridSearchResult> records;
}