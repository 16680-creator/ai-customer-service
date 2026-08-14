package com.aics.chat.security;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索 ACL 过滤器（3.2 F5 RAG 数据防泄漏）。
 *
 * <p>行为契约（对应 Gherkin Feature 05）：在检索阶段按租户/角色/文档 ACL 过滤，</p>
 * <ul>
 *   <li><b>知识库级 ACL</b>：{@code aics.security.rag-acl-knowledge-bases}（kbId -&gt; 允许角色），
 *       用户角色无权限访问该知识库时，整个检索结果置空；</li>
 *   <li><b>文档级 ACL</b>：{@code aics.security.rag-acl-documents}（docId -&gt; 允许角色），
 *       无权限文档从召回结果中剔除；</li>
 *   <li>被过滤的文档记录 {@link SecurityEventType#RAG_ACL_DENIED} 审计事件；
 *       多轮对话每次检索都执行过滤（权限回收后立即生效）。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RagAclFilter {

    private final SecurityProperties properties;
    private final UserRoleResolver roleResolver;
    private final SecurityAuditRecorder auditRecorder;

    /**
     * 过滤检索结果：无权限的文档不进入 RAG 上下文，回答不得引用。
     *
     * @param knowledgeBase 知识库标识
     * @param docs          检索召回结果（可为 null/空）
     * @param userId        当前用户 ID（可为 null，按默认角色 USER 处理）
     * @return 过滤后的文档列表
     */
    public List<Document> filter(String knowledgeBase, List<Document> docs, Long userId) {
        // 学习点：为什么在“检索阶段”过滤而不是“回答后补救”？
        // 过滤发生在 buildContext 之前——无权限文档根本不进入 Prompt，
        // 模型无从引用、回答天然不含泄露内容；事后补救则引用可能已生成。
        if (docs == null || docs.isEmpty()) {
            return docs;
        }
        String role = roleResolver.resolve(userId);

        // 1. 知识库级 ACL：无权限直接整库过滤（租户/角色隔离）
        if (!canAccessKnowledgeBase(knowledgeBase, role)) {
            auditRecorder.record(SecurityEventType.RAG_ACL_DENIED, "RETRIEVAL", userId,
                    "kb=" + knowledgeBase, null, "FILTER",
                    "角色 " + role + " 无权限访问知识库 " + knowledgeBase);
            return List.of();
        }

        // 2. 文档级 ACL：逐条过滤无权限文档
        List<Document> result = new ArrayList<>();
        for (Document doc : docs) {
            String docKey = docKey(doc);
            if (canAccessDoc(docKey, role)) {
                result.add(doc);
            } else {
                auditRecorder.record(SecurityEventType.RAG_ACL_DENIED, "RETRIEVAL", userId,
                        "doc=" + docKey, null, "FILTER",
                        "角色 " + role + " 无权限访问文档 " + docKey);
            }
        }
        return result;
    }

    private boolean canAccessKnowledgeBase(String knowledgeBase, String role) {
        List<String> allowed = properties.getRagAclKnowledgeBases() == null
                ? null : properties.getRagAclKnowledgeBases().get(knowledgeBase);
        return allowed == null || allowed.isEmpty() || allowed.contains(role);
    }

    private boolean canAccessDoc(String docKey, String role) {
        List<String> allowed = properties.getRagAclDocuments() == null
                ? null : properties.getRagAclDocuments().get(docKey);
        return allowed == null || allowed.isEmpty() || allowed.contains(role);
    }

    /**
     * 文档标识：优先取 metadata.documentId（与引用溯源一致），否则回退 doc.getId()。
     */
    private static String docKey(Document doc) {
        Object id = doc.getMetadata() == null ? null : doc.getMetadata().get("documentId");
        return id == null ? doc.getId() : String.valueOf(id);
    }
}
