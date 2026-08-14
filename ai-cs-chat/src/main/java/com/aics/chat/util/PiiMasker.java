package com.aics.chat.util;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感信息脱敏工具（PII Mask，3.2 F3）。
 *
 * <p>覆盖类型与规则：</p>
 * <ul>
 *   <li><b>手机号</b>：11 位，保留前 3 位 + 后 4 位（{@code 138****5678}）；</li>
 *   <li><b>身份证号</b>：18 位，保留前 6 位 + 后 4 位（{@code 110101********771X}）；</li>
 *   <li><b>银行卡号</b>：13~19 位且通过 Luhn 校验，保留前 6 位 + 后 4 位（{@code 411111********1111}）；
 *       不通过 Luhn 的数字串（订单号、时间戳等）不做脱敏，避免误伤普通数据；</li>
 *   <li><b>邮箱</b>：本地部分整体遮蔽（{@code ***@example.com}）；</li>
 *   <li><b>地址门牌号</b>：X号/X号楼/X栋 的数字遮蔽（{@code 望京街道***号院}）。</li>
 * </ul>
 */
@Component
public class PiiMasker {

    /**
     * 11 位手机号：保留前 3 位 + 后 4 位（前后不能是数字，防止在更长数字串中误伤）
     * 正则拆解：
     *   (?<!\d)      — 前向不能是数字
     *   (1[3-9]\d)   — 第1组：前3位（1 开头 + 3~9 + 任意数字，即手机号号段）
     *   \d{4}        — 中间4位（被脱敏，不保留）
     *   (\d{4})      — 第2组：后4位（保留）
     *   (?!\d)       — 后向不能是数字
     */
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)");

    /**
     * 18 位身份证号：保留前 6 位 + 后 4 位（前后不能是数字，防止把 18+ 位数字串
     * 如订单号误判为身份证）
     * 正则拆解：
     *   (?<!\d)      — 前向不能是数字
     *   (\d{6})      — 第1组：前6位（行政区划码）
     *   \d{8}        — 中间8位（出生日期，被脱敏）
     *   (\d{3}[\dXx])— 第2组：后4位（顺序码3位 + 校验码，校验码可为 X）
     *   (?!\d)       — 后向不能是数字
     */
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)(\\d{6})\\d{8}(\\d{3}[\\dXx])(?!\\d)");

    /**
     * 银行卡号：13~19 位连续数字（前后不能是数字），命中后还需通过 Luhn 校验才脱敏
     * 拆解：
     *   (?<!\d)      — 前向不能是数字
     *   (\d{6})      — 第1组：前6位（发卡行标识 BIN）
     *   \d{3,9}      — 中间 3~9 位（被脱敏）
     *   (\d{4})      — 第2组：后4位（保留）
     *   (?!\d)       — 后向不能是数字
     */
    private static final Pattern BANK_CARD = Pattern.compile("(?<!\\d)(\\d{6})\\d{3,9}(\\d{4})(?!\\d)");

    /**
     * 邮箱：本地部分遮蔽
     *   ([A-Za-z0-9._%+-]+)  — 本地部分（被脱敏）
     *   @([A-Za-z0-9.-]+\.[A-Za-z]{2,}) — 域名（保留）
     */
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    /**
     * 地址门牌号：数字 + 号/号楼/栋 形式（数字被遮蔽）
     *   (\d{1,6})  — 门牌数字（被脱敏）
     *   (号(?:楼|院|栋)?|号楼|栋) — 单位后缀（保留）
     */
    private static final Pattern ADDRESS_NO = Pattern.compile("(\\d{1,6})(号(?:楼|院|栋)?|号楼|栋)");

    /**
     * 脱敏文本中的手机号、身份证、银行卡、邮箱与地址门牌号。
     *
     * @param text 原始文本（可为 null）
     * @return 脱敏后的文本
     */
    public String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // 学习点（脱敏顺序）：先长后短、先具体后宽泛——
        // 身份证（18位）→ 银行卡（13-19位+Luhn）→ 手机号（11位）→ 邮箱 → 地址。
        // 学习点（数字边界）：所有定长数字类正则必须带 (?<!\d)(?!\d) 前后向断言。
        // 实战踩坑：20 位订单号 "20260814000000123456" 曾先被身份证正则（18位子串）、
        // 再被手机号正则（11位子串）在长数字串内部滑动命中——加边界后不再误伤。
        String masked = ID_CARD.matcher(text).replaceAll("$1********$2");
        masked = maskBankCards(masked);
        masked = PHONE.matcher(masked).replaceAll("$1****$2");
        masked = EMAIL.matcher(masked).replaceAll("***@$1");
        masked = ADDRESS_NO.matcher(masked).replaceAll("***$2");
        return masked;
    }

    /**
     * 银行卡脱敏：命中 13~19 位数字串后做 Luhn 校验，通过才遮蔽中间位。
     */
    private String maskBankCards(String text) {
        Matcher matcher = BANK_CARD.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String digits = matcher.group().replace(" ", "");
            String replacement = luhnValid(digits)
                    ? matcher.group(1) + "********" + matcher.group(2)
                    : matcher.group();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Luhn 校验（银行卡号合法性）：从右向左，隔位乘 2 后减 9，累加和能被 10 整除。
     */
    private static boolean luhnValid(String digits) {
        if (digits.length() < 13 || digits.length() > 19) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (d < 0 || d > 9) {
                return false;
            }
            if (alternate) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
