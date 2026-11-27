// learning-docs/tools/tmp/m15-load-probe.java
// 用途：制造"线程数远超核数"的现场，供 Get-Counter 同时采样 Processor Queue Length / Context Switches。
// 用法：java "-Dfile.encoding=UTF-8" learning-docs\tools\tmp\m15-load-probe.java <线程数> <持续秒数>
// 例：  java "-Dfile.encoding=UTF-8" learning-docs\tools\tmp\m15-load-probe.java 32 12
class M15LoadProbe {
    public static void main(String[] args) throws Exception {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 32;
        int sec = args.length > 1 ? Integer.parseInt(args[1]) : 12;
        System.out.println("spinning " + n + " threads for " + sec + "s on "
                + Runtime.getRuntime().availableProcessors() + " logical processors");
        long deadline = System.currentTimeMillis() + sec * 1000L;
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) {
            ts[i] = new Thread(() -> {
                long x = 0;
                while (System.currentTimeMillis() < deadline) {
                    for (int j = 0; j < 100_000; j++) { x += j ^ (x >>> 3); }
                }
                if (x == 42) System.out.print("");
            }, "spin-" + i);
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        System.out.println("done");
    }
}
