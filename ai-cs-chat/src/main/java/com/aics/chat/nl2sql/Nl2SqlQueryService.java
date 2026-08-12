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
 * AI 智能问数（NL2SQL）工具服务。
 *
 * <p>以 Spring AI {@link Tool} 方式注册给 LLM：用户用自然语言提问后，
 * 模型自行判断需要查哪个库、组装 SELECT SQL，调用本工具执行并返回结果，
 * 再由模型基于结果组织自然语言回复。整个过程对用户透明。</p>
 *
 * <h3>安全策略（只读 + 白名单）</h3>
 * <ul>
 *   <li>仅允许 {@code SELECT} 开头的单条查询，拦截多语句（分号拼接）与注释绕过；</li>
 *   <li>拦截 INSERT/UPDATE/DELETE/DDL/GRANT/SHOW 等全部写操作与危险关键字；</li>
 *   <li>禁止访问 information_schema/mysql/sys 等系统库与 sleep/benchmark 等危险函数；</li>
 *   <li>强制追加 {@code LIMIT 100}（已有 LIMIT 且超过 100 则改写），防止拖库；</li>
 *   <li>连接串追加 {@code readOnly=true}，JDBC 查询超时 10s，双保险只读。</li>
 * </ul>
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
     * 执行只读 SQL 查询（LLM 工具入口）。
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
