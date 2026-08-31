package com.aics.search.cdc;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MySQL product 表 CDC → Elasticsearch 商品目录索引。
 *
 * <p>幂等策略：ES document id 固定为 product.id，同一条 INSERT/UPDATE 重放均是覆盖 upsert；
 * DELETE 反复执行时 documentMissing 忽略。Canal 事件的最终一致性由定时对账兜底。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "c-product-sync", consumerGroup = "search-product-cdc-consumer")
public class ProductCdcConsumer implements RocketMQListener<CanalChangeEvent> {

    static final String INDEX = "product_catalog";
    private final ElasticsearchClient elasticsearchClient;

    @Value("${aics.cdc.product.enabled:false}")
    private boolean enabled;

    @Override
    public void onMessage(CanalChangeEvent event) {
        if (!enabled || event == null || !"product".equalsIgnoreCase(event.getTable())
                || event.getData() == null) {
            return;
        }
        for (Map<String, Object> row : event.getData()) {
            syncOne(event.getType(), row);
        }
    }

    void syncOne(String type, Map<String, Object> row) {
        Object id = row.get("id");
        if (id == null) {
            log.warn("忽略无主键 CDC 商品事件: {}", row);
            return;
        }
        String docId = String.valueOf(id);
        try {
            if ("DELETE".equalsIgnoreCase(type) || "1".equals(String.valueOf(row.get("deleted")))) {
                try {
                    elasticsearchClient.delete(d -> d.index(INDEX).id(docId));
                } catch (Exception ignored) {
                    // 重复 delete/doc 不存在保持幂等
                }
                return;
            }
            elasticsearchClient.index(i -> i.index(INDEX).id(docId).document(row));
            log.debug("CDC 商品索引 upsert: id={}", docId);
        } catch (Exception e) {
            // 抛出使 RocketMQ 重试；ES 暂时不可用不允许静默丢消息
            throw new IllegalStateException("CDC 商品索引同步失败: id=" + docId, e);
        }
    }
}
