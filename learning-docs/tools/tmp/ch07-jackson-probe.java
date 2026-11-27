import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * learning-docs/15-计算机基础/07-字符编码与时区.md 的 C 级实测脚本。
 * 需要 m2 里的 jackson-databind / core / annotations / datatype-jsr310（版本 2.21.8），
 * 运行示例见正文 §6.6（-cp 指向 4 个 jar）。
 */
public class Ch07JacksonProbe {

    public static class PayInfo {
        public String orderNo = "A20261127001";
        public LocalDateTime payTime = LocalDateTime.of(2026, 11, 27, 9, 0, 0);
        public Instant paidAt = Instant.parse("2026-11-27T01:00:00Z");
        public ZonedDateTime zonedPayTime = ZonedDateTime.of(2026, 11, 27, 9, 0, 0, 0, ZoneId.of("Asia/Shanghai"));
        public LocalDate bizDate = LocalDate.of(2026, 11, 27);
        public String remark = "订单已支付😊";
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        PayInfo info = new PayInfo();
        String json = mapper.writeValueAsString(info);
        System.out.println("[1] 默认 Jackson（Spring Boot 行为：JavaTimeModule + 关闭时间戳）序列化结果");
        System.out.println("    " + json);
        System.out.println("[2] 同一份 JSON 的 UTF-8 字节数（审计留痕/字段长度评估要看这个）");
        System.out.println("    utf8 bytes = " + json.getBytes(StandardCharsets.UTF_8).length);

        System.out.println("[3] 反序列化回来：哪些字段保住了绝对时刻，哪些只保住了“墙上时间”");
        PayInfo back = mapper.readValue(json, PayInfo.class);
        System.out.println("    payTime       = " + back.payTime + "   （LocalDateTime，无偏移）");
        System.out.println("    paidAt        = " + back.paidAt + "   （Instant，绝对时刻）");
        System.out.println("    zonedPayTime  = " + back.zonedPayTime + "   （带偏移与地区）");

        System.out.println("[4] LocalDateTime 的 JSON 文本在“按 UTC 解读”的消费方手里会变成什么");
        String asText = back.payTime.toString();
        Instant ifUtc = LocalDateTime.parse(asText).toInstant(ZoneOffset.UTC);
        Instant ifShanghai = LocalDateTime.parse(asText).atZone(ZoneId.of("Asia/Shanghai")).toInstant();
        System.out.println("    JSON 文本 = " + asText);
        System.out.println("    消费方按 UTC 解读      → " + ifUtc);
        System.out.println("    生产方按 Asia/Shanghai → " + ifShanghai);
        System.out.println("    两者相差秒数 = " + (ifShanghai.getEpochSecond() - ifUtc.getEpochSecond()));

        System.out.println("[5] 显式带上偏移的写法：OffsetDateTime/ISO_OFFSET_DATE_TIME 才能自证");
        String withOffset = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(ZonedDateTime.of(2026, 11, 27, 9, 0, 0, 0, ZoneId.of("Asia/Shanghai")));
        System.out.println("    " + withOffset + " → 解析回 Instant = "
                + java.time.OffsetDateTime.parse(withOffset).toInstant());
    }
}
