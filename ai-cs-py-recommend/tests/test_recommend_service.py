"""
推荐服务单元测试（对标 Spring Boot 的 @SpringBootTest）

学习章节：22-Python速成/18-测试进阶-pytest与可测性设计.md
"""
import pytest


@pytest.mark.asyncio
async def test_get_recommendations_success(recommend_service):
    """
    测试正常推荐流程

    对标:
    @Test
    public void testGetRecommendationsSuccess() {
        // given
        // when
        List<ProductDto> result = service.getRecommendations(1, 5);
        // then
        assertEquals(2, result.size());
    }
    """
    result = await recommend_service.get_recommendations(user_id=1, limit=5)

    assert len(result) > 0
    assert all(p.product_id > 0 for p in result)
    assert all(p.product_name for p in result)


@pytest.mark.asyncio
async def test_get_recommendations_user_not_found(recommend_service):
    """
    测试用户不存在的情况

    对标:
    @Test(expected = NotFoundException.class)
    public void testUserNotFound() {
        service.getRecommendations(999, 5);
    }
    """
    with pytest.raises(ValueError, match="用户 999 不存在"):
        await recommend_service.get_recommendations(user_id=999, limit=5)


@pytest.mark.asyncio
async def test_cache_hit(recommend_service, mock_cache):
    """
    测试缓存命中机制

    对标:
    @Test
    public void testCacheHit() {
        // 第一次调用（缓存写入）
        service.getRecommendations(1, 5);

        // 验证缓存命中
        verify(repository, times(1)).getPurchaseHistory(1);
    }
    """
    # 第一次调用（缓存未命中）
    result1 = await recommend_service.get_recommendations(user_id=1, limit=5)
    stats_before = await mock_cache.get_stats()

    # 第二次调用（缓存命中）
    result2 = await recommend_service.get_recommendations(user_id=1, limit=5)
    stats_after = await mock_cache.get_stats()

    # 验证两次结果相同
    assert len(result1) == len(result2)

    # 验证缓存命中率提升
    assert stats_after["hit_count"] > stats_before["hit_count"]


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "user_id,limit,expected_min",
    [
        (1, 5, 1),  # 用户 1 有推荐
        (2, 3, 1),  # 用户 2 有推荐
    ],
)
async def test_get_recommendations_parametrized(
    recommend_service, user_id, limit, expected_min
):
    """
    参数化测试

    对标:
    @ParameterizedTest
    @CsvSource({"1,5,1", "2,3,1"})
    public void testGetRecommendations(int userId, int limit, int expectedMin) {
        // ...
    }
    """
    result = await recommend_service.get_recommendations(user_id=user_id, limit=limit)

    assert len(result) >= expected_min
    assert len(result) <= limit


@pytest.mark.asyncio
async def test_recommendation_count_respect_limit(recommend_service):
    """
    测试推荐数量不超过限制

    对标:
    @Test
    public void testRecommendationLimit() {
        List<ProductDto> result = service.getRecommendations(1, 3);
        assertTrue(result.size() <= 3);
    }
    """
    result = await recommend_service.get_recommendations(user_id=1, limit=3)

    assert len(result) <= 3


@pytest.mark.asyncio
async def test_product_fields_not_null(recommend_service):
    """
    测试返回的商品字段不为空

    对标:
    @Test
    public void testProductFieldsNotNull() {
        List<ProductDto> result = service.getRecommendations(1, 5);
        for (ProductDto p : result) {
            assertNotNull(p.getProductId());
            assertNotNull(p.getProductName());
            assertNotNull(p.getPrice());
        }
    }
    """
    result = await recommend_service.get_recommendations(user_id=1, limit=5)

    for product in result:
        assert product.product_id is not None
        assert product.product_name is not None
        assert product.price is not None


@pytest.mark.asyncio
async def test_concurrent_recommendations(recommend_service):
    """
    测试并发场景（多个用户同时请求）

    对标:
    @Test
    public void testConcurrentRecommendations() {
        // 使用 Thread 或 ExecutorService
    }
    """
    import asyncio

    # 并发请求 5 个用户
    tasks = [
        recommend_service.get_recommendations(user_id=i, limit=5)
        for i in range(1, 6)
    ]

    results = await asyncio.gather(*tasks)

    # 验证所有请求都成功
    assert len(results) == 5
    assert all(len(r) > 0 for r in results)
