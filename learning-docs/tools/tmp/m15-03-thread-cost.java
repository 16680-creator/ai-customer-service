// m15-03-thread-cost.java
// 目的：量化「一个连接一个线程」这条路的成本上限 —— 平台线程（platform thread）不是免费的，
//       它的代价是「创建/销毁的开销 + 每线程独立栈 + 调度器里的一个可运行实体」。
//
// 【性质说明】纯线程创建/同步开销测量，不发网络请求、不读写文件。
//   测的是「JVM 在一台本机上限内能多快地把 n 个线程拉起来并让它们同时挂着等」，
//   不代表任何业务的 QPS/延迟，也不能反推 Tomcat 的真实并发上限（那条上限还取决于堆、
//   连接数、fd、后端依赖）。
//
// 运行：java .\m15-03-thread-cost.java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class m15_03_thread_cost {
    public static void main(String[] args) throws Exception {
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        Runtime rt = Runtime.getRuntime();
        System.out.printf("可用处理器 = %d 个；JVM 堆上限 = %d MB；探测前线程数 = %d%n",
                rt.availableProcessors(), rt.maxMemory() / 1024 / 1024, mx.getThreadCount());
        System.out.println();

        for (int n : new int[] { 200, 1000, 2000 }) {
            CountDownLatch gate = new CountDownLatch(1);   // 所有线程卡在同一个闸门上 = 同时"占着连接不干活"
            CountDownLatch done = new CountDownLatch(n);
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                Thread t = new Thread(() -> {
                    try { gate.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    done.countDown();
                }, "io-worker-" + i);
                t.setDaemon(true);
                t.start();
            }
            long t1 = System.nanoTime();
            int peak = mx.getPeakThreadCount();
            gate.countDown();
            boolean allDone = done.await(30, TimeUnit.SECONDS);
            System.out.printf("n=%4d  拉起耗时=%7.1f ms  峰值线程数=%4d  全部通过闸门=%s%n",
                    n, (t1 - t0) / 1e6, peak, allDone ? "是" : "否");
            Thread.sleep(300);                            // 给线程退出留出时间，避免下一轮峰值叠加
        }

        System.out.println();
        System.out.println("读法：");
        System.out.println("  1) 耗时随 n 近似线性增长 —— 每多一条连接就多一次内核线程创建，这是无法摊销的固定成本；");
        System.out.println("  2) 峰值线程数就是「同时占着连接不干活的线程」数量：它们既不消耗 CPU，也照样占着栈和调度实体；");
        System.out.println("  3) 所以『一个连接一个线程』的上限不是 CPU，而是线程数 × 栈大小 + 连接数 × fd，两者任一先满就崩；");
        System.out.println("  4) 本机 JVM 未显式设置 -Xss（java -XX:+PrintFlagsFinal -version 中 ThreadStackSize=0 {default}），");
        System.out.println("     每线程栈的具体保留量本文不实测，故只用『线程数』这一个可测量指标做结论。");
    }
}
