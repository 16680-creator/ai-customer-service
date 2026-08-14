package com.aics.chat.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档加载器
 * 负责从不同格式的文件中加载文档内容，用于 RAG 检索增强生成
 *
 * <p>提供三种加载策略，由 {@link com.aics.chat.service.KnowledgeBaseService#addFile} 按文件后缀选择：</p>
 * <ul>
 *   <li>{@link #loadPdf} —— PDF，使用 {@link PagePdfDocumentReader} 按页读取，
 *       每页生成一个 Document，metadata 带 {@code page_number}（用于引用溯源）。</li>
 *   <li>{@link #loadText} —— 纯文本（.txt/.md 等），使用 {@link TextReader} 整篇读取。</li>
 *   <li>{@link #loadTika} —— Office/HTML 等，使用 Apache Tika（{@link TikaDocumentReader}）
 *       解析 .docx / .xlsx / .html / .htm 等富文档格式，提取正文文本。</li>
 * </ul>
 *
 * <p>加载后的原始 Document 会由 {@link com.aics.chat.service.KnowledgeBaseService#addChunks}
 * 进一步用 {@code TokenTextSplitter} 按 token 切分成更小的片段（chunk），再向量化入库。</p>
 */
@Component
public class DocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(DocumentLoader.class);

    /**
     * 加载 PDF 文档
     *
     * @param resource PDF 文件资源
     * @return 文档列表
     */
    public List<Document> loadPdf(Resource resource) {
        log.info("加载PDF文档: {}", resource.getFilename());
        try {
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                    resource,
                    PdfDocumentReaderConfig.builder()
                            .withPageTopMargin(0)
                            .withPageBottomMargin(0)
                            .withPagesPerDocument(1)
                            .build()
            );
            List<Document> documents = pdfReader.get();
            log.info("PDF文档加载完成, 共{}页", documents.size());
            return documents;
        } catch (Exception e) {
            log.error("PDF文档加载失败: {}", resource.getFilename(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 加载文本文档
     *
     * @param resource 文本文件资源
     * @return 文档列表
     */
    public List<Document> loadText(Resource resource) {
        log.info("加载文本文档: {}", resource.getFilename());
        try {
            TextReader textReader = new TextReader(resource);
            List<Document> documents = textReader.get();
            log.info("文本文档加载完成, 共{}段", documents.size());
            return documents;
        } catch (Exception e) {
            log.error("文本文档加载失败: {}", resource.getFilename(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 加载 Tika 支持的文档（docx/xlsx/html/htm 等）
     *
     * @param resource 文档文件资源
     * @return 文档列表
     */
    public List<Document> loadTika(Resource resource) {
        log.info("加载Tika文档: {}", resource.getFilename());
        try {
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            return reader.get();
        } catch (Exception e) {
            log.error("Tika文档加载失败: {}", resource.getFilename(), e);
            return new ArrayList<>();
        }
    }
}
