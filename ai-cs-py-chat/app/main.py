"""FastAPI 入口:创建应用、挂载 CORS、注册路由。

启动:python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.chat import router as chat_router
from app.config import get_settings


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(
        title="ai-cs-py-chat",
        description="简单 AI 对话服务(OpenAI 兼容协议,默认 DeepSeek)",
        version="0.1.0",
    )

    # 开发环境默认全放开,方便前端本地直连;生产在 .env 配置具体来源
    origins = [o.strip() for o in settings.cors_origins.split(",") if o.strip()]
    if origins == ["*"]:
        app.add_middleware(
            CORSMiddleware,
            allow_origins=["*"],
            allow_methods=["*"],
            allow_headers=["*"],
        )
    else:
        app.add_middleware(
            CORSMiddleware,
            allow_origins=origins,
            allow_credentials=True,
            allow_methods=["*"],
            allow_headers=["*"],
        )

    app.include_router(chat_router)
    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn

    _settings = get_settings()
    uvicorn.run("app.main:app", host=_settings.app_host, port=_settings.app_port, reload=True)
