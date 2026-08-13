package com.aics.chat.nl2sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * <h3>【AI 技术详解】安全五道闸</h3>
 * <ol>
 *   <li><b>仅 SELECT 单条</b>：拦分号/注释绕过，防止多语句注入</li>
 *   <li><b>拦写操作关键字</b>：INSERT/UPDATE/DELETE/DDL/GRANT/SHOW 等全部拦截</li>
 *   <li><b>禁系统库/危险函数</b>：information_schema/mysql/sys、sleep/benchmark 等</li>
 *   <li><b>强制 LIMIT 100</b>：防止拖库，已有 LIMIT 且超过 100 则改写</li>
 *   <li><b>JDBC readOnly + 10s 超时</b>：双保险只读，即使绕过白名单也无法写入</li>
 * </ol>
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
     * 危险关键字（\b 单词边界避免误伤 update_time / deleted 等列名；
     * update/delete 后跟下划线时不属于单词边界，可安全放行列名）。
     */
    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|truncate|create|replace|grant|revoke|call|exec|execute|"
                    + "lock|unlock|kill|handler|load|use|show|describe|explain|rename)\\b",
            Pattern.DOTALL);

    /** 系统库探测（库名.表 引用形式） */
    private static final Pattern SYSTEM_SCHEMA = Pattern.compile(
            "(?i)\\b(information_schema|performance_schema|mysql|sys)\\s*\\.",
            Pattern.DOTALL);

    /** 危险函数/文件操作 */
    private static final Pattern FUNC_ABUSE = Pattern.compile(
            "(?i)\\b(sleep|benchmark|procedure|into\\s+(outfile|dumpfile))\\b",
            Pattern.DOTALL);

    /** SQL 注释（可被用于绕过白名单） */
    private static final Pattern COMMENTS = Pattern.compile("--|#|/\\*|\\*/");

    private static final Pattern LIMIT_PATTERN = Pattern.compile("(?i)\\blimit\\s+(\\d+)");

    /** 库标识 -> 只读 JdbcTemplate */
    private final Map<String, JdbcTemplate> jdbcTemplates;

    private final ObjectMapper objectMapper;

    public Nl2SqlQueryService(Map<String, JdbcTemplate> nl2SqlJdbcTemplates) {
        this.jdbcTemplates = nl2SqlJdbcTemplates;
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

        // 1. 数据源校验
        JdbcTemplate jdbc = database == null ? null : jdbcTemplates.get(database.trim().toLowerCase());
        if (jdbc == null) {
            return "无效的数据库标识: " + database + "，可选值：user/product/order/chat/knowledge";
        }

        // 2. SQL 安全校验
        String error = validateSql(sql);
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
     * 只读 SQL 白名单校验。
     *
     * @return null 表示通过；否则返回拒绝原因
     */
    private String validateSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return "SQL 不能为空";
        }
        String s = sql.trim();

        // 注释绕过拦截
        if (COMMENTS.matcher(s).find()) {
            return "不允许包含 SQL 注释";
        }

        // 去除末尾分号后检查多语句
        String noTrailing = s;
        while (noTrailing.endsWith(";")) {
            noTrailing = noTrailing.substring(0, noTrailing.length() - 1).trim();
        }
        if (noTrailing.contains(";")) {
            return "不允许一次执行多条 SQL";
        }

        // 仅允许 SELECT 开头
        if (!noTrailing.toUpperCase().startsWith("SELECT")) {
            return "仅允许 SELECT 查询语句";
        }

        // 写操作 / 危险关键字拦截
        if (DANGEROUS_KEYWORDS.matcher(noTrailing).find()) {
            return "检测到写操作或危险关键字";
        }

        // 系统库拦截
        if (SYSTEM_SCHEMA.matcher(noTrailing).find()) {
            return "不允许访问系统库";
        }

        // 危险函数/文件操作拦截
        if (FUNC_ABUSE.matcher(noTrailing).find()) {
            return "不允许使用危险函数或文件操作";
        }
        return null;
    }

    /**
     * 强制查询结果行数上限：无 LIMIT 则追加；LIMIT 超过上限则改写。
     */
    private String enforceLimit(String sql) {
        String s = sql.trim();
        while (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        Matcher m = LIMIT_PATTERN.matcher(s);
        if (m.find()) {
            if (Integer.parseInt(m.group(1)) > MAX_ROWS) {
                return m.replaceFirst("LIMIT " + MAX_ROWS);
            }
            return s;
        }
        return s + " LIMIT " + MAX_ROWS;
    }
}
