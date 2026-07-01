package com.aics.search.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 搜索服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public Result<List<Map<String, Object>>> search(String index, String query, int page, int size) {
        log.info("全文搜索: index={}, query={}, page={}, size={}", index, query, page, size);
        try {
            Criteria criteria = new Criteria("content").is(query)
                    .or(new Criteria("title").is(query));
            Query searchQuery = new CriteriaQuery(criteria)
                    .setPageable(org.springframework.data.domain.PageRequest.of(page - 1, size));

            SearchHits<Map> searchHits = elasticsearchTemplate.search(searchQuery, Map.class, org.springframework.data.elasticsearch.core.IndexCoordinates.of(index));
            List<Map<String, Object>> results = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(content -> (Map<String, Object>) content)
                    .toList();

            log.info("搜索完成: index={}, 结果数={}", index, results.size());
            return Result.success(results);
        } catch (Exception e) {
            log.error("搜索失败: index={}, query={}", index, query, e);
            throw new BusinessException(ResultCode.SEARCH_QUERY_FAIL, "搜索查询失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> createIndex(String index, Map<String, Object> mappings) {
        log.info("创建索引: index={}", index);
        try {
            boolean created = elasticsearchTemplate.indexOps(org.springframework.data.elasticsearch.core.IndexCoordinates.of(index))
                    .create();
            if (!created) {
                throw new BusinessException(ResultCode.SEARCH_INDEX_CREATE_FAIL, "索引创建失败");
            }
            log.info("索引创建成功: index={}", index);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("索引创建异常: index={}", index, e);
            throw new BusinessException(ResultCode.SEARCH_INDEX_CREATE_FAIL, "索引创建失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> indexDocument(String index, Map<String, Object> document) {
        log.info("索引文档: index={}", index);
        try {
            elasticsearchTemplate.save(document, org.springframework.data.elasticsearch.core.IndexCoordinates.of(index));
            log.info("文档索引成功: index={}", index);
            return Result.success();
        } catch (Exception e) {
            log.error("文档索引失败: index={}", index, e);
            throw new BusinessException(ResultCode.SEARCH_INDEX_CREATE_FAIL, "文档索引失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteIndex(String index) {
        log.info("删除索引: index={}", index);
        try {
            elasticsearchTemplate.indexOps(org.springframework.data.elasticsearch.core.IndexCoordinates.of(index))
                    .delete();
            log.info("索引删除成功: index={}", index);
            return Result.success();
        } catch (Exception e) {
            log.error("索引删除失败: index={}", index, e);
            throw new BusinessException(ResultCode.SEARCH_INDEX_NOT_FOUND, "索引删除失败: " + e.getMessage());
        }
    }
}
