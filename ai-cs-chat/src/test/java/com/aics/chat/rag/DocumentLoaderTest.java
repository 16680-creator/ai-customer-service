package com.aics.chat.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentLoader 单元测试
 *
 * <p>loadTika 需要真实文件资源：测试在 {@code @TempDir} 中动态生成
 * docx（最小 OOXML 包）、xlsx（最小 OOXML 包）、html、md 文件，
 * 验证 TikaDocumentReader 能解析出非空 Document 列表。</p>
 */
class DocumentLoaderTest {

    private DocumentLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        loader = new DocumentLoader();
    }

    @Test
    @DisplayName("loadTika: 解析 docx 返回非空文档列表且内容正确")
    void loadTika_withDocx_returnsDocuments() throws IOException {
        Path file = createDocx("Hello docx content from tika test");

        List<Document> documents = loader.loadTika(new FileSystemResource(file));

        assertThat(documents).isNotEmpty();
        assertThat(documents.get(0).getText()).contains("Hello docx content from tika test");
    }

    @Test
    @DisplayName("loadTika: 解析 xlsx 返回非空文档列表且内容正确")
    void loadTika_withXlsx_returnsDocuments() throws IOException {
        Path file = createXlsx("Hello xlsx content from tika test");

        List<Document> documents = loader.loadTika(new FileSystemResource(file));

        assertThat(documents).isNotEmpty();
        assertThat(documents.get(0).getText()).contains("Hello xlsx content from tika test");
    }

    @Test
    @DisplayName("loadTika: 解析 html 返回非空文档列表且提取正文文本")
    void loadTika_withHtml_returnsDocuments() throws IOException {
        Path file = tempDir.resolve("test.html");
        Files.writeString(file, """
                <!DOCTYPE html>
                <html>
                <head><title>Test Page</title></head>
                <body>
                <h1>Product Guide</h1>
                <p>Hello html content from tika test</p>
                </body>
                </html>
                """, StandardCharsets.UTF_8);

        List<Document> documents = loader.loadTika(new FileSystemResource(file));

        assertThat(documents).isNotEmpty();
        assertThat(documents.get(0).getText()).contains("Hello html content from tika test");
    }

    @Test
    @DisplayName("loadTika: 解析 md 返回非空文档列表且保留正文文本")
    void loadTika_withMarkdown_returnsDocuments() throws IOException {
        Path file = tempDir.resolve("test.md");
        Files.writeString(file, """
                # 商品退换货规则

                Hello markdown content from tika test

                - 7 天无理由退货
                - 15 天质量问题换货
                """, StandardCharsets.UTF_8);

        List<Document> documents = loader.loadTika(new FileSystemResource(file));

        assertThat(documents).isNotEmpty();
        assertThat(documents.get(0).getText()).contains("Hello markdown content from tika test");
    }

    @Test
    @DisplayName("loadTika: 不存在的文件返回空列表（异常降级）")
    void loadTika_withMissingFile_returnsEmpty() {
        Resource resource = new FileSystemResource(tempDir.resolve("not-exist.docx"));

        List<Document> documents = loader.loadTika(resource);

        assertThat(documents).isEmpty();
    }

    // ---------- 测试辅助：动态生成最小 OOXML 包 ----------

    /**
     * 生成最小合法 docx（zip 包：ContentTypes + rels + document.xml）。
     */
    private Path createDocx(String text) throws IOException {
        Path file = tempDir.resolve("test.docx");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                    </Types>
                    """.getBytes(StandardCharsets.UTF_8));

            zip.putNextEntry(new ZipEntry("_rels/.rels"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                    </Relationships>
                    """.getBytes(StandardCharsets.UTF_8));

            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                      <w:body>
                        <w:p><w:r><w:t>%s</w:t></w:r></w:p>
                      </w:body>
                    </w:document>
                    """).formatted(text).getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    /**
     * 生成最小合法 xlsx（zip 包：ContentTypes + rels + workbook + sheet1）。
     */
    private Path createXlsx(String text) throws IOException {
        Path file = tempDir.resolve("test.xlsx");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """.getBytes(StandardCharsets.UTF_8));

            zip.putNextEntry(new ZipEntry("_rels/.rels"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """.getBytes(StandardCharsets.UTF_8));

            zip.putNextEntry(new ZipEntry("xl/workbook.xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets>
                    </workbook>
                    """.getBytes(StandardCharsets.UTF_8));

            zip.putNextEntry(new ZipEntry("xl/_rels/workbook.xml.rels"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    </Relationships>
                    """.getBytes(StandardCharsets.UTF_8));

            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <sheetData>
                        <row r="1"><c r="A1" t="inlineStr"><is><t>%s</t></is></c></row>
                      </sheetData>
                    </worksheet>
                    """).formatted(text).getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }
}
