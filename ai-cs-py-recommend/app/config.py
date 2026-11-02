"""
配置管理（对标 @ConfigurationProperties）

学习章节：22-Python速成/11-FastAPI实战-对着SpringBoot学.md §12
"""
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    应用配置（对标 Spring Boot 的 application.yml + @ConfigurationProperties）

    Java 等价写法:
    @ConfigurationProperties(prefix="app")
    @Validated
    public record AppProperties(
        String appHost, int appPort, String corsOrigins, ...
    ) {}
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",  # 忽略未定义的环境变量
    )

    # ============== 服务配置 ==============
    app_host: str = "0.0.0.0"
    app_port: int = 8082
    cors_origins: str = "*"

    # ============== Nacos 配置 ==============
    nacos_hosts: str = "localhost:8848"
    nacos_namespace: str = "ai-customer-service"
    service_name: str = "ai-cs-py-recommend"

    # ============== Redis 配置 ==============
    redis_url: str = "redis://localhost:6379/0"
    redis_password: str = ""
    cache_ttl: int = 3600  # 默认缓存 1 小时

    # ============== 下游服务 ==============
    product_service_url: str = "http://localhost:8085"
    order_service_url: str = "http://localhost:8086"

    # ============== 推荐算法参数 ==============
    recommend_limit: int = 5  # 默认推荐数量
    similar_products_limit: int = 10  # 相似商品候选数
    user_history_limit: int = 20  # 用户历史购买记录数

    # ============== 可观测性 ==============
    log_level: str = "INFO"
    jaeger_host: str = "localhost"
    jaeger_port: int = 6831


@lru_cache
def get_settings() -> Settings:
    """
    单例配置（对标 Spring Boot 的单例 Bean）

    lru_cache(maxsize=1) 确保只初始化一次

    Java 等价:
    @Bean
    public AppProperties appProperties() {
        return new AppProperties(...);
    }
    """
    return Settings()
