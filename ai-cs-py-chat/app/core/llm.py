"""LLM 客户端封装:OpenAI 兼容协议,默认 DeepSeek,可通过 .env 切换供应商。"""
from collections.abc import AsyncIterator, Sequence
from functools import lru_cache

from openai import AsyncOpenAI

from app.config import Settings, get_settings
from app.schemas.chat import ChatMessage


class LLMClient:
    """异步聊天客户端:普通对话返回全文,流式对话逐段产出增量。"""

    def __init__(self, settings: Settings | None = None):
        self.settings = settings or get_settings()
        # api-key 为空时用占位符启动,保证服务能起(真调用时会 401,由路由层兜底)
        self._client = AsyncOpenAI(
            base_url=self.settings.llm_base_url,
            api_key=self.settings.llm_api_key or "not-configured",
            timeout=self.settings.llm_timeout,
        )

    def _build_messages(self, history: Sequence[ChatMessage], message: str) -> list[dict]:
        """组装消息列表:系统提示词 + 多轮历史 + 当前输入。"""
        msgs: list[dict] = [{"role": "system", "content": self.settings.system_prompt}]
        msgs += [{"role": m.role, "content": m.content} for m in history]
        msgs.append({"role": "user", "content": message})
        return msgs

    async def chat(self, history: Sequence[ChatMessage], message: str) -> str:
        """非流式对话:一次性返回完整回复文本。"""
        resp = await self._client.chat.completions.create(
            model=self.settings.llm_model,
            messages=self._build_messages(history, message),
            temperature=self.settings.llm_temperature,
            max_tokens=self.settings.llm_max_tokens,
        )
        return resp.choices[0].message.content or ""

    async def chat_stream(self, history: Sequence[ChatMessage], message: str) -> AsyncIterator[str]:
        """流式对话:逐段 yield 文本增量。"""
        stream = await self._client.chat.completions.create(
            model=self.settings.llm_model,
            messages=self._build_messages(history, message),
            temperature=self.settings.llm_temperature,
            max_tokens=self.settings.llm_max_tokens,
            stream=True,
        )
        async for chunk in stream:
            if chunk.choices and chunk.choices[0].delta.content:
                yield chunk.choices[0].delta.content


@lru_cache
def get_llm_client() -> LLMClient:
    """进程内单例客户端。"""
    return LLMClient()
