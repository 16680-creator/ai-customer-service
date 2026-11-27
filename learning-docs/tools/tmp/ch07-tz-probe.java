import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.TimeZone;

/**
 * learning-docs/15-计算机基础/07-字符编码与时区.md 的 C 级实测脚本（JDK 17 单文件运行）。
 * 运行：java learning-docs\tools\tmp\ch07-tz-probe.java <被测文件路径>
 */
public class Ch07TzProbe {

    public static void main(String[] args) throws Exception {
        System.out.println("[1] 运行期字符集相关属性");
        System.out.println("    Charset.defaultCharset()      = " + Charset.defaultCharset());
        System.out.println("    System.getProperty(file.encoding)   = " + System.getProperty("file.encoding"));
        System.out.println("    System.getProperty(native.encoding) = " + System.getProperty("native.encoding"));
        System.out.println("    System.getProperty(sun.jnu.encoding)= " + System.getProperty("sun.jnu.encoding"));

        System.out.println("[2] 运行期时区相关属性");
        System.out.println("    user.timezone                 = " + System.getProperty("user.timezone"));
        System.out.println("    TimeZone.getDefault().getID() = " + TimeZone.getDefault().getID());
        System.out.println("    ZoneId.systemDefault()        = " + ZoneId.systemDefault());
        System.out.println("    LocalDateTime.now()           = " + LocalDateTime.now());
        System.out.println("    ZonedDateTime.now()           = " + ZonedDateTime.now());
        System.out.println("    OffsetDateTime.now()          = " + OffsetDateTime.now());
        System.out.println("    Instant.now()                 = " + Instant.now());

        System.out.println("[3] 同一个 LocalDateTime 在两种 serverTimezone 解释下的绝对时刻");
        LocalDateTime ldt = LocalDateTime.of(2026, 11, 27, 9, 0, 0);
        Instant asShanghai = ldt.atZone(ZoneId.of("Asia/Shanghai")).toInstant();
        Instant asUtc = ldt.atZone(ZoneOffset.UTC).toInstant();
        System.out.println("    字面值 " + ldt + " 按 Asia/Shanghai 解释 -> " + asShanghai);
        System.out.println("    字面值 " + ldt + " 按 UTC          解释 -> " + asUtc);
        System.out.println("    两者相差小时数 = " + Duration.between(asUtc, asShanghai).toHours());

        System.out.println("[4] 同一个 Instant 在不同时区属于不同的“同一天”");
        Instant instant = Instant.parse("2026-11-27T17:30:00Z");
        System.out.println("    " + instant
                + " → Asia/Shanghai 日期 " + instant.atZone(ZoneId.of("Asia/Shanghai")).toLocalDate()
                + " / UTC 日期 " + instant.atZone(ZoneOffset.UTC).toLocalDate()
                + " / America/New_York 日期 " + instant.atZone(ZoneId.of("America/New_York")).toLocalDate());

        System.out.println("[5] LocalDateTime 不带时区：序列化后无法还原绝对时刻");
        ZonedDateTime zdt = ZonedDateTime.of(2026, 11, 27, 9, 0, 0, 0, ZoneId.of("Asia/Shanghai"));
        System.out.println("    ZonedDateTime -> " + zdt + "  (toString 带偏移 " + zdt.getOffset() + ")");
        System.out.println("    toLocalDateTime().toString() -> " + zdt.toLocalDateTime());
        System.out.println("    丢掉的偏移量再按 UTC 解析回去 = " + zdt.toLocalDateTime().atZone(ZoneOffset.UTC));
        System.out.println("    往返误差 = " + Duration.between(
                zdt.toInstant(), zdt.toLocalDateTime().atZone(ZoneOffset.UTC).toInstant()).toHours() + " 小时");

        System.out.println("[6] Java String 是 UTF-16：码元数 != 码点数");
        String msg = "订单😊";
        System.out.println("    msg.length()                            = " + msg.length());
        System.out.println("    msg.codePointCount(0, length())         = " + msg.codePointCount(0, msg.length()));
        System.out.println("    msg.getBytes(UTF_8).length              = " + msg.getBytes(Charset.forName("UTF-8")).length);
        System.out.println("    msg.substring(2, 3) 的码点              = "
                + Integer.toHexString(msg.substring(2, 3).codePointAt(0)).toUpperCase());
        System.out.println("    substring(2,3) 再转 UTF-8 的字节         = "
                + bytesToHex(msg.substring(2, 3).getBytes(Charset.forName("UTF-8"))));

        System.out.println("[7] 同一份 UTF-8 字节用 UTF-8 与 GBK 解码，行数与字符数对比");
        if (args.length > 0) {
            Path p = Paths.get(args[0]);
            byte[] raw = Files.readAllBytes(p);
            String utf8 = new String(raw, Charset.forName("UTF-8"));
            String gbk = new String(raw, Charset.forName("GBK"));
            System.out.println("    文件 = " + p + " (" + raw.length + " 字节)");
            System.out.println("    UTF-8 解码：行数 = " + countLineFeeds(utf8) + "，字符数 = " + utf8.length());
            System.out.println("    GBK   解码：行数 = " + countLineFeeds(gbk) + "，字符数 = " + gbk.length());
            System.out.println("    GBK 解码首行 = " + firstLine(gbk));
        } else {
            System.out.println("    (未传文件路径，跳过)");
        }

        System.out.println("[8] 默认字符集对“写文件”的影响");
        Path out = Files.createTempFile("ch07-", ".txt");
        Files.write(out, "订单😊".getBytes(Charset.defaultCharset()));
        System.out.println("    用 defaultCharset 写 “订单😊” 后文件字节数 = " + Files.size(out));
        System.out.println("    文件真实字节 = " + bytesToHex(Files.readAllBytes(out)));
        Files.deleteIfExists(out);
    }

    private static int countLineFeeds(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    private static String firstLine(String s) {
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, Math.min(i, 40));
    }

    private static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02X ", x));
        }
        return sb.toString().trim();
    }
}
