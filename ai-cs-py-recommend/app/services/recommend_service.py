"""
推荐服务（对标 @Service）

学习章节：22-Python速成/11-FastAPI实战-对着SpringBoot学.md §6
"""
import logging
from typing import List

from app.schemas.recommend import ProductDto
from app.repositories.cache import Cache
from app.repositories.product_repo import ProductRepository

logger = logging.getLogger(__name__)


class RecommendService:
    """
    推荐业务逻辑（对标 Spring Boot @Service）

    Java 等价:
    @Service
    public class RecommendService {
        private final ProductRepository repo;
        private final Cache cache;

        public RecommendService(ProductRepository repo, Cache cache) {
            this.repo = repo;
            this.cache = cache;
        }
    }

    Python 注意点：
    - 没有 IoC 容器，依赖通过构造函数传入
    - 通过 get_recommend_service() 工厂函数获取单例
    """

    def __init__(self, repo: ProductRepository | None = None, cache: Cache | None = None):
        # 构造注入 + 默认依赖（便于测试时替换）
        self.repo = repo or ProductRepository()
        self.cache = cache or Cache()

    async def get_recommendations(self, user_id: int, limit: int = 5) -> List[ProductDto]:
        """
        获取推荐商品

        算法流程：
        1. 从缓存获取用户购买历史
        2. 基于历史商品查找相似商品
        3. 排序并去重
        4. 返回 Top N

        对标 Java:
        @Transactional(readOnly = true)
        public List<ProductDto> getRecommendations(int userId, int limit) {
            // 业务逻辑
        }
        """
        logger.info("获取推荐", extra={"user_id": user_id, "limit": limit})

        # 1. 从缓存获取用户历史
        cache_key = f"user:history:{user_id}"
        history = await self.cache.get(cache_key)

        if not history:
            # 缓存未命中，回源数据库
            history = await self.repo.get_purchase_history(user_id)
            if not history:
                raise ValueError(f"用户 {user_id} 不存在")

            # 写回缓存（TTL 1 小时）
            await self.cache.set(cache_key, history, ttl=3600)
            logger.debug("用户历史缓存未命中，已写回", extra={"user_id": user_id})
        else:
            logger.debug("用户历史缓存命中", extra={"user_id": user_id})

        # 2. 基于历史商品查找相似商品
        candidates = []
        for product_id in history:
            similar = await self._find_similar_products(product_id, limit=10)
            candidates.extend(similar)

        # 3. 去重 + 排序
        unique_candidates = {p.product_id: p for p in candidates}.values()
        ranked = self._rank_products(list(unique_candidates), user_id)

        # 4. 截断返回 Top N
        result = ranked[:limit]
        logger.info(
            "推荐完成",
            extra={"user_id": user_id, "result_count": len(result)},
        )
        return result

    async def _find_similar_products(
        self, product_id: int, limit: int = 10
    ) -> List[ProductDto]:
        """
        查找相似商品

        实现策略（TODO 选择一个）：
        ① 基于类别（简单）
        ② 基于 embedding 相似度（精准）
        ③ 基于协同过滤（有效）
        """
        # 简单实现：同类别商品
        return await self.repo.get_products_by_category(product_id, limit)

    def _rank_products(self, candidates: List[ProductDto], user_id: int) -> List[ProductDto]:
        """
        对候选商品排序

        排序维度（可选）：
        - 销量
        - 评分
        - 用户偏好
        - 库存
        """
        # TODO: 根据用户偏好和商品热度排序
        # 临时实现：按相似度分数排序
        return sorted(
            candidates,
            key=lambda p: p.similarity_score if p.similarity_score else 0.0,
            reverse=True,
        )


# ============== 单例管理 ==============
_service: RecommendService | None = None


def get_recommend_service() -> RecommendService:
    """
    获取推荐服务单例

    对标 Spring Boot 的自动装配：
    @Autowired
    private RecommendService service;

    Python 模式：
    - 全局变量 + 惰性初始化
    - 便于测试时替换（setattr）
    """
    global _service
    if _service is None:
        _service = RecommendService()
    return _service


# ============== TODO 练习任务 ==============
# 1. 实现 _rank_products 的完整排序逻辑
# 2. 添加热门商品推荐（不需要用户历史）
# 3. 添加个性化推荐（基于用户标签）
# 4. 实现 @Transactional 等价物（如果涉及多步骤操作）
# 5. 添加重试机制（对标 Spring Retry）
