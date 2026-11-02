"""
推荐服务路由层（对标 @RestController）

学习章节：22-Python速成/11-FastAPI实战-对着SpringBoot学.md §4
"""
import logging
from typing import List

from fastapi import APIRouter, HTTPException, status

from app.schemas.recommend import RecommendRequest, RecommendResponse, ProductDto
from app.services import recommend_service

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/recommend", tags=["推荐服务"])


@router.get("/health")
async def health() -> dict:
    """
    健康检查

    对标:
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "up");
    }
    """
    return {"status": "up"}


@router.post("/", response_model=RecommendResponse)
async def get_recommendations(req: RecommendRequest) -> RecommendResponse:
    """
    获取用户推荐商品

    对标:
    @PostMapping("/")
    public Result<RecommendResponse> getRecommendations(
        @Valid @RequestBody RecommendRequest req
    ) {
        return Result.success(recommendService.getRecommendations(req));
    }

    学习章节：
    - 参数校验：11篇 §5 (Pydantic 自动校验)
    - 异常处理：11篇 §9 (HTTPException)
    """
    try:
        service = recommend_service.get_recommend_service()
        products = await service.get_recommendations(
            user_id=req.user_id,
            limit=req.limit or 5,
        )

        return RecommendResponse(
            user_id=req.user_id,
            products=products,
            total=len(products),
        )

    except ValueError as e:
        # 业务异常（如用户不存在）
        logger.warning("业务异常: %s", str(e), extra={"user_id": req.user_id})
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        )
    except Exception as e:
        # 未预期异常
        logger.exception("推荐服务异常", extra={"user_id": req.user_id})
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="服务暂时不可用，请稍后再试",
        )


@router.get("/user/{user_id}", response_model=List[ProductDto])
async def get_user_recommendations(
    user_id: int,
    limit: int = 5,
) -> List[ProductDto]:
    """
    获取指定用户的推荐（GET 方式）

    对标:
    @GetMapping("/user/{userId}")
    public Result<List<ProductDto>> getUserRecommendations(
        @PathVariable int userId,
        @RequestParam(defaultValue = "5") int limit
    ) {
        return Result.success(recommendService.getRecommendations(userId, limit));
    }

    学习重点：
    - user_id: 路径参数（@PathVariable）
    - limit: 查询参数（@RequestParam），有默认值
    """
    service = recommend_service.get_recommend_service()
    return await service.get_recommendations(user_id, limit)


# ============== TODO 练习任务 ==============
# 1. 添加 POST /similar 端点，根据商品 ID 查找相似商品
# 2. 添加 GET /trending 端点，返回热门商品（无需用户 ID）
# 3. 为所有端点添加 OpenAPI 文档注解（description, response_description, responses）
# 4. 实现请求耗时统计（用装饰器或中间件）
