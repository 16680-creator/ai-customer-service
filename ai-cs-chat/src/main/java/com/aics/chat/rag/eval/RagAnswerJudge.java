package com.aics.chat.rag.eval;

/**
 * 回答质量打分器（LLM-as-Judge）。
 *
 * <h3>【AI 技术详解】为什么定义成接口而非具体类？</h3>
 * <ul>
 *   <li><b>策略可替换</b>：离线 golden 集评估（RagEvaluator）与线上采样评估
 *       （OnlineEvalService）都需要"给回答打分"，但打分策略可能不同（离线用
 *       参考答案对比、线上纯 LLM 评判）；接口抽象让调用方依赖契约而非实现；</li>
 *   <li><b>失败语义</b>：{@link #score} 约定"不可用/异常返回 null"——评估是增强数据，
 *       Judge 调用失败不应中断整个评估流程，调用方以 null 跳过该样本即可。</li>
 * </ul>
 */
public interface RagAnswerJudge {

    /**
     * 对回答打分（1-5）。
     *
     * @param question        用户问题
     * @param answer          模型回答
     * @param referenceAnswer 参考答案（可空）
     * @return 分数；不可用/异常时返回 null
     */
    // 1-5 档位：与用户反馈 score 同尺度，便于离线评估与线上反馈对齐；
    // referenceAnswer 可空：无参考答案时 Judge 仅凭问题与回答做相对评判
    Integer score(String question, String answer, String referenceAnswer);

    /**
     * 最近一次打分的 LLM 调用总 Token 数（可空）。
     *
     * <p>默认返回 null；实现类可返回最近一次 {@link #score} 调用消耗的 Token，
     * 供评估门禁计算单请求平均 Token（spec：CI 门禁增加单请求平均 Token 上限）。</p>
     *
     * @return 总 Token 数；不可用返回 null
     */
    // 用 default 方法而非抽象方法：Token 采集是"可选能力"，默认 null 让既有实现
    // （如纯规则打分器、无 usage 暴露的 Judge）无需改动即可编译——接口演进不破坏
    // 已有实现，符合开闭原则；需要采集成本的实现（LlmJudgeService）覆写即可
    default Integer lastTotalTokens() {
        return null;
    }
}
