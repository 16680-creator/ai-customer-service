<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/AutoConfigurations.java -->
<!-- 正文引用：[[源码:AutoConfigurations.java#L起-L止]] ；代码块头部：(src: AutoConfigurations.java#L起-L止) -->

# AutoConfigurations.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/AutoConfigurations.java` |
| 行数 | 71 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:06.581Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.autoconfigure;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Arrays;
    9 | import java.util.Collection;
   10 | import java.util.List;
   11 | import java.util.Set;
   12 | import java.util.function.UnaryOperator;
   13 | import java.util.stream.Collectors;
   14 | 
   15 | import org.springframework.boot.context.annotation.Configurations;
   16 | import org.springframework.core.Ordered;
   17 | import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
   18 | import org.springframework.util.ClassUtils;
   19 | 
   20 | /**
   21 |  * {@link Configurations} representing auto-configuration {@code @Configuration} classes.
   22 |  *
   23 |  * @author Phillip Webb
   24 |  * @since 2.0.0
   25 |  */
   26 | public class AutoConfigurations extends Configurations implements Ordered {
   27 | 
   28 | 	private static final SimpleMetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
   29 | 
   30 | 	private static final int ORDER = AutoConfigurationImportSelector.ORDER;
   31 | 
   32 | 	static final AutoConfigurationReplacements replacements = AutoConfigurationReplacements
   33 | 		.load(AutoConfiguration.class, null);
   34 | 
   35 | 	private final UnaryOperator<String> replacementMapper;
   36 | 
   37 | 	protected AutoConfigurations(Collection<Class<?>> classes) {
   38 | 		this(replacements::replace, classes);
   39 | 	}
   40 | 
   41 | 	AutoConfigurations(UnaryOperator<String> replacementMapper, Collection<Class<?>> classes) {
   42 | 		super(sorter(replacementMapper), classes, Class::getName);
   43 | 		this.replacementMapper = replacementMapper;
   44 | 	}
   45 | 
   46 | 	private static UnaryOperator<Collection<Class<?>>> sorter(UnaryOperator<String> replacementMapper) {
   47 | 		AutoConfigurationSorter sorter = new AutoConfigurationSorter(metadataReaderFactory, null, replacementMapper);
   48 | 		return (classes) -> {
   49 | 			List<String> names = classes.stream().map(Class::getName).map(replacementMapper).toList();
   50 | 			List<String> sorted = sorter.getInPriorityOrder(names);
   51 | 			return sorted.stream()
   52 | 				.map((className) -> ClassUtils.resolveClassName(className, null))
   53 | 				.collect(Collectors.toCollection(ArrayList::new));
   54 | 		};
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	public int getOrder() {
   59 | 		return ORDER;
   60 | 	}
   61 | 
   62 | 	@Override
   63 | 	protected AutoConfigurations merge(Set<Class<?>> mergedClasses) {
   64 | 		return new AutoConfigurations(this.replacementMapper, mergedClasses);
   65 | 	}
   66 | 
   67 | 	public static AutoConfigurations of(Class<?>... classes) {
   68 | 		return new AutoConfigurations(Arrays.asList(classes));
   69 | 	}
   70 | 
   71 | }
```
