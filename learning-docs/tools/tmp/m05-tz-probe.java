// learning-docs\tools\tmp\m05-tz-probe.java
// 目的：看清 @Scheduled / LocalDateTime.now() 依赖的到底是哪个时区。
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

public class m05_tz_probe {
    public static void main(String[] args) {
        System.out.println("user.timezone property : " + System.getProperty("user.timezone"));
        System.out.println("TimeZone.getDefault()  : " + TimeZone.getDefault().getID());
        System.out.println("ZoneId.systemDefault() : " + ZoneId.systemDefault());
        System.out.println("LocalDateTime.now()    : " + LocalDateTime.now() + "  <- written into orders.expire_time");
        System.out.println("expire_time = now+30m  : " + LocalDateTime.now().plusMinutes(30));
        System.out.println("Instant.now()          : " + Instant.now() + "  <- absolute, timezone-free");
    }
}
