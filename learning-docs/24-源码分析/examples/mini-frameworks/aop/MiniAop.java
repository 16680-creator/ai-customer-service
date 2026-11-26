package mini.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JDK interface proxy with an ordered, nested interceptor chain.
 *
 * 迷你 AOP：只演示四件事（对应真实 Spring 的四个类/方法）——
 *   1) 切点（pointcut）是"这个方法的注解算不算匹配"的静态判断，演示的是 @Logged；
 *   2) 匹配必须在**实现类**上找方法：接口上的方法没有 @Logged，只有 ServiceImpl.call 上有，
 *      这与真实 Spring 用 targetClass 解析切点一致，也是"为什么能给实现类方法织入"的答案；
 *   3) 拦截器链用"下标 + 递归"实现有序嵌套（对应 ReflectiveMethodInvocation#proceed）；
 *   4) 目标方法抛出的已检查异常必须原样抛出：要拆掉 InvocationTargetException，
 *      否则调用方拿到的异常身份变了（对应 AopUtils#invokeJoinpointUsingReflection）。
 *
 * 编译与运行（在 docs/learning-docs 目录下，JDK 17；Windows 命令）：
 *   javac -encoding UTF-8 -d .source-cache/mini-check/aop examples/mini-frameworks/aop/MiniAop.java
 *   java -cp .source-cache/mini-check/aop mini.aop.MiniAop
 *
 * 正常退出时打印 3 行 PASS，退出码 0；任何断言不成立都会抛 AssertionError。
 */
public final class MiniAop {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Logged {}
    public interface Proceeding { Object proceed() throws Throwable; }
    public interface Interceptor { Object invoke(Proceeding next) throws Throwable; }
    public static final class BusinessException extends Exception {
        public BusinessException(String message) { super(message); }
    }
    public interface Service {
        String call(String value) throws BusinessException;
        String plain();
    }
    public static final class ServiceImpl implements Service {
        private final List<String> events;
        final BusinessException failure = new BusinessException("business failed");
        public ServiceImpl(List<String> events) { this.events = events; }
        @Logged
        public String call(String value) throws BusinessException {
            events.add("target");
            if ("fail".equals(value)) throw failure;
            return "hello:" + value;
        }
        public String plain() { events.add("plain.target"); return "plain"; }
    }

    /**
     * 生成代理：把"要不要拦"和"怎么串链"全部放进 InvocationHandler。
     * interceptors 的顺序就是嵌套顺序：下标 0 在最外层（真实 Spring 里由 Advisor 的 order 决定）。
     */
    public static <T> T proxy(Class<T> api, T target, List<Interceptor> interceptors) {
        InvocationHandler handler = (proxy, method, args) -> {
            // 关键一步：拿"实现类"上的同名方法判切点。接口 Service.call 上没有 @Logged，
            // 只有 ServiceImpl.call 上有 —— 所以这里必须用 target.getClass()，不能用 api。
            Method targetMethod = findTargetMethod(target, method);
            if (!targetMethod.isAnnotationPresent(Logged.class)) {
                return invokeTarget(target, targetMethod, args);   // 不匹配 → 直接放行，不进链
            }
            Object[] arguments = args == null ? new Object[0] : args;
            return new Chain(interceptors, target, targetMethod, arguments, 0).proceed();
        };
        return api.cast(Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] { api }, handler));
    }

    /** 一条拦截器链：每 proceed 一次就推进一格，走到链尾才反射调用目标方法 */
    private static final class Chain implements Proceeding {
        private final List<Interceptor> interceptors;
        private final Object target;
        private final Method method;
        private final Object[] args;
        private final int index;      // 当前该执行第几条拦截器（关键状态）

        Chain(List<Interceptor> interceptors, Object target, Method method, Object[] args, int index) {
            this.interceptors = interceptors;
            this.target = target;
            this.method = method;
            this.args = args;
            this.index = index;
        }

        public Object proceed() throws Throwable {
            if (index == interceptors.size()) {
                return invokeTarget(target, method, args);          // 链尾 → 真正调用目标
            }
            return interceptors.get(index).invoke(
                    new Chain(interceptors, target, method, args, index + 1));   // 把"下一格"交出去
        }
    }

    /** 在实现类上找与被调接口方法签名相同的方法 */
    private static Method findTargetMethod(Object target, Method interfaceMethod) {
        try {
            return target.getClass().getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Target has no method " + interfaceMethod.getName(), ex);
        }
    }

    /** 反射调用目标方法，并拆掉 InvocationTargetException：抛出的必须是原始异常对象本身 */
    private static Object invokeTarget(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();     // 不包装、不转换身份，调用方 catch 到的就是 target.failure 那个实例
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot access " + method, ex);
        }
    }

    private static Interceptor trace(String name, List<String> events) {
        return next -> {
            events.add(name + ".before");
            try { return next.proceed(); }
            finally { events.add(name + ".after"); }
        };
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        List<String> events = new ArrayList<String>();
        ServiceImpl target = new ServiceImpl(events);
        Service service = proxy(Service.class, target, Arrays.asList(trace("outer", events), trace("inner", events)));
        List<String> expected = Arrays.asList("outer.before", "inner.before", "target", "inner.after", "outer.after");
        check("hello:Spring".equals(service.call("Spring")), "Return value changed");
        check(events.equals(expected), "Wrong success order: " + events);
        System.out.println("PASS aop: " + events);
        events.clear();
        try {
            service.call("fail");
            throw new AssertionError("Business exception was swallowed");
        } catch (BusinessException ex) {
            check(ex == target.failure, "Original checked exception identity was lost");
        }
        check(events.equals(expected), "Wrong failure/finally order: " + events);
        System.out.println("PASS aop: same checked exception propagated; after advice ran in reverse order");
        events.clear();
        check("plain".equals(service.plain()), "Plain result changed");
        check(events.equals(Arrays.asList("plain.target")), "Unmatched method was advised");
        System.out.println("PASS aop: unmatched method bypasses advice");
    }
}
