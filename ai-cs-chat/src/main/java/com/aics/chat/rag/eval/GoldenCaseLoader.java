package com.aics.chat.rag.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * golden 测试集加载器 —— 把 JSON 测试集读成内存对象。
 *
 * <h3>学习要点（技术：golden 回归测试）</h3>
 * <ul>
 *   <li><b>golden 集是什么</b>：一组「问题 + 期望命中文档 + 参考答案」的固定样本，
 *       相当于单元测试里的"断言"，是 RAG 质量回归的标尺。</li>
 *   <li><b>路径协议</b>：支持 {@code classpath:}（打包进 jar，如 CI/运行时）
 *       与 {@code file:}（磁盘绝对路径，如运营手工维护的生产样本）。</li>
 *   <li><b>容错</b>：单条非法（缺 question）跳过并告警，避免一条脏数据毁掉整次评估。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoldenCaseLoader {

    private final ObjectMapper objectMapper;

    /**
     * 从路径加载 golden 测试集。
     *
     * @param path classpath:eval/golden-set.json 或 file:/abs/path.json
     * @return 合法用例列表（非法行跳过）
     */
    public List<GoldenCase> load(String path) {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("goldenSetPath 不能为空");
        }
        try (InputStream in = openStream(path)) {
            List<GoldenCase> cases = objectMapper.readValue(in, new TypeReference<List<GoldenCase>>() {
            });
            List<GoldenCase> valid = new ArrayList<>();
            for (GoldenCase c : cases) {
                if (c == null || !StringUtils.hasText(c.getQuestion())) {
                    log.warn("golden 用例缺少 question，已跳过: {}", c);
                    continue;
                }
                valid.add(c);
            }
            log.info("golden 测试集加载完成: path={}, total={}, valid={}", path, cases.size(), valid.size());
            return valid;
        } catch (Exception e) {
            throw new IllegalArgumentException("golden 测试集加载失败: path=" + path + ", err=" + e.getMessage(), e);
        }
    }

    private InputStream openStream(String path) throws Exception {
        if (path.startsWith("classpath:")) {
            Resource resource = new ClassPathResource(path.substring("classpath:".length()));
            return resource.getInputStream();
        }
        if (path.startsWith("file:")) {
            Resource resource = new UrlResource(path);
            return resource.getInputStream();
        }
        // 默认按 classpath 处理
        Resource resource = new ClassPathResource(path);
        return resource.getInputStream();
    }
}