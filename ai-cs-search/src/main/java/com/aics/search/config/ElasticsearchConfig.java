package com.aics.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.Arrays;

/**
 * Elasticsearch 客户端配置
 *
 * <p>混合检索（hybrid search）的关键词检索路使用 Elasticsearch：
 * 关键词路（BM25/multiMatch）+ Chroma 向量路（语义相似度）经 RRF 融合。
 * 地址从 spring.elasticsearch.uris 读取，支持逗号分隔的多个节点，默认 http://localhost:9200。</p>
 */
@Configuration
public class ElasticsearchConfig {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

    /**
     * 创建 Elasticsearch Java Client（官方 elasticsearch-java 8.12，RestClient + RestClientTransport）。
     *
     * @param uris ES 地址，逗号分隔，如 http://localhost:9200,http://node2:9200
     * @return ElasticsearchClient
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(
            @Value("${spring.elasticsearch.uris:http://localhost:9200}") String uris) {
        String uri = Arrays.stream(uris.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse("http://localhost:9200");
        log.info("创建 ElasticsearchClient: uri={}", uri);
        URI esUri = URI.create(uri);
        RestClient restClient = RestClient.builder(
                new HttpHost(esUri.getHost(), esUri.getPort(), esUri.getScheme())).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
