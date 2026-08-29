# 03-ShardingSphere 用户表分库分表实战

> 2026-08 落地记录：用户表 `sys_user` 分库分表（2 库 × 4 表，取 ID 后四位取模）。

## 一、分片设计

| 项 | 方案 |
|---|---|
| 分片键 | `sys_user.id`（雪花 ID，MyBatis-Plus `ASSIGN_ID`，插入前已生成，可路由） |
| 分片布局 | `user_db_0` / `user_db_1` 各含 `sys_user_0..3`，共 8 张物理表 |
| 路由算法 | 取 ID 十进制字符串**末尾最多 4 位数字**转整数：库 = `后四位 % 2`，表 = `后四位 % 4` |
| 单表 | `sys_role`、`sys_user_role` 不分片，留在 `user_db`（`!SINGLE` 规则 `defaultDataSource` 收敛） |
| 非分片键查询 | 如按 username 登录查询 → 广播到 2 库聚合；用户名唯一性由应用层查重保证（`uk_username` 只在单表内生效） |

路由示例：`id=1` → 后四位 `"1"` → 库 `user_db_1`、表 `sys_user_1`；`id=12345678` → `"5678"` → 库 `user_db_0`、表 `sys_user_2`。

## 二、代码与配置落点

```
ai-cs-user/
├── pom.xml                                          # shardingsphere-jdbc 5.5.0 + jaxb-runtime + h2(test)
└── src/main/
    ├── java/com/aics/user/sharding/
    │   └── UserIdLast4ModShardingAlgorithm.java     # CLASS_BASED 自定义算法（库/表共用，props 传 sharding-count）
    └── resources/
        ├── application.yml                          # datasource -> ShardingSphereDriver + jdbc:shardingsphere:classpath:...
        └── sharding-user.yaml                       # 3 数据源 + !SINGLE + !SHARDING 规则
deploy/mysql/
├── init.sql / all-init.sql                          # 建 user_db_0/1 的 8 张分片表 + admin 种子按路由直插
└── user-sharding-migrate.sql                        # 存量数据按路由迁移（旧环境专用）
```

算法接口（ShardingSphere 5.5.0）：

```java
public class UserIdLast4ModShardingAlgorithm
        implements StandardShardingAlgorithm<Comparable<?>> {
    // init(Properties)：接收 sharding-count（库算法配 2，表算法配 4）
    // doSharding(precise)：后四位 % sharding-count，按目标名数字后缀匹配
    // doSharding(range)：取模无法收敛范围查询，直接广播
}
```

## 三、踩坑记录（重点，面试可讲）

1. **ShardingSphere 5.4.1 不支持 `props:` 子 map**：`YamlDataSourceConfigurationSwapper.getProperties()` 只剔除 `dataSourceClassName`、合并 `customPoolProps`，`props:` 整块被当作自定义属性丢弃 → 数据源连接全空 → `StorageResourceUtils` NPE。**5.4.1 起数据源属性必须平铺在数据源条目下**。
2. **5.4.1 与 Spring Boot 3.2 的 snakeyaml 冲突**：5.4.1 用 snakeyaml 1.x API（`new Representer()`），Boot 3.2 强制 snakeyaml 2.2（API 不兼容），同一 classpath 无解 → 升级 ShardingSphere 5.5.0（官方适配 snakeyaml 2.2）。
3. **5.5.0 pom 缺陷**：传递依赖 `shardingsphere-test-util:5.5.0` 未发布到中央仓库，需在依赖声明中 exclusion 排除。
4. **JDK 9+ 缺 JAXB**：5.x 内部走 `javax.xml.bind`，需补 `org.glassfish.jaxb:jaxb-runtime`（runtime scope）。
5. **H2 集成测试元数据为空**：5.5.0 的 `H2MetaDataLoader` 硬编码查 `PUBLIC` schema；URL 加 `DATABASE_TO_LOWER=TRUE` 会把 schema 也变小写 → 查不到 → 逻辑表不存在。改用 `CASE_INSENSITIVE_IDENTIFIERS=TRUE` + 建表语句加引号保小写。
6. **单表与分片表通配冲突**：`!SINGLE` 的 `tables: ["*.*"]` 会把分片实际表也当单表，应收窄为 `user_db.*`。
7. **5.5.0 环境变量占位符三要素**：`$${ENV::默认值}` 语法不变，但（a）必须在 SS URL 声明 `?placeholder-type=ENVIRONMENT`（默认 NONE 完全不替换）；（b）正则锚定行尾（`\$\$\{(.+::.*)}$`），**占位符必须在行尾且每行至多一个**——URL 中段的 host/port 无法用占位符，只能写死或下沉到独立行。
8. **5.5.0 移除 Memory 模式**：不配 `mode` 时默认 Standalone + JDBC 仓库（H2 provider），运行时需要 `org.h2.Driver`——生产 jar 里只有 MySQL 驱动会报 `Failed to load driver class org.h2.Driver`。需将 H2 提为 runtime 依赖并显式配置 `mode`（内存 H2 仓库，重启即重新加载元数据）。

## 三点五、真实环境冒烟（2026-08-29）

- 远程 MySQL 迁移：6 条存量用户按路由迁入 4 个分片，数量校验一致（user_db_0.sys_user_0=1、user_db_0.sys_user_2=2、user_db_1.sys_user_1=2、user_db_1.sys_user_3=1）
- 网关登录冒烟：admin（落 user_db_1.sys_user_1）与 zhangsan（落 user_db_0.sys_user_0）均返回 200 + JWT
- 注意：Nacos 的 `spring.datasource.url` 热刷新不会重建 ShardingSphere 数据源，改 SS 配置后需重启服务

## 四、验证方式（TDD）

- 单元测试：`UserIdLast4ModShardingAlgorithmTest`（9 用例：库/表路由、边界、异常、范围广播、库表同余一致性）
- 路由集成测试：`UserShardingRouteTest`（H2 内存库跑真 ShardingSphere：插入落表、按 ID 精确路由、跨分片聚合、单表收敛）

```bash
mvn -pl ai-cs-user test          # 全部测试
mvn -pl ai-cs-user verify        # 含 JaCoCo
```

## 五、存量数据迁移

旧环境执行 `deploy/mysql/user-sharding-migrate.sql`：RENAME 备份 → 8 条 `INSERT ... SELECT` 按 `CAST(RIGHT(id,4) AS UNSIGNED)` 路由 → 联合查询校验总数 → 确认后手动 DROP 备份表。

## 六、可继续深挖的点

- 分布式 ID 与分片键同源（雪花 ID 低位是序列号，取后四位取模是否均匀？可写脚本统计）
- 广播查询的归并开销：登录查 username 命中 2 库 × 4 表，如何用 ES/缓存减少广播
- 扩容路线：2 库 × 4 表 → 4 库时数据重分布（一致性哈希 vs 基因法）
- `uk_username` 单表内唯一 → 全局唯一的应用层保障方案与并发窗口
