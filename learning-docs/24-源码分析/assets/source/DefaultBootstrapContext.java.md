<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/bootstrap/DefaultBootstrapContext.java -->
<!-- 正文引用：[[源码:DefaultBootstrapContext.java#L起-L止]] ；代码块头部：(src: DefaultBootstrapContext.java#L起-L止) -->

# DefaultBootstrapContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/bootstrap/DefaultBootstrapContext.java` |
| 行数 | 131 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-26T09:20:38.644Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.bootstrap;
    6 | 
    7 | import java.util.HashMap;
    8 | import java.util.Map;
    9 | import java.util.function.Supplier;
   10 | 
   11 | import org.jspecify.annotations.Nullable;
   12 | 
   13 | import org.springframework.context.ApplicationContext;
   14 | import org.springframework.context.ApplicationListener;
   15 | import org.springframework.context.ConfigurableApplicationContext;
   16 | import org.springframework.context.event.ApplicationEventMulticaster;
   17 | import org.springframework.context.event.SimpleApplicationEventMulticaster;
   18 | import org.springframework.util.Assert;
   19 | 
   20 | /**
   21 |  * Default {@link ConfigurableBootstrapContext} implementation.
   22 |  *
   23 |  * @author Phillip Webb
   24 |  * @since 4.0.0
   25 |  */
   26 | public class DefaultBootstrapContext implements ConfigurableBootstrapContext {
   27 | 
   28 | 	private final Map<Class<?>, InstanceSupplier<?>> instanceSuppliers = new HashMap<>();
   29 | 
   30 | 	private final Map<Class<?>, Object> instances = new HashMap<>();
   31 | 
   32 | 	private final ApplicationEventMulticaster events = new SimpleApplicationEventMulticaster();
   33 | 
   34 | 	@Override
   35 | 	public <T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier) {
   36 | 		register(type, instanceSupplier, true);
   37 | 	}
   38 | 
   39 | 	@Override
   40 | 	public <T> void registerIfAbsent(Class<T> type, InstanceSupplier<T> instanceSupplier) {
   41 | 		register(type, instanceSupplier, false);
   42 | 	}
   43 | 
   44 | 	private <T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier, boolean replaceExisting) {
   45 | 		Assert.notNull(type, "'type' must not be null");
   46 | 		Assert.notNull(instanceSupplier, "'instanceSupplier' must not be null");
   47 | 		synchronized (this.instanceSuppliers) {
   48 | 			boolean alreadyRegistered = this.instanceSuppliers.containsKey(type);
   49 | 			if (replaceExisting || !alreadyRegistered) {
   50 | 				Assert.state(!this.instances.containsKey(type), () -> type.getName() + " has already been created");
   51 | 				this.instanceSuppliers.put(type, instanceSupplier);
   52 | 			}
   53 | 		}
   54 | 	}
   55 | 
   56 | 	@Override
   57 | 	public <T> boolean isRegistered(Class<T> type) {
   58 | 		synchronized (this.instanceSuppliers) {
   59 | 			return this.instanceSuppliers.containsKey(type);
   60 | 		}
   61 | 	}
   62 | 
   63 | 	@Override
   64 | 	@SuppressWarnings("unchecked")
   65 | 	public <T> @Nullable InstanceSupplier<T> getRegisteredInstanceSupplier(Class<T> type) {
   66 | 		synchronized (this.instanceSuppliers) {
   67 | 			return (InstanceSupplier<T>) this.instanceSuppliers.get(type);
   68 | 		}
   69 | 	}
   70 | 
   71 | 	@Override
   72 | 	public void addCloseListener(ApplicationListener<BootstrapContextClosedEvent> listener) {
   73 | 		this.events.addApplicationListener(listener);
   74 | 	}
   75 | 
   76 | 	@Override
   77 | 	public <T> @Nullable T get(Class<T> type) throws IllegalStateException {
   78 | 		return getOrElseThrow(type, () -> new IllegalStateException(type.getName() + " has not been registered"));
   79 | 	}
   80 | 
   81 | 	@Override
   82 | 	public <T> @Nullable T getOrElse(Class<T> type, @Nullable T other) {
   83 | 		Supplier<@Nullable T> supplier = () -> other;
   84 | 		return getOrElseSupply(type, supplier);
   85 | 	}
   86 | 
   87 | 	@Override
   88 | 	public <T> @Nullable T getOrElseSupply(Class<T> type, Supplier<@Nullable T> other) {
   89 | 		synchronized (this.instanceSuppliers) {
   90 | 			InstanceSupplier<?> instanceSupplier = this.instanceSuppliers.get(type);
   91 | 			return (instanceSupplier != null) ? getInstance(type, instanceSupplier) : other.get();
   92 | 		}
   93 | 	}
   94 | 
   95 | 	@Override
   96 | 	public <T, X extends Throwable> @Nullable T getOrElseThrow(Class<T> type, Supplier<? extends X> exceptionSupplier)
   97 | 			throws X {
   98 | 		synchronized (this.instanceSuppliers) {
   99 | 			InstanceSupplier<?> instanceSupplier = this.instanceSuppliers.get(type);
  100 | 			if (instanceSupplier == null) {
  101 | 				throw exceptionSupplier.get();
  102 | 			}
  103 | 			return getInstance(type, instanceSupplier);
  104 | 		}
  105 | 	}
  106 | 
  107 | 	@SuppressWarnings("unchecked")
  108 | 	private <T> @Nullable T getInstance(Class<T> type, InstanceSupplier<?> instanceSupplier) {
  109 | 		T instance = (T) this.instances.get(type);
  110 | 		if (instance == null) {
  111 | 			instance = (T) instanceSupplier.get(this);
  112 | 			if (instance == null) {
  113 | 				return null;
  114 | 			}
  115 | 			if (instanceSupplier.getScope() == Scope.SINGLETON) {
  116 | 				this.instances.put(type, instance);
  117 | 			}
  118 | 		}
  119 | 		return instance;
  120 | 	}
  121 | 
  122 | 	/**
  123 | 	 * Method to be called when {@link BootstrapContext} is closed and the
  124 | 	 * {@link ApplicationContext} is prepared.
  125 | 	 * @param applicationContext the prepared context
  126 | 	 */
  127 | 	public void close(ConfigurableApplicationContext applicationContext) {
  128 | 		this.events.multicastEvent(new BootstrapContextClosedEvent(this, applicationContext));
  129 | 	}
  130 | 
  131 | }
```
