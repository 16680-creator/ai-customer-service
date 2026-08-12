package com.aics.search.hybrid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RRF（Reciprocal Rank Fusion，倒数排名融合）合并器单元测试
 *
 * <p>验证 {@link RrfMerger#merge(List, List, int, int)} 的 RRF 公式（k=60）与边界行为：
 * <ul>
 *   <li>RRF 分数 = Σ 1/(k + rank)，按分数降序输出</li>
 *   <li>任一路为空时直接返回另一路；两路都为空返回空列表</li>
 *   <li>topK 截断、空 id 跳过</li>
 * </ul>
 */
class RrfMergerTest {

    /** RRF 平滑常数，与生产代码保持一致 */
    private static final int K = 60;

    @Test
    @DisplayName("RRF 公式计算：每路第 rank 名贡献 1/(k+rank)，k=60")
    void merge_shouldCalculateRrfScores() {
        // list1: id1(rank1=1) id2(rank1=2) id3(rank1=3)；list2: id2(rank2=1) id4(rank2=2)
        List<RankedItem> list1 = List.of(item("1", "ES-1"), item("2", "ES-2"), item("3", "ES-3"));
        List<RankedItem> list2 = List.of(item("2", "向量-2"), item("4", "向量-4"));

        List<RankedItem> merged = RrfMerger.merge(list1, list2, 10, K);

        assertEquals(4, merged.size());
        // id1 仅在 ES 路第 1 名：1/61
        assertEquals(1.0 / 61, scoreOf(merged, "1"), 1e-9);
        // id2 双路命中：1/62 + 1/61
        assertEquals(1.0 / 61 + 1.0 / 62, scoreOf(merged, "2"), 1e-9);
        // id3 仅在 ES 路第 3 名：1/63
        assertEquals(1.0 / 63, scoreOf(merged, "3"), 1e-9);
        // id4 仅在向量路第 2 名：1/62
        assertEquals(1.0 / 62, scoreOf(merged, "4"), 1e-9);
    }

    @Test
    @DisplayName("双路结果正常融合：按融合分数降序，保留各路排名")
    void merge_shouldFuseBothListsInScoreOrder() {
        List<RankedItem> list1 = List.of(item("1", "ES-1"), item("2", "ES-2"), item("3", "ES-3"));
        List<RankedItem> list2 = List.of(item("2", "向量-2"), item("4", "向量-4"));

        List<RankedItem> merged = RrfMerger.merge(list1, list2, 10, K);

        // 期望顺序：id2(0.0325) > id1(0.01639) > id4(0.01613) > id3(0.01587)
        assertEquals(List.of("2", "1", "4", "3"),
                merged.stream().map(RankedItem::getId).collect(Collectors.toList()));

        RankedItem first = merged.get(0);
        assertEquals("2", first.getId());
        assertEquals(2, first.getRank1(), "ES 路排名应为 2");
        assertEquals(1, first.getRank2(), "向量路排名应为 1");
        assertEquals("ES-2", first.getTitle(), "字段以先到者（ES 路）优先");

        RankedItem onlyEs = merged.get(1);
        assertEquals(1, onlyEs.getRank1());
        assertEquals(0, onlyEs.getRank2(), "未命中向量路时 rank2 应为 0");
    }

    @Test
    @DisplayName("list1 为空时直接返回 list2 融合结果")
    void merge_shouldReturnList2WhenList1Empty() {
        List<RankedItem> list2 = List.of(item("b", "B"), item("d", "D"));

        List<RankedItem> merged = RrfMerger.merge(Collections.emptyList(), list2, 10, K);

        assertEquals(2, merged.size());
        assertEquals("b", merged.get(0).getId());
        assertEquals("d", merged.get(1).getId());
        assertEquals(1.0 / 61, merged.get(0).getScore(), 1e-9);
        assertEquals(1, merged.get(0).getRank2());
        assertEquals(0, merged.get(0).getRank1());
    }

    @Test
    @DisplayName("list2 为空时直接返回 list1 融合结果")
    void merge_shouldReturnList1WhenList2Empty() {
        List<RankedItem> list1 = List.of(item("a", "A"), item("c", "C"));

        List<RankedItem> merged = RrfMerger.merge(list1, Collections.emptyList(), 10, K);

        assertEquals(2, merged.size());
        assertEquals("a", merged.get(0).getId());
        assertEquals("c", merged.get(1).getId());
        assertEquals(1.0 / 61, merged.get(0).getScore(), 1e-9);
        assertEquals(1, merged.get(0).getRank1());
        assertEquals(0, merged.get(0).getRank2());
    }

    @Test
    @DisplayName("两路都为空时返回空列表")
    void merge_shouldReturnEmptyWhenBothEmpty() {
        List<RankedItem> merged = RrfMerger.merge(Collections.emptyList(), Collections.emptyList(), 10, K);

        assertNotNull(merged);
        assertTrue(merged.isEmpty());
    }

    @Test
    @DisplayName("topK 截断：只返回融合分数最高的前 topK 条")
    void merge_shouldTruncateToTopK() {
        // list1: a1..a10（rank1 = 1..10），list2: a1..a5（rank2 = 1..5，与 list1 前 5 重叠）
        List<RankedItem> list1 = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> item("a" + i, "ES-" + i))
                .collect(Collectors.toList());
        List<RankedItem> list2 = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> item("a" + i, "向量-" + i))
                .collect(Collectors.toList());

        List<RankedItem> merged = RrfMerger.merge(list1, list2, 5, K);

        // 前 5 名：a1(2/61) > a2(2/62) > a3(2/63) > a4(2/64) > a5(2/65)
        assertEquals(5, merged.size());
        assertEquals(List.of("a1", "a2", "a3", "a4", "a5"),
                merged.stream().map(RankedItem::getId).collect(Collectors.toList()));
        assertEquals(2.0 / 61, merged.get(0).getScore(), 1e-9);
        assertEquals(2.0 / 65, merged.get(4).getScore(), 1e-9);
    }

    @Test
    @DisplayName("空 id 的条目应被跳过")
    void merge_shouldSkipBlankIdItems() {
        RankedItem blankId = new RankedItem();
        blankId.setTitle("无 id 条目");
        List<RankedItem> list1 = Arrays.asList(item("1", "ES-1"), blankId);

        List<RankedItem> merged = RrfMerger.merge(list1, Collections.emptyList(), 10, K);

        assertEquals(1, merged.size());
        assertEquals("1", merged.get(0).getId());
    }

    /** 从融合结果中取指定 id 的分数 */
    private double scoreOf(List<RankedItem> merged, String id) {
        return merged.stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 id=" + id))
                .getScore();
    }

    /** 构造带 id 与标题的测试条目 */
    private RankedItem item(String id, String title) {
        RankedItem item = new RankedItem();
        item.setId(id);
        item.setTitle(title);
        return item;
    }
}
