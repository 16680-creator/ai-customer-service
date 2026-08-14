package com.aics.chat.agent.intent;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.model.AgentIntent;
import com.aics.chat.agent.model.AgentIntentType;
import com.aics.chat.agent.model.IntentResult;
import com.aics.chat.agent.model.SentimentType;
import com.aics.chat.observability.TraceSpans;
import com.aics.chat.service.impl.ResilientAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 意图识别服务：LLM 结构化输出 + 规则兜底。
 *
 * <p>优先调用 LLM 返回 JSON（意图类型/置信度/参数/情绪），解析失败或 LLM 不可用时
 * 降级为确定性规则分类器，保证任何情况下 Agent 都能路由。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentClassifierService {

    private final AgentProperties properties;
    private final ResilientAiService resilientAiService;
    private final ObjectMapper objectMapper;
    private final ObservationRegistry observationRegistry;

    /** 售后关键词 */
    private static final List<String> AFTER_SALE_KEYWORDS = List.of(
            "换货", "退货", "退款", "售后", "保修", "维修", "坏了", "质量问题", "无法开机", "故障", "补发");

    /** 商品推荐关键词 */
    private static final List<String> RECOMMEND_KEYWORDS = List.of(
            "推荐", "同价位", "预算", "帮我看看", "类似的");

    /** 转人工关键词 */
    private static final List<String> HANDOFF_KEYWORDS = List.of(
            "转人工", "人工客服", "人工服务", "投诉", "找真人");

    /** 情绪关键词 */
    private static final List<String> ANGRY_KEYWORDS = List.of(
            "投诉", "太差", "垃圾", "愤怒", "气愤", "差评", "坑人", "欺诈");
    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "失望", "不满意", "不好用", "恼火");
    private static final List<String> POSITIVE_KEYWORDS = List.of(
            "谢谢", "满意", "太好了", "好评", "感谢");

    private static final Pattern BUDGET_PATTERN = Pattern.compile("(\\d+)\\s*(元|块)");
    private static final Pattern ACTION_PATTERN = Pattern.compile("(换货|退货|退款)");

    /**
     * 意图分类入口（计入 LLM 调用链观测：intent span——类型、置信度、路由结果）
     *
     * <p>学习点：意图识别是 Agent 链路的第一环，把它计入 trace 的意义在于——
     * 当 Agent 行为不符合预期时（该走售后却走了普通对话），可以先看 intent span
     * 的置信度与降级路径（LLM 还是规则兜底），快速定位是"识别错了"还是"下游处理错了"。</p>
     */
    public IntentResult classify(String input) {
        return TraceSpans.observeReturn(observationRegistry, "INTENT", "agent.intent",
                Map.of(),
                Map.of("detail", input == null ? "" : input),
                () -> doClassify(input));
    }

    private IntentResult doClassify(String input) {
        // 开启 LLM 意图识别时优先走 LLM 路径
        if (properties.isLlmIntentEnabled()) {
            try {
                // LLM 调用限时 10 秒，超时/异常走规则兜底
                String json = resilientAiService.callRagChat(buildPrompt(input))
                        .get(10, TimeUnit.SECONDS);
                IntentResult parsed = parseLlmJson(json);
                // 解析成功且非空：应用置信度门禁
                if (parsed != null && !parsed.intents().isEmpty()) {
                    return applyThreshold(parsed);
                }
                log.warn("LLM 意图识别输出无法解析，降级规则分类: {}", json);
            } catch (Exception e) {
                log.warn("LLM 意图识别失败，降级规则分类: {}", e.getMessage());
            }
        }
        // 降级：确定性规则分类
        return ruleBasedClassify(input);
    }

    /**
     * 置信度门禁：低于阈值的意图剔除；全部低于阈值时按普通对话路由
     * （规格边界情况：意图置信度低于阈值时走默认普通对话路由，不触发工具）
     */
    private IntentResult applyThreshold(IntentResult parsed) {
        List<AgentIntent> filtered = parsed.intents().stream()
                // 剔除低于置信度阈值的意图
                .filter(i -> i.confidence() >= properties.getIntentThreshold())
                .toList();
        if (filtered.isEmpty()) {
            // 全部低于阈值：按普通对话路由，不触发任何工具
            return IntentResult.of(List.of(AgentIntent.of(AgentIntentType.NORMAL_CHAT, 0.6, Map.of())),
                    parsed.sentiment(), false, parsed.rawJson());
        }
        return IntentResult.of(filtered, parsed.sentiment(), parsed.needsHandoff(), parsed.rawJson());
    }

    /**
     * 构建 LLM 结构化输出提示词
     */
    String buildPrompt(String input) {
        return """
                你是智能客服的意图识别器。请对用户输入进行意图分类，只输出 JSON，不要输出任何其他内容。
                意图类型（type）只能是：AFTER_SALE（售后：换货/退货/退款/保修）、PRODUCT_RECOMMEND（商品推荐）、NORMAL_CHAT（普通咨询）、HUMAN_HANDOFF（转人工）、OTHER（其他）。
                输出格式：
                {"intents":[{"type":"AFTER_SALE","confidence":0.95,"params":{"action":"EXCHANGE","reason":"质量问题"}}],"sentiment":"NEUTRAL","needsHandoff":false}
                - intents：可能包含多个意图（如用户同时要售后和推荐）
                - params：抽取结构化参数（action 取值 EXCHANGE/RETURN/REFUND；budget 为预算金额数字字符串；keywords 为商品特性关键词，逗号分隔；reason 为售后原因）
                - sentiment：POSITIVE/NEUTRAL/NEGATIVE/ANGRY（用户明显愤怒时填 ANGRY 且 needsHandoff=true）
                用户输入：%s
                """.formatted(input);
    }

    /**
     * 解析 LLM JSON 输出（容忍代码块与前后杂文本）
     */
    IntentResult parseLlmJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String json = raw.trim();
            if (json.startsWith("```")) {
                // 容忍 LLM 输出被 markdown 代码块包裹
                json = json.replaceFirst("```(json)?", "").replaceFirst("```$", "").trim();
            }
            // 截取首个 { 到最后一个 } 之间的 JSON 片段
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return null;
            }
            JsonNode root = objectMapper.readTree(json.substring(start, end + 1));
            List<AgentIntent> intents = new ArrayList<>();
            JsonNode intentsNode = root.get("intents");
            if (intentsNode != null && intentsNode.isArray()) {
                for (JsonNode node : intentsNode) {
                    AgentIntentType type = parseType(node.path("type").asText());
                    if (type == null) {
                        continue;
                    }
                    double confidence = node.path("confidence").asDouble(0.5);
                    Map<String, String> params = new HashMap<>();
                    JsonNode paramsNode = node.get("params");
                    if (paramsNode != null && paramsNode.isObject()) {
                        paramsNode.fields().forEachRemaining(e ->
                                params.put(e.getKey(), e.getValue().asText()));
                    }
                    intents.add(AgentIntent.of(type, confidence, params));
                }
            }
            if (intents.isEmpty()) {
                return null;
            }
            SentimentType sentiment = parseSentiment(root.path("sentiment").asText());
            // 情绪为 ANGRY 时强制转人工（与 LLM 返回的 needsHandoff 取或）
            boolean needsHandoff = root.path("needsHandoff").asBoolean(false)
                    || sentiment == SentimentType.ANGRY;
            return IntentResult.of(intents, sentiment, needsHandoff, raw);
        } catch (Exception e) {
            log.warn("意图 JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 规则兜底分类器（确定性，用于 LLM 不可用时的降级与评估）
     */
    public IntentResult ruleBasedClassify(String input) {
        String text = input == null ? "" : input;
        List<AgentIntent> intents = new ArrayList<>();
        // 命中转人工关键词：优先标记转人工意图
        if (containsAny(text, HANDOFF_KEYWORDS)) {
            intents.add(AgentIntent.of(AgentIntentType.HUMAN_HANDOFF, 0.95, Map.of("reason", text)));
        }
        // 命中售后关键词：抽取动作参数并补齐原因
        if (containsAny(text, AFTER_SALE_KEYWORDS)) {
            Map<String, String> params = new HashMap<>();
            Matcher actionMatcher = ACTION_PATTERN.matcher(text);
            if (actionMatcher.find()) {
                // 将中文动作词映射为枚举编码（换货/退货/其余→退款）
                String action = switch (actionMatcher.group(1)) {
                    case "换货" -> "EXCHANGE";
                    case "退货" -> "RETURN";
                    default -> "REFUND";
                };
                params.put("action", action);
            }
            params.put("reason", text);
            intents.add(AgentIntent.of(AgentIntentType.AFTER_SALE, 0.95, params));
        }
        // 命中推荐关键词（含固定句式兜底）：抽取预算与特性关键词
        if (containsAny(text, RECOMMEND_KEYWORDS)
                || (text.contains("有没有") && text.contains("耳机"))
                || (text.contains("有没有") && text.contains("商品"))) {
            Map<String, String> params = new HashMap<>();
            Matcher budgetMatcher = BUDGET_PATTERN.matcher(text);
            if (budgetMatcher.find()) {
                // 抽取预算金额（如 "300元" → "300"）
                params.put("budget", budgetMatcher.group(1));
            }
            if (text.contains("降噪")) {
                params.put("keywords", "降噪");
            } else if (text.contains("蓝牙")) {
                params.put("keywords", "蓝牙");
            }
            intents.add(AgentIntent.of(AgentIntentType.PRODUCT_RECOMMEND, 0.95, params));
        }
        if (intents.isEmpty()) {
            // 无任何命中：普通对话兜底
            intents.add(AgentIntent.of(AgentIntentType.NORMAL_CHAT, 0.6, Map.of()));
        }
        SentimentType sentiment = detectSentiment(text);
        // 主动要求转人工或情绪愤怒：触发转人工
        boolean needsHandoff = intents.stream().anyMatch(i -> i.type() == AgentIntentType.HUMAN_HANDOFF)
                || sentiment == SentimentType.ANGRY;
        return IntentResult.of(intents, sentiment, needsHandoff, null);
    }

    SentimentType detectSentiment(String text) {
        if (containsAny(text, ANGRY_KEYWORDS)) {
            return SentimentType.ANGRY;
        }
        if (containsAny(text, NEGATIVE_KEYWORDS)) {
            return SentimentType.NEGATIVE;
        }
        if (containsAny(text, POSITIVE_KEYWORDS)) {
            return SentimentType.POSITIVE;
        }
        return SentimentType.NEUTRAL;
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private static AgentIntentType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return AgentIntentType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static SentimentType parseSentiment(String sentiment) {
        if (sentiment == null || sentiment.isBlank()) {
            return SentimentType.NEUTRAL;
        }
        try {
            return SentimentType.valueOf(sentiment.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SentimentType.NEUTRAL;
        }
    }
}
