<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-test-autoconfigure:4.2.0 | path: org/springframework/boot/test/autoconfigure/TestSliceTestContextBootstrapper.java -->
<!-- 正文引用：[[源码:TestSliceTestContextBootstrapper.java#L起-L止]] ；代码块头部：(src: TestSliceTestContextBootstrapper.java#L起-L止) -->

# TestSliceTestContextBootstrapper.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-test-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/test/autoconfigure/TestSliceTestContextBootstrapper.java` |
| 行数 | 51 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-test-autoconfigure/4.2.0/spring-boot-test-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:19:42.092Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.test.autoconfigure;
    6 | 
    7 | import java.lang.annotation.Annotation;
    8 | 
    9 | import org.jspecify.annotations.Nullable;
   10 | 
   11 | import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
   12 | import org.springframework.core.ResolvableType;
   13 | import org.springframework.core.annotation.MergedAnnotation;
   14 | import org.springframework.core.annotation.MergedAnnotations;
   15 | import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
   16 | import org.springframework.test.context.TestContextAnnotationUtils;
   17 | import org.springframework.test.context.TestContextBootstrapper;
   18 | import org.springframework.util.Assert;
   19 | 
   20 | /**
   21 |  * Base class for test slice {@link TestContextBootstrapper test context bootstrappers}.
   22 |  *
   23 |  * @param <T> the test slice annotation
   24 |  * @author Yanming Zhou
   25 |  * @since 4.0.0
   26 |  */
   27 | public abstract class TestSliceTestContextBootstrapper<T extends Annotation> extends SpringBootTestContextBootstrapper {
   28 | 
   29 | 	private final Class<T> annotationType;
   30 | 
   31 | 	@SuppressWarnings("unchecked")
   32 | 	protected TestSliceTestContextBootstrapper() {
   33 | 		Class<T> annotationType = (Class<T>) ResolvableType.forClass(getClass())
   34 | 			.as(TestSliceTestContextBootstrapper.class)
   35 | 			.getGeneric(0)
   36 | 			.resolve();
   37 | 		Assert.notNull(annotationType, "'%s' doesn't contain type parameter of '%s'".formatted(getClass().getName(),
   38 | 				TestSliceTestContextBootstrapper.class.getName()));
   39 | 		this.annotationType = annotationType;
   40 | 	}
   41 | 
   42 | 	@Override
   43 | 	protected String @Nullable [] getProperties(Class<?> testClass) {
   44 | 		MergedAnnotation<T> annotation = MergedAnnotations.search(SearchStrategy.TYPE_HIERARCHY)
   45 | 			.withEnclosingClasses(TestContextAnnotationUtils::searchEnclosingClass)
   46 | 			.from(testClass)
   47 | 			.get(this.annotationType);
   48 | 		return annotation.isPresent() ? annotation.getStringArray("properties") : null;
   49 | 	}
   50 | 
   51 | }
```
