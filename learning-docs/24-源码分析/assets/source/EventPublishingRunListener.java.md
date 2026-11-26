<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/event/EventPublishingRunListener.java -->
<!-- 正文引用：[[源码:EventPublishingRunListener.java#L起-L止]] ；代码块头部：(src: EventPublishingRunListener.java#L起-L止) -->

# EventPublishingRunListener.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/event/EventPublishingRunListener.java` |
| 行数 | 143 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-26T09:21:47.115Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.event;
    6 | 
    7 | import java.time.Duration;
    8 | 
    9 | import org.apache.commons.logging.Log;
   10 | import org.apache.commons.logging.LogFactory;
   11 | import org.jspecify.annotations.Nullable;
   12 | 
   13 | import org.springframework.boot.SpringApplication;
   14 | import org.springframework.boot.SpringApplicationRunListener;
   15 | import org.springframework.boot.availability.AvailabilityChangeEvent;
   16 | import org.springframework.boot.availability.LivenessState;
   17 | import org.springframework.boot.availability.ReadinessState;
   18 | import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
   19 | import org.springframework.context.ApplicationContextAware;
   20 | import org.springframework.context.ApplicationEvent;
   21 | import org.springframework.context.ApplicationListener;
   22 | import org.springframework.context.ConfigurableApplicationContext;
   23 | import org.springframework.context.event.ApplicationEventMulticaster;
   24 | import org.springframework.context.event.SimpleApplicationEventMulticaster;
   25 | import org.springframework.context.support.AbstractApplicationContext;
   26 | import org.springframework.core.Ordered;
   27 | import org.springframework.core.env.ConfigurableEnvironment;
   28 | import org.springframework.util.ErrorHandler;
   29 | 
   30 | /**
   31 |  * {@link SpringApplicationRunListener} to publish {@link SpringApplicationEvent}s.
   32 |  * <p>
   33 |  * Uses an internal {@link ApplicationEventMulticaster} for the events that are fired
   34 |  * before the context is actually refreshed.
   35 |  *
   36 |  * @author Phillip Webb
   37 |  * @author Stephane Nicoll
   38 |  * @author Andy Wilkinson
   39 |  * @author Artsiom Yudovin
   40 |  * @author Brian Clozel
   41 |  * @author Chris Bono
   42 |  */
   43 | class EventPublishingRunListener implements SpringApplicationRunListener, Ordered {
   44 | 
   45 | 	private final SpringApplication application;
   46 | 
   47 | 	private final String[] args;
   48 | 
   49 | 	private final SimpleApplicationEventMulticaster initialMulticaster;
   50 | 
   51 | 	EventPublishingRunListener(SpringApplication application, String[] args) {
   52 | 		this.application = application;
   53 | 		this.args = args;
   54 | 		this.initialMulticaster = new SimpleApplicationEventMulticaster();
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	public int getOrder() {
   59 | 		return 0;
   60 | 	}
   61 | 
   62 | 	@Override
   63 | 	public void starting(ConfigurableBootstrapContext bootstrapContext) {
   64 | 		multicastInitialEvent(new ApplicationStartingEvent(bootstrapContext, this.application, this.args));
   65 | 	}
   66 | 
   67 | 	@Override
   68 | 	public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext,
   69 | 			ConfigurableEnvironment environment) {
   70 | 		multicastInitialEvent(
   71 | 				new ApplicationEnvironmentPreparedEvent(bootstrapContext, this.application, this.args, environment));
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	public void contextPrepared(ConfigurableApplicationContext context) {
   76 | 		multicastInitialEvent(new ApplicationContextInitializedEvent(this.application, this.args, context));
   77 | 	}
   78 | 
   79 | 	@Override
   80 | 	public void contextLoaded(ConfigurableApplicationContext context) {
   81 | 		for (ApplicationListener<?> listener : this.application.getListeners()) {
   82 | 			if (listener instanceof ApplicationContextAware contextAware) {
   83 | 				contextAware.setApplicationContext(context);
   84 | 			}
   85 | 			context.addApplicationListener(listener);
   86 | 		}
   87 | 		multicastInitialEvent(new ApplicationPreparedEvent(this.application, this.args, context));
   88 | 	}
   89 | 
   90 | 	@Override
   91 | 	public void started(ConfigurableApplicationContext context, @Nullable Duration timeTaken) {
   92 | 		context.publishEvent(new ApplicationStartedEvent(this.application, this.args, context, timeTaken));
   93 | 		AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
   94 | 	}
   95 | 
   96 | 	@Override
   97 | 	public void ready(ConfigurableApplicationContext context, @Nullable Duration timeTaken) {
   98 | 		context.publishEvent(new ApplicationReadyEvent(this.application, this.args, context, timeTaken));
   99 | 		AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
  100 | 	}
  101 | 
  102 | 	@Override
  103 | 	public void failed(@Nullable ConfigurableApplicationContext context, Throwable exception) {
  104 | 		ApplicationFailedEvent event = new ApplicationFailedEvent(this.application, this.args, context, exception);
  105 | 		if (context != null && context.isActive()) {
  106 | 			// Listeners have been registered to the application context so we should
  107 | 			// use it at this point if we can
  108 | 			context.publishEvent(event);
  109 | 		}
  110 | 		else {
  111 | 			// An inactive context may not have a multicaster so we use our multicaster to
  112 | 			// call all the context's listeners instead
  113 | 			if (context instanceof AbstractApplicationContext abstractApplicationContext) {
  114 | 				for (ApplicationListener<?> listener : abstractApplicationContext.getApplicationListeners()) {
  115 | 					this.initialMulticaster.addApplicationListener(listener);
  116 | 				}
  117 | 			}
  118 | 			this.initialMulticaster.setErrorHandler(new LoggingErrorHandler());
  119 | 			this.initialMulticaster.multicastEvent(event);
  120 | 		}
  121 | 	}
  122 | 
  123 | 	private void multicastInitialEvent(ApplicationEvent event) {
  124 | 		refreshApplicationListeners();
  125 | 		this.initialMulticaster.multicastEvent(event);
  126 | 	}
  127 | 
  128 | 	private void refreshApplicationListeners() {
  129 | 		this.application.getListeners().forEach(this.initialMulticaster::addApplicationListener);
  130 | 	}
  131 | 
  132 | 	private static final class LoggingErrorHandler implements ErrorHandler {
  133 | 
  134 | 		private static final Log logger = LogFactory.getLog(EventPublishingRunListener.class);
  135 | 
  136 | 		@Override
  137 | 		public void handleError(Throwable throwable) {
  138 | 			logger.warn("Error calling ApplicationEventListener", throwable);
  139 | 		}
  140 | 
  141 | 	}
  142 | 
  143 | }
```
