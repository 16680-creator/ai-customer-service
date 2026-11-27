// learning-docs/tools/tmp/m15-switch-cost.java
// 用途：量一次"线程交接"的端到端开销（Windows 上 LockSupport.park/unpark 乒乓）。
// 这是用户态 + 内核路径的合计值，不等于纯内核 context switch 时间，只能当量级参考。
// 跑法：java "-Dfile.encoding=UTF-8" learning-docs\tools\tmp\m15-switch-cost.java
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

class M15SwitchCost {

    static volatile Thread A, B;
    static final int ROUNDS = 200_000;

    public static void main(String[] args) throws Exception {
        for (int warm = 0; warm < 2; warm++) pingpong(ROUNDS / 10); // 预热 JIT

        long ns = pingpong(ROUNDS);
        System.out.printf("pingpong: %d 次交接, 总 %d ms, 单次 %.0f ns (%.2f us)%n",
                ROUNDS * 2, ns / 1_000_000, ns / (double) (ROUNDS * 2), ns / 1000.0 / (ROUNDS * 2));

        // 对照：同一台机器上"不交接"的忙等循环跑同样轮次
        long t0 = System.nanoTime();
        long spin = 0;
        for (int i = 0; i < ROUNDS * 2; i++) spin += i;
        long spinNs = System.nanoTime() - t0;
        System.out.printf("busy-loop 对照: %d 次循环 %d ns, 平均 %.1f ns/次 (sink=%d)%n",
                ROUNDS * 2, spinNs, spinNs / (double) (ROUNDS * 2), spin % 10);
        System.out.println("[判读] 两值之比 ≈ 一次交接相当于多少次纯循环；比值越大说明交接越贵，但绝对值随机器负载漂移");
    }

    static long pingpong(int rounds) throws Exception {
        AtomicLong done = new AtomicLong();
        A = new Thread(() -> {
            for (int i = 0; i < rounds; i++) { LockSupport.park(); LockSupport.unpark(B); }
            done.incrementAndGet();
        }, "ping");
        B = new Thread(() -> {
            for (int i = 0; i < rounds; i++) { LockSupport.unpark(A); LockSupport.park(); }
            done.incrementAndGet();
        }, "pong");
        A.start(); B.start();
        long t0 = System.nanoTime();
        LockSupport.unpark(A);
        while (done.get() < 2) LockSupport.parkNanos(100_000);
        long ns = System.nanoTime() - t0;
        A.join(); B.join();
        return ns;
    }
}
