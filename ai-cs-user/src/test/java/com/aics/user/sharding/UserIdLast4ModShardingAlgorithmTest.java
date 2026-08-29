package com.aics.user.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * 用户ID后四位取模分片算法单元测试
 * TDD: 库路由（后四位 % 2）、表路由（后四位 % 4）、边界与异常分支
 */
class UserIdLast4ModShardingAlgorithmTest {

    private UserIdLast4ModShardingAlgorithm algorithm(int shardingCount) {
        UserIdLast4ModShardingAlgorithm algorithm = new UserIdLast4ModShardingAlgorithm();
        Properties props = new Properties();
        props.setProperty("sharding-count", String.valueOf(shardingCount));
        algorithm.init(props);
        return algorithm;
    }

    @SuppressWarnings("unchecked")
    private PreciseShardingValue<Comparable<?>> preciseValue(Comparable<?> value) {
        PreciseShardingValue<Comparable<?>> shardingValue = mock(PreciseShardingValue.class);
        doReturn(value).when(shardingValue).getValue();
        return shardingValue;
    }

    @SuppressWarnings("unchecked")
    private RangeShardingValue<Comparable<?>> rangeValue() {
        return mock(RangeShardingValue.class);
    }

    @Test
    @DisplayName("库路由：雪花ID后四位 % 2 命中对应库")
    void routeDatabaseByLast4Mod() {
        UserIdLast4ModShardingAlgorithm algorithm = algorithm(2);
        // 6208 % 2 = 0
        assertEquals("user_db_0", algorithm.doSharding(
                Set.of("user_db_0", "user_db_1"), preciseValue(1948329203219486208L)));
        // id=1，后四位 "1"，1 % 2 = 1（admin 种子用户落 user_db_1）
        assertEquals("user_db_1", algorithm.doSharding(
                Set.of("user_db_0", "user_db_1"), preciseValue(1L)));
        // 5678 % 2 = 0
        assertEquals("user_db_0", algorithm.doSharding(
                Set.of("user_db_0", "user_db_1"), preciseValue(12345678L)));
    }

    @Test
    @DisplayName("表路由：雪花ID后四位 % 4 命中对应表")
    void routeTableByLast4Mod() {
        UserIdLast4ModShardingAlgorithm algorithm = algorithm(4);
        // 6208 % 4 = 0
        assertEquals("sys_user_0", algorithm.doSharding(
                Set.of("sys_user_0", "sys_user_1", "sys_user_2", "sys_user_3"),
                preciseValue(1948329203219486208L)));
        // 1 % 4 = 1
        assertEquals("sys_user_1", algorithm.doSharding(
                Set.of("sys_user_0", "sys_user_1", "sys_user_2", "sys_user_3"),
                preciseValue(1L)));
        // 5678 % 4 = 2
        assertEquals("sys_user_2", algorithm.doSharding(
                Set.of("sys_user_0", "sys_user_1", "sys_user_2", "sys_user_3"),
                preciseValue(12345678L)));
        // 4567 % 4 = 3
        assertEquals("sys_user_3", algorithm.doSharding(
                Set.of("sys_user_0", "sys_user_1", "sys_user_2", "sys_user_3"),
                preciseValue(202508291234567L)));
    }

    @Test
    @DisplayName("不足四位的ID取全部数字参与取模")
    void shortIdUsesAllDigits() {
        UserIdLast4ModShardingAlgorithm algorithm = algorithm(2);
        assertEquals("user_db_0", algorithm.doSharding(
                Set.of("user_db_0", "user_db_1"), preciseValue(100L)));
    }

    @Test
    @DisplayName("范围查询无法定位取模分片时全路由")
    void rangeQueryRoutesToAllTargets() {
        UserIdLast4ModShardingAlgorithm algorithm = algorithm(4);
        Set<String> targets = Set.of("sys_user_0", "sys_user_1", "sys_user_2", "sys_user_3");
        assertEquals(targets, algorithm.doSharding(targets, rangeValue()).stream()
                .collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("缺少 sharding-count 配置时初始化失败")
    void initFailsWithoutShardingCount() {
        UserIdLast4ModShardingAlgorithm algorithm = new UserIdLast4ModShardingAlgorithm();
        assertThrows(IllegalArgumentException.class, () -> algorithm.init(new Properties()));
    }

    @Test
    @DisplayName("分片键为null时拒绝路由")
    void nullValueRejected() {
        UserIdLast4ModShardingAlgorithm algorithm = algorithm(2);
        assertThrows(IllegalArgumentException.class, () ->
                algorithm.doSharding(Set.of("user_db_0", "user_db_1"), preciseValue(null)));
    }

    @Test
    @DisplayName("分片键不含数字时拒绝路由")
    void nonNumericValueRejected() {
        UserIdLast4ModShardingAlgorithm algorithm = algorithm(2);
        assertThrows(IllegalArgumentException.class, () ->
                algorithm.doSharding(Set.of("user_db_0", "user_db_1"), preciseValue("abcdefg")));
    }

    @Test
    @DisplayName("取模结果不在目标集合时拒绝路由")
    void unmatchedTargetRejected() {
        UserIdLast4ModShardingAlgorithm algorithm = algorithm(4);
        // 6208 % 4 = 0，但目标集合中只有 1/3 库，无 sys_user_0 可落
        assertThrows(IllegalStateException.class, () ->
                algorithm.doSharding(Set.of("user_db_1", "sys_user_3"), preciseValue(1948329203219486208L)));
    }

    @Test
    @DisplayName("同分母下库路由与表路由结果保持同余一致性")
    void databaseAndTableStayConsistent() {
        // 同一批 ID，库路由（%2）与表路由（%4）满足 table = db + 4 的交错分布不要求，
        // 但必须保证 tableIndex % 2 == dbIndex 同余成立（防数据错位）
        UserIdLast4ModShardingAlgorithm dbAlgorithm = algorithm(2);
        UserIdLast4ModShardingAlgorithm tableAlgorithm = algorithm(4);
        for (long id : new long[]{1L, 12345678L, 1948329203219486208L, 202508291234567L}) {
            String db = dbAlgorithm.doSharding(Set.of("user_db_0", "user_db_1"), preciseValue(id));
            String table = tableAlgorithm.doSharding(
                    Set.of("sys_user_0", "sys_user_1", "sys_user_2", "sys_user_3"), preciseValue(id));
            int dbIndex = Integer.parseInt(db.substring(db.lastIndexOf('_') + 1));
            int tableIndex = Integer.parseInt(table.substring(table.lastIndexOf('_') + 1));
            assertTrue(tableIndex % 2 == dbIndex,
                    () -> "id=" + id + " 表分片与库分片不同余: db=" + dbIndex + ", table=" + tableIndex);
        }
    }
}
