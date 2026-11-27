// learning-docs/tools/tmp/m15-02-stack-depth.java
// 目的：量「线程栈到底能装多少层递归」，同一份代码换 -Xss 就能看出栈是虚拟内存、按需提交。
// 运行（参数是每帧额外申请的局部数组字节数，0 表示只压栈帧本身）：
//   java                          m15-02-stack-depth.java 64    (默认栈：本机 ThreadStackSize=0，即用 OS 默认)
//   java -Xss256k                 m15-02-stack-depth.java 64
//   java -Xss2m                   m15-02-stack-depth.java 64
// 输出用 ASCII：本机 JDK 17 默认按平台编码（GBK）写 stdout，中文会乱码。
public class m15_02_stack_depth {

    static int depth = 0;

    static void recurse(int frameBytes) {
        depth++;
        byte[] local = frameBytes > 0 ? new byte[frameBytes] : null;
        if (local != null) local[0] = (byte) depth;   // 防止 JIT 把局部变量与帧优化掉
        recurse(frameBytes);
        if (local != null && local[0] == 0) System.out.print("");
    }

    public static void main(String[] args) {
        int frameBytes = args.length > 0 ? Integer.parseInt(args[0]) : 64;
        long t0 = System.nanoTime();
        try {
            recurse(frameBytes);
        } catch (StackOverflowError e) {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("StackOverflowError depth=" + depth
                + " localArrayBytes=" + frameBytes
                + " elapsedMs=" + ms);
        }
    }
}
