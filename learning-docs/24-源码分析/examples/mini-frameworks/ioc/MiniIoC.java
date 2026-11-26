package mini.ioc;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal constructor-injection container; no Spring dependencies.
 *
 * 迷你 IoC 容器：只演示五件事（对应真实 Spring 的五个类/方法）——
 *   1) 登记：把候选类型当"图纸"存起来（对应 DefaultListableBeanFactory 的 beanDefinitionMap）；
 *   2) 按类型注入：构造器每个参数都回头向容器要（对应 autowireConstructor）；
 *   3) 单例：一个类型只 new 一次，接口与它的实现类共享同一个实例（对应 singletonObjects）；
 *   4) 递归解析：依赖先于自己创建，这是"构造器注入"的自然结果；
 *   5) 三类失败必须显式报错：依赖缺失、依赖歧义、构造器循环依赖（对应 NoSuchBeanDefinition /
 *      NoUniqueBeanDefinition / BeanCurrentlyInCreation 三个异常）。
 *
 * 编译与运行（在 docs/learning-docs 目录下，JDK 17；Windows 命令）：
 *   javac -encoding UTF-8 -d .source-cache/mini-check/ioc examples/mini-frameworks/ioc/MiniIoC.java
 *   java -cp .source-cache/mini-check/ioc mini.ioc.MiniIoC
 *
 * 正常退出时打印 4 行 PASS，退出码 0；任何断言不成立都会抛 AssertionError。
 */
public final class MiniIoC {
    public interface Repository { String find(long id); }
    public static final class MemoryRepository implements Repository {
        public String find(long id) { return "order:" + id; }
    }
    public static final class OtherRepository implements Repository {
        public String find(long id) { return "other:" + id; }
    }
    public static final class OrderService {
        final Repository repository;
        public OrderService(Repository repository) { this.repository = repository; }
        public String find(long id) { return repository.find(id); }
    }
    public static final class CycleA { public CycleA(CycleB b) {} }
    public static final class CycleB { public CycleB(CycleA a) {} }

    public static final class Container {
        /** 图纸：登记进来的候选类型（真实 Spring 里是 beanDefinitionMap 里的 BeanDefinition） */
        private final List<Class<?>> definitions = new ArrayList<Class<?>>();
        /** 一级缓存：已创建完成的单例，key 是实例的真实类（对应 singletonObjects） */
        private final Map<Class<?>, Object> singletons = new LinkedHashMap<Class<?>, Object>();
        /** "正在创建中"的类型栈，用来在递归回到自己时立刻报错，而不是栈溢出 */
        private final List<Class<?>> inCreation = new ArrayList<Class<?>>();

        public Container(Class<?>... types) {
            definitions.addAll(Arrays.asList(types));   // register：登记图纸，不 new
        }

        /** 按类型取 bean。真实 Spring 的 getBean(Class) 最终也走到按类型解析这一层 */
        public <T> T getBean(Class<T> type) {
            return type.cast(resolve(type));
        }

        private Object resolve(Class<?> type) {
            Object cached = lookupCache(type);       // 分水岭 1：先查缓存（单例语义的全部实现）
            if (cached != null) {
                return cached;
            }
            Class<?> candidate = pickCandidate(type); // 再在图纸里挑一个能被 new 的实现类
            if (inCreation.contains(candidate)) {
                // 真实的 Spring 抛 BeanCurrentlyInCreationException；
                // 构造器注入阶段的循环依赖在 Spring 里通常是 BeanCreationException 包着这个原因
                throw new IllegalStateException("Constructor cycle detected: "
                        + candidate.getSimpleName() + " is already being created");
            }
            inCreation.add(candidate);
            Object instance;
            try {
                instance = instantiate(candidate);
            } finally {
                inCreation.remove(candidate);        // 出栈，保证下一次解析不受影响
            }
            singletons.put(candidate, instance);     // ← 创建完立刻回填一级缓存
            return instance;
        }

