<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-health:4.2.0 | path: org/springframework/boot/health/registry/DefaultHealthContributorRegistry.java -->
<!-- 正文引用：[[源码:DefaultHealthContributorRegistry.java#L起-L止]] ；代码块头部：(src: DefaultHealthContributorRegistry.java#L起-L止) -->

# DefaultHealthContributorRegistry.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-health:4.2.0` |
| 路径 | `org/springframework/boot/health/registry/DefaultHealthContributorRegistry.java` |
| 行数 | 64 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-health/4.2.0/spring-boot-health-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:03.255Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.health.registry;
    6 | 
    7 | import java.util.Collection;
    8 | import java.util.function.BiConsumer;
    9 | import java.util.function.Consumer;
   10 | import java.util.stream.Stream;
   11 | 
   12 | import org.jspecify.annotations.Nullable;
   13 | 
   14 | import org.springframework.boot.health.contributor.HealthContributor;
   15 | import org.springframework.boot.health.contributor.HealthContributors.Entry;
   16 | 
   17 | /**
   18 |  * Default {@link HealthContributorRegistry} implementation.
   19 |  *
   20 |  * @author Phillip Webb
   21 |  * @since 4.0.0
   22 |  */
   23 | public class DefaultHealthContributorRegistry extends AbstractRegistry<HealthContributor, Entry>
   24 | 		implements HealthContributorRegistry {
   25 | 
   26 | 	/**
   27 | 	 * Create a new empty {@link DefaultHealthContributorRegistry} instance.
   28 | 	 */
   29 | 	public DefaultHealthContributorRegistry() {
   30 | 		this(null, null);
   31 | 	}
   32 | 
   33 | 	/**
   34 | 	 * Create a new {@link DefaultHealthContributorRegistry} instance.
   35 | 	 * @param nameValidators the name validators to apply
   36 | 	 * @param initialRegistrations callback to setup any initial registrations
   37 | 	 */
   38 | 	public DefaultHealthContributorRegistry(
   39 | 			@Nullable Collection<? extends HealthContributorNameValidator> nameValidators,
   40 | 			@Nullable Consumer<BiConsumer<String, HealthContributor>> initialRegistrations) {
   41 | 		super(Entry::new, nameValidators, initialRegistrations);
   42 | 	}
   43 | 
   44 | 	@Override
   45 | 	public @Nullable HealthContributor getContributor(String name) {
   46 | 		return super.getContributor(name);
   47 | 	}
   48 | 
   49 | 	@Override
   50 | 	public Stream<Entry> stream() {
   51 | 		return super.stream();
   52 | 	}
   53 | 
   54 | 	@Override
   55 | 	public void registerContributor(String name, HealthContributor contributor) {
   56 | 		super.registerContributor(name, contributor);
   57 | 	}
   58 | 
   59 | 	@Override
   60 | 	public @Nullable HealthContributor unregisterContributor(String name) {
   61 | 		return super.unregisterContributor(name);
   62 | 	}
   63 | 
   64 | }
```
