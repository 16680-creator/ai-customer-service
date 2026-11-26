<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-test:4.2.0 | path: org/springframework/boot/test/context/filter/TestTypeExcludeFilter.java -->
<!-- 正文引用：[[源码:TestTypeExcludeFilter.java#L起-L止]] ；代码块头部：(src: TestTypeExcludeFilter.java#L起-L止) -->

# TestTypeExcludeFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-test:4.2.0` |
| 路径 | `org/springframework/boot/test/context/filter/TestTypeExcludeFilter.java` |
| 行数 | 95 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-test/4.2.0/spring-boot-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:16.173Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.test.context.filter;
    6 | 
    7 | import java.io.IOException;
    8 | 
    9 | import org.jspecify.annotations.Nullable;
   10 | 
   11 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   12 | import org.springframework.boot.context.TypeExcludeFilter;
   13 | import org.springframework.boot.test.context.TestComponent;
   14 | import org.springframework.core.type.classreading.MetadataReader;
   15 | import org.springframework.core.type.classreading.MetadataReaderFactory;
   16 | 
   17 | /**
   18 |  * {@link TypeExcludeFilter} to exclude classes annotated with
   19 |  * {@link TestComponent @TestComponent} as well as inner-classes of tests.
   20 |  *
   21 |  * @author Phillip Webb
   22 |  * @author Andy Wilkinson
   23 |  */
   24 | class TestTypeExcludeFilter extends TypeExcludeFilter {
   25 | 
   26 | 	private static final String BEAN_NAME = TestTypeExcludeFilter.class.getName();
   27 | 
   28 | 	private static final String[] CLASS_ANNOTATIONS = { "org.junit.runner.RunWith",
   29 | 			"org.junit.jupiter.api.extension.ExtendWith", "org.junit.platform.commons.annotation.Testable",
   30 | 			"org.testng.annotations.Test" };
   31 | 
   32 | 	private static final String[] METHOD_ANNOTATIONS = { "org.junit.Test",
   33 | 			"org.junit.platform.commons.annotation.Testable", "org.testng.annotations.Test" };
   34 | 
   35 | 	private static final TestTypeExcludeFilter INSTANCE = new TestTypeExcludeFilter();
   36 | 
   37 | 	@Override
   38 | 	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
   39 | 			throws IOException {
   40 | 		if (isTestConfiguration(metadataReader)) {
   41 | 			return true;
   42 | 		}
   43 | 		if (isTestClass(metadataReader)) {
   44 | 			return true;
   45 | 		}
   46 | 		String enclosing = metadataReader.getClassMetadata().getEnclosingClassName();
   47 | 		if (enclosing != null) {
   48 | 			try {
   49 | 				if (match(metadataReaderFactory.getMetadataReader(enclosing), metadataReaderFactory)) {
   50 | 					return true;
   51 | 				}
   52 | 			}
   53 | 			catch (Exception ex) {
   54 | 				// Ignore
   55 | 			}
   56 | 		}
   57 | 		return false;
   58 | 	}
   59 | 
   60 | 	@Override
   61 | 	public boolean equals(@Nullable Object obj) {
   62 | 		return (obj != null) && (getClass() == obj.getClass());
   63 | 	}
   64 | 
   65 | 	@Override
   66 | 	public int hashCode() {
   67 | 		return getClass().hashCode();
   68 | 	}
   69 | 
   70 | 	private boolean isTestConfiguration(MetadataReader metadataReader) {
   71 | 		return (metadataReader.getAnnotationMetadata().isAnnotated(TestComponent.class.getName()));
   72 | 	}
   73 | 
   74 | 	private boolean isTestClass(MetadataReader metadataReader) {
   75 | 		for (String annotation : CLASS_ANNOTATIONS) {
   76 | 			if (metadataReader.getAnnotationMetadata().hasAnnotation(annotation)) {
   77 | 				return true;
   78 | 			}
   79 | 
   80 | 		}
   81 | 		for (String annotation : METHOD_ANNOTATIONS) {
   82 | 			if (metadataReader.getAnnotationMetadata().hasAnnotatedMethods(annotation)) {
   83 | 				return true;
   84 | 			}
   85 | 		}
   86 | 		return false;
   87 | 	}
   88 | 
   89 | 	static void registerWith(ConfigurableListableBeanFactory beanFactory) {
   90 | 		if (!beanFactory.containsSingleton(BEAN_NAME)) {
   91 | 			beanFactory.registerSingleton(BEAN_NAME, INSTANCE);
   92 | 		}
   93 | 	}
   94 | 
   95 | }
```
