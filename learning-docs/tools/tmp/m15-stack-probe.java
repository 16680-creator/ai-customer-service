// learning-docs/tools/tmp/m15-stack-probe.java
// 用途：量"创建 N 个线程"实际让 JVM 提交了多少虚拟内存/驻留内存，
//      用来验证"每个线程栈 1MB 只是预留，不是立刻吃掉 1MB 物理内存"。
// 跑法：java "-Dfile.encoding=UTF-8" learning-docs\tools\tmp\m15-stack-probe.java
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

class M15StackProbe {
    public static void main(String[] args) throws Exception {
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        int n = 500;
        long committed0 = os.getCommittedVirtualMemorySize();
        long rss0 = os.getProcessCpuTime(); // 占位，避免未使用告警
        CountDownLatch hold = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Thread t = new Thread(() -> {
                try { hold.await(); } catch (InterruptedException ignored) { }
            }, "stack-probe-" + i);
            t.setDaemon(true);
            t.start();
            threads.add(t);
        }
        Thread.sleep(500);
        long committed1 = os.getCommittedVirtualMemorySize();
        System.out.printf("线程数=%d%n", threads.size());
        System.out.printf("创建前 committed virtual memory = %d MB%n", committed0 / 1024 / 1024);
        System.out.printf("创建后 committed virtual memory = %d MB%n", committed1 / 1024 / 1024);
        System.out.printf("每个线程实际多提交 = %.2f MB%n", (committed1 - committed0) / 1024.0 / 1024.0 / n);
        System.out.printf("ThreadMXBean.getThreadCount() = %d%n", ManagementFactory.getThreadMXBean().getThreadCount());
        System.out.println("[判读] 每线程增量远小于 1MB → 栈是预留+按需提交；但虚拟地址空间确实被占，" +
                "32 位 JVM 或 cgroup 限制虚拟内存时会先撞墙 (rss0=" + (rss0 == 0 ? "n/a" : "ok") + ")");
        hold.countDown();
        for (Thread t : threads) t.join(1000);
    }
}
