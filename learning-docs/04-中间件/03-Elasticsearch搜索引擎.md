# Elasticsearch 搜索引擎

> 本项目使用 **Elasticsearch 8.12** 实现商品搜索、知识库全文检索。
> 对应项目文件：`ai-cs-search` 模块、`docker-compose.yml`（ES 容器）

---

## 一、为什么需要 Elasticsearch？

```
MySQL LIKE 搜索：
  SELECT * FROM product WHERE name LIKE '%蓝牙%耳机%'
  → 全表扫描、不支持分词、不能按相关度排序、百万级数据很慢

Elasticsearch 搜索：
  → 倒排索引、中文分词、相关度评分、百万级毫秒响应
```

### 本项目的使用场景

| 场景 | 索引 | 说明 |
|------|------|------|
| 商品搜索 | product_index | 按名称、描述、分类搜索商品 |
| 知识库检索 | knowledge_index | AI 对话时检索相关文档（RAG） |
| 搜索建议 | suggest_index | 输入联想、热门搜索 |

---

## 二、核心概念对照

| MySQL | Elasticsearch | 说明 |
|-------|--------------|------|
| Database | Index（索引） | 数据的容器 |
| Table | Type（7.x 后废弃） | — |
| Row | Document（文档） | 一条数据（JSON） |
| Column | Field（字段） | 一个属性 |
| Schema | Mapping（映射） | 字段类型定义 |
| SQL | DSL（Query DSL） | 查询语言 |

---

## 三、Docker 部署

```yaml
# docker-compose.yml
elasticsearch:
  image: elasticsearch:8.12.0
  container_name: aics-elasticsearch
  ports:
    - "9200:9200"     # HTTP API
    - "9300:9300"     # 节点间通信
  environment:
    - discovery.type=single-node       # 单节点模式
    - xpack.security.enabled=false     # 关闭安全认证（开发用）
    - xpack.security.http.ssl.enabled=false
    - ES_JAVA_OPTS=-Xms512m -Xmx512m  # JVM 内存
  volumes:
    - es-data:/usr/share/elasticsearch/data
  healthcheck:
    test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
    interval: 15s
    timeout: 10s
    retries: 10
    start_period: 40s    # ES 启动较慢，给 40 秒
```

验证：`curl http://localhost:9200` 返回集群信息 JSON。

---

## 四、索引操作（REST API）

### 4.1 创建索引

```bash
# 创建商品索引
curl -X PUT "localhost:9200/product_index" -H 'Content-Type: application/json' -d'
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "ik_smart_pinyin": {
          "type": "custom",
          "tokenizer": "ik_max_word"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id":          { "type": "long" },
      "name":        { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "description": { "type": "text", "analyzer": "ik_max_word" },
      "category":    { "type": "keyword" },
      "price":       { "type": "double" },
      "sales":       { "type": "integer" },
      "createTime":  { "type": "date" }
    }
  }
}'
```

### 4.2 字段类型说明

| 类型 | 用途 | 是否分词 |
|------|------|---------|
| `text` | 全文搜索（商品名、描述） | 是 |
| `keyword` | 精确匹配（分类、状态） | 否 |
| `long/integer/double` | 数值 | 否 |
| `date` | 日期 | 否 |
| `boolean` | 布尔 | 否 |

### 4.3 文档 CRUD

```bash
# 新增文档
curl -X POST "localhost:9200/product_index/_doc" -H 'Content-Type: application/json' -d'
{
  "id": 1001,
  "name": "无线蓝牙耳机 降噪运动款",
  "description": "高品质蓝牙5.3，主动降噪，续航30小时",
  "category": "数码配件",
  "price": 199.00,
  "sales": 5680
}'

# 查询文档
curl "localhost:9200/product_index/_doc/1001"

# 删除文档
curl -X DELETE "localhost:9200/product_index/_doc/1001"
```

---

## 五、搜索查询（DSL）

### 5.1 全文搜索

