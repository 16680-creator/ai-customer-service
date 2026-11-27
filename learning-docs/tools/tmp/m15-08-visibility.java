// m15-08-visibility.java —— 单文件可见性实验（JDK 17 直接 java 运行，无需编译）
// 用途：对比「非 volatile 字段」与「volatile 字段」在跨线程停止信号上的差别。
// 保护：写线程 200ms 后置位；主线程最多自旋 3000ms；无论如何进程 5s 内退出。
// 运行：java m15-08-visibility.java <plain|volatile> [自旋预算毫秒]
class M15Visibility {

    // 对照组：普通字段。JMM 不承诺写线程的值何时被读线程看到。
    static boolean stopPlain = false;
    // 实验组：volatile 字段。JMM 承诺写之后的读一定能看到。
    static volatile boolean stopVolatile = false;

    static long spinsPlain = 0L;
    static long spinsVolatile = 0L;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "plain";
        long budgetMs = args.length > 1 ? Long.parseLong(args[1]) : 3000L;
        boolean useVolatile = "volatile".equals(mode);

        Thread writer = new Thread(() -> {
            sleep(200L);
            stopPlain = true;      // 先写普通字段
            stopVolatile = true;   // 再写 volatile 字段
        }, "writer");
        writer.setDaemon(true);

        Thread spinner = new Thread(() -> {
            if (useVolatile) {
                long s = 0L;
                while (!stopVolatile) { s++; }   // volatile 读：每次循环都必须重新读主存
                spinsVolatile = s;
            } else {
                long s = 0L;
                while (!stopPlain) { s++; }      // 普通读：JIT 允许把它提到循环外
                spinsPlain = s;
            }
        }, "spinner");
        spinner.setDaemon(true);

        long t0 = System.nanoTime();
        writer.start();
        spinner.start();
        spinner.join(budgetMs);                 // 超时保护：最多等 budgetMs
        boolean exited = !spinner.isAlive();
        long costMs = (System.nanoTime() - t0) / 1_000_000L;

        System.out.println("mode            = " + (useVolatile ? "volatile" : "plain"));
        System.out.println("java.version    = " + System.getProperty("java.version"));
        System.out.println("vm.name         = " + System.getProperty("java.vm.name"));
        System.out.println("file.encoding   = " + System.getProperty("file.encoding"));
        System.out.println("budgetMs        = " + budgetMs);
        System.out.println("spinnerExited   = " + exited);
        if (exited) {
            System.out.println("spinCount       = " + (useVolatile ? spinsVolatile : spinsPlain));
        } else {
            System.out.println("spinCount       = n/a (spinner never exited, local counter not published)");
        }
        System.out.println("elapsedMs       = " + costMs);
        if (!exited) {
            System.out.println("verdict         = LOOP-NOT-EXITED: reader never observed the flag write (or JIT hoisted the read)");
        } else {
            System.out.println("verdict         = LOOP-EXITED: reader observed the flag write after " + costMs + "ms");
        }
        System.exit(0);                          // 强制收尾，避免残留自旋线程拖住进程
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
