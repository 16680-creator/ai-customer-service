package mini.boot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Explicit candidates + class/missing-bean conditions; user beans register first.
 *
 * 迷你 Boot 自动配置：只演示四件事（对应真实 Spring Boot 的四个环节）——
 *   1) 候选清单：外部显式传进来（真实 Boot 是从 META-INF/...imports 里读全限定类名）；
 *   2) 类条件：@ConditionalOnClass 里的类不在 classpath 上就整个配置类跳过，
 *      而且**不能实例化它**（跳过的配置类一 new 就报错，本文件的 MissingConfiguration 就是这么自证的）；
 *   3) 顺序是语义的一部分：用户 bean 必须先注册，@ConditionalOnMissingBean 才看得到它，
 *      自动配置才会"退让"（对应真实 Boot 把自动配置放在用户配置之后处理）；
 *   4) 每次"接受 / 跳过"都要记进 report，这样"为什么我的 bean 没生效"才有据可查。
 *
 * 编译与运行（在 docs/learning-docs 目录下，JDK 17；Windows 命令）：
 *   javac -encoding UTF-8 -d .source-cache/mini-check/boot examples/mini-frameworks/boot/MiniBoot.java
 *   java -cp .source-cache/mini-check/boot mini.boot.MiniBoot
 *
 * 正常退出时打印 4 行 PASS（第 4 行带完整决策日志），退出码 0；任何断言不成立都会抛 AssertionError。
 */
public final class MiniBoot {
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    public @interface ConditionalOnClass { String value(); }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface Bean {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface ConditionalOnMissingBean { Class<?> value(); }
    public interface Greeting { String greet(); }
    public static final class DefaultGreeting implements Greeting {
        static int created;
        public DefaultGreeting() { created++; }
        public String greet() { return "default"; }
    }
    public static final class UserGreeting implements Greeting {
        public String greet() { return "user"; }
    }
    @ConditionalOnClass("java.lang.String")
    public static final class GreetingConfiguration {
        @Bean @ConditionalOnMissingBean(Greeting.class)
        public Greeting greeting() { return new DefaultGreeting(); }
    }
    @ConditionalOnClass("mini.optional.DoesNotExist")
    public static final class MissingConfiguration {
        public MissingConfiguration() { throw new AssertionError("Skipped configuration was instantiated"); }
        @Bean public String forbidden() { return "must not exist"; }
    }
    public static final class Context {
        /** 决策日志：每一条"接受/跳过"都记下来，这就是 Boot 的 ConditionEvaluationReport */
        final List<String> report = new ArrayList<String>();
        /** 单例表：按注册顺序保存在容器里的对象 */
        private final List<Object> singletons = new ArrayList<Object>();

        public <T> T getBean(Class<T> type) {
            for (Object singleton : singletons) {
                if (type.isInstance(singleton)) {
                    return type.cast(singleton);
                }
            }
            return null;
        }

        public int count(Class<?> type) {
            int count = 0;
            for (Object singleton : singletons) {
                if (type.isInstance(singleton)) {
                    count++;
                }
            }
            return count;
        }

        void register(Object bean) {
            singletons.add(bean);
        }
    }

    /**
     * 五步：①先收用户 bean（必须在自动配置之前）→ ②逐个判类条件 → ③不满足就跳过整个配置类
     * → ④满足就实例化配置类并调它的 @Bean 方法（每个方法再过一次 @ConditionalOnMissingBean）
     * → ⑤返回装了 bean 和决策日志的 Context。
     */
    public static Context run(Class<?>[] candidates, Object... userBeans) {
        Context context = new Context();
        for (Object userBean : userBeans) {                 // ① 用户 bean 先注册
            context.register(userBean);
            context.report.add("register user bean " + userBean.getClass().getSimpleName());
        }
        for (Class<?> candidate : candidates) {
            if (!classPresent(candidate)) {                 // ② 类条件
                context.report.add("skip class " + candidate.getSimpleName());   // ③ 整体跳过
                continue;
            }
            Object configuration = instantiate(candidate);
            context.report.add("accept class " + candidate.getSimpleName());
            for (Method method : candidate.getDeclaredMethods()) {              // ④ @Bean 方法
                if (!method.isAnnotationPresent(Bean.class)) {
                    continue;
                }
                ConditionalOnMissingBean backoff = method.getAnnotation(ConditionalOnMissingBean.class);
                String beanName = candidate.getSimpleName() + "." + method.getName();
                if (backoff != null && context.count(backoff.value()) > 0) {
                    context.report.add("skip bean " + beanName);                // 用户已经定义过 → 退让
                    continue;
                }
                context.register(invoke(method, configuration));
                context.report.add("register bean " + beanName);
            }
        }
        return context;
    }

    /** 类条件：注解里写的类在不在 classpath 上（用 Class.forName 判断，不触发初始化） */
    private static boolean classPresent(Class<?> candidate) {
        ConditionalOnClass condition = candidate.getAnnotation(ConditionalOnClass.class);
        if (condition == null) {
            return true;
        }
        try {
            Class.forName(condition.value(), false, candidate.getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static Object instantiate(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to instantiate configuration " + type.getSimpleName(), ex);
        }
    }

    private static Object invoke(Method method, Object configuration) {
        try {
            return method.invoke(configuration);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("@Bean method " + method.getName() + " failed", ex.getCause());
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot access @Bean method " + method.getName(), ex);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Class<?>[] candidates = { GreetingConfiguration.class, MissingConfiguration.class };
        Context defaults = run(candidates);
        check(defaults.getBean(Greeting.class) instanceof DefaultGreeting, "Default configuration did not apply");
        check(defaults.count(Greeting.class) == 1, "Default bean duplicated");
        check(defaults.count(String.class) == 0, "Missing-class configuration was not skipped");
        check(defaults.report.contains("skip class MissingConfiguration"), "Missing-class decision not recorded");
        System.out.println("PASS boot: present class enables default bean; missing class skips configuration");
        UserGreeting user = new UserGreeting();
        int before = DefaultGreeting.created;
        Context customized = run(candidates, user);
        check(customized.getBean(Greeting.class) == user, "User bean identity was replaced");
        check(customized.count(Greeting.class) == 1, "Default and user beans both registered");
        check(DefaultGreeting.created == before, "Default factory ran despite user bean");
        check(customized.report.contains("skip bean GreetingConfiguration.greeting"), "Backoff decision not recorded");
        System.out.println("PASS boot: user bean wins by identity; default factory was not called");
        System.out.println("PASS boot: " + customized.report);
    }
}
