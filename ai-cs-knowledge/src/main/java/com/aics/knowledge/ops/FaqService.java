package com.aics.knowledge.ops;

import com.aics.knowledge.entity.KnowledgeDocument;
import com.aics.knowledge.entity.KnowledgeFaq;
import com.aics.knowledge.mapper.KnowledgeFaqMapper;
import com.aics.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * FAQ 收录服务 —— 把运营确认的主题变成可检索的知识。
 *
 * <h3>学习要点（技术：复用增量同步链路）</h3>
 * <ul>
 *   <li><b>两步落库</b>：①写入 kb_faq 表（FAQ 元数据）；②调用 KnowledgeService.createDocument
 *       创建知识文档，后者会发 RocketMQ 消息异步向量化到 Chroma（复用已有增量同步能力）。</li>
 *   <li><b>为什么复用</b>：知识文档的"DB 落库 → MQ → 向量化"链路在 002 功能已建好，
 *       FAQ 作为知识文档的一种直接复用，零新增链路、天然幂等可重试。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaqService {

    private final KnowledgeFaqMapper faqMapper;
    private final KnowledgeService knowledgeService;

    /**
     * 收录 FAQ。
     *
     * @param suggestion FAQ 建议
     * @return 结果（faqId + 是否触发向量化）
     */
    public Map<String, Object> adopt(FaqSuggestion suggestion) {
        if (suggestion == null || !StringUtils.hasText(suggestion.getQuestion())
                || !StringUtils.hasText(suggestion.getAnswer())) {
            throw new IllegalArgumentException("FAQ 问题与答案不能为空");
        }
        KnowledgeFaq faq = new KnowledgeFaq();
        faq.setQuestion(suggestion.getQuestion().trim());
        faq.setAnswer(suggestion.getAnswer().trim());
        faq.setKnowledgeBase(StringUtils.hasText(suggestion.getKnowledgeBase())
                ? suggestion.getKnowledgeBase() : "faq");
        faq.setTopicId(suggestion.getClusterTopicId());
        faq.setStatus("DRAFT");
        faqMapper.insert(faq);
        log.info("FAQ 已收录: id={}, question={}", faq.getId(), faq.getQuestion());

        // 创建知识文档（FAQ 作为知识文档入库 → RocketMQ 异步向量化）
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle("FAQ-" + faq.getQuestion());
        doc.setContent(faq.getQuestion() + "\n" + faq.getAnswer());
        doc.setDocType("markdown");
        doc.setTags("faq," + faq.getKnowledgeBase());
        knowledgeService.createDocument(doc);

        return Map.of("faqId", faq.getId(), "vectorized", true);
    }
}