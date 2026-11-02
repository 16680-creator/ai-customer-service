"""
请求/响应数据模型（对标 DTO）

学习章节：22-Python速成/11-FastAPI实战-对着SpringBoot学.md §5
"""
from pydantic import BaseModel, Field


class ProductDto(BaseModel):
    """
    商品 DTO（对标 Java @Entity 或 VO）

    学习重点：
    - Pydantic 自动处理 JSON 序列化/反序列化
    - Field 定义校验规则和文档
    - populate_by_name=True 实现 camelCase 兼容
    """

    product_id: int = Field(..., description="商品 ID", gt=0)
    product_name: str = Field(..., description="商品名称", min_length=1)
    price: float = Field(..., description="价格", ge=0.0)
    similarity_score: float | None = Field(None, description="相似度分数 (0-1)", ge=0.0, le=1.0)

    class Config:
        # 前端可以用 camelCase 发送 (productId)，Python 内部用 snake_case
        populate_by_name = True


class RecommendRequest(BaseModel):
    """
    推荐请求

    对标 Java:
    @Data
    public class RecommendRequest {
        @NotNull int userId;
        @Min(1) int limit = 5;
    }
    """

    user_id: int = Field(..., description="用户 ID", gt=0)
    limit: int | None = Field(5, description="推荐数量", ge=1, le=20)

    class Config:
        populate_by_name = True


class RecommendResponse(BaseModel):
    """
    推荐响应

    对标 Java:
    @Data
    public class RecommendResponse {
        int userId;
        List<ProductDto> products;
        int total;
    }
    """

    user_id: int = Field(..., description="用户 ID")
    products: list[ProductDto] = Field(default_factory=list, description="推荐商品列表")
    total: int = Field(0, description="推荐商品总数")

    class Config:
        populate_by_name = True


# ============== TODO 练习任务 ==============
# 1. 添加 UserHistoryDto，包含用户购买历史
# 2. 添加 AlgorithmStatDto，返回算法执行统计（耗时、缓存命中率等）
# 3. 为 ProductDto 添加 category_id 和 rating 字段
# 4. 实现 Field 的 examples 参数，使 Swagger UI 显示示例数据
