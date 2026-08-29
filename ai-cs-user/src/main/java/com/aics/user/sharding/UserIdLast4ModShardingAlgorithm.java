package com.aics.user.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * 用户ID后四位取模分片算法
 *
 * <p>取分片键（用户ID）末尾最多 4 位数字转为整数，对 sharding-count 取模：
 * 库路由配置 sharding-count=2（user_db_0/user_db_1），
 * 表路由配置 sharding-count=4（sys_user_0..sys_user_3）。
 * 取后四位使 ID 视觉上与分片位置可对应，便于人工排查。
 */
public class UserIdLast4ModShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    private static final String SHARDING_COUNT_KEY = "sharding-count";
    private static final int MAX_LAST_DIGITS = 4;

    private int shardingCount;

    @Override
    public void init(Properties props) {
        String count = props.getProperty(SHARDING_COUNT_KEY);
        if (count == null || count.isBlank()) {
            throw new IllegalArgumentException("分片算法缺少 " + SHARDING_COUNT_KEY + " 配置");
        }
        this.shardingCount = Integer.parseInt(count.trim());
        if (this.shardingCount <= 0) {
            throw new IllegalArgumentException(SHARDING_COUNT_KEY + " 必须为正整数: " + count);
        }
    }

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Comparable<?>> shardingValue) {
        long lastDigits = extractLastDigits(shardingValue.getValue());
        long targetIndex = lastDigits % shardingCount;
        return availableTargetNames.stream()
                .filter(name -> targetIndexOf(name) == targetIndex)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "取模结果 " + targetIndex + " 在目标集合中无可落分片: " + availableTargetNames));
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Comparable<?>> shardingValue) {
        // 取模路由无法按范围收敛，范围查询广播到全部分片
        return availableTargetNames;
    }

    private long extractLastDigits(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("分片键（用户ID）不能为空");
        }
        String text = String.valueOf(value);
        StringBuilder digits = new StringBuilder();
        for (int i = text.length() - 1; i >= 0 && digits.length() < MAX_LAST_DIGITS; i--) {
            char ch = text.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digits.insert(0, ch);
            }
        }
        if (digits.length() == 0) {
            throw new IllegalArgumentException("分片键（用户ID）末尾不含数字: " + text);
        }
        return Long.parseLong(digits.toString());
    }

    private long targetIndexOf(String targetName) {
        int lastUnderscore = targetName.lastIndexOf('_');
        if (lastUnderscore < 0 || lastUnderscore == targetName.length() - 1) {
            throw new IllegalStateException("分片目标名称缺少数字后缀: " + targetName);
        }
        return Long.parseLong(targetName.substring(lastUnderscore + 1));
    }
}
