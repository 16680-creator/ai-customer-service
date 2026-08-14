package com.aics.chat.agent.safety;

import com.aics.chat.agent.model.SafetyCheckResult;
import com.aics.chat.security.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 输入安全检查（Prompt 注入 / 违规内容检测，3.2 F1）
 *
 * <p>命中任一规则即拦截：Agent 不触发任何工具调用。规则为确定性正则匹配，
 * 便于单元测试与对抗样本验证；内置规则 + 配置追加规则
 * （{@code aics.security.injection-extra-rules}，每条 {@code 描述|正则}）合并生效。</p>
 *
 * <p>对抗加固（3.2 新增）：除正则外增加"紧凑文本"二次检测——去除所有空白后
 * 检查核心指令短语（忽略所有指令/忽略以上指令/忽略系统指令），防"忽 略 指 令"
 * 这类分割拼接绕过。</p>
 */
@Slf4j
@Service
public class SafetyGuardService {

    /** 注入/违规检测规则：规则描述 → 正则 */
    private static final List<Rule> BUILTIN_RULES = List.of(
            new Rule("提示词注入（忽略指令）", Pattern.compile("忽略\\s*(之前|以上|所有|系统)?\\s*的?\\s*(?:所有|任何)?\\s*(指令|提示|规则|要求)", Pattern.CASE_INSENSITIVE)),
            new Rule("提示词注入（英文忽略指令）", Pattern.compile("ignore\\s+((previous|all|above|system)\\s+)*(instructions?|prompts?|rules?)", Pattern.CASE_INSENSITIVE)),
            new Rule("泄露系统提示词", Pattern.compile("(输出|打印|展示|透露|泄露).{0,8}(系统提示|system\\s*prompt|知识库原文|内部指令|prompt)")),
            new Rule("索要内部内容", Pattern.compile("(知识库原文|系统提示词|原始提示词|内部指令)")),
            new Rule("越权/绕过", Pattern.compile("(越权|绕过|绕过权限|提权|扮演管理员|冒充管理员)")),
            new Rule("越狱/角色扮演攻击", Pattern.compile("(越狱|jailbreak|DAN模式|role\\s*play|套取|诱导)")),
            new Rule("直接调用工具指令", Pattern.compile("直接调用.{0,8}工具|调用所有工具|执行任意工具")),
            new Rule("索要全部回答/输出", Pattern.compile("(输出|打印|复述|展示).{0,6}(全部|所有).{0,6}(内容|回答|文本|输出)")),
            new Rule("编码混淆注入", Pattern.compile("(?i)(base64|hex|rot13|凯撒).{0,12}(解码|解密|还原).{0,12}(指令|prompt|系统)")),
            new Rule("英文泄露提示词", Pattern.compile("(?i)(reveal|show|print|output).{0,12}(system\\s*prompt|prompt)")),
            new Rule("直接调用任意工具（英文）", Pattern.compile("(?i)(call|invoke|execute).{0,8}(all|any).{0,8}(tools?|functions?)"))
    );

    /** 紧凑文本二次检测的核心指令短语（去空白后匹配，防分割拼接绕过） */
    private static final List<String> COMPACT_PHRASES = List.of(
            "忽略所有指令", "忽略以上指令", "忽略系统指令", "忽略之前指令", "忽略之前的指令",
            "输出系统提示词", "打印知识库原文", "泄露系统提示词"
    );

    /** 合并后的规则（内置 + 配置追加） */
    private final List<Rule> rules;

    public SafetyGuardService() {
        this(List.of());
    }

    /**
     * Spring 装配入口：注入 {@link SecurityProperties}，合并配置追加规则。
     */
    @Autowired
    public SafetyGuardService(SecurityProperties properties) {
        this(properties == null || properties.getInjectionExtraRules() == null
                ? List.of() : properties.getInjectionExtraRules());
    }

    private SafetyGuardService(List<String> extraRules) {
        List<Rule> merged = new ArrayList<>(BUILTIN_RULES);
        if (extraRules != null) {
            for (String entry : extraRules) {
                int sep = entry.indexOf('|');
                if (sep > 0) {
                    String desc = entry.substring(0, sep).trim();
                    String regex = entry.substring(sep + 1).trim();
                    if (!regex.isEmpty()) {
                        try {
                            merged.add(new Rule(desc, Pattern.compile(regex, Pattern.CASE_INSENSITIVE)));
                        } catch (Exception e) {
                            log.warn("注入检测追加规则解析失败: {}, err={}", entry, e.getMessage());
                        }
                    }
                }
            }
        }
        this.rules = List.copyOf(merged);
    }

    /**
     * 检查用户输入是否安全
     *
     * @param input 用户输入
     * @return 通过或拦截（含原因）
     */
    public SafetyCheckResult check(String input) {
        // 空输入直接拦截
        if (input == null || input.isBlank()) {
            return SafetyCheckResult.block("输入内容为空");
        }
        // 超长输入拦截（防注入超长载荷）
        if (input.length() > 2000) {
            return SafetyCheckResult.block("输入内容超过长度限制（2000 字符）");
        }
        // 逐条命中正则规则即拦截（确定性检测，零工具调用）
        for (Rule rule : rules) {
            if (rule.pattern().matcher(input).find()) {
                log.warn("输入安全检查拦截, reason={}, input={}", rule.desc(), input);
                return SafetyCheckResult.block("检测到" + rule.desc() + "，已拦截本次请求");
            }
        }
        // 紧凑文本二次检测：去除所有空白后匹配核心指令短语（防"忽 略 指 令"分割绕过）
        // 学习点：对抗“插入空白/特殊字符”的绕过，与其写一条覆盖任意空白变体的正则
        // （指数级组合），不如先把空白全部剥掉再 contains——一次匹配覆盖所有变体。
        String compact = input.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        for (String phrase : COMPACT_PHRASES) {
            if (compact.contains(phrase)) {
                log.warn("输入安全检查拦截(紧凑文本), phrase={}, input={}", phrase, input);
                return SafetyCheckResult.block("检测到" + phrase + "，已拦截本次请求");
            }
        }
        // 全部规则未命中：判定安全
        return SafetyCheckResult.pass();
    }

    private record Rule(String desc, Pattern pattern) {
    }
}
