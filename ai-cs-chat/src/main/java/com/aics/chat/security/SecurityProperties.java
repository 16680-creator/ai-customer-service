package com.aics.chat.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 安全网关与 Guardrails 配置项（前缀 {@code aics.security}）。
 *
 * <p>3.2 P0 全部 Guardrail 的规则与权限矩阵均可配置，未配置时使用内置默认值：</p>
 * <ul>
 *   <li>{@code injection-extra-rules}：注入检测追加规则（每条 {@code 描述|正则}）；</li>
 *   <li>{@code content-*}：内容安全分类正则与审核服务故障降级模式（BLOCK/ALLOW）；</li>
 *   <li>{@code tool-permissions}：工具名 -&gt; 允许角色列表（角色-权限矩阵）；</li>
 *   <li>{@code user-roles}：userId -&gt; 角色（未配置默认 USER）；</li>
 *   <li>{@code rag-acl-*}：知识库/文档级 ACL（kbId/docId -&gt; 允许角色列表）；</li>
 *   <li>{@code sql-*}：NL2SQL 表/列白名单。</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aics.security")
public class SecurityProperties {

    /** 注入检测追加规则：每条 "描述|正则"，与内置规则合并生效 */
    private List<String> injectionExtraRules = List.of();

    /** 内容安全分类：分类名 -> 正则列表（命中任一即违规） */
    private Map<String, List<String>> contentCategories = new LinkedHashMap<>(Map.of(
            "ABUSE", List.of("(妈的|傻逼|去死|白痴|废物)"),
            "ILLEGAL", List.of("(毒品|枪支|走私|洗钱|赌博网站|诈骗)"),
            "PORNO", List.of("(色情|裸聊|约炮|成人视频)"),
            "SELF_HARM", List.of("(自杀|轻生|不想活了|割腕)")
    ));

    /** 内容审核服务故障降级模式：BLOCK（默认拦截）/ ALLOW（放行并告警） */
    private String contentFailMode = "BLOCK";

    /** 输出侧内容审核开关（false 时仅做输入审核） */
    private boolean contentOutputCheckEnabled = true;

    /** 工具权限矩阵：工具名 -> 允许角色列表；未配置的工具对所有已登录用户开放 */
    private Map<String, List<String>> toolPermissions = new LinkedHashMap<>();

    /** 用户角色映射：userId -> 角色；未配置的用户默认角色 USER */
    private Map<Long, String> userRoles = new LinkedHashMap<>();

    /** RAG 检索 ACL：知识库标识 -> 允许角色列表（租户/角色级过滤） */
    private Map<String, List<String>> ragAclKnowledgeBases = new LinkedHashMap<>();

    /** RAG 检索 ACL：文档标识 -> 允许角色列表（文档级过滤） */
    private Map<String, List<String>> ragAclDocuments = new LinkedHashMap<>();

    /** NL2SQL 表白名单：库标识 -> 允许的表名列表（空集合=不启用表白名单） */
    private Map<String, List<String>> sqlTableWhitelist = new LinkedHashMap<>();

    /** NL2SQL 列白名单：库标识 -> 允许的 "表.列" 列表（空集合=不启用列白名单） */
    private Map<String, List<String>> sqlColumnWhitelist = new LinkedHashMap<>();

    /** 安全审计开关（false 时审计事件仅内存缓存不落库） */
    private boolean auditEnabled = true;
}
