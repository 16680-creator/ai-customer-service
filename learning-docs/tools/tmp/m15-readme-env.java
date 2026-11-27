// learning-docs/tools/tmp/m15-readme-env.java
// 用途：打印「本机 + 本 JVM」的基线事实，供 15-计算机基础/README.md 第 4 节表格取证。
// 跑法：java learning-docs\tools\tmp\m15-readme-env.java
// 只读：不写文件、不联网、不启动任何服务。
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.TimeZone;

class M15ReadmeEnv {
    public static void main(String[] args) {
        Runtime rt = Runtime.getRuntime();
        long maxBytes = rt.maxMemory();

        System.out.println("os.name                = " + System.getProperty("os.name"));
        System.out.println("os.version             = " + System.getProperty("os.version"));
        System.out.println("os.arch                = " + System.getProperty("os.arch"));
        System.out.println("java.version           = " + System.getProperty("java.version"));
        System.out.println("java.vendor            = " + System.getProperty("java.vendor"));
        System.out.println("java.home              = " + System.getProperty("java.home"));

        // CPU：JVM 眼里的核数（受 cgroup/亲和性影响，本机无容器限制时等于逻辑核数）
        System.out.println("availableProcessors    = " + rt.availableProcessors());
        // 公共池并行度：默认 = 核数 - 1，这是"线程数不等于核数"的第一个反例
        System.out.println("commonPoolParallelism  = "
                + java.util.concurrent.ForkJoinPool.commonPool().getParallelism());

        // 堆上限：Runtime.maxMemory() 就是 -XX:MaxHeapSize 的实际生效值
        System.out.println("MaxHeapSize(bytes)     = " + maxBytes);
        System.out.println("MaxHeapSize(MiB)       = " + (maxBytes / 1024 / 1024));
        System.out.printf("MaxHeapSize(GiB)       = %.2f%n", maxBytes / 1024.0 / 1024.0 / 1024.0);

        // 时区：user.timezone 未显式设置时是 null，真正的默认值来自宿主
        String prop = System.getProperty("user.timezone");
        System.out.println("user.timezone(property)= " + prop);
        System.out.println("TimeZone.getDefault()  = " + TimeZone.getDefault().getID());
        System.out.println("ZoneId.systemDefault() = " + ZoneId.systemDefault().getId());

        // 编码：file.encoding 是 JVM 的口径，native.encoding 是操作系统的口径
        System.out.println("file.encoding          = " + System.getProperty("file.encoding"));
        System.out.println("native.encoding        = " + System.getProperty("native.encoding"));
        System.out.println("defaultCharset         = " + Charset.defaultCharset());
        System.out.println("stdout.encoding        = " + System.getProperty("stdout.encoding"));

        // 物理内存（JDK 自带 com.sun.management 扩展，不需要第三方库）
        com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long total = os.getTotalMemorySize();
        System.out.println("TotalPhysicalMemory(MB)= " + (total / 1024 / 1024));
        System.out.printf("TotalPhysicalMemory(GiB)= %.1f%n", total / 1024.0 / 1024.0 / 1024.0);
    }
}
