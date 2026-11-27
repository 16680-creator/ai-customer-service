// learning-docs/tools/tmp/m15-02-heap-stages.java
// 目的：把 JVM 进程的内存按「堆 / 堆外 direct / 线程栈」分三段造出来，每段停 2.5 秒，
// 方便外部用 Get-Process 采 WorkingSet(RSS) 与 VirtualMemorySize(VSZ)。
// 输出刻意用 ASCII：本机 JDK 17 默认按平台编码（GBK）写 stdout，中文会变乱码，见正文 §6。
// 运行：java -Xmx512m m15-02-heap-stages.java
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class m15_02_heap_stages {

    static final long MB = 1024 * 1024;
    static final List<byte[]> heap = new ArrayList<>();
    static final List<ByteBuffer> directs = new ArrayList<>();
    static final List<Thread> threads = new ArrayList<>();

    static void report(String stage) {
        var mem = ManagementFactory.getMemoryMXBean();
        MemoryUsage h = mem.getHeapMemoryUsage();
        MemoryUsage nh = mem.getNonHeapMemoryUsage();
        long virt = -1;
        var os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof com.sun.management.OperatingSystemMXBean sub) {
            virt = sub.getCommittedVirtualMemorySize();
        }
        System.out.printf(
            "%-14s maxHeap=%3dMB heapUsed=%3dMB heapCommitted=%3dMB nonHeap=%3dMB committedVirt=%4dMB threads=%d%n",
            stage, Runtime.getRuntime().maxMemory() / MB, h.getUsed() / MB, h.getCommitted() / MB,
            nh.getCommitted() / MB, virt / MB, Thread.activeCount());
        System.out.flush();
    }

    static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        report("0.baseline");
        pause(2500);

        // stage 1: 200MB of live heap arrays, every 4KB touched.
        // NOTE: with -Xmx512m you cannot keep ~512MB alive (G1 needs copy space); ~254MB is the ceiling.
        for (int i = 0; i < 200; i++) {
            byte[] b = new byte[(int) MB];
            for (int p = 0; p < b.length; p += 4096) {
                b[p] = 1;
            }
            heap.add(b);
        }
        report("1.heap+200MB");
        pause(2500);

        // stage 2: 64MB off-heap direct buffers (not governed by -Xmx, governed by MaxDirectMemorySize)
        for (int i = 0; i < 64; i++) {
            ByteBuffer bb = ByteBuffer.allocateDirect((int) MB);
            for (int p = 0; p < bb.capacity(); p += 4096) {
                bb.put(p, (byte) 1);
            }
            directs.add(bb);
        }
        report("2.direct+64MB");
        pause(2500);

        // stage 3: 200 threads x explicit 1MB stack (stack = virtual memory; pages touched on demand)
        for (int i = 0; i < 200; i++) {
            Thread t = new Thread(null, () -> pause(60_000), "probe-" + i, 1024 * 1024);
            t.setDaemon(true);
            t.start();
            threads.add(t);
        }
        report("3.threads+200");
        pause(3000);

        System.out.printf("done: heapArrays=%dMB directBuffers=%dMB extraThreads=%d%n",
            heap.size(), directs.size(), threads.size());
        System.out.flush();
    }
}
