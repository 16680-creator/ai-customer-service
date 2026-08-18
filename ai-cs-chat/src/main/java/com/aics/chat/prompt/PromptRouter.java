package com.aics.chat.prompt;

import com.aics.chat.prompt.PromptProperties.RolloutConfig;
import com.aics.chat.prompt.PromptProperties.ScenarioConfig;
import com.aics.chat.prompt.PromptProperties.UserIdModConfig;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Prompt 灰度路由：依据 scenario 的 {@link RolloutConfig} 在多个版本中选出本次请求使用的版本。
 *
 * <p>支持三种策略（见 OpenSpec change 2026-08-18-prompt-config）：
 * <ul>
 *   <li>{@code weights}：按权重随机抽样（默认抽样源
 *       {@link ThreadLocalRandom}，可注入便于测试）；</li>
 *   <li>{@code userId-mod}：按 userId 取模命中固定版本，结果可复现（同一用户稳定命中）；</li>
 *   <li>{@code pinned}：固定返回指定版本（等价于全量指定）。</li>
 * </ul>
 * 命中版本不存在时回退到 {@code activeVersion}。
 */
public class PromptRouter {

    /** 抽样随机源（可注入，便于单测确定性） */
    private final java.util.function.DoubleSupplier random;

    public PromptRouter() {
        this(ThreadLocalRandom.current()::nextDouble);
    }

    public PromptRouter(java.util.function.DoubleSupplier random) {
        this.random = random;
    }

    /**
     * 选定版本。userId 仅 userId-mod 策略使用，其余策略传 null 即可。
     *
     * @return 选中的版本号（保证存在于 scenario.versions 中，否则回退 activeVersion）
     */
    public String resolveVersion(String scenario, ScenarioConfig config, String userId) {
        RolloutConfig rollout = config.getRollout();
        String strategy = rollout.getStrategy();
        Map<String, PromptProperties.VersionConfig> versions = config.getVersions();

        String chosen;
        if ("weights".equalsIgnoreCase(strategy)) {
            chosen = byWeights(rollout.getWeights());
        } else if ("userId-mod".equalsIgnoreCase(strategy)) {
            chosen = byUserIdMod(rollout.getUserIdMod(), userId, config.getActiveVersion());
        } else { // pinned
            chosen = StringUtils.hasText(rollout.getPinned()) ? rollout.getPinned() : config.getActiveVersion();
        }

        // 命中版本不存在 → 回退 activeVersion；activeVersion 也不存在则回退 versions 首个 key
        if (!versions.containsKey(chosen)) {
            chosen = config.getActiveVersion();
        }
        if (!versions.containsKey(chosen) && !versions.isEmpty()) {
            chosen = versions.keySet().iterator().next();
        }
        return chosen;
    }

    private String byWeights(Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return null;
        }
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            return null;
        }
        double r = random.getAsDouble() * total;
        double acc = 0.0;
        // 按 key 排序保证确定性：Map.of 等无序 map 迭代顺序不稳定，会破坏权重区间
        List<String> sortedKeys = weights.keySet().stream().sorted().toList();
        for (String key : sortedKeys) {
            acc += weights.get(key);
            if (r < acc) {
                return key;
            }
        }
        // 浮点兜底：返回最后一个
        return sortedKeys.get(sortedKeys.size() - 1);
    }

    private String byUserIdMod(UserIdModConfig mod, String userId, String activeVersion) {
        if (mod == null || !StringUtils.hasText(userId)) {
            return activeVersion;
        }
        try {
            long id = Long.parseLong(userId);
            if (Math.floorMod(id, mod.getMod()) == mod.getRemainder()) {
                return mod.getVersion();
            }
        } catch (NumberFormatException ignored) {
            // userId 非数字 → 视为未命中，用 activeVersion
        }
        return activeVersion;
    }
}
