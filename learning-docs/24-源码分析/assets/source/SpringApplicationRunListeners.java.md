<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/SpringApplicationRunListeners.java -->
<!-- 正文引用：[[源码:SpringApplicationRunListeners.java#L起-L止]] ；代码块头部：(src: SpringApplicationRunListeners.java#L起-L止) -->

# SpringApplicationRunListeners.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/SpringApplicationRunListeners.java` |
| 行数 | 122 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-26T09:21:08.565Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot;
    6 | 
    7 | import java.time.Duration;
    8 | import java.util.List;
    9 | import java.util.function.Consumer;
   10 | 
   11 | import org.apache.commons.logging.Log;
   12 | import org.jspecify.annotations.Nullable;
   13 | 
   14 | import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
   15 | import org.springframework.context.ConfigurableApplicationContext;
   16 | import org.springframework.core.env.ConfigurableEnvironment;
   17 | import org.springframework.core.metrics.ApplicationStartup;
   18 | import org.springframework.core.metrics.StartupStep;
   19 | import org.springframework.util.ReflectionUtils;
   20 | 
   21 | /**
   22 |  * A collection of {@link SpringApplicationRunListener}.
   23 |  *
   24 |  * @author Phillip Webb
   25 |  * @author Andy Wilkinson
   26 |  * @author Chris Bono
   27 |  */
   28 | class SpringApplicationRunListeners {
   29 | 
   30 | 	private final Log log;
   31 | 
   32 | 	private final List<SpringApplicationRunListener> listeners;
   33 | 
   34 | 	private final ApplicationStartup applicationStartup;
   35 | 
   36 | 	SpringApplicationRunListeners(Log log, List<SpringApplicationRunListener> listeners,
   37 | 			ApplicationStartup applicationStartup) {
   38 | 		this.log = log;
   39 | 		this.listeners = List.copyOf(listeners);
   40 | 		this.applicationStartup = applicationStartup;
   41 | 	}
   42 | 
   43 | 	void starting(ConfigurableBootstrapContext bootstrapContext, @Nullable Class<?> mainApplicationClass) {
   44 | 		doWithListeners("spring.boot.application.starting", (listener) -> listener.starting(bootstrapContext),
   45 | 				(step) -> {
   46 | 					if (mainApplicationClass != null) {
   47 | 						step.tag("mainApplicationClass", mainApplicationClass.getName());
   48 | 					}
   49 | 				});
   50 | 	}
   51 | 
   52 | 	void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
   53 | 		doWithListeners("spring.boot.application.environment-prepared",
   54 | 				(listener) -> listener.environmentPrepared(bootstrapContext, environment));
   55 | 	}
   56 | 
   57 | 	void contextPrepared(ConfigurableApplicationContext context) {
   58 | 		doWithListeners("spring.boot.application.context-prepared", (listener) -> listener.contextPrepared(context));
   59 | 	}
   60 | 
   61 | 	void contextLoaded(ConfigurableApplicationContext context) {
   62 | 		doWithListeners("spring.boot.application.context-loaded", (listener) -> listener.contextLoaded(context));
   63 | 	}
   64 | 
   65 | 	void started(ConfigurableApplicationContext context, Duration timeTaken) {
   66 | 		if (context.isRunning()) {
   67 | 			doWithListeners("spring.boot.application.started", (listener) -> listener.started(context, timeTaken));
   68 | 		}
   69 | 	}
   70 | 
   71 | 	void ready(ConfigurableApplicationContext context, Duration timeTaken) {
   72 | 		if (context.isRunning()) {
   73 | 			doWithListeners("spring.boot.application.ready", (listener) -> listener.ready(context, timeTaken));
   74 | 		}
   75 | 	}
   76 | 
   77 | 	void failed(@Nullable ConfigurableApplicationContext context, Throwable exception) {
   78 | 		doWithListeners("spring.boot.application.failed",
   79 | 				(listener) -> callFailedListener(listener, context, exception), (step) -> {
   80 | 					step.tag("exception", exception.getClass().toString());
   81 | 					String message = exception.getMessage();
   82 | 					if (message != null) {
   83 | 						step.tag("message", message);
   84 | 					}
   85 | 				});
   86 | 	}
   87 | 
   88 | 	private void callFailedListener(SpringApplicationRunListener listener,
   89 | 			@Nullable ConfigurableApplicationContext context, Throwable exception) {
   90 | 		try {
   91 | 			listener.failed(context, exception);
   92 | 		}
   93 | 		catch (Throwable ex) {
   94 | 			if (exception == null) {
   95 | 				ReflectionUtils.rethrowRuntimeException(ex);
   96 | 			}
   97 | 			if (this.log.isDebugEnabled()) {
   98 | 				this.log.error("Error handling failed", ex);
   99 | 			}
  100 | 			else {
  101 | 				String message = ex.getMessage();
  102 | 				message = (message != null) ? message : "no error message";
  103 | 				this.log.warn("Error handling failed (" + message + ")");
  104 | 			}
  105 | 		}
  106 | 	}
  107 | 
  108 | 	private void doWithListeners(String stepName, Consumer<SpringApplicationRunListener> listenerAction) {
  109 | 		doWithListeners(stepName, listenerAction, null);
  110 | 	}
  111 | 
  112 | 	private void doWithListeners(String stepName, Consumer<SpringApplicationRunListener> listenerAction,
  113 | 			@Nullable Consumer<StartupStep> stepAction) {
  114 | 		StartupStep step = this.applicationStartup.start(stepName);
  115 | 		this.listeners.forEach(listenerAction);
  116 | 		if (stepAction != null) {
  117 | 			stepAction.accept(step);
  118 | 		}
  119 | 		step.end();
  120 | 	}
  121 | 
  122 | }
```
