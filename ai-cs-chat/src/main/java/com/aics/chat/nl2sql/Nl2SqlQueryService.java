package com.aics.chat.nl2sql;

import com.aics.chat.observability.TraceSpans;
import com.aics.chat.security.SqlGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能问数（NL2SQL）工具服务 —— 让大模型直接查数据库。
 *
 * <h3>【AI 技术详解】NL2SQL（Natural Language to SQL）</h3>
 * <ul>
 *   <li><b>什么是 NL2SQL</b>：将自然语言问题转换为 SQL 查询语句的技术</li>
 *   <li><b>应用场景</b>：运营人员不懂 SQL，但想查数据（"这个月有多少订单？"）</li>
 *   <li><b>技术原理</b>：
 *       <ol>
 *         <li>用户提供自然语言问题（"这个月有多少订单？"）</li>
 *         <li>LLM 分析问题，判断查哪个库、组装 SQL（SELECT COUNT(*) FROM orders WHERE ...）</li>
 *         <li>调用本工具执行 SQL</li>
 *         <li>LLM 把查询结果组织成自然语言回复（"本月共有 1234 个订单"）</li>
 *       </ol>
 *   </li>
 * </ul>
 *
 * <h3>【AI 技术详解】Function Calling（函数调用）机制</h3>
 * <ul>
 *   <li><b>原理</b>：以 Spring AI {@link Tool} 方式注册给 LLM，LLM 输出"我要调用某个工具"的 JSON 指令，
 *       Spring AI 框架拦截该指令，调用对应的 Java 方法，再把结果返回给 LLM</li>
 *   <li><b>为什么这么设计</b>：把"写 SQL"交给模型，把"执行安全"交给代码。
 *       模型不直接连数据库，只通过白名单校验后的只读通道查询</li>
 *   <li><b>优势</b>：
 *       <ul>
 *         <li>LLM 负责理解意图和生成 SQL（擅长自然语言处理）</li>
 *         <li>代码负责安全校验和执行（擅长规则执行）</li>
 *         <li>各司其职，安全与智能兼得</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>【AI 技术详解】安全七道闸</h3>
 * <ol>
 *   <li><b>仅 SELECT 单条</b>：拦分号/注释绕过，防止多语句注入</li>
 *   <li><b>拦写操作关键字</b>：INSERT/UPDATE/DELETE/DDL/GRANT/SHOW 等全部拦截</li>
 *   <li><b>禁系统库/危险函数</b>：information_schema/mysql/sys、sleep/benchmark 等</li>
 *   <li><b>AST 语法树校验</b>：jsqlparser 解析，仅允许纯 SELECT（3.2 F6）</li>
 *   <li><b>表/列白名单</b>：按库配置允许的表与列，白名单之外一律拒绝（3.2 F6）</li>
 *   <li><b>强制 LIMIT 100</b>：防止拖库，已有 LIMIT 且超过 100 则改写</li>
 *   <li><b>JDBC readOnly + 10s 超时</b>：双保险只读，即使绕过白名单也无法写入</li>
 * </ol>
 * <p>安全校验全部委托 {@link SqlGuard}（正则初筛 + AST 白名单），命中即记录安全审计事件。</p>
 *
 * <h3>【AI 技术详解】多库路由</h3>
 * <ul>
 *   <li><b>问题</b>：业务数据分散在多个库（用户库、商品库、订单库等）</li>
 *   <li><b>方案</b>：通过 database 参数路由到对应的只读数据源</li>
 *   <li><b>LLM 如何选择库</b>：System Prompt 中提供数据库 Schema，LLM 根据问题语义选择</li>
 * </ul>
 *
 * <h3>【技术关联】与 SpringAiConfig 的关系</h3>
 * <pre>
 *   SpringAiConfig.toolCallbackProvider()
 *       └── MethodToolCallbackProvider.builder()
 *               .toolObjects(orderQueryService, nl2SqlQueryService)  // 注册为 LLM 工具
 *               .build()
 *
 *   LLM 调用流程：
 *       用户问"这个月有多少订单？"
 *           → LLM 判断需要查数据库
 *           → LLM 输出：调用 executeReadOnlyQuery(database="order", sql="SELECT COUNT(*) FROM orders WHERE ...")
 *           → Spring AI 调用本方法执行 SQL
 *           → 返回查询结果 JSON
 *           → LLM 组织成自然语言回复
 * </pre>
 */
