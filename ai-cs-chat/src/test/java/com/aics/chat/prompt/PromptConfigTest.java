package com.aics.chat.prompt;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prompt 配置化核心：渲染、缺参异常、灰度路由、热回滚。
 */
class PromptConfigTest {

    private PromptProperties buildProps() {
        PromptProperties props = new PromptProperties();
        props.setEnabled(true);

        // intent：v1 与 v2 两个版本，灰度按 userId 尾号
        PromptProperties.ScenarioConfig intent = new PromptProperties.ScenarioConfig();
        intent.setActiveVersion("v1");
        PromptProperties.RolloutConfig intentRollout = new PromptProperties.RolloutConfig();
        intentRollout.setStrategy("userId-mod");
        PromptProperties.UserIdModConfig mod = new PromptProperties.UserIdModConfig();
        mod.setVersion("v2");
        mod.setMod(10);
        mod.setRemainder(3);
        intentRollout.setUserIdMod(mod);
        intent.setRollout(intentRollout);
        PromptProperties.VersionConfig i1 = new PromptProperties.VersionConfig();
        i1.setSystem("sys-{{input}}");
        i1.setUser("user-{{input}}");
        PromptProperties.VersionConfig i2 = new PromptProperties.VersionConfig();
        i2.setSystem("sys2-{{input}}");
        i2.setUser("user2-{{input}}");
        intent.setVersions(Map.of("v1", i1, "v2", i2));

        // rewrite：按比例权重灰度
        PromptProperties.ScenarioConfig rewrite = new PromptProperties.ScenarioConfig();
        rewrite.setActiveVersion("v1");
        PromptProperties.RolloutConfig rwRollout = new PromptProperties.RolloutConfig();
        rwRollout.setStrategy("weights");
        rwRollout.setWeights(Map.of("v1", 0.9, "v2", 0.1));
        rewrite.setRollout(rwRollout);
        PromptProperties.VersionConfig r1 = new PromptProperties.VersionConfig();
        r1.setUser("rw1-{{count}}");
        PromptProperties.VersionConfig r2 = new PromptProperties.VersionConfig();
        r2.setUser("rw2-{{count}}");
        rewrite.setVersions(Map.of("v1", r1, "v2", r2));

        props.setScenarios(new HashMap<>(Map.of("intent", intent, "rewrite", rewrite)));
        return props;
    }

    private PromptRegistry registry(PromptProperties props) {
        PromptRegistry reg = new PromptRegistry(props, new PromptRouter());
        reg.afterPropertiesSet();
        return reg;
    }

    @Test
    void render_replacesPlaceholders() {
        PromptRegistry reg = registry(buildProps());
        PromptRegistry.RenderedPrompt rp = reg.render("intent",
                java.util.Map.of("input", "hello"));
        assertEquals("sys-hello", rp.getSystem());
        assertEquals("user-hello", rp.getUser());
        assertEquals("intent", rp.getScenario());
        assertEquals("v1", rp.getVersion());
    }

    @Test
    void render_missingVariable_throws() {
        PromptRegistry reg = registry(buildProps());
        PromptRenderException ex = assertThrows(PromptRenderException.class,
                () -> reg.render("intent", java.util.Map.of()));
        assertTrue(ex.getMessage().contains("input"));
    }

    @Test
    void router_userIdMod_stableBucketing() {
        PromptRegistry reg = registry(buildProps());
        // userId 尾号 3 → v2；尾号 1 → v1
        PromptRegistry.RenderedPrompt hit = reg.render("intent", "123",
                java.util.Map.of("input", "x"), null);
        assertEquals("v2", hit.getVersion());
        PromptRegistry.RenderedPrompt miss = reg.render("intent", "121",
                java.util.Map.of("input", "x"), null);
        assertEquals("v1", miss.getVersion());
    }

    @Test
    void router_weights_samplesBothVersions() {
        PromptRouter router = new PromptRouter(() -> 0.95); // 命中 v2（权重 0.9 之后）
        PromptProperties props = buildProps();
        PromptProperties.ScenarioConfig cfg = props.getScenarios().get("rewrite");
        assertEquals("v2", router.resolveVersion("rewrite", cfg, "1"));

        PromptRouter router2 = new PromptRouter(() -> 0.1); // 命中 v1
        assertEquals("v1", router2.resolveVersion("rewrite", cfg, "1"));
    }

    @Test
    void hotRollback_switchesActiveVersion() {
        PromptRegistry reg = registry(buildProps());
        assertEquals("v1", reg.getActiveVersion("intent"));
        reg.setActiveVersion("intent", "v2");
        assertEquals("v2", reg.getActiveVersion("intent"));
        // 默认（无 userId）渲染走 activeVersion
        PromptRegistry.RenderedPrompt rp = reg.render("intent",
                java.util.Map.of("input", "z"));
        assertEquals("v2", rp.getVersion());
        assertTrue(rp.getUser().startsWith("user2-"));
        // 旧版本仍在，可切回
        reg.setActiveVersion("intent", "v1");
        assertEquals("v1", reg.getActiveVersion("intent"));
    }
}
