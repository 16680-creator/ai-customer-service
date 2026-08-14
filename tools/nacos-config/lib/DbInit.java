import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 在目标 MySQL 上创建 nacos_config 数据库并执行 Nacos mysql-schema.sql。
 * 用法: java -cp .;mysql-connector-j-8.0.33.jar DbInit <host> <port> <user> <pwd> <schemaFile> [dbName]
 */
public class DbInit {
    public static void main(String[] args) throws Exception {
        String host = args[0], port = args[1], user = args[2], pwd = args[3], schema = args[4];
        String db = args.length > 5 ? args[5] : "nacos_config";
        String rootUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8";

        Class.forName("com.mysql.cj.jdbc.Driver");
        System.out.println("[1/3] 连接 " + host + ":" + port + " ...");
        try (Connection conn = DriverManager.getConnection(rootUrl, user, pwd); Statement st = conn.createStatement()) {
            System.out.println("[2/3] 创建数据库 " + db + " ...");
            st.execute("CREATE DATABASE IF NOT EXISTS `" + db + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            System.out.println("[3/3] 执行 schema ...");
            st.execute("USE `" + db + "`");
            List<String> stmts = splitStatements(schema);
            int n = 0;
            for (String s : stmts) {
                try {
                    st.execute(s);
                    n++;
                } catch (java.sql.SQLSyntaxErrorException e) {
                    // 已存在的表跳过（幂等重跑）
                    if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                        System.out.println("跳过已存在对象: " + firstLine(s));
                    } else {
                        throw e;
                    }
                } catch (java.sql.SQLIntegrityConstraintViolationException e) {
                    // 初始化数据已存在则跳过
                    System.out.println("跳过已存在数据: " + firstLine(s));
                }
            }
            System.out.println("成功执行 " + n + " 条建表语句");
            // 校验表
            var rs = st.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='" + db + "'");
            int t = 0;
            while (rs.next()) { t++; }
            System.out.println("数据库 " + db + " 现有表数量: " + t);
        }
    }

    private static String firstLine(String s) {
        int i = s.indexOf('\n');
        return (i > 0 ? s.substring(0, i) : s).trim();
    }

    private static List<String> splitStatements(String file) throws IOException {
        String sql = Files.readString(Paths.get(file), StandardCharsets.UTF_8);
        // 去掉块注释（跨行）
        String noBlock = sql.replaceAll("(?s)/\\*.*?\\*/", "");
        // 去掉行注释
        String clean = noBlock.replaceAll("(?m)^\\s*--.*$", "");
        List<String> out = new ArrayList<>();
        for (String part : clean.split(";")) {
            String s = part.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }
}
