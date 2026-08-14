import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/** 验证 nacos_config 数据库中的配置数据。用法: java -cp .;mysql-connector-j-8.0.33.jar DbCheck <host> <port> <user> <pwd> <db> */
public class DbCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://" + args[0] + ":" + args[1] + "/" + args[4]
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, args[2], args[3]); Statement st = conn.createStatement()) {
            var rs = st.executeQuery("SELECT data_id, tenant_id FROM config_info");
            System.out.println("=== config_info 中的数据 ===");
            int n = 0;
            while (rs.next()) {
                System.out.println("  dataId=" + rs.getString(1) + "  tenant=" + (rs.getString(2) == null || rs.getString(2).isEmpty() ? "(public)" : rs.getString(2)));
                n++;
            }
            System.out.println("配置总数: " + n);
            var rs2 = st.executeQuery("SELECT username FROM users");
            System.out.println("=== users 表 ===");
            while (rs2.next()) System.out.println("  user: " + rs2.getString(1));
        }
    }
}
