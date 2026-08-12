# 任务清单：升级 Spring AI 以支持 Chroma v2 API

> 本文件供其他 agent 独立执行。请按顺序完成任务，每步完成后更新状态。

## 背景与目标

项目 `ai-cs-chat` 使用 Spring AI 接入 Chroma 向量库，但远程 Chroma 实例只支持 **v2 API**
（v1 API 返回 410 deprecated）。Spring AI `1.0.0` 的 `ChromaVectorStore` 走 v1 API，无法连接。
经核验，**Spring AI `1.1.4` 修复了 Chroma 字符串比对 bug 并支持 v2 API**，需升级版本。

相关文件：
- `pom.xml`（父 POM，管理 `spring-ai.version`）
- `deploy/nacos/configs/ai-cs-chat.yml`（Chroma 连接配置，已发布到 Nacos）
- `ai-cs-chat/src/main/resources/application.yml`（本地兜底配置）
- `ai-cs-chat/src/main/java/com/aics/chat/config/SpringAiConfig.java`（QuestionAnswerAdvisor 注册）

版本现状：
- Spring Boot：`3.2.5`
- Spring Cloud Alibaba：`2023.0.1.0`
- Spring AI：`1.0.0`（**已改为 1.1.4，见 "已完成"**）

---

## 已完成 ✅

- [x] `pom.xml` 中 `spring-ai.version` 由 `1.0.0` 改为 `1.1.4`

---

## 待办任务

### 任务 1：编译验证与 Spring Boot 兼容性评估
**状态**：✅ 已完成（2026-08-06，JDK 21 编译通过）

Spring AI `1.1.4` 官方基线是 Spring Boot 3.4.x，但已知可与 3.3.x 配合。当前项目是 `3.2.5`，存在不兼容风险。

执行：
```bash
# JDK 需 17+（本机默认 JDK8，建议用 JDK21）
set JAVA_HOME=D:\Tools\IT\enviroment\jdk\jdk-21.0.11+10
set Path=%JAVA_HOME%\bin;%Path%
mvn -q -pl ai-cs-chat -am compile -DskipTests
```

**判定**：
- 若编译通过 → 跳到任务 3
- 若报错（如找不到 Spring Boot 3.4 的类、依赖版本冲突）→ 执行任务 2

### 任务 2：按需升级 Spring Boot（仅当任务 1 失败）
**状态**：✅ 已完成（跳过——任务 1 编译通过，Spring AI 1.1.4 与 Spring Boot 3.2.5 兼容，无需升级 Boot）

将 `pom.xml` 的 `spring-boot.version` 由 `3.2.5` 升级到 `3.4.x`（如 `3.4.5`），并同步评估：
- `spring-cloud.version`：`2023.0.1` → 需匹配 Spring Boot 3.4（如 `2024.0.x` 或验证兼容性）
- `spring-cloud-alibaba.version`：`2023.0.1.0` → 需匹配（参考可升到 `2023.0.3.4`）
- 检查 `seata`、`shardingsphere`、`mybatis-plus` 等依赖在 Spring Boot 3.4 下的兼容性

升级后重新执行任务 1 的编译命令。

### 任务 3：适配 1.1.x 的 API / 配置变更
**状态**：✅ 已完成（2026-08-06，三个子项全部确认完成）

Spring AI 1.1.x 有以下可能变化，需逐一核对并修正：

1. ✅ **QuestionAnswerAdvisor 包名**：已确认——1.1.4 包名未变，import 仍为 `org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor`，编译通过，无需修改
   - 1.0.0 在 `org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor`
   - 若 1.1.x 包名变化需同步修改 import

2. ✅ **Chroma 配置项**：已完成——`tenant-name: default_tenant`、`database-name: default` 已加入本地 `ai-cs-chat/src/main/resources/application.yml` 与 `deploy/nacos/configs/ai-cs-chat.yml`，并执行 `publish-to-nacos.ps1` 发布到本地 Nacos（127.0.0.1:8848，9 个配置全部 OK，已回读验证新字段生效）
   - `deploy/nacos/configs/ai-cs-chat.yml` 的 `spring.ai.vectorstore.chroma` 下增加：
   ```yaml
   tenant-name: default_tenant
   database-name: default
   ```
   - 同步修改 `ai-cs-chat/src/main/resources/application.yml` 本地兜底
   - 修改后重新发布 Nacos：`powershell -ExecutionPolicy Bypass -File deploy/nacos/publish-to-nacos.ps1`

3. ✅ **Chroma starter 坐标**：已确认有效——本地仓库存在 `spring-ai-starter-vector-store-chroma:1.1.4`，模块编译通过
   （若失效，查 BOM 确认新坐标）

### 任务 4：更新文档
**状态**：✅ 已完成（2026-08-06）

- [x] `learning-docs/05-AI集成/02-RAG全栈实战/03-RAG向量检索实战.md`：更新 Spring AI 版本说明（1.0.0 → 1.1.4）、
      补充 `tenant-name`/`database-name` 配置示例、新增 “Chroma v2 API 兼容性说明“ 小节与 FAQ Q6
- [x] 记录 Chroma v2 API 的兼容性说明：v1 返回 410、1.1.4 字符串比对 bug 修复、tenant/database 配置
- [x] 同步修正 `learning-docs/05-AI集成/01-SpringAI框架集成/01-SpringAI入门.md`、`learning-docs/01-Java基础/02-Maven多模块管理.md`
      中的版本号，及 `VectorStoreConfig.java` 中过时的 starter 坐标注释

---

## 进度备注（2026-08-06 下班前快照）
- 任务 1：✅ 编译通过（JDK 21，`mvn -pl ai-cs-chat -am compile -DskipTests`），Spring AI 1.1.4 + Spring Boot 3.2.5 无冲突，任务 2 跳过
- 任务 3：子项 1（包名）与子项 3（starter 坐标）已确认；子项 2（tenant/database 配置）已改本地与 Nacos 源文件，并已发布到本地 Nacos
- 任务 4：✅ 已完成——更新 `learning-docs/05-AI集成/02-RAG全栈实战/03-RAG向量检索实战.md`（版本说明、Chroma v2 兼容性小节、FAQ Q6），
  同步修正 `01-SpringAI入门.md`、`02-Maven多模块管理.md` 版本号与 `VectorStoreConfig.java` 注释
- 最终验证：未执行。远端 Chroma `123.60.31.79:8000` 当前从本机不可达，需在有网络/内网环境启动服务后验证入库与检索

## 最终验证

1. 启动 Chroma（地址已配置为 `123.60.31.79:8000`，与 MySQL 同主机）
2. 启动 `ai-cs-chat` 服务（JDK 21）
3. 入库一条测试数据并检索，确认不再报 v1 API 410 错误
```bash
curl -X POST "http://localhost:8083/rag/knowledge-base/text" \
     -d "knowledgeBase=product-manual" \
     -d "text=我们支持15天无理由退货，运费由买家承担。"
curl "http://localhost:8083/rag/knowledge-base/search?knowledgeBase=product-manual&query=退货政策"
```

## 参考陷阱（来自实践）
- Remote Chroma v1 API 返回 410，必须用 v2
- Spring AI 1.0.0 存在错误消息字符串比对 bug（`"does not exists"` vs `"does not exist"`），导致集合存在也报 404，1.1.4 已修复
- Chroma host 配置需确保可达，端口默认 8000
