package com.aics.chat.feign;

import com.aics.chat.dto.ModelUsageDTO;
import com.aics.chat.dto.ModelUsageQuotaVO;
import com.aics.chat.dto.ModelUsageStatsVO;
import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

/**
 * 消息服务模型用量 Feign 客户端（调用 ai-cs-message 计量与统计 model_usage）
 *
 * <h3>【AI 技术详解】三个接口的职责边界</h3>
 * <ul>
 *   <li><b>recordUsage（写）</b>：聊天主链路异步上报单次调用用量，失败仅告警不阻塞；
 *       本客户端只是传输层，异步编排在 {@code usageExecutor} 线程池侧。</li>
 *   <li><b>getStats（读）</b>：聚合统计接口，参数全可空 —— 按 (userId, scenario, model)
 *       任意维度组合聚合，配额检查（对比配额）与成本看板（展示消耗）共用一套口径；</li>
 *   <li><b>getQuota（读）</b>：查询配额定义，不存在时返回 {@code success(null)} 而非
 *       报错 —— "未配置配额"与"配额为 0"是两种语义，前者等于不限。</li>
 * </ul>
 *
 * <p><b>【技术详解】GET 参数上的 @DateTimeFormat 是必须的吗</b>：Feign 把方法参数拼进
 * URL 查询串，LocalDateTime 没有原生字符串表示；不声明格式时 Spring 按默认 ISO 格式
 * 解析，与 message 侧约定的 {@code yyyy-MM-dd HH:mm:ss} 不匹配会直接 400。
 * 两侧必须声明<b>完全相同</b>的 pattern。</p>
 */
@FeignClient(name = "ai-cs-message", contextId = "modelUsage")
public interface ModelUsageFeignClient {

    /**
     * 上报一次 LLM 调用用量（落 model_usage 表）
     */
    @PostMapping("/api/model-usage/records")
    Result<Void> recordUsage(@RequestBody ModelUsageDTO dto);

    /**
     * 按用户/场景/模型/时间窗口聚合统计（配额检查与成本看板用）
     *
     * @param userId    用户 ID（可空）
     * @param scenario  场景（可空）
     * @param model     模型名（可空）
     * @param startTime 窗口起始时间（可空）
     * @param endTime   窗口结束时间（可空）
     */
    // required=false 实现"维度可空"：userId/scenario/model 全空时返回全局统计，
    // 这是配额检查与成本看板共用的弹性聚合入口，而非每个维度一个专用接口
    @GetMapping("/api/model-usage/stats")
    Result<ModelUsageStatsVO> getStats(@RequestParam(value = "userId", required = false) Long userId,
                                       @RequestParam(value = "scenario", required = false) String scenario,
                                       @RequestParam(value = "model", required = false) String model,
                                       @RequestParam(value = "startTime", required = false)
                                       @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                       @RequestParam(value = "endTime", required = false)
                                       @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime);

    /**
     * 按 (userId, scenario) 查询模型用量配额（不存在返回 success(null)）
     */
    // 必填参数不加 required=false：配额查询必须有明确对象，缺参属于调用方 bug，尽早失败
    @GetMapping("/api/model-usage/quota")
    Result<ModelUsageQuotaVO> getQuota(@RequestParam("userId") Long userId,
                                       @RequestParam("scenario") String scenario);
}
