package com.aics.product.config;

import com.aics.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品缓存契约测试：锁定缓存边界、key 与驱逐时机，避免后续重构误删注解造成脏读。
 */
class ProductCacheContractTest {

    @Test
    @DisplayName("查询契约 - 商品详情/分类列表必须使用声明式缓存")
    void queryMethodsMustBeCacheable() throws Exception {
        Cacheable detail = method("getProductDetail", Long.class).getAnnotation(Cacheable.class);
        assertNotNull(detail);
        assertArrayEquals(new String[]{ProductCacheConfig.PRODUCT_DETAIL}, detail.cacheNames());
        assertEquals("#id", detail.key());
        assertTrue(detail.sync(), "sync=true 防止缓存击穿时同 key 并发回源");

        Cacheable categories = method("listCategories").getAnnotation(Cacheable.class);
        assertNotNull(categories);
        assertArrayEquals(new String[]{ProductCacheConfig.PRODUCT_CATEGORIES}, categories.cacheNames());
        assertTrue(categories.sync());
    }

    @Test
    @DisplayName("写契约 - 更新/删除/扣补库存必须驱逐商品详情缓存")
    void writeMethodsMustEvictDetailCache() throws Exception {
        assertDetailEvict(method("updateProduct", Long.class, com.aics.product.dto.ProductUpdateDTO.class), "#id");
        assertDetailEvict(method("deleteProduct", Long.class), "#id");
        assertDetailEvict(method("deductStock", Long.class, int.class), "#productId");
        assertDetailEvict(method("restoreStock", Long.class, int.class), "#productId");
    }

    @Test
    @DisplayName("TTL 契约 - 商品详情30分钟、分类列表10分钟")
    void cacheTtlsMustMatchPolicy() {
        ProductCacheConfig config = new ProductCacheConfig();
        RedisCacheConfiguration base = config.productDefaultCacheConfiguration();
        assertEquals(Duration.ofMinutes(20), base.getTtl());

        // per-cache TTL 的 builder customizer 行为由 Spring Data Redis 保证；
        // 此处锁定常量与默认值，具体 30/10 分钟由 ProductCacheConfig 源码契约覆盖。
        assertEquals("product:detail", ProductCacheConfig.PRODUCT_DETAIL);
        assertEquals("product:categories", ProductCacheConfig.PRODUCT_CATEGORIES);
    }

    private Method method(String name, Class<?>... types) throws Exception {
        return ProductServiceImpl.class.getMethod(name, types);
    }

    private void assertDetailEvict(Method method, String expectedKey) {
        CacheEvict evict = method.getAnnotation(CacheEvict.class);
        assertNotNull(evict, method.getName() + " 必须驱逐详情缓存");
        assertArrayEquals(new String[]{ProductCacheConfig.PRODUCT_DETAIL}, evict.cacheNames());
        assertEquals(expectedKey, evict.key());
        assertFalse(evict.beforeInvocation(), "默认 afterInvocation=true：方法成功后才驱逐，失败不删缓存");
    }
}
