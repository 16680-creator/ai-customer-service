package com.aics.chat.feign;

import com.aics.chat.dto.ChatHybridPageVO;
import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 搜索服务 Feign 客户端（调用 ai-cs-search 混合检索接口）。
 */
@FeignClient(name = "ai-cs-search")
public interface SearchFeignClient {

    /**
     * 混合检索（ES 关键词 + 向量语义，RRF 融合）。
     *
     * @param index 知识库标识（index）
     * @param query 检索词
     * @param page  页码
     * @param size  每页大小
     * @return 分页混合检索结果
     */
    @GetMapping("/search/hybrid")
    Result<ChatHybridPageVO> hybridSearch(@RequestParam("index") String index,
                                          @RequestParam("query") String query,
                                          @RequestParam("page") int page,
                                          @RequestParam("size") int size);
}