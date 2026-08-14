package com.aics.message.service;

import com.aics.message.dto.LlmTraceDTO;
import com.aics.message.dto.PageResult;
import com.aics.message.vo.LlmTraceVO;

/**
 * LLM 调用链追踪服务接口
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：定义 LLM 调用链追踪（llm_trace 表）的持久化与查询能力，供 chat 模块
 * LLM 编排链路通过 Feign 上报调用链元数据，实现链路追踪、失败分析与场景耗时统计。
 * 幂等约定：createTrace 按 requestId 幂等，requestId 已存在时直接返回已有 requestId，
 * 不覆盖首次记录。
 * 查询约定：getTrace 不存在时返回 null（不抛异常），由调用方自行决定展示策略。
 * 实现类：{@link com.aics.message.service.impl.LlmTraceServiceImpl}。
 * 调用方：{@link com.aics.message.controller.LlmTraceController}。
 *
 * <h3>【设计原理】为什么接口与实现分离、查询缺失返回 null 而非抛异常</h3>
 * <ul>
 *   <li>接口即契约：Feign 调用方 / Controller 只依赖接口，实现可替换（本地实现/远程代理），
 *       且契约（幂等/查询语义）在接口 javadoc 里一次性写清；</li>
 *   <li>可观测数据是"读多写少"的追加型数据，查不到一条 trace 是常态而非异常：
 *       getTrace 返回 null（而非抛 {@code BusinessException}）让调用方把"无数据"当正常分支处理，
 *       与 Agent 轨迹（存在性影响流程状态机）的强校验语义刻意区分。</li>
 * </ul>
 * </p>
 */
public interface LlmTraceService {

    /**
     * 创建 LLM 调用链追踪（幂等：requestId 已存在直接返回，不覆盖）
     *
     * @param dto 调用链追踪信息
     * @return 请求ID（requestId）
     */
    String createTrace(LlmTraceDTO dto);

    /**
     * 查询 LLM 调用链追踪（不存在返回 null，不抛异常）
     *
     * @param requestId 请求ID
     * @return 调用链追踪信息，不存在时为 null
     */
    LlmTraceVO getTrace(String requestId);

    /**
     * 分页查询 LLM 调用链追踪（userId/scenario 可空过滤，create_time 倒序）
     *
     * @param userId   用户ID（可空）
     * @param scenario 场景（可空）
     * @param page     页码（从 1 开始）
     * @param size     每页大小
     * @return 分页查询结果
     */
    PageResult<LlmTraceVO> pageTraces(Long userId, String scenario, int page, int size);
}
