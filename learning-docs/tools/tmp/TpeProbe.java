import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用真实 JDK ThreadPoolExecutor 验证三件事：
 * 1) 提交顺序是 核心线程 -> 队列 -> 扩容到 max -> 拒绝（不是"核心满了就扩容"）
 * 2) 无界队列下 maximumPoolSize 形同虚设（线程数永远等于 core）
 * 3) CallerRunsPolicy 会让"提交者线程"亲自干活，从而把过载反压给调用方
 *
 * 运行: java learning-docs/tools/tmp/TpeProbe.java
 */
public class TpeProbe {

    static void probe(String title, ThreadPoolExecutor pool, int tasks) throws Exception {
        System.out.println("== " + title + " ==");
        for (int i = 1; i <= tasks; i++) {
            final int id = i;
            try {
                pool.execute(() -> {
                    System.out.printf("  task-%d 运行于 %s%n", id, Thread.currentThread().getName());
                    sleep(120);
                });
                System.out.printf("  提交 task-%d 后: poolSize=%d active=%d queue=%d%n",
                        id, pool.getPoolSize(), pool.getActiveCount(), pool.getQueue().size());
            } catch (Exception e) {
                System.out.printf("  提交 task-%d 被拒: %s%n", id, e.getClass().getSimpleName());
            }
        }
        sleep(400);
        System.out.printf("  最终: poolSize=%d largestPoolSize=%d 剩余队列=%d 完成=%d%n%n",
                pool.getPoolSize(), pool.getLargestPoolSize(), pool.getQueue().size(), pool.getCompletedTaskCount());
        pool.shutdownNow();
        pool.awaitTermination(2, TimeUnit.SECONDS);
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws Exception {
        AtomicInteger seq = new AtomicInteger();
        System.out.println("main 线程名 = " + Thread.currentThread().getName()
                + "，可用核心数 = " + Runtime.getRuntime().availableProcessors() + "\n");

        // A: core=1 max=2 队列=1 -> 第 4 个任务必须被拒绝
        probe("A. core=1 max=2 queue=1 AbortPolicy",
                new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1),
                        r -> new Thread(r, "worker-" + seq.incrementAndGet())),
                4);

        // B: 无界队列 -> max=8 但线程数只会到 1
        seq.set(0);
        probe("B. core=1 max=8 queue=无界 AbortPolicy",
                new ThreadPoolExecutor(1, 8, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                        r -> new Thread(r, "worker-" + seq.incrementAndGet())),
                6);

        // C: 同样的池换成 CallerRunsPolicy -> 提交者(main)亲自执行，队列不再涨
        seq.set(0);
        RejectedExecutionHandler callerRuns = new ThreadPoolExecutor.CallerRunsPolicy();
        probe("C. core=1 max=2 queue=1 CallerRunsPolicy",
                new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1),
                        r -> new Thread(r, "worker-" + seq.incrementAndGet()), callerRuns),
                4);
    }
}
