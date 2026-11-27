// learning-docs/tools/tmp/m15-pool-probe.java
// 用途：复现 ThreadPoolTaskExecutor(core=1, max=4, queue=1000) 的真实扩容行为、
//       AbortPolicy 的拒绝点，以及"线程数超过核数后加速比不再上涨"。
// 跑法：java -Dfile.encoding=UTF-8 learning-docs\tools\tmp\m15-pool-probe.java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class M15PoolProbe {

    static ThreadPoolExecutor pool(String tag, int core, int max, int queue) {
        AtomicInteger seq = new AtomicInteger();
        return new ThreadPoolExecutor(core, max, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queue),
                r -> new Thread(r, tag + "-" + seq.incrementAndGet()));
        // 默认拒绝策略 = AbortPolicy（抛 RejectedExecutionException），与项目配置一致
    }

    static String snap(ThreadPoolExecutor p) {
        return String.format("poolSize=%d active=%d queue=%d", p.getPoolSize(), p.getActiveCount(),
                p.getQueue().size());
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 实验 A：core=1 / max=4 / queue=1000（项目 usageExecutor 同款） ===");
        ThreadPoolExecutor a = pool("usage-report", 1, 4, 1000);
        for (int i = 1; i <= 10; i++) {
            a.execute(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException ignored) { }
            });
            System.out.println("submit #" + i + " -> " + snap(a));
        }
        System.out.println("[结论] 队列没满之前 maxPoolSize 不生效：poolSize 一直是 1");
        a.shutdownNow();

        System.out.println();
        System.out.println("=== 实验 B：core=1 / max=4 / queue=8（把队列压小） ===");
        ThreadPoolExecutor b = pool("small-q", 1, 4, 8);
        int ok = 0, rejected = 0;
        for (int i = 1; i <= 20; i++) {
            try {
                b.execute(() -> {
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) { }
                });
                ok++;
                System.out.println("submit #" + i + " -> " + snap(b));
            } catch (RejectedExecutionException e) {
                rejected++;
                System.out.println("submit #" + i + " -> REJECTED(RejectedExecutionException) " + snap(b));
            }
        }
        System.out.println("[结论] 接受=" + ok + " 拒绝=" + rejected + "：扩容发生在队列满之后，不是之前");
        b.shutdownNow();

        System.out.println();
        System.out.println("=== 实验 C：同样 32 份纯 CPU 任务，1/2/4/8/16/32 个线程分别跑 ===");
        int total = 32;
        runCpuTasks(1, total);                          // 预热一次 JIT
        long t1 = runCpuTasks(1, total);                // 单线程基线
        System.out.printf("threads=%-3d total=%d  cost=%4d ms  speedup=1.00x%n", 1, total, t1);
        for (int th : new int[]{2, 4, 8, 16, 32}) {
            long ms = runCpuTasks(th, total);
            System.out.printf("threads=%-3d total=%d  cost=%4d ms  speedup=%.2fx%n",
                    th, total, ms, t1 / (double) ms);
        }
        System.out.println("[结论] 加速比在核数附近封顶；线程超过核数只是排队，不会更快");
    }

    static long baseline = 1;

    static long runCpuTasks(int threads, int total) throws Exception {
        ThreadPoolExecutor p = new ThreadPoolExecutor(threads, threads, 1, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        CountDownLatch done = new CountDownLatch(total);
        long t0 = System.nanoTime();
        for (int i = 0; i < total; i++) {
            p.execute(() -> {
                long x = 0;
                for (int j = 0; j < 20_000_000; j++) { x += j ^ (x >>> 3); }
                if (x == 42) System.out.print(""); // 防止 JIT 把整个循环优化掉
                done.countDown();
            });
        }
        done.await();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        p.shutdownNow();
        return ms;
    }
}
