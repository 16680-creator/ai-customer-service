package com.aics.user.sharding;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分片路由集成测试（H2 内存库）
 *
 * <p>加载与生产同结构的 sharding-route-h2.yaml，验证：
 * 1. 分片规则与自定义算法能被 ShardingSphere 5.4.1 正确解析加载
 * 2. 按用户ID（后四位取模）插入/查询落库落表正确
 * 3. 单表（sys_role）收敛到 user_db 默认数据源
 */
class UserShardingRouteTest {

    private static final String H2_USER_DB = "jdbc:h2:mem:user_db;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";
    private static final String H2_USER_DB_0 = "jdbc:h2:mem:user_db_0;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";
    private static final String H2_USER_DB_1 = "jdbc:h2:mem:user_db_1;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";

    private static final String CREATE_SYS_USER = """
            CREATE TABLE IF NOT EXISTS "sys_user" (
                id          BIGINT       NOT NULL PRIMARY KEY,
                username    VARCHAR(64)  NOT NULL,
                password    VARCHAR(128) NOT NULL,
                nickname    VARCHAR(64),
                phone       VARCHAR(20),
                email       VARCHAR(128),
                avatar      VARCHAR(512),
                status      TINYINT      NOT NULL DEFAULT 1,
                role        VARCHAR(32)  DEFAULT 'user',
                create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                deleted     TINYINT      NOT NULL DEFAULT 0
            )""";

    private static DataSource shardingDataSource;

    @BeforeAll
    static void initShardingDataSource() throws Exception {
        // 模拟生产：先由 SQL 初始化脚本在各存储节点建表，ShardingSphere 只负责路由
        try (Connection conn = DriverManager.getConnection(H2_USER_DB_0, "sa", ""); Statement st = conn.createStatement()) {
            for (int i = 0; i < 4; i++) {
                st.execute(CREATE_SYS_USER.replace("\"sys_user\"", "\"sys_user_" + i + "\""));
            }
        }
        try (Connection conn = DriverManager.getConnection(H2_USER_DB_1, "sa", ""); Statement st = conn.createStatement()) {
            for (int i = 0; i < 4; i++) {
                st.execute(CREATE_SYS_USER.replace("\"sys_user\"", "\"sys_user_" + i + "\""));
            }
        }
        try (Connection conn = DriverManager.getConnection(H2_USER_DB, "sa", ""); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS \"sys_role\" (id BIGINT NOT NULL PRIMARY KEY, role_code VARCHAR(64) NOT NULL, role_name VARCHAR(128) NOT NULL)");
        }

        byte[] yamlBytes;
        try (var in = UserShardingRouteTest.class.getClassLoader().getResourceAsStream("sharding-route-h2.yaml")) {
            yamlBytes = in.readAllBytes();
        }
        shardingDataSource = YamlShardingSphereDataSourceFactory.createDataSource(yamlBytes);
    }

    @Test
    @DisplayName("按ID插入路由到正确的库和表（后四位取模）")
    void insertRoutesToCorrectShard() throws Exception {
        // id=1 -> 后四位 "1" -> 1%2=1 库、1%4=1 表 -> user_db_1.sys_user_1
        insertUser(1L, "admin");
        // id=12345678 -> 5678 -> 0 库、2 表 -> user_db_0.sys_user_2
        insertUser(12345678L, "mod_user");
        // id=1948329203219486208 -> 6208 -> 0 库、0 表 -> user_db_0.sys_user_0
        insertUser(1948329203219486208L, "snowflake_user");

        assertEquals(1, countRows("jdbc:h2:mem:user_db_1;MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sys_user_1"));
        assertEquals(1, countRows("jdbc:h2:mem:user_db_0;MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sys_user_2"));
        assertEquals(1, countRows("jdbc:h2:mem:user_db_0;MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sys_user_0"));
        // 其他分片表不应有数据
        assertEquals(0, countRows("jdbc:h2:mem:user_db_1;MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sys_user_0"));
        assertEquals(0, countRows("jdbc:h2:mem:user_db_0;MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sys_user_1"));
    }

    @Test
    @DisplayName("按ID查询走精确路由且返回正确数据")
    void queryByIdRoutesPrecisely() throws Exception {
        insertUser(202508291234567L, "route_probe");

        try (Connection conn = shardingDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT username FROM sys_user WHERE id = ?")) {
            ps.setLong(1, 202508291234567L);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("route_probe", rs.getString(1));
            }
        }
    }

    @Test
    @DisplayName("全表查询聚合所有分片数据")
    void countAcrossShardsAggregates() throws Exception {
        try (Connection conn = shardingDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM sys_user");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            int inserted = 4; // 前两个用例累计插入 4 条
            int actual = rs.getInt(1);
            assertTrue(actual >= inserted,
                    () -> "分片聚合行数应 >= " + inserted + "，实际: " + actual);
        }
    }

    @Test
    @DisplayName("单表sys_role收敛到user_db默认数据源")
    void singleTableStaysInDefaultDataSource() throws Exception {
        try (Connection conn = shardingDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO sys_role (id, role_code, role_name) VALUES (1, 'admin', '管理员')")) {
            ps.executeUpdate();
        }
        // 单表必须出现在 user_db（默认数据源），分片库中不应存在该表数据
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:user_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sys_role WHERE id = 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    private void insertUser(long id, String username) throws Exception {
        try (Connection conn = shardingDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO sys_user (id, username, password, status, deleted) VALUES (?, ?, ?, 1, 0)")) {
            ps.setLong(1, id);
            ps.setString(2, username);
            ps.setString(3, "bcrypt-hash");
            ps.executeUpdate();
        }
    }

    private int countRows(String jdbcUrl, String tableName) throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }
}
