package com.aics.chat.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工具调用授权服务（3.2 F2）：工具端角色-权限矩阵校验。
 *
 * <p>行为契约（对应 Gherkin Feature 02）：</p>
 * <ul>
 *   <li>工具在 {@code aics.security.tool-permissions} 中配置了允许角色时，
 *       当前用户角色不在其中即拒绝（工具端重新校验，不能只信模型参数）；</li>
 *   <li>未配置的工具对所有已登录用户开放（资源归属校验仍在工具内部完成，
 *       如 {@code OrderLocatorTool} 本人订单匹配）；</li>
 *   <li>拒绝时记录 {@link SecurityEventType#TOOL_UNAUTHORIZED} 审计事件。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ToolAuthorizationService {

    private final SecurityProperties properties;
    private final UserRoleResolver roleResolver;
    private final SecurityAuditRecorder auditRecorder;

    /**
     * 校验工具调用权限。
     *
     * @param userId       当前用户 ID
     * @param toolName     工具名
     * @param paramsDigest 参数摘要（审计用，可为 null）
     * @return 授权结果（拒绝时已记录审计事件）
     */
    public ToolAuthResult authorize(Long userId, String toolName, String paramsDigest) {
        // 学习点（纵深防御）：LLM 只是“建议调用哪个工具、传什么参数”，
        // 工具端必须重新校验权限——模型参数可被注入诱导，不能只信模型参数。
        // 即使 LLM 被诱导调用受限工具，这里也会在工具真正执行前拒绝（拒绝即记录审计）。
        String role = roleResolver.resolve(userId);
        List<String> allowed = properties.getToolPermissions() == null
                ? null : properties.getToolPermissions().get(toolName);
        // 未配置权限矩阵：所有已登录用户可调用（归属校验由工具内部保证）
        if (allowed == null || allowed.isEmpty()) {
            return ToolAuthResult.allowed(role);
        }
        if (allowed.contains(role)) {
            return ToolAuthResult.allowed(role);
        }
        auditRecorder.record(SecurityEventType.TOOL_UNAUTHORIZED, "TOOL", userId, toolName,
                paramsDigest, "BLOCK", "角色 " + role + " 无权限调用工具 " + toolName);
        return ToolAuthResult.denied(role, "角色 " + role + " 无权限调用工具 " + toolName);
    }
}
