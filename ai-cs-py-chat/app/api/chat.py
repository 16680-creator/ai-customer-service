"""对话路由:健康检查、普通对话、SSE 流式对话。"""
import json
import logging

from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from app.schemas.chat import ChatRequest, ChatResponse
from app.services import chat_service

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api")


@router.get("/health")
async def health() -> dict:
    """健康检查。"""
    return {"status": "up"}


@router.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest) -> ChatResponse:
    """普通对话:JSON 进,JSON 出。"""
    return await chat_service.get_chat_service().reply(req)


@router.post("/chat/stream")
async def chat_stream(req: ChatRequest) -> StreamingResponse:
    """流式对话:SSE 输出。

    分帧格式与前端现有解析方式兼容(按空行分帧,取 data: 行):
        data: {"delta": "增量文本"}

  ...  data: [DONE]


    """
    async def event_gen():
        try:
            async for delta in chat_service.get_chat_service().stream(req):
                yield f"data: {json.dumps({'delta': delta}, ensure_ascii=False)}\n\n"
        except Exception as exc:
            # 流中途出错也以 SSE 事件告知前端,保证 [DONE] 收尾
            logger.exception("流式对话出错")
            yield f"data: {json.dumps({'error': str(exc)}, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_gen(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
