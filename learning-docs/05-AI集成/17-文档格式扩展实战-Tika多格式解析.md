# 文档格式扩展实战：Tika 多格式解析

> 本文档讲解知识库**文档格式扩展**：用 Apache Tika 让 RAG 支持 docx / xlsx / html 等
> 常见办公格式，而不再局限于 PDF / TXT。
> 前置知识：[10-RAG向量检索实战.md](10-RAG向量检索实战.md)。
>
> **核心目标**：上传任何常见文档 → 自动路由到正确的解析器 → 提取纯文本 → 向量化入库。

---

## 一、实战背景：为什么需要 Tika？

客服知识库的资料远不止 PDF/TXT：
- 运营同事的习惯是 Word（.docx）写话术、Excel（.xlsx）维护价格表；
- 产品 FAQ 可能是 HTML 页面导出的；
- 早期采购的 .doc / .ppt 老文件也有归档价值。

如果只支持 PDF/TXT，意味着这些资料要么人工转格式，要么放弃入库——**知识库覆盖度直接打折**。
Apache Tika 是 Java 生态最成熟的内容检测与文本提取库，一个依赖解决几十种格式。

```
旧：上传 PDF ──► PagePdfDocumentReader        │ 上传 TXT ──► TextReader
                │                             │
新：上传 docx/xlsx/html/... ──► TikaDocumentReader（统一提取纯文本）
    格式路由：.pdf → PDF 按页读；.docx/.xlsx/.html/.htm → Tika；其余 → 文本读
```

---

## 二、Apache Tika 简介

**Tika**（Apache 顶级项目）是一个内容分析工具包，核心能力：

1. **格式检测（Detector）**：不依赖扩展名，通过文件头（Magic Bytes）识别真实格式；
2. **文本提取（Parser）**：把二进制/结构化文档统一提取为纯文本；
3. **元数据提取**：提取作者、创建时间、标题等元信息；
4. **支持格式**（300+）：Office 全家桶（doc/docx/xls/xlsx/ppt/pptx）、PDF、HTML/XML、
   RTF、OpenDocument（odt/ods/odp）、EPUB、邮件（eml/msg）、压缩包等。

**Spring AI 集成**：`spring-ai-tika-document-reader` 提供 `TikaDocumentReader`，
底层封装 Tika，把文件读成 Spring AI 的 `Document` 列表，与向量化链路无缝衔接。

---

## 三、依赖引入

文件：`ai-cs-chat/pom.xml`

```xml
<!-- Spring AI Tika Document Reader（docx/xlsx/html 等格式解析） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-tika-document-reader</artifactId>
</dependency>
```

> 版本由父 POM 的 Spring AI BOM（本项目 1.1.4）统一管理，无需写 version。
> 该依赖会传递引入 Apache Tika 核心库。

---

## 四、DocumentLoader 实现（格式路由）

文件：`ai-cs-chat/src/main/java/com/aics/chat/rag/DocumentLoader.java`

### 1. 三种加载器并存

```java
@Component
public class DocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(DocumentLoader.class);

    /** 加载 PDF 文档（按页读取，metadata 带 page_number） */
    public List<Document> loadPdf(Resource resource) {
        log.info("加载PDF文档: {}", resource.getFilename());
        try {
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                    resource,
                    PdfDocumentReaderConfig.builder()
                            .withPageTopMargin(0)
                            .withPageBottomMargin(0)
                            .withPagesPerDocument(1)   // 每页一个 Document
                            .build()
            );
            List<Document> documents = pdfReader.get();
            log.info("PDF文档加载完成, 共{}页", documents.size());
            return documents;
        } catch (Exception e) {
            log.error("PDF文档加载失败: {}", resource.getFilename(), e);
            return new ArrayList<>();   // 失败返回空列表，不让入库流程崩溃
        }
    }

    /** 加载文本文档 */
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

    /** 加载 Tika 支持的文档（docx/xlsx/html/htm 等） */
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
```

