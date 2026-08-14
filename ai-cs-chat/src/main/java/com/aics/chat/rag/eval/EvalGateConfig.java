package com.aics.chat.rag.eval;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 评估门禁扩展配置（前缀 aics.eval.gate）
 *
 * <p>对应 docs/15 第 3.3 节「CI 门禁除正确率外，增加 P95 延迟和单请求平均 Token 上限」：
 * <ul>
 *   <li>{@code p95-latency-ms}：P95 延迟上限（毫秒），可空；配置后任一评估样本的 P95 延迟
 *       超过上限即判定门禁不通过；</li>
 *   <li>{@code avg-tokens-per-request}：单请求平均 Token 上限，可空；配置后平均 Token
 *       超过上限即判定门禁不通过。</li>
 * </ul>
 * 阈值未配置的维度只记录指标值，不参与门禁判定（向后兼容既有 CI）。</p>
 *
 * <h3>【AI 技术详解】"可空阈值"的设计意图</h3>
 * <ul>
 *   <li><b>渐进式治理</b>：先只观测（不判定）再逐步收紧——未配置时指标照常写入报告，
 *       团队对齐基线后再配置阈值正式卡门禁，避免"一上线就误杀所有流水线"；</li>
 *   <li><b>向后兼容</b>：老 CI 配置没有这两个字段，null 语义保证旧配置无需改动即可运行；</li>
 *   <li><b>判定语义</b>：配置了阈值但样本无数据（如 P95 计算不出）时同样放行，
 *       即"没有依据就不判定"，符合门禁"宁可放行不可误杀"的保守原则。</li>
 * </ul>
 */
@Getter
@Setter
@Component
// prefix 绑定 aics.eval.gate.* 配置项：配置缺失时字段保持 null（而非抛绑定异常），
// 正是"未配置不判定"语义能成立的前提
@ConfigurationProperties(prefix = "aics.eval.gate")
public class EvalGateConfig {

    /** P95 延迟上限（毫秒，null=不校验） */
    // 为什么用 Long 而非 long：基本类型默认 0，会把"未配置"误判为"上限是 0ms"，
    // 包装类型 null 才能表达"未配置不判定"
    private Long p95LatencyMs;

    /** 单请求平均 Token 上限（null=不校验） */
    private Long avgTokensPerRequest;
}
