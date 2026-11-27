// m16-02-heap-headroom.java —— 把 -Xmx 吃满，量「堆以外」到底占多少
//
// 目的：清单里写 `memory: 512Mi` 而 JVM 写 `-Xmx256m` 时，容器内存上限到底该留多少余量？
//      这个探针把堆填到 85%，然后挂住 6 秒，由 m16-02-heap-headroom.ps1 采样进程 RSS（WorkingSet64），
//      就能看到「堆 256Mi 的 JVM，整进程 RSS 是多少 Mi」。
//
// 注意边界：本机是 Windows + 非容器化 JVM（无 cgroup 内存上限），所以这里量到的是
//          「JVM 堆外开销」的量级，不等于 Linux 容器里同一份 Java 进程的 RSS；
//          Linux 上还有 page cache（容器里算进 memory.usage）等差异。正文已按此口径标注。
//
// 运行（JDK 17 单文件源码启动）：
//   java -Xms256m -Xmx256m learning-docs\tools\tmp\m16-02-heap-headroom.java

import java.util.ArrayList;
import java.util.List;

class HeapHeadroom {
    public static void main(String[] args) throws Exception {
        Runtime rt = Runtime.getRuntime();
        long maxMi = rt.maxMemory() / 1024 / 1024;
        System.out.println("maxHeapMi        = " + maxMi
                + "   (= -Xmx 实际值，JDK17 容器里可能被 cgroup 改写)");
        System.out.println("cores            = " + Runtime.getRuntime().availableProcessors());
        System.out.println("javaVersion      = " + System.getProperty("java.version"));
        System.out.println("containerSupport = cgroup 感知：" + System.getProperty("jdk.internal.platform.metrics", "(none)"));

        List<byte[]> hold = new ArrayList<>();
        long target = (long) (maxMi * 0.85);
        try {
            while ((rt.totalMemory() - rt.freeMemory()) / 1024 / 1024 < target) {
                hold.add(new byte[1024 * 1024]);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("OOM caught while filling heap");
        }
        System.out.println("heldMi           = " + hold.size() + "   (已占用堆的 MiB 数)");
        System.out.println("usedAfterFillMi  = " + (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024);
        System.out.println("committedMi      = " + rt.totalMemory() / 1024 / 1024
                + "   (JVM 向 OS 要了多少；RSS 至少要覆盖这部分)");
        System.out.println("pid              = " + ProcessHandle.current().pid());
        System.out.println("READY");
        System.out.flush();

        Thread.sleep(6000);              // 采样窗口
        System.out.println("done, still holding " + hold.size() + " MiB");
    }
}
