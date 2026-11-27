import java.util.concurrent.*;

/**
 * m15 08 证据脚本：ThreadLocal 在池化线程上不 clear() 会串数据（"越权"复现）。
 *
 * 用法： java m15-08-threadlocal-leak.java
 * 输出： ASCII 键值行，便于正文引用与脚本断言。
 *
 * 与项目对应： ai-cs-chat/src/main/java/com/aics/chat/util/ChatUserContext.java
 *   private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
 *   setUserId / getUserId / clear()  —— 本脚本用同名同语义的本地副本，不改项目代码。
 */
class M15ThreadLocalLeak {

    /** 项目里的 ChatUserContext.USER_ID 的等价物 */
    static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /** 模拟一个请求：设用户 ID，读回，可选 clear() */
    static Long handleRequest(long userId, boolean doClear) {
        USER_ID.set(userId);
        Long seen = USER_ID.get();
        if (doClear) {
            USER_ID.remove();
        }
        return seen;
    }

    /** 模拟"忘了 clear"的请求：只有 set，没有 clear */
    static Long handleRequestNoClear(long userId) {
        USER_ID.set(userId);
        return USER_ID.get();
    }

    /** 模拟"下一个请求进来，本来没登录（匿名），却读到上一个用户 ID" */
    static Long readCurrent() {
        return USER_ID.get();
    }

    public static void main(String[] args) throws Exception {
        StringBuilder out = new StringBuilder();
        out.append("java.version       = ").append(System.getProperty("java.version")).append('\n');
        out.append("file.encoding      = ").append(System.getProperty("file.encoding")).append('\n');

        // 单线程池 = 线程复用模型（Tomcat 工作线程、@Async 线程池都是这个形态）
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            // 场景 A：请求 1 正常 clear()
            pool.submit(() -> handleRequest(1001L, true)).get();
            Long afterClean = pool.submit(M15ThreadLocalLeak::readCurrent).get();
            out.append("afterCleanRead     = ").append(afterClean).append('\n');
            out.append("afterCleanVerdict  = ").append(afterClean == null ? "OK-NULL" : "LEAK").append('\n');

            // 场景 B：请求 2 "忘了 clear()"，请求 3 匿名访问
            pool.submit(() -> handleRequestNoClear(2002L)).get();
            Long afterLeak = pool.submit(M15ThreadLocalLeak::readCurrent).get();
            out.append("afterNoClearRead   = ").append(afterLeak).append('\n');
            out.append("afterNoClearVerdict= ").append(afterLeak == null ? "OK-NULL" : "LEAK-CROSS-USER").append('\n');

            // 场景 C：补一次 clear()，同一线程上立刻恢复干净
            pool.submit(() -> {
                USER_ID.remove();
                return null;
            }).get();
            Long afterFix = pool.submit(M15ThreadLocalLeak::readCurrent).get();
            out.append("afterRemoveRead    = ").append(afterFix).append('\n');
            out.append("afterRemoveVerdict = ").append(afterFix == null ? "OK-NULL" : "LEAK").append('\n');
        } finally {
            pool.shutdown();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }

        System.out.print(out);
        System.out.flush();
        // 有线程池时兜底退出，避免脚挂住（与 m15-08-visibility.java 同一手法）
        System.exit(0);
    }
}
