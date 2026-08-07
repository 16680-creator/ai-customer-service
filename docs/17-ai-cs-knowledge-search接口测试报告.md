# 17 - ai-cs-knowledge & ai-cs-search 接口测试报告

> 测试日期：2026-08-07
> 测试方式：本地启动服务 + 自动化 HTTP 用例（23 个）
> 结论：**23/23 用例符合预期**

---

## 一、测试环境

| 项目 | 说明 |
|---|---|
| ai-cs-knowledge | 端口 8082，MySQL 123.60.31.79:3306（knowledge_db） |
| ai-cs-search | 端口 8084，Elasticsearch 127.0.0.1:9200（**本次本地新装 ES 8.12.2**） |
| 网关 | 8080（/api/knowledge、/api/search 路由） |
| JDK / 构建 | JDK 21 / Spring Boot 3.2.5 / MyBatis-Plus 3.5.6 / spring-data-elasticsearch 5.2.5 |

> 测试同时覆盖**服务直连**与**网关链路**两种访问方式。

---

## 二、接口清单

### ai-cs-knowledge（5 个接口）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /knowledge | 创建知识文档 |
| GET | /knowledge/{id} | 查询文档详情 |
| GET | /knowledge/list | 分页查询文档列表 |
| PUT | /knowledge | 更新文档 |
| DELETE | /knowledge/{id} | 删除文档 |

### ai-cs-search（4 个接口）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /search/index/{index} | 创建索引 |
| DELETE | /search/index/{index} | 删除索引 |
| POST | /search/document/{index} | 索引文档 |
| GET | /search/{index} | 全文搜索 |

---

## 三、测试结果明细（23/23）

### 3.1 ai-cs-knowledge（12 个用例）

| 用例 | 结果 | 耗时 |
|---|---|---|
| 创建文档 | ✅ 200 | 0.23s |
| 详情查询 | ✅ 200 | 0.07s |
| 分页列表（默认） | ✅ 200 | 0.14s |
| 列表带 keyword 过滤 | ✅ 200 | 0.08s |
| 列表超大 page | ✅ 200 | 0.08s |
| 更新文档 | ✅ 200 | 0.15s |
| 删除文档 | ✅ 200 | 0.12s |
| 查询不存在 id（负向） | ✅ 500（业务异常） | 0.13s |
| 网关-创建 | ✅ 200 | 0.14s |
| 网关-列表 | ✅ 200 | 0.13s |
| 网关-查询不存在 id（负向） | ✅ 500（业务异常） | 0.07s |
| 完整 CRUD 流程（建→查→改→删） | ✅ 全 200 | - |

### 3.2 ai-cs-search（11 个用例）

| 用例 | 结果 | 耗时 |
|---|---|---|
| 创建索引 | ✅ 200 | 1.32s |
| 索引文档 | ✅ 200 | 0.28s |
| 搜索（英文关键词） | ✅ 200 | 0.03s |
| 搜索（中文关键词） | ✅ 200 | 0.03s |
| 搜索不存在索引（负向） | ✅ 500（业务异常） | 0.03s |
| 删除索引 | ✅ 200 | 0.35s |
| 删除不存在索引 | ✅ 200（ES 删除幂等） | 0.01s |
| 网关-创建索引 | ✅ 200 | 1.23s |
| 网关-索引文档 | ✅ 200 | 0.36s |
| 网关-搜索 | ✅ 200 | 0.05s |
| 网关-删除索引 | ✅ 200 | 0.35s |

---

## 四、测试中发现并修复的问题

| # | 服务 | 问题 | 根因 | 修复 |
|---|---|---|---|---|
| 1 | search | `SearchController.java` 文件被污染，开头混入 knowledge 模块 `KnowledgeMapper` 代码，编译报 `com.aics.knowledge cannot be resolved` | 版本库中文件内容错误（既有 bug） | 重写为正常的 SearchController |
| 2 | search | 所有接口 500：`Name for argument ... not available via reflection` | `@PathVariable`/`@RequestParam` 未显式命名，编译未开 `-parameters` | 显式命名 `@PathVariable("index")`、`@RequestParam("query")`、`page`/`size` |
| 3 | search | 运行期 `Unresolved compilation problem: IndexCoordinates cannot be resolved` | 编译污染残留 class + spring-data-elasticsearch **5.x** 中 `IndexCoordinates` 从 `core` 包移到 `core.mapping` 包，代码用旧包路径 | 4 处改为 `org.springframework.data.elasticsearch.core.mapping.IndexCoordinates`，并 clean 重新编译 |
| 4 | knowledge | 列表 500：`Table 'knowledge_db.t_knowledge_document' doesn't exist` | 实体 `@TableName("t_knowledge_document")` 与数据库/初始化脚本表 `kb_document` 不一致 | 实体表名改为 `kb_document` |
| 5 | knowledge | 创建文档 500：`Column 'create_time' cannot be null` | 缺 MyBatis-Plus `MetaObjectHandler`，`createTime/updateTime` 无法自动填充 | 新增 `MybatisPlusConfig`（分页插件 + 自动填充） |
| 6 | knowledge | 列表 500：`Name for argument ... not available via reflection` | `@RequestParam` 未显式命名 | 显式命名 `page`/`pageSize`/`keyword` |

> 注：搜索/查询**不存在的资源**返回 500 是 `BusinessException` 包装的合理错误处理（非 bug）；删除不存在的 ES 索引返回 200 是 ES 删除幂等语义。

---

## 五、新增基础设施

为让 search 接口可完整测试，本地安装了 **Elasticsearch 8.12.2**（与项目基础设施版本一致）：

- 位置：`tools/elasticsearch/elasticsearch-8.12.2/`
- 配置：单节点（`discovery.type: single-node`）、禁用安全认证（`xpack.security.enabled: false`）
- 端口：9200
- 启动脚本：`logs/start-es.ps1`（日志 `logs/elasticsearch.log`）

> ES 为本地测试环境安装，未纳入 git（tools 目录）。生产环境按 `deploy/` 部署真实 ES 集群。

---

## 六、遗留说明

1. 测试用临时脚本与数据：`logs/ks_test_all.py`、`logs/ks-test/test-results.json`（`logs/` 已被 gitignore）。
2. 本次修复均是可逆的代码修正（多为既有 bug），如需还原可参考 git diff。
3. knowledge 数据库使用远程 MySQL（123.60.31.79），测试数据已清理（创建后删除）。

---

## 附：原始测试证据

`logs/ks-test/test-results.json`（23 条用例，含请求参数/HTTP 状态/耗时/响应全文）。