**设计要点**：
- **三个方法各司其职**：PDF 走按页读（保留 page_number，引用溯源要用）、纯文本走 TextReader、
  Office/HTML 走 Tika——每种格式选择最合适的解析策略；
- **异常兜底**：解析失败返回空列表而不是抛异常，上传接口不会因为个别坏文件而整体失败；
- **PDF 按页分块**：`withPagesPerDocument(1)` 让每页成为独立 Document，
  检索命中后能精确到页码（配合引用溯源）。

### 2. 格式路由逻辑（KnowledgeBaseService.addFile）

文件：`ai-cs-chat/src/main/java/com/aics/chat/service/KnowledgeBaseService.java`

```java
/**
 * 将上传的文件（PDF/TXT/Markdown/Office/HTML）写入知识库（入库）。
 */
public int addFile(String knowledgeBase, MultipartFile file) {
    // 把 MultipartFile 转成 Spring Resource，交给 DocumentLoader 读取
    Resource resource = file.getResource();
    List<Document> documents;
    if (isPdf(file)) {
        // PDF：按页读取（metadata 带 page_number）
        documents = documentLoader.loadPdf(resource);
    } else if (isTika(file)) {
        // Office/HTML 等：由 Apache Tika 统一解析（docx/xlsx/html/htm）
        documents = documentLoader.loadTika(resource);
    } else {
        // 其余（txt/md 等纯文本）：按文本读取
        documents = documentLoader.loadText(resource);
    }
    return addChunks(knowledgeBase, documents);
}

/** 判断是否 PDF 文件 */
private boolean isPdf(MultipartFile file) {
    String name = file.getOriginalFilename();
    return name != null && name.toLowerCase().endsWith(".pdf");
}

/** 判断是否 Tika 可解析的文档（Office/HTML） */
private boolean isTika(MultipartFile file) {
    String name = file.getOriginalFilename();
    if (name == null) {
        return false;
    }
    String lower = name.toLowerCase();
    return lower.endsWith(".docx")
            || lower.endsWith(".xlsx")
            || lower.endsWith(".html")
            || lower.endsWith(".htm");
}
```

**路由优先级**：`.pdf` → PDF 按页读；`.docx/.xlsx/.html/.htm` → Tika；
其余（txt/md 等）→ 文本读。三者覆盖了客服知识库的绝大多数资料形态。

### 3. 路由图

```
上传文件 addFile()
   │
   ├─ 扩展名 .pdf ──────────► loadPdf()      PagePdfDocumentReader（按页，带 page_number）
   ├─ 扩展名 .docx/.xlsx/.html/.htm ─► loadTika()  TikaDocumentReader（统一文本提取）
   └─ 其他（txt/md/无扩展名）─► loadText()   TextReader（整篇一个 Document）
                     │
                     ▼
               addChunks()：TokenTextSplitter 分块 → metadata(documentId/title) → vectorStore.add()
```

---

## 五、支持的格式列表

| 扩展名 | 路由 | 解析方式 | 特点 |
|--------|------|----------|------|
| .pdf | PDF | PagePdfDocumentReader | 按页切分，带 page_number，引用可精确到页 |
| .docx | Tika | TikaDocumentReader | Word 正文提取，含表格文字 |
| .xlsx | Tika | TikaDocumentReader | Excel 单元格文本提取（每格内容按行拼接） |
| .html / .htm | Tika | TikaDocumentReader | 网页正文提取，去除标签 |
| .txt / .md | 文本 | TextReader | 纯文本整篇读取 |
| .doc / .ppt / .pptx / .odt 等 | — | Tika 底层能力支持 | 本项目路由未列，可自行扩展 isTika() |

> Tika 底层实际支持 300+ 格式（含 .doc/.ppt/.pptx/.rtf/.odt/.epub/.eml/.msg 等），
> 需要扩展时只需在 `isTika()` 里补扩展名即可，零额外代码。

---

## 六、配置说明

### 1. Tika 相关配置（一般无需额外配置）

