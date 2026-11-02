"""
Pytest 配置与全局 Fixtures

学习章节：22-Python速成/18-测试进阶-pytest与可测性设计.md §3
"""
import pytest
from httpx import ASGITransport, AsyncClient

from app.main import create_app
from app.repositories.product_repo import ProductRepository
from app.repositories.cache import Cache
from app.services.recommend_service import RecommendService
from app.schemas.recommend import ProductDto


@pytest.fixture
def app():
    """
    创建测试应用实例

    对标 Spring Boot:
    @SpringBootTest
    class RecommendServiceTest {
        @Autowired
        private MockMvc mvc;
    }
    """
    return create_app()


@pytest.fixture
async def client(app):
    """
    HTTP 客户端（对标 MockMvc）

    对标:
    @Test
    public void test() {
        mvc.perform(get("/api/recommend")).andExpect(status().isOk());
    }
    """
    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://test",
    ) as c:
        yield c


@pytest.fixture
def mock_repo():
    """
    模拟 Repository（对标 @MockBean）

    对标 Spring Boot:
    @MockBean
    ProductRepository repository;
    """

    class MockProductRepository(ProductRepository):
        async def get_purchase_history(self, user_id: int, limit: int = 20):
            if user_id == 1:
                return [101, 102, 103]
            elif user_id == 2:
                return [104, 105]
            return []

        async def get_products_by_category(self, product_id: int, limit: int = 10):
            return [
                ProductDto(
                    product_id=201,
                    product_name="推荐商品 A",
                    price=99.0,
                    similarity_score=0.95,
                ),
                ProductDto(
                    product_id=202,
                    product_name="推荐商品 B",
                    price=149.0,
                    similarity_score=0.89,
                ),
            ]

    return MockProductRepository()


@pytest.fixture
def mock_cache():
    """
    模拟 Cache（对标 @MockBean for CacheService）
    """

    class MockCache(Cache):
        def __init__(self):
            super().__init__()
            self.hit_count = 0
            self.miss_count = 0

        async def get(self, key: str):
            result = await super().get(key)
            if result:
                self.hit_count += 1
            else:
                self.miss_count += 1
            return result

        async def get_stats(self):
            """获取缓存统计"""
            return {
                "hit_count": self.hit_count,
                "miss_count": self.miss_count,
                "hit_rate": self.hit_count / (self.hit_count + self.miss_count)
                if (self.hit_count + self.miss_count) > 0
                else 0.0,
            }

    return MockCache()


@pytest.fixture
def recommend_service(mock_repo, mock_cache):
    """
    测试服务实例（注入 Mock 依赖）

    对标:
    @Autowired
    RecommendService service;
    """
    return RecommendService(repo=mock_repo, cache=mock_cache)