        /**
         * 先按实例的真实类精确命中；没有再按"接口 / 父类兼容"找。
         * 这就是"按接口取到实现类那个单例"的实现方式：缓存 key 是真实类，查找时用 isInstance 判兼容。
         */
        private Object lookupCache(Class<?> type) {
            Object direct = singletons.get(type);
            if (direct != null) {
                return direct;
            }
            for (Object instance : singletons.values()) {
                if (type.isInstance(instance)) {
                    return instance;
                }
            }
            return null;
        }

        /** 在登记的图纸里找"能被赋值给 type"的具体类；0 个 → 报缺失，多个 → 报歧义 */
        private Class<?> pickCandidate(Class<?> type) {
            List<Class<?>> found = new ArrayList<Class<?>>();
            for (Class<?> definition : definitions) {
                if (type.isAssignableFrom(definition) && !definition.isInterface()
                        && !java.lang.reflect.Modifier.isAbstract(definition.getModifiers())) {
                    found.add(definition);
                }
            }
            if (found.isEmpty()) {
                throw new IllegalStateException("No bean for " + type.getSimpleName()
                        + " (registered: " + names(definitions) + ")");
            }
            if (found.size() > 1) {
                throw new IllegalStateException("Multiple beans for " + type.getSimpleName()
                        + ": " + names(found));
            }
            return found.get(0);
        }

        /**
         * 构造器注入：参数多的优先（Spring 也是"最贪婪且能解析成功"的那一个）。
         * 任何一个参数解析失败就换下一个构造器；全失败则抛出第一个失败原因，
         * 这样"缺依赖""歧义""循环依赖"三种错误不会被包装成一句含糊的 "创建失败"。
         */
        private Object instantiate(Class<?> type) {
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            Arrays.sort(constructors, (a, b) -> Integer.compare(b.getParameterCount(), a.getParameterCount()));
            IllegalStateException firstFailure = null;
            for (Constructor<?> constructor : constructors) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                Object[] args = new Object[parameterTypes.length];
                try {
                    for (int i = 0; i < parameterTypes.length; i++) {
                        args[i] = resolve(parameterTypes[i]);   // 分水岭 2：每个参数都去容器里要
                    }
                } catch (IllegalStateException ex) {
                    if (firstFailure == null) {
                        firstFailure = ex;
                    }
                    continue;
                }
                try {
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), ex);
                }
            }
            throw firstFailure != null ? firstFailure
                    : new IllegalStateException("No usable constructor for " + type.getSimpleName());
        }

        private static String names(List<Class<?>> types) {
            StringBuilder builder = new StringBuilder();
            for (Class<?> type : types) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(type.getSimpleName());
            }
            return builder.toString();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void expectFailure(Runnable action, String message) {
        try { action.run(); }
        catch (IllegalStateException ex) {
            check(ex.getMessage().contains(message), "Wrong failure: " + ex);
            return;
        }
        throw new AssertionError("Expected failure containing: " + message);
    }
    public static void main(String[] args) {
        Container container = new Container(OrderService.class, MemoryRepository.class);
        OrderService service = container.getBean(OrderService.class);
        check(service != null && "order:42".equals(service.find(42)), "By-type injection failed");
        check(service.repository == container.getBean(Repository.class), "Injected identity differs");
        check(service == container.getBean(OrderService.class), "Service is not a singleton");
        check(container.getBean(Repository.class) == container.getBean(MemoryRepository.class),
                "Interface and implementation must share one instance");
        System.out.println("PASS ioc: by-type constructor injection and singleton identity");
        expectFailure(() -> new Container(OrderService.class).getBean(OrderService.class), "No bean for");
        System.out.println("PASS ioc: missing dependency rejected");
        expectFailure(() -> new Container(OrderService.class, MemoryRepository.class, OtherRepository.class)
                .getBean(OrderService.class), "Multiple beans for");
        System.out.println("PASS ioc: ambiguous dependency rejected");
        expectFailure(() -> new Container(CycleA.class, CycleB.class).getBean(CycleA.class), "Constructor cycle");
        System.out.println("PASS ioc: constructor cycle rejected");
    }
}
