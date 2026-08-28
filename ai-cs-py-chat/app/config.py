"""应用配置:从 .env / 环境变量读取,pydantic-settings 负责加载与校验。"""
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """全部配置项均可通过环境变量或 .env 覆盖,字段说明见 .env.example。"""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # --- LLM(OpenAI 兼容协议,默认 DeepSeek)---
    llm_base_url: str = "https://api.deepseek.com"
    llm_api_key: str = ""
    llm_model: str = "deepseek-chat"
    llm_temperature: float = 0.7
    llm_max_tokens: int = 2048
    llm_timeout: float = 60.0

    # --- 对话 ---
    system_prompt: str = "你是一个乐于助人的 AI 助手,请用简洁的中文回答。"

    # --- 应用 ---
    app_host: str = "0.0.0.0"
    app_port: int = 8000
    cors_origins: str = "*"  # 逗号分隔的来源列表,* 表示全部放开(仅建议开发环境)


@lru_cache
def get_settings() -> Settings:
    """进程内单例配置。"""
    return Settings()
