package com.aics.order.lock;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 下单防重分布式锁（Redisson）。
 *
 * <p>学习要点：分库分表后用户名查重/下单去重都是「跨分片广播 + 落库」两步，
 * 无法用一条唯一索引兜住并发窗口，必须引入分布式锁。相比手写 SETNX：
 * <ul>
 *   <li>看门狗：不指定 leaseTime 时默认 30s 持有并每 10s 自动续期，业务超时不会中途失锁；</li>
 *   <li>可重入：同线程重复加锁安全；</li>
 *   <li>解锁安全：{@code isHeldByCurrentThread} 判断避免解掉别人的锁。</li>
 * </ul>
 * waitTime 取 0：同一用户的并发下单请求直接快速失败，而不是排队等锁。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateLockService {

    /** 锁 key 前缀，完整 key：lock:order:create:{userId} */
    public static final String KEY_PREFIX = "lock:order:create:";

    private final RedissonClient redissonClient;

    /**
     * 以用户维度互斥执行下单动作。
     *
     * @param userId 用户 ID（锁粒度）
     * @param action 加锁成功后执行的业务动作
     * @return 业务动作返回值
     * @throws BusinessException 同用户已有下单请求在执行时抛「请求过于频繁」
     */
    public <T> T withCreateLock(Long userId, Supplier<T> action) {
        RLock lock = redissonClient.getLock(KEY_PREFIX + userId);
        boolean acquired = false;
        try {
            // waitTime=0：快速失败；未指定 leaseTime → 触发看门狗自动续期
            acquired = lock.tryLock(0, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("下单防重锁竞争失败: userId={}", userId);
                throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "您有订单正在创建中，请勿重复提交");
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "下单操作被中断");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
