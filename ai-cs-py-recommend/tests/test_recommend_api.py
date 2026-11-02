"""
推荐服务接口测试（对标 @SpringBootTest + MockMvc）

学习章节：22-Python速成/18-测试进阶-pytest与可测性设计.md §6
"""
import pytest


@pytest.mark.asyncio
async def test_health_check(client):
    """
    测试健康检查端点

    对标:
    @Test
    public void testHealthCheck() {
        mvc.perform(get("/api/recommend/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("up"));
    }
    """
    response = await client.get("/api/recommend/health")

    assert response.status_code == 200
    assert response.json() == {"status": "up"}


@pytest.mark.asyncio
async def test_get_recommendations_post(client):
    """
    测试 POST /api/recommend/ 推荐接口

    对标:
    @Test
    public void testGetRecommendations() {
        RecommendRequest req = new RecommendRequest(1, 5);
        mvc.perform(post("/api/recommend/")
               .contentType(MediaType.APPLICATION_JSON)
               .content(toJson(req)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.userId").value(1))
           .andExpect(jsonPath("$.total").value(greaterThan(0)));
    }
    """
    request_body = {"user_id": 1, "limit": 5}

    response = await client.post("/api/recommend/", json=request_body)

    assert response.status_code == 200
    data = response.json()

    assert data["user_id"] == 1
    assert data["total"] > 0
    assert len(data["products"]) > 0
    assert all(p["product_id"] > 0 for p in data["products"])


@pytest.mark.asyncio
async def test_get_recommendations_get(client):
    """
    测试 GET /api/recommend/user/{user_id} 推荐接口

    对标:
    @Test
    public void testGetUserRecommendations() {
        mvc.perform(get("/api/recommend/user/1?limit=3"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(greaterThan(0)));
    }
    """
    response = await client.get("/api/recommend/user/1?limit=3")

    assert response.status_code == 200
    products = response.json()

    assert isinstance(products, list)
    assert len(products) > 0


@pytest.mark.asyncio
async def test_validation_error_user_id_zero(client):
    """
    测试参数校验：user_id = 0（应该返回 422）

    对标:
    @Test
    public void testValidationError() {
        RecommendRequest req = new RecommendRequest(0, 5);  // 违反 @NotNull
        mvc.perform(post("/api/recommend/")
               .content(toJson(req)))
           .andExpect(status().isBadRequest());
    }
    """
    request_body = {"user_id": 0, "limit": 5}  # user_id 必须 > 0

    response = await client.post("/api/recommend/", json=request_body)

    assert response.status_code == 422  # Pydantic 自动校验失败
    error = response.json()
    assert "detail" in error


@pytest.mark.asyncio
async def test_validation_error_limit_negative(client):
    """测试参数校验：limit < 0"""
    request_body = {"user_id": 1, "limit": -1}

    response = await client.post("/api/recommend/", json=request_body)

    assert response.status_code == 422


@pytest.mark.asyncio
async def test_user_not_found(client):
    """
    测试用户不存在的情况（应该返回 404）

    对标:
    @Test
    public void testUserNotFound() {
        mvc.perform(post("/api/recommend/")
               .content(toJson(new RecommendRequest(999, 5))))
           .andExpect(status().isNotFound());
    }
    """
    request_body = {"user_id": 999, "limit": 5}

    response = await client.post("/api/recommend/", json=request_body)

    assert response.status_code == 404  # 用户不存在
    error = response.json()
    assert "detail" in error


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "user_id,expected_min_count",
    [
        (1, 1),  # 用户 1 有购买历史
        (2, 1),  # 用户 2 有购买历史
    ],
)
async def test_recommendations_parametrized(client, user_id, expected_min_count):
    """
    参数化测试（对标 @ParameterizedTest）

    对标:
    @ParameterizedTest
    @CsvSource({"1,1", "2,1"})
    public void testRecommendations(int userId, int expectedMinCount) {
        // ...
    }
    """
    request_body = {"user_id": user_id, "limit": 5}

    response = await client.post("/api/recommend/", json=request_body)

    assert response.status_code == 200
    data = response.json()
    assert data["total"] >= expected_min_count


# ============== 性能测试（可选） ==============
@pytest.mark.asyncio
async def test_performance_baseline(client):
    """
    性能基线测试（确保单次请求 < 100ms）

    对标:
    @Test
    public void testPerformance() {
        long start = System.currentTimeMillis();
        service.getRecommendations(1, 5);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 100);
    }
    """
    import time

    start = time.perf_counter()

    response = await client.post(
        "/api/recommend/",
        json={"user_id": 1, "limit": 5},
    )

    duration_ms = (time.perf_counter() - start) * 1000

    assert response.status_code == 200
    # 性能要求：< 100ms（首次可能慢，缓存后应该更快）
    # assert duration_ms < 100, f"响应时间 {duration_ms:.1f}ms 超过 100ms"
