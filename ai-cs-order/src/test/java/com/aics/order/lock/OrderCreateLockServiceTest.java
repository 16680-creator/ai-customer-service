package com.aics.order.lock;

import com.aics.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 下单防重分布式锁单元测试（纯 Mockito，不连 Redis）
 * TDD: 锁 key 规约、竞争失败快速拒绝、正常执行后释放、锁内异常仍释放
 */
@ExtendWith(MockitoExtension.class)
class OrderCreateLockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private OrderCreateLockService lockService;

    private void givenLock() {
        when(redissonClient.getLock(OrderCreateLockService.KEY_PREFIX + 100L)).thenReturn(lock);
    }

    @Test
    @DisplayName("锁 key 按 userId 规约生成，执行成功后释放")
    void executeAndRelease() throws Exception {
        givenLock();
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        AtomicBoolean executed = new AtomicBoolean(false);
        String result = lockService.withCreateLock(100L, () -> {
            executed.set(true);
            return "order-1";
        });

        assertEquals("order-1", result);
        assertTrue(executed.get());
        verify(lock).unlock();
    }

    @Test
    @DisplayName("锁竞争失败时快速拒绝且不执行业务")
    void lockContentionRejected() throws Exception {
        givenLock();
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(false);

        AtomicBoolean executed = new AtomicBoolean(false);
        assertThrows(BusinessException.class,
                () -> lockService.withCreateLock(100L, () -> {
                    executed.set(true);
                    return "should-not-run";
                }));

        assertFalse(executed.get());
        verify(lock, never()).unlock();
    }

    @Test
    @DisplayName("业务异常时也必须释放锁（finally 兜底）")
    void releaseOnBusinessException() throws Exception {
        givenLock();
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> lockService.withCreateLock(100L, () -> {
                    throw new IllegalStateException("boom");
                }));

        verify(lock).unlock();
    }

    @Test
    @DisplayName("waitTime 为 0：tryLock 不等待立即返回")
    void zeroWaitTime() throws Exception {
        givenLock();
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        lockService.withCreateLock(100L, () -> "ok");

        // 不指定 leaseTime（触发看门狗自动续期），等待时长必须为 0
        verify(lock).tryLock(0, TimeUnit.SECONDS);
    }
}
