// learning-docs/tools/tmp/m15-vt-probe.java
// 用途：证明 JDK 17 的源码里没有 Thread.ofVirtual()（虚拟线程是 JDK 21 才有的正式 API）。
// 单独编译本文件，把编译错误原样贴进文档即可，不需要能跑通。
// 跑法：javac -d learning-docs\tools\tmp\out learning-docs\tools\tmp\m15-vt-probe.java
class M15VtProbe {
    public static void main(String[] args) {
        Thread t = Thread.ofVirtual().name("vt-1").start(() -> System.out.println("hello from virtual thread"));
        try { t.join(); } catch (InterruptedException ignored) { }
    }
}
