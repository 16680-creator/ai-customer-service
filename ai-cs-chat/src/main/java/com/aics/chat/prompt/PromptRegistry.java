package com.aics.chat.prompt;

import com.aics.chat.prompt.PromptProperties.ScenarioConfig;
import com.aics.chat.prompt.PromptProperties.VersionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 注册中心：加载、渲染、版本管理与热回滚。
 *
 * <p>职责（见 OpenSpec change 2026-08-18-prompt-config）：
 * <ul>
 *   <li><b>加载校验</b>：启动时校验每个 scenario 的 activeVersion 存在、灰度权重和约为 1；</li>
 *   <li><b>渲染</b>：{@link #render} 经 {@link PromptRouter} 选版本后用 {@code {{var}}} 占位符渲染，
 *       未提供变量抛 {@link PromptRenderException}；</li>
 *   <li><b>版本管理</b>：{@link #listVersions}/{@link #getVersion}/{@link #getActiveVersion} 查询；</li>
 *   <li><b>热回滚</b>：{@link #setActiveVersion} 改内存态生效版本，无需发版。</li>
 * </ul>
 * {@code enabled=false} 时，{@link #render} 回退到调用方提供的 fallback（过渡期双写）。
 */
@Slf4j
@Component
public class PromptRegistry implements InitializingBean {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

    private final PromptProperties properties;
    private final PromptRouter router;

    /** 内存态 activeVersion（支持热回滚，覆盖 YAML 初始值） */
    private final Map<String, String> activeOverrides = new ConcurrentHashMap<>();

    public PromptRegistry(PromptProperties properties, PromptRouter router) {
        this.properties = properties;
        this.router = router;
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.isEnabled()) {
            log.warn("[Prompt] 配置化已关闭(enabled=false)，将回退硬编码副本");
            return;
        }
        for (Map.Entry<String, ScenarioConfig> e : properties.getScenarios().entrySet()) {
            validate(e.getKey(), e.getValue());
        }
    }

    private void validate(String scenario, ScenarioConfig config) {
        Map<String, VersionConfig> versions = config.getVersions();
        if (versions == null || versions.isEmpty()) {
            throw new PromptRenderException("Prompt scenario [" + scenario + "] 未配置任何版本");
        }
        if (!versions.containsKey(config.getActiveVersion())) {
            throw new PromptRenderException("Prompt scenario [" + scenario + "] 的 activeVersion ["
                    + config.getActiveVersion() + "] 不存在于 versions");
        }
        if ("weights".equalsIgnoreCase(config.getRollout().getStrategy())) {
            double sum = config.getRollout().getWeights().values().stream()
                    .mapToDouble(Double::doubleValue).sum();
            if (Math.abs(sum - 1.0) > 0.01) {
                log.warn("[Prompt] scenario [{}] 灰度权重和={}，建议为 1.0", scenario, sum);
            }
        }
    }

    /**
     * 渲染提示词（含版本路由 + 占位符替换）。
     *
     * @param scenario   场景名
     * @param userId     用户 ID（灰度 userId-mod 用，可空）
     * @param variables  占位符变量
     * @param fallback   配置化关闭/缺失时的回退内容（过渡期），为 null 则抛异常
     * @return 渲染结果（system 可能为空）
     */
    public RenderedPrompt render(String scenario, String userId,
                                Map<String, Object> variables, FallbackSupplier fallback) {
        if (!properties.isEnabled()) {
            if (fallback == null) {
                throw new PromptRenderException("Prompt 配置化已关闭且无 fallback: " + scenario);
            }
            return fallback.get();
        }
        ScenarioConfig config = properties.getScenarios().get(scenario);
        if (config == null) {
            if (fallback != null) {
                return fallback.get();
            }
            throw new PromptRenderException("未配置的 Prompt scenario: " + scenario);
        }
        String active = activeOverrides.getOrDefault(scenario, config.getActiveVersion());
        String version = router.resolveVersion(scenario, withActive(config, active), userId);
        VersionConfig vc = config.getVersions().get(version);
        if (vc == null) {
            throw new PromptRenderException("Prompt scenario [" + scenario + "] 版本 [" + version + "] 不存在");
        }
        String system = vc.getSystem() == null ? null : renderTemplate(vc.getSystem(), variables);
        String user = vc.getUser() == null ? null : renderTemplate(vc.getUser(), variables);
        return new RenderedPrompt(system, user, scenario, version);
    }

    /** 便捷方法：无 userId、无 fallback */
    public RenderedPrompt render(String scenario, Map<String, Object> variables) {
        return render(scenario, null, variables, null);
    }

    /** 便捷方法：带 fallback（过渡期关闭开关时使用） */
    public RenderedPrompt renderOrFallback(String scenario, String userId,
                                           Map<String, Object> variables, FallbackSupplier fallback) {
        return render(scenario, userId, variables, fallback);
    }

    private ScenarioConfig withActive(ScenarioConfig config, String active) {
        if (active.equals(config.getActiveVersion())) {
            return config;
        }
        ScenarioConfig copy = new ScenarioConfig();
        copy.setActiveVersion(active);
        copy.setRollout(config.getRollout());
        copy.setVersions(config.getVersions());
        return copy;
    }

    private String renderTemplate(String template, Map<String, Object> variables) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object val = variables == null ? null : variables.get(key);
            if (val == null) {
                throw new PromptRenderException("Prompt 模板缺少变量占位符 {{" + key + "}}");
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(val)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ===== 版本管理 / 热回滚 =====

    public String getActiveVersion(String scenario) {
        ScenarioConfig config = properties.getScenarios().get(scenario);
        if (config == null) {
            return null;
        }
        return activeOverrides.getOrDefault(scenario, config.getActiveVersion());
    }

    /** 热回滚/切换生效版本（即时生效，无需发版） */
    public void setActiveVersion(String scenario, String version) {
        ScenarioConfig config = properties.getScenarios().get(scenario);
        if (config == null) {
            throw new PromptRenderException("未配置的 Prompt scenario: " + scenario);
        }
        if (!config.getVersions().containsKey(version)) {
            throw new PromptRenderException("Prompt scenario [" + scenario + "] 版本 [" + version + "] 不存在");
        }
        activeOverrides.put(scenario, version);
        log.info("[Prompt] scenario [{}] 生效版本已热切换为 [{}]", scenario, version);
    }

    public List<String> listVersions(String scenario) {
        ScenarioConfig config = properties.getScenarios().get(scenario);
        if (config == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(config.getVersions().keySet());
    }

    public VersionConfig getVersion(String scenario, String version) {
        ScenarioConfig config = properties.getScenarios().get(scenario);
        if (config == null) {
            return null;
        }
        return config.getVersions().get(version);
    }

    /** 所有 scenario 的生效版本快照（管理接口用） */
    public Map<String, String> activeSnapshot() {
        Map<String, String> snap = new LinkedHashMap<>();
        for (String scenario : properties.getScenarios().keySet()) {
            snap.put(scenario, getActiveVersion(scenario));
        }
        return snap;
    }

    /** 渲染结果（携带 scenario/version 供效果关联写入 trace） */
    public static class RenderedPrompt {
        private final String system;
        private final String user;
        private final String scenario;
        private final String version;

        public RenderedPrompt(String system, String user, String scenario, String version) {
            this.system = system;
            this.user = user;
            this.scenario = scenario;
            this.version = version;
        }

        public String getSystem() { return system; }
        public String getUser() { return user; }
        public String getScenario() { return scenario; }
        public String getVersion() { return version; }

        /** 取最终 user 文本（RAG/改写等场景用） */
        public String text() {
            return user != null ? user : system;
        }
    }

    /** 回退供应商（过渡期关闭开关时使用） */
    @FunctionalInterface
    public interface FallbackSupplier {
        RenderedPrompt get();
    }
}
