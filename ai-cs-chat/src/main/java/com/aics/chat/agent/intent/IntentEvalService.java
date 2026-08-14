package com.aics.chat.agent.intent;

import com.aics.chat.agent.model.AgentIntentType;
import com.aics.chat.agent.model.IntentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 意图分类评估（SC-001：意图分类 Macro-F1 ≥ 0.90）
 *
 * <p>使用固定标注数据集对规则兜底分类器做离线评估：
 * 以「置信度最高的首个意图」作为预测标签（数据集样本均为单意图），
 * 计算 4 个类别（AFTER_SALE/PRODUCT_RECOMMEND/HUMAN_HANDOFF/NORMAL_CHAT）的
 * 精确率、召回率与 F1，Macro-F1 为各类 F1 的算术平均。</p>
 */
@Service
@RequiredArgsConstructor
public class IntentEvalService {

    private final IntentClassifierService classifier;

    /** 固定评估数据集（输入 → 期望标签） */
    private static final List<Sample> DATASET = List.of(
            // 售后
            new Sample("我昨天买的耳机坏了，想换货", AgentIntentType.AFTER_SALE),
            new Sample("怎么申请退货", AgentIntentType.AFTER_SALE),
            new Sample("耳机有质量问题，我要退款", AgentIntentType.AFTER_SALE),
            new Sample("这个订单还在保修期内吗", AgentIntentType.AFTER_SALE),
            new Sample("蓝牙耳机无法开机了，怎么办", AgentIntentType.AFTER_SALE),
            new Sample("商品有故障，能补发吗", AgentIntentType.AFTER_SALE),
            // 商品推荐
            new Sample("帮我推荐300元以内的降噪耳机", AgentIntentType.PRODUCT_RECOMMEND),
            new Sample("有没有500块以内的蓝牙耳机", AgentIntentType.PRODUCT_RECOMMEND),
            new Sample("同价位的耳机还有别的推荐吗", AgentIntentType.PRODUCT_RECOMMEND),
            new Sample("帮我看看类似的手机壳", AgentIntentType.PRODUCT_RECOMMEND),
            new Sample("预算200左右有什么好的充电宝", AgentIntentType.PRODUCT_RECOMMEND),
            new Sample("推荐一个续航久的耳机", AgentIntentType.PRODUCT_RECOMMEND),
            // 转人工
            new Sample("转人工", AgentIntentType.HUMAN_HANDOFF),
            new Sample("我要找人工客服", AgentIntentType.HUMAN_HANDOFF),
            new Sample("帮我转接人工服务", AgentIntentType.HUMAN_HANDOFF),
            new Sample("我要投诉你们客服", AgentIntentType.HUMAN_HANDOFF),
            new Sample("麻烦找真人处理", AgentIntentType.HUMAN_HANDOFF),
            new Sample("人工客服在吗", AgentIntentType.HUMAN_HANDOFF),
            // 普通咨询
            new Sample("你们公司地址在哪里", AgentIntentType.NORMAL_CHAT),
            new Sample("快递一般几天到", AgentIntentType.NORMAL_CHAT),
            new Sample("优惠券怎么使用", AgentIntentType.NORMAL_CHAT),
            new Sample("谢谢", AgentIntentType.NORMAL_CHAT),
            new Sample("你好，在吗", AgentIntentType.NORMAL_CHAT),
            new Sample("订单号是多少", AgentIntentType.NORMAL_CHAT)
    );

    /**
     * 运行评估并返回 Macro-F1 报告
     */
    public EvalReport evaluate() {
        // 各类别 TP/FP/FN 计数（数组下标：0=TP, 1=FP, 2=FN）
        Map<AgentIntentType, int[]> tpFpFn = new EnumMap<>(AgentIntentType.class);
        for (AgentIntentType type : AgentIntentType.values()) {
            tpFpFn.put(type, new int[3]);
        }
        for (Sample sample : DATASET) {
            IntentResult result = classifier.ruleBasedClassify(sample.input());
            // 取置信度最高（首个）意图作为预测标签
            AgentIntentType predicted = result.intents().isEmpty() ? AgentIntentType.OTHER
                    : result.intents().get(0).type();
            int[] stats = tpFpFn.get(predicted);
            if (predicted == sample.expected()) {
                stats[0]++; // TP
            } else {
                stats[1]++; // FP（预测类）
                tpFpFn.get(sample.expected())[2]++; // FN（期望类）
            }
        }
        Map<String, Double> perClassF1 = new LinkedHashMap<>();
        double sum = 0;
        int counted = 0;
        // 逐类计算精确率/召回率/F1 并累加
        for (AgentIntentType type : List.of(AgentIntentType.AFTER_SALE, AgentIntentType.PRODUCT_RECOMMEND,
                AgentIntentType.HUMAN_HANDOFF, AgentIntentType.NORMAL_CHAT)) {
            int[] s = tpFpFn.get(type);
            double precision = s[0] + s[1] == 0 ? 0 : (double) s[0] / (s[0] + s[1]);
            double recall = s[0] + s[2] == 0 ? 0 : (double) s[0] / (s[0] + s[2]);
            double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
            perClassF1.put(type.name(), f1);
            sum += f1;
            counted++;
        }
        // Macro-F1 = 各类别 F1 的算术平均
        return new EvalReport(counted == 0 ? 0 : sum / counted, perClassF1, DATASET.size());
    }

    /**
     * 评估报告
     *
     * @param macroF1   Macro-F1
     * @param perClassF1 各类别 F1
     * @param sampleCount 样本数
     */
    public record EvalReport(double macroF1, Map<String, Double> perClassF1, int sampleCount) {
    }

    private record Sample(String input, AgentIntentType expected) {
    }
}
