package com.aics.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 泛型分页查询结果 VO
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：承载分页查询的统一返回结构（当前页数据 + 总记录数 + 页码 + 每页大小），
 * 供 LLM 可观测性相关的列表查询（如 llm_trace 分页）使用，屏蔽 MyBatis-Plus
 * {@code Page} 的内部结构。
 *
 * <h3>【设计原理】为什么自建分页结构而不是直接返回 MyBatis-Plus 的 Page</h3>
 * <ul>
 *   <li>{@code Page} 携带大量持久层内部状态（orders/optimizeCountSql/搜索模式等），
 *       直接暴露给前端/Feign 调用方属于"接口泄漏实现"；自建 VO 只保留契约字段；</li>
 *   <li>long 而非 int：MySQL 的 COUNT(*) 返回 BIGINT，int 在超过 21 亿行时溢出；
 *       page/size 用 long 与 Page.getCurrent()/getSize() 直接对齐，免去类型转换。</li>
 *   <li>泛型 {@code <T>} 让分页结构可复用于任意列表查询，一个类服务所有分页接口。</li>
 * </ul>
 * </p>
 *
 * @param <T> 当前页记录类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页查询结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页数据")
    private List<T> records; // 当前页记录：由 Service 层把实体逐个转 VO 后填入

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "当前页码（从 1 开始）")
    private long page;

    @Schema(description = "每页大小")
    private long size;
}
