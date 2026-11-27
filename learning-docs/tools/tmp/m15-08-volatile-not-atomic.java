// m15-08-volatile-not-atomic.java —— volatile 不保证原子性的实验（JDK 17 直接 java 运行）
// 三个计数器都用同样的 4 线程 × 每线程 2,000,000 次自增，唯一区别是同步手段。
// 运行：java m15-08-volatile-not-atomic.java
import java.util.concurrent.atomic.AtomicInteger;

class M15VolatileNotAtomic {

    static volatile int volatileCount = 0;   // volatile：可见性有，原子性没有
    static int plainCount = 0;               // 普通字段：既无可见性也无原子性
    static final AtomicInteger atomicCount = new AtomicInteger(0); // CAS：原子
    static final Object LOCK = new Object();
    static int syncCount = 0;                // 互斥：原子

    static final int THREADS = 4;
    static final int ITER = 2_000_000;

    public static void main(String[] args) throws Exception {
        long t0 = System.nanoTime();
        runAll(() -> volatileCount++);
        long vMs = (System.nanoTime() - t0) / 1_000_000L;
        t0 = System.nanoTime();
        runAll(() -> plainCount++);
        long pMs = (System.nanoTime() - t0) / 1_000_000L;
        t0 = System.nanoTime();
        runAll(() -> atomicCount.incrementAndGet());
        long aMs = (System.nanoTime() - t0) / 1_000_000L;
        t0 = System.nanoTime();
        runAll(() -> { synchronized (LOCK) { syncCount++; } });
        long sMs = (System.nanoTime() - t0) / 1_000_000L;

        long expect = (long) THREADS * ITER;
        line("expected", expect, expect, 0);
        line("plain", plainCount, expect, pMs);
        line("volatile", volatileCount, expect, vMs);
        line("AtomicInteger", atomicCount.get(), expect, aMs);
        line("synchronized", syncCount, expect, sMs);
    }

    static void line(String name, long actual, long expect, long ms) {
        long lost = expect - actual;
        System.out.printf("%-14s actual=%-10d lost=%-10d elapsedMs=%-6d %s%n",
                name + ":", actual, lost, ms, lost == 0 ? "OK" : "LOST-UPDATES");
    }

    static void runAll(Runnable body) throws Exception {
        Thread[] ts = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            ts[i] = new Thread(() -> { for (int k = 0; k < ITER; k++) body.run(); });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
    }
}
