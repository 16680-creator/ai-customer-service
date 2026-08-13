package com.aics.chat.agent.safety;

import com.aics.chat.agent.model.SafetyCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入安全检查（Prompt 注入 / 违规内容检测）
 *
 * <p>命中任一规则即拦截：Agent 不触发任何工具调用。规则为确定性正则匹配，
 * 便于单元测试与对抗样本验证；后续可扩展为 LLM 审核链（保持接口不变）。</p>
 */
@Slf4j
@Service
public class SafetyGuardService {

    /** 注入/违规检测规则：规则描述 → 正则 */
    private static final List<Rule> RULES = List.of(
            new Rule("提示词注入（忽略指令）", Pattern.compile("忽略\\s*(之前|以上|所有|系统)?\\s*的?\\s*(指令|提示|规则|要求)", Pattern.CASE_INSENSITIVE)),
            new Rule("提示词注入（英文忽略指令）", Pattern.compile("ignore\\s+((previous|all|above|system)\\s+)*(instructions?|prompts?|rules?)", Pattern.CASE_INSENSITIVE)),
            new Rule("泄露系统提示词", Pattern.compile("(输出|打印|展示|透露|泄露).{0,8}(系统提示|system\\s*prompt|知识库原文|内部指令|prompt)")),
            new Rule("索要内部内容", Pattern.compile("(知识库原文|系统提示词|原始提示词|内部指令)")),
            new Rule("越权/绕过", Pattern.compile("(越权|绕过|绕过权限|提权|扮演管理员|冒充管理员)")),
            new Rule("越狱/角色扮演攻击", Pattern.compile("(越狱|jailbreak|DAN模式|role\\s*play|套取|诱导)")),
            new Rule("直接调用工具指令", Pattern.compile("直接调用.{0,8}工具|调用所有工具|执行任意工具"))
    );

    /**
     * 检查用户输入是否安全
     *
     * @param input 用户输入
     * @return 通过或拦截（含原因）
     */
    public SafetyCheckResult check(String input) {
        if (input == null || input.isBlank()) {
            return SafetyCheckResult.block("输入内容为空");
        }
        if (input.length() > 2000) {
            return SafetyCheckResult.block("输入内容超过长度限制（2000 字符）");
        }
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(input).find()) {
                log.warn("输入安全检查拦截, reason={}, input={}", rule.desc(), input);
                return SafetyCheckResult.block("检测到" + rule.desc() + "，已拦截本次请求");
            }
        }
        return SafetyCheckResult.pass();
    }

    private record Rule(String desc, Pattern pattern) {
    }
}
