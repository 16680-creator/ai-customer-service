package com.aics.chat.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NL2SQL 只读 SQL 安全守卫（3.2 F6 SQL 安全）。
 *
 * <p>两道防线（对应 Gherkin Feature 06）：</p>
 * <ul>
 *   <li><b>第一道：正则初筛</b> —— 注释/分号绕过、非 SELECT、写操作关键字、系统库、危险函数；</li>
 *   <li><b>第二道：AST 校验（jsqlparser）</b> —— 语法树必须为纯 SELECT，且表名/列名
 *       通过 {@code aics.security.sql-table-whitelist} / {@code sql-column-whitelist} 白名单。</li>
 * </ul>
 *
 * <p>拒绝时记录 {@link SecurityEventType#SQL_BLOCKED} 审计事件。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlGuard {

    private final SecurityProperties properties;
    private final SecurityAuditRecorder auditRecorder;

    /** 危险关键字（\b 单词边界避免误伤 update_time / deleted 等列名；
     *  update/delete 后跟下划线时不属于单词边界，可安全放行列名） */
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

    /** LIMIT 子句（用于强制行数上限） */
    private static final Pattern LIMIT_PATTERN = Pattern.compile("(?i)\\blimit\\s+(\\d+)");

    /**
     * 校验 SQL 是否只读安全。
     *
     * @param database 逻辑库标识（user/product/order/chat/knowledge）
     * @param sql      待校验 SQL
     * @return null 表示通过；否则返回拒绝原因
     */
    public String validate(String database, String sql) {
        if (sql == null || sql.isBlank()) {
            return "SQL 不能为空";
        }
        String s = sql.trim();

        // ===== 第一道防线：正则初筛 =====
        if (COMMENTS.matcher(s).find()) {
            return reject(database, sql, "不允许包含 SQL 注释");
        }
        String noTrailing = s;
        while (noTrailing.endsWith(";")) {
            noTrailing = noTrailing.substring(0, noTrailing.length() - 1).trim();
        }
        if (noTrailing.contains(";")) {
            return reject(database, sql, "不允许一次执行多条 SQL");
        }
        if (!noTrailing.toUpperCase(Locale.ROOT).startsWith("SELECT")) {
            return reject(database, sql, "仅允许 SELECT 查询语句");
        }
        if (DANGEROUS_KEYWORDS.matcher(noTrailing).find()) {
            return reject(database, sql, "检测到写操作或危险关键字");
        }
        if (SYSTEM_SCHEMA.matcher(noTrailing).find()) {
            return reject(database, sql, "不允许访问系统库");
        }
        if (FUNC_ABUSE.matcher(noTrailing).find()) {
            return reject(database, sql, "不允许使用危险函数或文件操作");
        }

        // ===== 第二道防线：AST 校验（jsqlparser） =====
        // 学习点：正则初筛是“字符串匹配”，能被同义词/嵌套子查询/别名引用绕过；
        // AST 校验把 SQL 解析成语法树（对象模型），从结构上确认“这是且仅是一个 SELECT”，
        // 再对树中的表名/列名做白名单判定——这就是“正则拦形状、AST 拦结构”的两道防线。
        // 注意：jsqlparser 4.9 的类布局与旧版教程差异较大（Column 在 schema 包、
        // Select 是抽象类、无 SelectBody/SubSelect），写遍历代码前务必 javap 核对 API。
        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(noTrailing);
        } catch (Exception e) {
            log.warn("NL2SQL AST 解析失败: db={}, sql={}, err={}", database, sql, e.getMessage());
            return reject(database, sql, "SQL 语法无法解析，已拒绝执行");
        }
        if (!(statement instanceof Select select)) {
            return reject(database, sql, "仅允许 SELECT 查询语句");
        }
        String tableError = checkTables(database, sql, select);
        if (tableError != null) {
            return tableError;
        }
        String columnError = checkColumns(database, sql, select);
        if (columnError != null) {
            return columnError;
        }
        return null;
    }

    /** 表级白名单：库标识 -> 允许的表名（未配置或配置为空集合 = 不启用） */
    private String checkTables(String database, String sql, Select select) {
        Map<String, List<String>> whitelist = properties.getSqlTableWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return null;
        }
        List<String> allowed = whitelist.get(database);
        if (allowed == null || allowed.isEmpty()) {
            return null;
        }
        List<String> tables;
        try {
            // Select 同时实现 Statement 与 Expression，显式强转为 Statement 消除重载歧义
            tables = new TablesNamesFinder().getTableList((Statement) select);
        } catch (Exception e) {
            log.warn("NL2SQL 表解析失败: db={}, sql={}, err={}", database, sql, e.getMessage());
            return reject(database, sql, "SQL 表解析失败，已拒绝执行");
        }
        for (String table : tables) {
            String name = normalize(table);
            if (!allowed.contains(name)) {
                return reject(database, sql, "表 " + table + " 不在白名单内");
            }
        }
        return null;
    }

    /** 列级白名单：库标识 -> 允许的 "表.列"（未配置或配置为空集合 = 不启用） */
    private String checkColumns(String database, String sql, Select select) {
        Map<String, List<String>> whitelist = properties.getSqlColumnWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return null;
        }
        List<String> allowed = whitelist.get(database);
        if (allowed == null || allowed.isEmpty()) {
            return null;
        }
        List<String> columns = new ArrayList<>();
        select.accept(new SelectColumnVisitor(columns));
        for (String column : columns) {
            String name = normalize(column);
            // SELECT * / t.*：由表白名单约束，列级放行
            if ("*".equals(name) || name.endsWith(".*")) {
                continue;
            }
            // 列名可能带表别名（o.order_no），统一按"列后缀"匹配白名单（table.column 或 .column）
            String simple = name.contains(".") ? name.substring(name.indexOf('.') + 1) : name;
            boolean ok = allowed.contains(name)
                    || allowed.stream().anyMatch(a -> a.endsWith("." + simple));
            if (!ok) {
                return reject(database, sql, "列 " + column + " 不在白名单内");
            }
        }
        return null;
    }

    /**
     * 强制查询结果行数上限：无 LIMIT 则追加；LIMIT 超过上限则改写（3.2 F6 防拖库）。
     *
     * @param sql     校验通过的 SQL
     * @param maxRows 行数上限
     * @return 改写后的 SQL
     */
    public String enforceLimit(String sql, int maxRows) {
        String s = sql == null ? "" : sql.trim();
        while (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        Matcher m = LIMIT_PATTERN.matcher(s);
        if (m.find()) {
            if (Integer.parseInt(m.group(1)) > maxRows) {
                return m.replaceFirst("LIMIT " + maxRows);
            }
            return s;
        }
        return s + " LIMIT " + maxRows;
    }

    /** 拒绝并记录审计事件 */
    private String reject(String database, String sql, String reason) {
        auditRecorder.record(SecurityEventType.SQL_BLOCKED, "TOOL", null,
                "nl2sql." + database, sql, "BLOCK", reason);
        return reason;
    }

    private static String normalize(String name) {
        return name.replace("`", "").trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 遍历 SELECT 语法树收集被引用的列（含 SELECT 列表/WHERE/HAVING/GROUP BY/ORDER BY/JOIN ON
     * 与嵌套子查询），适配 jsqlparser 4.9（Select 为抽象类：PlainSelect/SetOperationList/
     * ParenthesedSelect/Values）。
     */
    private static final class SelectColumnVisitor extends SelectVisitorAdapter {

        private final List<String> columns;

        private final ExpressionVisitorAdapter exprVisitor = new ExpressionVisitorAdapter() {
            @Override
            public void visit(Column column) {
                columns.add(column.getFullyQualifiedName());
            }
        };

        SelectColumnVisitor(List<String> columns) {
            this.columns = columns;
        }

        @Override
        public void visit(PlainSelect plainSelect) {
            if (plainSelect.getSelectItems() != null) {
                for (SelectItem<?> item : plainSelect.getSelectItems()) {
                    Expression expr = item.getExpression();
                    if (expr instanceof AllColumns) {
                        columns.add("*");
                    } else if (expr instanceof AllTableColumns atc) {
                        columns.add(atc.getTable().getFullyQualifiedName() + ".*");
                    } else if (expr != null) {
                        expr.accept(exprVisitor);
                    }
                }
            }
            visitFrom(plainSelect.getFromItem());
            if (plainSelect.getJoins() != null) {
                for (Join join : plainSelect.getJoins()) {
                    visitFrom(join.getRightItem());
                    if (join.getOnExpression() != null) {
                        join.getOnExpression().accept(exprVisitor);
                    }
                }
            }
            if (plainSelect.getWhere() != null) {
                plainSelect.getWhere().accept(exprVisitor);
            }
            if (plainSelect.getHaving() != null) {
                plainSelect.getHaving().accept(exprVisitor);
            }
            GroupByElement groupBy = plainSelect.getGroupBy();
            if (groupBy != null && groupBy.getGroupByExpressions() != null
                    && groupBy.getGroupByExpressions().getExpressions() != null) {
                for (Object e : groupBy.getGroupByExpressions().getExpressions()) {
                    ((Expression) e).accept(exprVisitor);
                }
            }
            if (plainSelect.getOrderByElements() != null) {
                for (OrderByElement o : plainSelect.getOrderByElements()) {
                    o.getExpression().accept(exprVisitor);
                }
            }
        }

        @Override
        public void visit(SetOperationList setOpList) {
            for (Select select : setOpList.getSelects()) {
                select.accept(this);
            }
        }

        @Override
        public void visit(ParenthesedSelect parenthesedSelect) {
            Select inner = parenthesedSelect.getSelect();
            if (inner != null) {
                inner.accept(this);
            }
        }

        @Override
        public void visit(Values values) {
            if (values.getExpressions() != null && values.getExpressions().getExpressions() != null) {
                for (Expression e : values.getExpressions().getExpressions()) {
                    e.accept(exprVisitor);
                }
            }
        }

        private void visitFrom(FromItem fromItem) {
            if (fromItem instanceof ParenthesedSelect ps) {
                Select inner = ps.getSelect();
                if (inner != null) {
                    inner.accept(this);
                }
            }
            // Table 等其余 FromItem 无列引用（表名由 TablesNamesFinder 收集）
        }
    }
}
