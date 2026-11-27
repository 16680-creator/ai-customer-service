// learning-docs/tools/tmp/m15-jvm-runtime.java
// 用途：在 JDK 17 单文件模式下打印"JVM 眼里的 CPU 数"与派生线程数。
// 跑法：java learning-docs\tools\tmp\m15-jvm-runtime.java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ForkJoinPool;

class M15JvmRuntime {
    public static void main(String[] args) throws Exception {
        Runtime rt = Runtime.getRuntime();
        ForkJoinPool common = ForkJoinPool.commonPool();

        System.out.println("java.version              = " + System.getProperty("java.version"));
        System.out.println("java.vm.info              = " + System.getProperty("java.vm.info"));
        System.out.println("Runtime.availableProcessors() = " + rt.availableProcessors());
        System.out.println("ForkJoinPool.commonPool().getParallelism() = " + common.getParallelism());
        System.out.println("ForkJoinPool.commonPool().getPoolSize()    = " + common.getPoolSize());
        System.out.println("ForkJoinPool.getCommonPoolParallelism()    = " + ForkJoinPool.getCommonPoolParallelism());
        System.out.println("Runtime.maxMemory() MB    = " + rt.maxMemory() / 1024 / 1024);
        System.out.println("host name                 = " + java.net.InetAddress.getLocalHost().getHostName());

        // JDK 17 的线程计数：只统计 Java 线程（平台线程 = OS 线程，1:1）
        ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
        System.out.println("ThreadMXBean.getThreadCount()      = " + tmx.getThreadCount());
        System.out.println("ThreadMXBean.getPeakThreadCount()  = " + tmx.getPeakThreadCount());

        // 1:1 映射的现场证据：每个 Java 线程都有自己的 threadId，且 JVM 会为它创建 OS 线程
        Thread t = new Thread(() -> {
            try { Thread.sleep(200); } catch (InterruptedException ignored) { }
        }, "probe-1to1");
        t.start();
        System.out.println("probe thread: name=" + t.getName() + " threadId=" + t.getId()
                + " state=" + t.getState());
        t.join();

        // 虚拟线程探针：JDK 17 没有 Thread.ofVirtual()，这一行只能编译失败，故注释掉。
        // Thread.ofVirtual().name("vt").start(() -> { });
        System.out.println("Thread.ofVirtual exists in JDK 17? -> 否（编译期不可用，见 §7 实测）");
    }
}
