<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/SpringApplicationAotProcessor.java -->
<!-- 正文引用：[[源码:SpringApplicationAotProcessor.java#L起-L止]] ；代码块头部：(src: SpringApplicationAotProcessor.java#L起-L止) -->

# SpringApplicationAotProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/SpringApplicationAotProcessor.java` |
| 行数 | 129 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:29.631Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot;
    6 | 
    7 | import java.lang.reflect.Method;
    8 | import java.nio.file.Paths;
    9 | import java.util.Arrays;
   10 | 
   11 | import org.springframework.boot.SpringApplication.AbandonedRunException;
   12 | import org.springframework.context.ApplicationContext;
   13 | import org.springframework.context.ConfigurableApplicationContext;
   14 | import org.springframework.context.aot.ContextAotProcessor;
   15 | import org.springframework.context.support.GenericApplicationContext;
   16 | import org.springframework.util.Assert;
   17 | import org.springframework.util.ReflectionUtils;
   18 | import org.springframework.util.StringUtils;
   19 | import org.springframework.util.function.ThrowingSupplier;
   20 | 
   21 | /**
   22 |  * Entry point for AOT processing of a {@link SpringApplication}.
   23 |  * <p>
   24 |  * <strong>For internal use only.</strong>
   25 |  *
   26 |  * @author Stephane Nicoll
   27 |  * @author Andy Wilkinson
   28 |  * @author Phillip Webb
   29 |  * @since 3.0.0
   30 |  */
   31 | public class SpringApplicationAotProcessor extends ContextAotProcessor {
   32 | 
   33 | 	private final String[] applicationArgs;
   34 | 
   35 | 	/**
   36 | 	 * Create a new processor for the specified application and settings.
   37 | 	 * @param application the application main class
   38 | 	 * @param settings the general AOT processor settings
   39 | 	 * @param applicationArgs the arguments to provide to the main method
   40 | 	 */
   41 | 	public SpringApplicationAotProcessor(Class<?> application, Settings settings, String[] applicationArgs) {
   42 | 		super(application, settings);
   43 | 		this.applicationArgs = applicationArgs;
   44 | 	}
   45 | 
   46 | 	@Override
   47 | 	protected GenericApplicationContext prepareApplicationContext(Class<?> application) {
   48 | 		return new AotProcessorHook(application).run(() -> {
   49 | 			Method mainMethod = getMainMethod(application);
   50 | 			mainMethod.setAccessible(true);
   51 | 			if (mainMethod.getParameterCount() == 0) {
   52 | 				ReflectionUtils.invokeMethod(mainMethod, null);
   53 | 			}
   54 | 			else {
   55 | 				ReflectionUtils.invokeMethod(mainMethod, null, new Object[] { this.applicationArgs });
   56 | 			}
   57 | 			return Void.class;
   58 | 		});
   59 | 	}
   60 | 
   61 | 	private static Method getMainMethod(Class<?> application) throws Exception {
   62 | 		try {
   63 | 			return application.getDeclaredMethod("main", String[].class);
   64 | 		}
   65 | 		catch (NoSuchMethodException ex) {
   66 | 			return application.getDeclaredMethod("main");
   67 | 		}
   68 | 	}
   69 | 
   70 | 	public static void main(String[] args) throws Exception {
   71 | 		int requiredArgs = 6;
   72 | 		Assert.state(args.length >= requiredArgs, () -> "Usage: " + SpringApplicationAotProcessor.class.getName()
   73 | 				+ " <applicationMainClass> <sourceOutput> <resourceOutput> <classOutput> <groupId> <artifactId> <originalArgs...>");
   74 | 		Class<?> application = Class.forName(args[0]);
   75 | 		Settings settings = Settings.builder()
   76 | 			.sourceOutput(Paths.get(args[1]))
   77 | 			.resourceOutput(Paths.get(args[2]))
   78 | 			.classOutput(Paths.get(args[3]))
   79 | 			.groupId((StringUtils.hasText(args[4])) ? args[4] : "unspecified")
   80 | 			.artifactId(args[5])
   81 | 			.build();
   82 | 		String[] applicationArgs = (args.length > requiredArgs) ? Arrays.copyOfRange(args, requiredArgs, args.length)
   83 | 				: new String[0];
   84 | 		new SpringApplicationAotProcessor(application, settings, applicationArgs).process();
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * {@link SpringApplicationHook} used to capture the {@link ApplicationContext} and
   89 | 	 * trigger early exit of main method.
   90 | 	 */
   91 | 	private static final class AotProcessorHook implements SpringApplicationHook {
   92 | 
   93 | 		private final Class<?> application;
   94 | 
   95 | 		private AotProcessorHook(Class<?> application) {
   96 | 			this.application = application;
   97 | 		}
   98 | 
   99 | 		@Override
  100 | 		public SpringApplicationRunListener getRunListener(SpringApplication application) {
  101 | 			return new SpringApplicationRunListener() {
  102 | 
  103 | 				@Override
  104 | 				public void contextLoaded(ConfigurableApplicationContext context) {
  105 | 					throw new AbandonedRunException(context);
  106 | 				}
  107 | 
  108 | 			};
  109 | 		}
  110 | 
  111 | 		private <T> GenericApplicationContext run(ThrowingSupplier<T> action) {
  112 | 			try {
  113 | 				SpringApplication.withHook(this, action);
  114 | 			}
  115 | 			catch (AbandonedRunException ex) {
  116 | 				ApplicationContext context = ex.getApplicationContext();
  117 | 				Assert.state(context instanceof GenericApplicationContext,
  118 | 						() -> "AOT processing requires a GenericApplicationContext but got a "
  119 | 								+ ((context != null) ? context.getClass().getName() : "null"));
  120 | 				return (GenericApplicationContext) context;
  121 | 			}
  122 | 			throw new IllegalStateException(
  123 | 					"No application context available after calling main method of '%s'. Does it run a SpringApplication?"
  124 | 						.formatted(this.application.getName()));
  125 | 		}
  126 | 
  127 | 	}
  128 | 
  129 | }
```
