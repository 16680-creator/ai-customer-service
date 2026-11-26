<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/WebApplicationType.java -->
<!-- 正文引用：[[源码:WebApplicationType.java#L起-L止]] ；代码块头部：(src: WebApplicationType.java#L起-L止) -->

# WebApplicationType.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/WebApplicationType.java` |
| 行数 | 104 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-26T09:20:37.721Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot;
    6 | 
    7 | import org.jspecify.annotations.Nullable;
    8 | 
    9 | import org.springframework.aot.hint.RuntimeHints;
   10 | import org.springframework.aot.hint.RuntimeHintsRegistrar;
   11 | import org.springframework.aot.hint.TypeReference;
   12 | import org.springframework.core.io.support.SpringFactoriesLoader;
   13 | import org.springframework.util.ClassUtils;
   14 | 
   15 | /**
   16 |  * An enumeration of possible types of web application.
   17 |  *
   18 |  * @author Andy Wilkinson
   19 |  * @author Brian Clozel
   20 |  * @author Phillip Webb
   21 |  * @since 2.0.0
   22 |  */
   23 | public enum WebApplicationType {
   24 | 
   25 | 	/**
   26 | 	 * The application should not run as a web application and should not start an
   27 | 	 * embedded web server.
   28 | 	 */
   29 | 	NONE,
   30 | 
   31 | 	/**
   32 | 	 * The application should run as a servlet-based web application and should start an
   33 | 	 * embedded servlet web server.
   34 | 	 */
   35 | 	SERVLET,
   36 | 
   37 | 	/**
   38 | 	 * The application should run as a reactive web application and should start an
   39 | 	 * embedded reactive web server.
   40 | 	 */
   41 | 	REACTIVE;
   42 | 
   43 | 	private static final String[] SERVLET_INDICATOR_CLASSES = { "jakarta.servlet.Servlet",
   44 | 			"org.springframework.web.context.ConfigurableWebApplicationContext" };
   45 | 
   46 | 	/**
   47 | 	 * Deduce the {@link WebApplicationType} from the current classpath.
   48 | 	 * @return the deduced web application
   49 | 	 * @since 4.0.1
   50 | 	 */
   51 | 	public static WebApplicationType deduce() {
   52 | 		for (Deducer deducer : SpringFactoriesLoader.forDefaultResourceLocation().load(Deducer.class)) {
   53 | 			WebApplicationType deduced = deducer.deduceWebApplicationType();
   54 | 			if (deduced != null) {
   55 | 				return deduced;
   56 | 			}
   57 | 		}
   58 | 		return isServletApplication() ? WebApplicationType.SERVLET : WebApplicationType.NONE;
   59 | 	}
   60 | 
   61 | 	private static boolean isServletApplication() {
   62 | 		for (String servletIndicatorClass : SERVLET_INDICATOR_CLASSES) {
   63 | 			if (!ClassUtils.isPresent(servletIndicatorClass, null)) {
   64 | 				return false;
   65 | 			}
   66 | 		}
   67 | 		return true;
   68 | 	}
   69 | 
   70 | 	static class WebApplicationTypeRuntimeHints implements RuntimeHintsRegistrar {
   71 | 
   72 | 		@Override
   73 | 		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
   74 | 			for (String servletIndicatorClass : SERVLET_INDICATOR_CLASSES) {
   75 | 				registerTypeIfPresent(servletIndicatorClass, classLoader, hints);
   76 | 			}
   77 | 		}
   78 | 
   79 | 		private void registerTypeIfPresent(String typeName, @Nullable ClassLoader classLoader, RuntimeHints hints) {
   80 | 			if (ClassUtils.isPresent(typeName, classLoader)) {
   81 | 				hints.reflection().registerType(TypeReference.of(typeName));
   82 | 			}
   83 | 		}
   84 | 
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Strategy that may be implemented by a module that can deduce the
   89 | 	 * {@link WebApplicationType}.
   90 | 	 *
   91 | 	 * @since 4.0.1
   92 | 	 */
   93 | 	@FunctionalInterface
   94 | 	public interface Deducer {
   95 | 
   96 | 		/**
   97 | 		 * Deduce the web application type.
   98 | 		 * @return the deduced web application type or {@code null}
   99 | 		 */
  100 | 		@Nullable WebApplicationType deduceWebApplicationType();
  101 | 
  102 | 	}
  103 | 
  104 | }
```
