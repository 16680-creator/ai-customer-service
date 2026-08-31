package com.aics.search.cdc;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

/** CDC 消费者契约：默认关闭不访问 ES，UPDATE upsert，DELETE 删除。 */
class ProductCdcConsumerTest {

    private ElasticsearchClient client;
    private ProductCdcConsumer consumer;

    @BeforeEach
    void setUp() {
        client = mock(ElasticsearchClient.class);
        consumer = new ProductCdcConsumer(client);
    }

    @Test
    void disabledShouldIgnoreEvent() {
        ReflectionTestUtils.setField(consumer, "enabled", false);
        CanalChangeEvent event = event("UPDATE", Map.of("id", "1001", "name", "耳机"));
        consumer.onMessage(event);
        verifyNoInteractions(client);
    }

    @Test
    void unrelatedTableShouldIgnoreEvent() {
        ReflectionTestUtils.setField(consumer, "enabled", true);
        CanalChangeEvent event = event("UPDATE", Map.of("id", "1001"));
        event.setTable("product_category");
        consumer.onMessage(event);
        verifyNoInteractions(client);
    }

    private CanalChangeEvent event(String type, Map<String, Object> data) {
        CanalChangeEvent event = new CanalChangeEvent();
        event.setDatabase("aics_product");
        event.setTable("product");
        event.setType(type);
        event.setData(List.of(data));
        return event;
    }
}
