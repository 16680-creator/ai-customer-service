package com.aics.chat.controller;

import com.aics.chat.prompt.PromptProperties;
import com.aics.chat.prompt.PromptRegistry;
import com.aics.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt 配置化管理接口（OpenSpec change 2026-08-18-prompt-config）。
 *
 * <p>提供版本查询与热回滚/灰度收敛能力——改 {@code activeVersion} 即时生效，无需发版。
 * 除查看类接口外，切换生效版本属于生产操作，建议配合权限控制使用。</p>
 */
@Tag(name = "Prompt配置")
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptRegistry promptRegistry;
    private final PromptProperties promptProperties;

    /**
     * 列出全部 scenario 的生效版本快照。
     */
    @Operation(summary = "列出所有 Prompt 场景的生效版本")
    @GetMapping
    public Result<Map<String, String>> listActive() {
        return Result.success(promptRegistry.activeSnapshot());
    }

    /**
     * 列出某 scenario 的全部版本及内容摘要。
     */
    @Operation(summary = "列出某场景的所有 Prompt 版本")
    @GetMapping("/{scenario}")
    public Result<Map<String, Object>> listVersions(@PathVariable String scenario) {
        List<String> versions = promptRegistry.listVersions(scenario);
        if (versions.isEmpty()) {
            return Result.fail("未配置的 Prompt scenario: " + scenario);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("activeVersion", promptRegistry.getActiveVersion(scenario));
        Map<String, Object> detail = new LinkedHashMap<>();
        for (String v : versions) {
            PromptProperties.VersionConfig vc = promptRegistry.getVersion(scenario, v);
            int sysLen = vc.getSystem() == null ? 0 : vc.getSystem().length();
            int usrLen = vc.getUser() == null ? 0 : vc.getUser().length();
            Map<String, Integer> meta = new LinkedHashMap<>();
            meta.put("systemLength", sysLen);
            meta.put("userLength", usrLen);
            detail.put(v, meta);
        }
        body.put("versions", detail);
        return Result.success(body);
    }

    /**
     * 热切换某 scenario 的生效版本（回滚 / 灰度收敛）。
     */
    @Operation(summary = "热切换某场景的生效 Prompt 版本")
    @PostMapping("/{scenario}/active")
    public Result<Map<String, String>> setActive(@PathVariable String scenario,
                                                 @RequestParam String version) {
        promptRegistry.setActiveVersion(scenario, version);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("scenario", scenario);
        result.put("activeVersion", promptRegistry.getActiveVersion(scenario));
        return Result.success(result);
    }
}
