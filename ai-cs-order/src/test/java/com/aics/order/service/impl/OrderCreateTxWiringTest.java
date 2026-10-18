package com.aics.order.service.impl;

import com.aics.order.bo.PriceCalcBO;
import com.aics.order.client.PayClient;
import com.aics.order.client.ProductClient;
import com.aics.order.entity.CartItem;
import com.aics.order.entity.Order;
import com.aics.order.lock.OrderCreateLockService;
import com.aics.order.mapper.CartItemMapper;
import com.aics.order.mapper.CouponMapper;
import com.aics.order.mapper.OrderItemMapper;
import com.aics.order.mapper.OrderMapper;
import com.aics.order.service.OrderService;
import com.aics.order.service.PromotionService;
import com.aics.order.statemachine.OrderStateTransitionService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 下单事务边界集成测试（真实的 Spring 事务代理 + H2 数据源）。
 *
 * <p><b>为什么需要这个测试</b>：项目原有 143 个测试类都是纯单测（Mockito 裸对象 / ApplicationContextRunner），
 * 没有任何一个能验证「{@code @Transactional} 到底生效没有」——因为单测里没有 Spring 代理。
 * 本测试用最小 Spring 上下文（{@code @EnableTransactionManagement} + H2）验证下单方法执行时
 * 线程上确实绑定了事务，覆盖"同类自调用导致事务失效"这类只有集成测试才能发现的问题
 * （背景见 docs/28 故障案例 C1）。</p>
 */
@SpringJUnitConfig(OrderCreateTxWiringTest.Config.class)
class OrderCreateTxWiringTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private PromotionService promotionService;

    @Test
    @DisplayName("createOrder 应运行在事务中（验证 @Transactional 通过代理生效）")
    void createOrder_shouldExecuteInsideTransaction() {
        CartItem cartItem = new CartItem();
        cartItem.setProductId(1L);
        cartItem.setQuantity(2);
        cartItem.setProductName("测试商品");
        cartItem.setProductPrice(new BigDecimal("10.00"));
        when(cartItemMapper.selectBatchIds(any())).thenReturn(List.of(cartItem));

        PriceCalcBO price = new PriceCalcBO();
        price.setTotalAmount(new BigDecimal("20.00"));
        price.setFullReductionAmount(BigDecimal.ZERO);
        price.setCouponAmount(BigDecimal.ZERO);
        price.setDiscountAmount(BigDecimal.ZERO);
        price.setPayAmount(new BigDecimal("20.00"));
        when(promotionService.calculatePrice(any(), any(), any())).thenReturn(price);

        // 在 orderMapper.insert 被调用的瞬间检查当前线程是否绑定了事务
        AtomicBoolean transactionActive = new AtomicBoolean(false);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return 1;
        });

        orderService.createOrder(1L, List.of(100L), null, "MOCK");

        assertThat(transactionActive.get())
                .as("doCreateOrder 必须在事务中执行；若为 false，说明 @Transactional 被同类自调用绕过（docs/28 C1）")
                .isTrue();
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {

        /** 解析 @Value("${order.timeout-minutes:30}") 等占位符 */
        @Bean
        static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:order_tx;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        OrderMapper orderMapper() {
            return mock(OrderMapper.class);
        }

        @Bean
        OrderItemMapper orderItemMapper() {
            return mock(OrderItemMapper.class);
        }

        @Bean
        CartItemMapper cartItemMapper() {
            return mock(CartItemMapper.class);
        }

        @Bean
        CouponMapper couponMapper() {
            return mock(CouponMapper.class);
        }

        @Bean
        PromotionService promotionService() {
            return mock(PromotionService.class);
        }

        @Bean
        ProductClient productClient() {
            return mock(ProductClient.class);
        }

        @Bean
        PayClient payClient() {
            return mock(PayClient.class);
        }

        @Bean
        RocketMQTemplate rocketMQTemplate() {
            return mock(RocketMQTemplate.class);
        }

        @Bean
        ApplicationEventPublisher applicationEventPublisher() {
            return mock(ApplicationEventPublisher.class);
        }

        @Bean
        OrderStateTransitionService orderStateTransitionService() {
            return mock(OrderStateTransitionService.class);
        }

        @Bean
        RedissonClient redissonClient() {
            RedissonClient client = mock(RedissonClient.class);
            RLock lock = mock(RLock.class);
            when(client.getLock(anyString())).thenReturn(lock);
            try {
                when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            when(lock.isHeldByCurrentThread()).thenReturn(true);
            return client;
        }

        @Bean
        OrderCreateLockService orderCreateLockService(RedissonClient redissonClient) {
            return new OrderCreateLockService(redissonClient);
        }

        @Bean
        OrderCreateTxService orderCreateTxService(OrderMapper orderMapper,
                                                  OrderItemMapper orderItemMapper,
                                                  CartItemMapper cartItemMapper,
                                                  CouponMapper couponMapper,
                                                  PromotionService promotionService,
                                                  ProductClient productClient,
                                                  RocketMQTemplate rocketMQTemplate) {
            return new OrderCreateTxService(orderMapper, orderItemMapper, cartItemMapper, couponMapper,
                    promotionService, productClient, rocketMQTemplate);
        }

        @Bean
        OrderService orderService(OrderMapper orderMapper,
                                  OrderItemMapper orderItemMapper,
                                  CartItemMapper cartItemMapper,
                                  CouponMapper couponMapper,
                                  ProductClient productClient,
                                  OrderCreateLockService orderCreateLockService,
                                  OrderCreateTxService orderCreateTxService,
                                  PayClient payClient,
                                  ApplicationEventPublisher applicationEventPublisher,
                                  OrderStateTransitionService orderStateTransitionService) {
            return new OrderServiceImpl(orderMapper, orderItemMapper, cartItemMapper, couponMapper,
                    productClient, orderCreateLockService, orderCreateTxService, payClient,
                    applicationEventPublisher, orderStateTransitionService);
        }
    }
}
