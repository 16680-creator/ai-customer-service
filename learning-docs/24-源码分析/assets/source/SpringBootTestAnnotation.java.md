<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-test:4.2.0 | path: org/springframework/boot/test/context/SpringBootTestAnnotation.java -->
<!-- 正文引用：[[源码:SpringBootTestAnnotation.java#L起-L止]] ；代码块头部：(src: SpringBootTestAnnotation.java#L起-L止) -->

# SpringBootTestAnnotation.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-test:4.2.0` |
| 路径 | `org/springframework/boot/test/context/SpringBootTestAnnotation.java` |
| 行数 | 104 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-test/4.2.0/spring-boot-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:37.601Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.test.context;
    6 | 
    7 | import java.util.Arrays;
    8 | import java.util.Objects;
    9 | 
   10 | import org.jspecify.annotations.Nullable;
   11 | 
   12 | import org.springframework.boot.test.context.SpringBootTest.UseMainMethod;
   13 | import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
   14 | import org.springframework.context.ConfigurableApplicationContext;
   15 | import org.springframework.test.context.ContextCustomizer;
   16 | import org.springframework.test.context.MergedContextConfiguration;
   17 | import org.springframework.test.context.TestContextAnnotationUtils;
   18 | 
   19 | /**
   20 |  * {@link ContextCustomizer} to track attributes of {@link SpringBootTest @SpringBootTest}
   21 |  * that are taken into account when evaluating a {@link MergedContextConfiguration} to
   22 |  * determine if a context can be shared between tests.
   23 |  *
   24 |  * @author Phillip Webb
   25 |  * @author Madhura Bhave
   26 |  * @author Andy Wilkinson
   27 |  */
   28 | class SpringBootTestAnnotation implements ContextCustomizer {
   29 | 
   30 | 	private static final String[] NO_ARGS = new String[0];
   31 | 
   32 | 	private static final SpringBootTestAnnotation DEFAULT = new SpringBootTestAnnotation((SpringBootTest) null);
   33 | 
   34 | 	private final String[] args;
   35 | 
   36 | 	private final WebEnvironment webEnvironment;
   37 | 
   38 | 	private final UseMainMethod useMainMethod;
   39 | 
   40 | 	SpringBootTestAnnotation(Class<?> testClass) {
   41 | 		this(TestContextAnnotationUtils.findMergedAnnotation(testClass, SpringBootTest.class));
   42 | 	}
   43 | 
   44 | 	private SpringBootTestAnnotation(@Nullable SpringBootTest annotation) {
   45 | 		this.args = (annotation != null) ? annotation.args() : NO_ARGS;
   46 | 		this.webEnvironment = (annotation != null) ? annotation.webEnvironment() : WebEnvironment.NONE;
   47 | 		this.useMainMethod = (annotation != null) ? annotation.useMainMethod() : UseMainMethod.NEVER;
   48 | 	}
   49 | 
   50 | 	@Override
   51 | 	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
   52 | 	}
   53 | 
   54 | 	@Override
   55 | 	public boolean equals(Object obj) {
   56 | 		if (this == obj) {
   57 | 			return true;
   58 | 		}
   59 | 		if (obj == null || getClass() != obj.getClass()) {
   60 | 			return false;
   61 | 		}
   62 | 		SpringBootTestAnnotation other = (SpringBootTestAnnotation) obj;
   63 | 		boolean result = Arrays.equals(this.args, other.args);
   64 | 		result = result && this.useMainMethod == other.useMainMethod;
   65 | 		result = result && this.webEnvironment == other.webEnvironment;
   66 | 		return result;
   67 | 	}
   68 | 
   69 | 	@Override
   70 | 	public int hashCode() {
   71 | 		final int prime = 31;
   72 | 		int result = 1;
   73 | 		result = prime * result + Arrays.hashCode(this.args);
   74 | 		result = prime * result + Objects.hash(this.useMainMethod, this.webEnvironment);
   75 | 		return result;
   76 | 	}
   77 | 
   78 | 	String[] getArgs() {
   79 | 		return this.args;
   80 | 	}
   81 | 
   82 | 	WebEnvironment getWebEnvironment() {
   83 | 		return this.webEnvironment;
   84 | 	}
   85 | 
   86 | 	UseMainMethod getUseMainMethod() {
   87 | 		return this.useMainMethod;
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * Return the application arguments from the given {@link MergedContextConfiguration}.
   92 | 	 * @param mergedConfig the merged config to check
   93 | 	 * @return a {@link SpringBootTestAnnotation} instance
   94 | 	 */
   95 | 	static SpringBootTestAnnotation get(MergedContextConfiguration mergedConfig) {
   96 | 		for (ContextCustomizer customizer : mergedConfig.getContextCustomizers()) {
   97 | 			if (customizer instanceof SpringBootTestAnnotation annotation) {
   98 | 				return annotation;
   99 | 			}
  100 | 		}
  101 | 		return DEFAULT;
  102 | 	}
  103 | 
  104 | }
```
