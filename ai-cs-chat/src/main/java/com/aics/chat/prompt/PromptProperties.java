package com.aics.chat.prompt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Prompt 配置化属性（前缀 aics.prompt）。
 *
 * <p>设计（见 OpenSpec change 2026-08-18-prompt-config）：
 * <ul>
 *   <li>{@code enabled}：总开关。关闭时 {@link PromptRegistry} 回退到各服务保留的硬编码副本
 *       （过渡期双写用），最终移除；</li>
 *   <li>{@code scenarios}：按业务场景（intent/rewrite/rag/summary/judge/chart/vision/default-system）
 *       组织的提示词配置，每个 scenario 支持多版本与灰度策略；</li>
 *   <li>{@code versions}：版本号 → 模板（system/user 可空），模板支持 {@code {{var}}} 占位符。</li>
 * </ul>
 * 单一事实来源为 YAML 配置（非持久化）；版本/灰度通过配置即改即生效需配合 {@code /api/prompts} 热刷新。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aics.prompt")
public class PromptProperties {

    /** 配置化总开关（默认开启）。关闭时回退硬编码。 */
    private boolean enabled = true;

    /** 各场景提示词配置，key 为 scenario 名 */
    private Map<String, ScenarioConfig> scenarios = new HashMap<>();

    @Getter
    @Setter
    public static class ScenarioConfig {
        /** 当前生效版本（必须存在于 versions） */
        private String activeVersion;

        /** 灰度发布策略 */
        private RolloutConfig rollout = new RolloutConfig();

        /** 版本号 → 模板 */
        private Map<String, VersionConfig> versions = new HashMap<>();
    }

    @Getter
    @Setter
    public static class RolloutConfig {
        /** 策略：weights 按比例 / userId-mod 按尾号 / pinned 全量指定 */
        private String strategy = "pinned";

        /** weights 策略下的版本权重（和应为 1） */
        private Map<String, Double> weights = new HashMap<>();

        /** pinned 策略下固定生效版本 */
        private String pinned;

        /** userId-mod 策略配置 */
        private UserIdModConfig userIdMod;
    }

    @Getter
    @Setter
    public static class UserIdModConfig {
        /** 命中目标版本 */
        private String version;
        /** 取模基数 */
        private int mod = 10;
        /** 命中余数（userId % mod == remainder 命中 version，否则用 activeVersion） */
        private int remainder = 0;
    }

    @Getter
    @Setter
    public static class VersionConfig {
        /** system 提示词（可为空 → 渲染返回 null，兼容仅 user 模板） */
        private String system;

        /** user 提示词（可为空） */
        private String user;
    }
}