```bash
curl -X POST "localhost:9200/product_index/_search" -H 'Content-Type: application/json' -d'
{
  "query": {
    "multi_match": {
      "query": "蓝牙耳机",
      "fields": ["name^3", "description"],
      "type": "best_fields"
    }
  },
  "highlight": {
    "fields": {
      "name": { "pre_tags": ["<em>"], "post_tags": ["</em>"] }
    }
  },
  "from": 0,
  "size": 10,
  "sort": [
    { "_score": "desc" },
    { "sales": "desc" }
  ]
}'
```

### 5.2 组合查询（Bool Query）

```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "耳机" } }
      ],
      "filter": [
        { "range": { "price": { "gte": 100, "lte": 500 } } },
        { "term": { "category": "数码配件" } }
      ],
      "should": [
        { "range": { "sales": { "gte": 1000, "boost": 2 } } }
      ]
    }
  }
}
```

- `must`：必须满足（参与评分）
- `filter`：必须满足（不参与评分，可缓存，更快）
- `should`：可选满足（加分项）

---

## 六、Java 客户端集成

### 6.1 依赖

```xml
<dependency>
    <groupId>co.elastic.clients</groupId>
    <artifactId>elasticsearch-java</artifactId>
    <version>8.12.2</version>
</dependency>
```

### 6.2 配置

```java
@Configuration
public class ElasticsearchConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http")
        ).build();
        
        ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper()
        );
        
        return new ElasticsearchClient(transport);
    }
}
```

### 6.3 搜索服务

```java
@Service
public class ProductSearchService {

    @Autowired
    private ElasticsearchClient esClient;

    public List<ProductVO> search(String keyword, int page, int size) throws IOException {
        SearchResponse<ProductDoc> response = esClient.search(s -> s
            .index("product_index")
            .query(q -> q
                .bool(b -> b
                    .must(m -> m
                        .multiMatch(mm -> mm
                            .query(keyword)
                            .fields("name^3", "description")
                        )
                    )
                    .filter(f -> f
                        .term(t -> t.field("deleted").value(false))
                    )
                )
            )
            .highlight(h -> h
                .fields("name", hf -> hf
                    .preTags("<em>")
                    .postTags("</em>")
                )
            )
            .from(page * size)
            .size(size)
            .sort(sort -> sort.score(s2 -> s2.order(SortOrder.Desc))),
            ProductDoc.class
        );

        return response.hits().hits().stream()
            .map(hit -> {
                ProductDoc doc = hit.source();
                // 用高亮内容替换原始名称
                if (hit.highlight() != null && hit.highlight().containsKey("name")) {
                    doc.setName(hit.highlight().get("name").get(0));
                }
                return convertToVO(doc);
            })
            .toList();
    }
}
```

---

## 七、数据同步策略

```
MySQL（主数据）→ Elasticsearch（搜索副本）

方案一：同步双写（简单，适合小项目）
  Service 中：save to MySQL → save to ES

方案二：异步消息（本项目推荐）
  Service → 发 MQ 消息 → Consumer 写入 ES

方案三：Canal 监听 Binlog（最可靠）
  MySQL Binlog → Canal → ES
```

---

## 八、动手练习

1. 启动 ES：`docker-compose up -d elasticsearch`
2. 验证：`curl http://localhost:9200/_cluster/health?pretty`
3. 创建索引、插入几条商品数据
4. 用 DSL 做全文搜索、范围过滤、高亮
5. 在 Kibana Dev Tools 中练习（可选部署 Kibana）

---

## 学习检查清单

- [ ] 理解倒排索引的原理
- [ ] 理解 text vs keyword 的区别
- [ ] 会创建索引和 Mapping
- [ ] 会写 Bool Query（must/filter/should）
- [ ] 理解分词器（ik_smart vs ik_max_word）
- [ ] 会在 Java 中使用 ElasticsearchClient
- [ ] 理解 MySQL → ES 的数据同步方案

---

## 下一步

→ [04-MinIO对象存储](./04-MinIO对象存储.md)
