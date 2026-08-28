"""对话接口的请求/响应模型。

JSON 契约对前端/Java 友好:camelCase(sessionId);Python 侧保持 snake_case,
populate_by_name=True 使两种命名都能接收,FastAPI 默认按别名序列化响应。
"""
from pydantic import BaseModel, ConfigDict, Field


class ChatMessage(BaseModel):
    """单条对话消息。"""

    model_config = ConfigDict(populate_by_name=True)

    role: str = Field(..., description="消息角色:user / assistant / system")
    content: str = Field(..., min_length=1, description="消息内容")


class ChatRequest(BaseModel):
    """对话请求:当前消息 + 可选多轮历史。"""

    model_config = ConfigDict(populate_by_name=True)

    message: str = Field(..., min_length=1, description="用户当前输入")
    session_id: str | None = Field(None, alias="sessionId", description="会话 ID,由前端生成")
    history: list[ChatMessage] = Field(default_factory=list, description="历史消息,不含当前 message")


class ChatResponse(BaseModel):
    """对话响应。"""

    model_config = ConfigDict(populate_by_name=True)

    session_id: str | None = Field(None, alias="sessionId")
    reply: str = Field(..., description="AI 回复全文")
    model: str = Field(..., description="实际使用的模型")