@Slf4j
@Service
public class Nl2SqlQueryService {

    /** 单次查询返回的最大行数 */
    private static final int MAX_ROWS = 100;

    /** JDBC 查询超时（秒） */
    private static final int QUERY_TIMEOUT_SECONDS = 10;

    /** 逻辑库标识常量（与 Nacos 配置 aics.nl2sql.urls 的 key 对应） */
    public static final String DB_USER = "user";
    public static final String DB_PRODUCT = "product";
    public static final String DB_ORDER = "order";
    public static final String DB_CHAT = "chat";
    public static final String DB_KNOWLEDGE = "knowledge";

    /**
     * 危险关键字/系统库/危险函数/注释等正则初筛与 LIMIT 强制上限
     * 已迁移至 {@link com.aics.chat.security.SqlGuard}
     * （3.2 F6 SQL 安全：正则初筛 + jsqlparser AST 表/列白名单双防线）。
     */

    /** 库标识 -> 只读 JdbcTemplate */
    private final Map<String, JdbcTemplate> jdbcTemplates;

    private final ObjectMapper objectMapper;

    private final ObservationRegistry observationRegistry;

    /** SQL 安全守卫（3.2 F6：正则初筛 + AST 表/列白名单） */
    private final SqlGuard sqlGuard;

    public Nl2SqlQueryService(Map<String, JdbcTemplate> nl2SqlJdbcTemplates,
                              ObservationRegistry observationRegistry,
                              SqlGuard sqlGuard) {
        this.jdbcTemplates = nl2SqlJdbcTemplates;
        this.observationRegistry = observationRegistry;
        this.sqlGuard = sqlGuard;
        // 日期序列化为可读字符串（默认 Timestamp 序列化成时间戳数字，不利于 AI 阅读）
        this.objectMapper = new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 【AI 核心】执行只读 SQL 查询 —— LLM 工具入口（@Tool 暴露给模型）。
     *
     * <p>执行顺序：①按 database 选只读数据源 → ②白名单校验 SQL → ③强制 LIMIT →
     * ④JDBC 执行（10s 超时）→ ⑤结果转 JSON 文本返回给模型。
     * 日期字段序列化为可读字符串，便于模型理解。</p>
     *
     * <p><b>【AI 技术详解】@Tool 注解的作用</b>：
     * <ul>
     *   <li><b>description</b>：告诉 LLM 这个工具的功能、参数含义、使用规则</li>
     *   <li><b>LLM 如何使用</b>：根据 description 判断何时调用、传什么参数</li>
     *   <li><b>重要性</b>：description 写得越好，LLM 调用越准确</li>
     * </ul>
     *
     * <p><b>【技术关联】与 SpringAiConfig 的关系</b>：
     * <ul>
     *   <li>本方法通过 @Tool 注解暴露给 LLM</li>
     *   <li>SpringAiConfig.toolCallbackProvider() 将本类注册为工具对象</li>
     *   <li>LLM 调用时，Spring AI 框架自动调用本方法</li>
     * </ul>
     *
     * @param database 逻辑库标识：user(用户) / product(商品) / order(订单支付) / chat(对话消息) / knowledge(知识库)
     * @param sql      只读 SELECT 查询语句
     * @return 查询结果 JSON 文本；校验不通过或执行失败时返回错误说明
     */
    @Tool(description = "执行只读SQL查询（仅支持SELECT），根据用户问题从指定业务库查询数据，返回查询结果。"
            + "database可选值：user(用户库，用户信息/角色)、product(商品库，商品/分类)、order(订单支付库，订单/购物车/优惠券/支付流水)、"
            + "chat(对话消息库，会话/消息)、knowledge(知识库文档)。"
            + "注意：必须根据用户问题中的业务含义选择正确的database，并编写合法SELECT语句（可带WHERE/ORDER BY/GROUP BY聚合）")
    public String executeReadOnlyQuery(
            @ToolParam(description = "数据库标识：user / product / order / chat / knowledge") String database,
            @ToolParam(description = "只读SELECT查询语句，如：SELECT * FROM orders WHERE status='PAID' LIMIT 10") String sql) {

        // 工具环节观测（scenario=nl2sql）：工具名、参数摘要、结果状态、耗时
        // SQL 摘要化（截断 + 防敏感），不落明文完整 SQL
        // 学习点：SQL 里可能带手机号/订单号等 PII，trace 数据可能导出第三方平台，
        // 故只留 200 字符内的摘要——可观测与数据安全在这里是同一件事的两面
        String sqlDigest = sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
        if (sqlDigest.length() > 200) {
            sqlDigest = sqlDigest.substring(0, 200) + "...";
        }
        return TraceSpans.observeReturn(observationRegistry, "TOOL", "nl2sql.query",
                Map.of("tool", "executeReadOnlyQuery", "database", database == null ? "" : database),
                Map.of("detail", sqlDigest),
                () -> doExecute(database, sql));
    }

    private String doExecute(String database, String sql) {
        // 1. 数据源校验
        JdbcTemplate jdbc = database == null ? null : jdbcTemplates.get(database.trim().toLowerCase());
        if (jdbc == null) {
            return "无效的数据库标识: " + database + "，可选值：user/product/order/chat/knowledge";
        }

        // 2. SQL 安全校验（委托 SqlGuard：正则初筛 + AST 表/列白名单，3.2 F6）
        String error = sqlGuard.validate(database, sql);
        if (error != null) {
            log.warn("NL2SQL 校验拦截: db={}, sql={}, reason={}", database, sql, error);
            return "SQL 校验不通过：" + error + "。仅支持单条 SELECT 只读查询。";
        }

        // 3. 强制执行 LIMIT 上限
        String finalSql = enforceLimit(sql);
        long start = System.currentTimeMillis();
        try {
            List<Map<String, Object>> rows = jdbc.query(finalSql,
                    ps -> ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS),
                    (rs, rowNum) -> {
                        ResultSetMetaData md = rs.getMetaData();
                        Map<String, Object> map = new LinkedHashMap<>();
                        for (int i = 1; i <= md.getColumnCount(); i++) {
                            map.put(md.getColumnLabel(i), rs.getObject(i));
                        }
                        return map;
                    });
            List<Map<String, Object>> result = rows.size() > MAX_ROWS ? new ArrayList<>(rows.subList(0, MAX_ROWS)) : rows;
            String json = objectMapper.writeValueAsString(result);
            log.info("NL2SQL 执行成功: db={}, sql={}, rows={}, cost={}ms", database, finalSql, result.size(),
                    System.currentTimeMillis() - start);
            return "查询成功，共 " + result.size() + " 条结果，结果如下（JSON）：\n" + json;
        } catch (Exception e) {
            log.warn("NL2SQL 执行失败: db={}, sql={}, err={}", database, finalSql, e.getMessage());
            return "SQL 执行失败：" + e.getMessage() + "。请检查 SQL 语法、表名、列名是否正确，"
                    + "注意表名/列名需与业务库 schema 一致。";
        }
    }

    /**
     * 强制查询结果行数上限（委托 {@link SqlGuard#enforceLimit}，3.2 F6 防拖库）。
     */
    private String enforceLimit(String sql) {
        return sqlGuard.enforceLimit(sql, MAX_ROWS);
    }
}
