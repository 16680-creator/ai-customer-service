"""
FastAPI 启动类（对标 @SpringBootApplication）

学习章节：22-Python速成/11-FastAPI实战-对着SpringBoot学.md §3
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from prometheus_client import start_http_server

from app.api import recommend
from app.config import get_settings
from app.core.observability import setup_logging, init_tracing

# ============== 初始化可观测性 ==============
# 学习章节：25-可观测性实战-日志、监控、链路追踪.md
setup_logging()
init_tracing()

# Prometheus 指标端口（8001）
start_http_server(8001)


def create_app() -> FastAPI:
    """
    应用工厂函数（便于测试时创建独立实例）

    对标 Spring Boot:
    @SpringBootApplication
    public class RecommendApplication {
        public static void main(String[] args) {
            SpringApplication.run(RecommendApplication.class, args);
        }
    }
    """
    settings = get_settings()

    app = FastAPI(
        title="ai-cs-py-recommend",
        description="Python 推荐服务 - 对标 ai-cs-order",
        version="1.0.0",
    )

    # ============== CORS 配置 ==============
    # 对标 Spring Boot: @Configuration + WebMvcConfigurer
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

    # ============== 注册路由 ==============
    # 对标 Spring Boot: @RestController 自动扫描
    app.include_router(recommend.router)

    # ============== 健康检查 ==============
    @app.get("/actuator/health")
    async def health():
        """对标 Spring Boot Actuator"""
        return {
            "status": "UP",
            "components": {
                "diskSpace": {"status": "UP"},
                "ping": {"status": "UP"},
            }
        }

    # ============== 启动事件 ==============
    @app.on_event("startup")
    async def on_startup():
        """
        启动时注册 Nacos（可选）
        学习章节：26-微服务治理（Nacos集成）.md
        """
        # TODO: Nacos 服务注册（见 26 篇）
        pass

    @app.on_event("shutdown")
    async def on_shutdown():
        """优雅停机"""
        # TODO: Nacos 注销
        pass

    return app


# ============== 模块入口 ==============
app = create_app()


if __name__ == "__main__":
    import uvicorn

    settings = get_settings()
    uvicorn.run(
        "app.main:app",
        host=settings.app_host,
        port=settings.app_port,
        reload=True,  # 生产环境设为 False
    )
