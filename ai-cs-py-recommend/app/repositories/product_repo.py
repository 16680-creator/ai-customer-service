"""
商品数据访问（对标 @Repository + MyBatis Mapper）

学习章节：22-Python速成/19-HTTP与数据库实战.md §4
"""
import logging
from typing import List

from app.schemas.recommend import ProductDto

logger = logging.getLogger(__name__)


class ProductRepository:
    """
    商品数据访问（对标 MyBatis Mapper 或 JPA Repository）

    Java 等价:
    @Repository
    public interface ProductRepository extends JpaRepository<Product, Integer> {
        @Query("SELECT * FROM products WHERE user_id = :userId ORDER BY created_at DESC")
        List<Product> findPurchaseHistory(int userId);
    }
    """

    def __init__(self):
        self._db = None

    def _get_db(self):
        """获取数据库连接（SQLite）"""
        if self._db is None:
            import sqlite3
            # 使用内存数据库（便于测试），生产环境改为 :file:db.sqlite3
            self._db = sqlite3.connect(":memory:", check_same_thread=False)
            self._init_schema()
        return self._db

    def _init_schema(self):
        """初始化数据库表结构"""
        conn = self._get_db()
        cursor = conn.cursor()

        # 建表（对标 schema-test.sql）
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS orders (
                id INTEGER PRIMARY KEY,
                user_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                price REAL NOT NULL,
                category_id INTEGER
            )
        """)

        # 插入测试数据
        test_data = [
            (1, 101, 1),  # user_id=1, product_id=101
            (2, 1, 102),
            (3, 1, 103),
            (4, 1, 104),
            (5, 1, 105),
        ]

        cursor.executemany(
            "INSERT OR IGNORE INTO orders (id, user_id, product_id) VALUES (?, ?, ?)",
            test_data,
        )

        product_data = [
            (101, "热销商品 A", 99.0, 1),
            (102, "热销商品 B", 149.0, 1),
            (103, "热销商品 C", 79.0, 1),
            (201, "推荐商品 A", 89.0, 1),
            (202, "推荐商品 B", 129.0, 1),
            (203, "推荐商品 C", 69.0, 1),
        ]

        cursor.executemany(
            "INSERT OR IGNORE INTO products (id, name, price, category_id) VALUES (?, ?, ?, ?)",
            product_data,
        )

        conn.commit()

    async def get_purchase_history(self, user_id: int, limit: int = 20) -> List[int]:
        """
        获取用户购买历史（商品 ID 列表）

        对标 Spring Data JPA:
        @Query("SELECT DISTINCT product_id FROM orders WHERE user_id = :userId ORDER BY created_at DESC")
        List<Integer> getPurchaseHistory(int userId);

        学习章节：19-HTTP与数据库实战.md §4 参数化查询
        """
        logger.debug("查询用户购买历史", extra={"user_id": user_id})

        conn = self._get_db()
        cursor = conn.cursor()

        # 参数化查询（防止 SQL 注入，对标 PreparedStatement）
        cursor.execute(
            "SELECT DISTINCT product_id FROM orders WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
            (user_id, limit),
        )

        result = [row[0] for row in cursor.fetchall()]
        logger.debug(
            "查询成功",
            extra={"user_id": user_id, "count": len(result)},
        )

        return result

    async def get_products_by_category(
        self, product_id: int, limit: int = 10
    ) -> List[ProductDto]:
        """
        根据商品 ID 查找同类别商品

        对标:
        @Query("SELECT * FROM products WHERE category_id = (SELECT category_id FROM products WHERE id = ?) LIMIT ?")
        List<Product> findSimilarProducts(int productId, int limit);

        学习章节：19-HTTP与数据库实战.md §4
        """
        logger.debug("查询相似商品", extra={"product_id": product_id})

        conn = self._get_db()
        cursor = conn.cursor()

        # 先查询该商品的类别
        cursor.execute(
            "SELECT category_id FROM products WHERE id = ?",
            (product_id,),
        )
        row = cursor.fetchone()

        if not row:
            logger.warning("商品不存在", extra={"product_id": product_id})
            return []

        category_id = row[0]

        # 再查询同类别的其他商品
        cursor.execute(
            "SELECT id, name, price FROM products WHERE category_id = ? AND id != ? LIMIT ?",
            (category_id, product_id, limit),
        )

        products = []
        for product_id_db, name, price in cursor.fetchall():
            products.append(
                ProductDto(
                    product_id=product_id_db,
                    product_name=name,
                    price=price,
                    similarity_score=0.85,  # 简化：固定相似度
                )
            )

        logger.debug(
            "查询完成",
            extra={"product_id": product_id, "count": len(products)},
        )

        return products


# ============== TODO 练习任务 ==============
# 1. 连接真实数据库（SQLite / MySQL）
# 2. 实现参数化查询（防止 SQL 注入）
# 3. 添加事务支持（如果需要多步操作）
# 4. 添加分页逻辑
# 5. 添加数据库连接池（SQLAlchemy）