`TikaDocumentReader` 开箱即用，无强制配置项。如要限制解析资源可自行配置 Tika 的超时等参数
（本项目未启用，保持默认）。

### 2. 文件上传大小限制（application.yml）

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 20MB        # 单个文件上限（Office 文件通常较大）
      max-request-size: 50MB     # 单次请求总大小
```

> 办公文档（尤其带图表的 xlsx/pptx）体积不小，默认 1MB 上限会导致上传失败，
> 建议按实际资料大小调整。

### 3. 依赖版本

| 组件 | 版本来源 |
|------|----------|
| spring-ai-tika-document-reader | 父 POM `spring-ai.version`（1.1.4） |
| Apache Tika（传递依赖） | 由 Spring AI BOM 锁定 |

---

## 七、验证方法

```bash
# 1. 准备测试文件：退货政策.docx（含标题和正文）

# 2. 上传 docx 入库
curl -X POST "http://localhost:8083/rag/knowledge-base/upload" \
     -F "knowledgeBase=product-manual" \
     -F "file=@退货政策.docx"
# 期望日志：加载Tika文档: 退货政策.docx
#         知识库[product-manual]入库完成, 共N个分块

# 3. 检索验证（确认内容真正被解析入库，而不是乱码/空）
curl "http://localhost:8083/rag/knowledge-base/search?knowledgeBase=product-manual&query=退货政策"

# 4. RAG 对话验证（命中 docx 内容并带引用）
curl -X POST "http://localhost:8083/chat/rag" \
     -H "Content-Type: application/json" \
     -d '{"sessionId":"s1","message":"退货政策是什么?","knowledgeBase":"product-manual"}'

# 5. 边界验证：
#    - 上传损坏的 docx → 日志出现"Tika文档加载失败"，接口不报 500
#    - 上传 xlsx / html 各试一次，确认同样入库成功
```

---

## 八、常见问题

**Q1：Tika 解析 xlsx 的效果如何？**
Tika 会把每个单元格的文本按行提取拼接，适合"文本型表格"（如价格表）。
带复杂公式/图表/批注的 xlsx，提取结果可能有噪声，建议入库前人工整理为纯文本表。

**Q2：解析失败会不会影响整个上传流程？**
不会。`DocumentLoader` 三个方法都 catch 异常并返回空列表，`addChunks` 对空列表
直接返回 0 分块，接口正常返回成功（日志记录失败原因）。如需"坏文件必须报错"，
可改为返回业务错误码。

**Q3：为什么 .doc（老版 Word）没在 isTika() 列表里？**
Tika 支持 .doc，只是本项目路由未列入。按需求在 `isTika()` 补一个 `.doc` 分支即可
（同样走 `loadTika`）。

**Q4：Tika 和 PDF 解析重复吗？为什么 PDF 不用 Tika？**
不重复。PDF 走 `PagePdfDocumentReader` 是为了**按页切分 + page_number**（引用溯源刚需）；
Tika 对 PDF 也能提文本，但丢失"按页"能力。所以 PDF 保留专用解析器，Tika 管 Office/HTML。

**Q5：中文编码乱码怎么办？**
Tika 自动检测编码（含 UTF-8/GBK），绝大多数场景无需干预。
若个别文件乱码，优先检查文件本身编码是否异常。

---

## 九、总结

| 环节 | 要点 |
|------|------|
| 为什么 | 知识库资料形态多样（Word/Excel/HTML），Tika 一次接入覆盖 |
| 依赖 | `spring-ai-tika-document-reader`（Spring AI BOM 管版本） |
| 路由 | .pdf → 按页读；docx/xlsx/html → Tika；其余 → 文本读 |
| 容错 | 解析失败返回空列表，入库流程不中断 |
| 扩展 | 新格式只需在 `isTika()` 加扩展名，零额外代码 |

至此，知识库从"只吃 PDF/TXT"升级为"Office/HTML 通吃"，
上传即解析、解析即入库，文档格式不再是知识库覆盖度的瓶颈。
