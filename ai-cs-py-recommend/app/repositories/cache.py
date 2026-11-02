"""
Redis 缓存（对标 Spring Cache / @Cacheable）

学习章节：22-Python速成/19-HTTP与数据库实战.md
"""
import json
import logging
from typing import Any

logger = logging.getLogger(__name__)


class Cache:
    """
    Redis 缓存封装（对标 Spring Cache）

    Java 等价:
    @Service
    public class CacheService {
        @Autowired
        private StringRedisTemplate redis;

        @Cacheable(value = "user:history", key = "#userId")
        public List<Integer> getUserHistory(int userId) {
            // 查询逻辑
        }
    }

    注意：生产环境需接入真实 Redis，这里实现一个内存缓存用于演示
    """

    def __init__(self):
        # 内存缓存（模拟 Redis，生产改为真实 Redis 客户端）
        self._store: dict[str, tuple[Any, int | None]] = {}

    async def get(self, key: str) -> Any | None:
        """
        从缓存获取数据

        对标:
        @Cacheable(value = "cache-name", key = "#key")

        学习重点：对照 Spring Cache 的标准用法
        """
        try:
            if key in self._store:
                value, _ = self._store[key]
                logger.debug("缓存命中", extra={"key": key})
                return value
            else:
                logger.debug("缓存未命中", extra={"key": key})
                return None
        except Exception as e:
            logger.error("缓存读取失败", extra={"key": key, "error": str(e)})
            return None

    async def set(self, key: str, value: Any, ttl: int | None = None) -> bool:
        """
        写入缓存

        对标:
        @CachePut(value = "cache-name", key = "#key")

        参数：
        - key: 缓存键
        - value: 缓存值（可序列化对象）
        - ttl: 过期时间（秒）
        """
        try:
            self._store[key] = (value, ttl)
            logger.debug("缓存写入成功", extra={"key": key, "ttl": ttl})
            return True
        except Exception as e:
            logger.error("缓存写入失败", extra={"key": key, "error": str(e)})
            return False

    async def delete(self, key: str) -> bool:
        """
        删除缓存

        对标:
        @CacheEvict(value = "cache-name", key = "#key")
        """
        try:
            if key in self._store:
                del self._store[key]
            logger.debug("缓存删除成功", extra={"key": key})
            return True
        except Exception as e:
            logger.error("缓存删除失败", extra={"key": key, "error": str(e)})
            return False

    async def clear(self) -> bool:
        """清空所有缓存（便于测试）"""
        try:
            self._store.clear()
            logger.debug("缓存已清空")
            return True
        except Exception as e:
            logger.error("缓存清空失败", extra={"error": str(e)})
            return False
