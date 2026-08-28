"""对话业务编排:组装上下文、调用 LLM、产出回复。"""
from collections.abc import AsyncIterator

from app.core.llm import LLMClient, get_llm_client
from app.schemas.chat import ChatRequest, ChatResponse


class ChatService:
    """对话服务:非流式整段回复,流式逐段增量。"""

    def __init__(self, llm: LLMClient | None = None):
        self.llm = llm or get_llm_client()

    async def reply(self, req: ChatRequest) -> ChatResponse:
        """非流式:返回完整回复 + 实际使用的模型。"""
        text = await self.llm.chat(req.history, req.message)
        return ChatResponse(session_id=req.session_id, reply=text, model=self.llm.settings.llm_model)

    async def stream(self, req: ChatRequest) -> AsyncIterator[str]:
        """流式:逐段产出文本增量(分帧由路由层负责)。"""
        async for delta in self.llm.chat_stream(req.history, req.message):
            yield delta


_service: ChatService | None = None


def get_chat_service() -> ChatService:
    """进程内单例,供路由层获取、测试替换。"""
    global _service
    if _service is None:
        _service = ChatService()
    return _service
