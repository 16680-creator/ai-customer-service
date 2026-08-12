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
 * golden 测试集加载器。
 *
 * <p>支持 {@code classpath:} 与 {@code file:} 前缀路径，JSON 为 GoldenCase 数组；
 * 单条非法（缺 question）跳过并告警，不影响整体加载。</p>
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