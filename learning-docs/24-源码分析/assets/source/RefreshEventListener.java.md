<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/endpoint/event/RefreshEventListener.java -->
<!-- 正文引用：[[源码:RefreshEventListener.java#L起-L止]] ；代码块头部：(src: RefreshEventListener.java#L起-L止) -->

# RefreshEventListener.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/endpoint/event/RefreshEventListener.java` |
| 行数 | 77 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:37.681Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package org.springframework.cloud.endpoint.event;
   18 | 
   19 | import java.util.Set;
   20 | import java.util.concurrent.atomic.AtomicBoolean;
   21 | 
   22 | import org.apache.commons.logging.Log;
   23 | import org.apache.commons.logging.LogFactory;
   24 | 
   25 | import org.springframework.boot.context.event.ApplicationReadyEvent;
   26 | import org.springframework.cloud.context.refresh.ContextRefresher;
   27 | import org.springframework.context.ApplicationEvent;
   28 | import org.springframework.context.event.SmartApplicationListener;
   29 | 
   30 | /**
   31 |  * Calls {@link ContextRefresher#refresh} when a {@link RefreshEvent} is received. Only
   32 |  * responds to {@link RefreshEvent} after receiving an {@link ApplicationReadyEvent}, as
   33 |  * the RefreshEvents might come too early in the application lifecycle.
   34 |  *
   35 |  * @author Spencer Gibb
   36 |  */
   37 | public class RefreshEventListener implements SmartApplicationListener {
   38 | 
   39 | 	private static Log log = LogFactory.getLog(RefreshEventListener.class);
   40 | 
   41 | 	private ContextRefresher refresh;
   42 | 
   43 | 	private AtomicBoolean ready = new AtomicBoolean(false);
   44 | 
   45 | 	public RefreshEventListener(ContextRefresher refresh) {
   46 | 		this.refresh = refresh;
   47 | 	}
   48 | 
   49 | 	@Override
   50 | 	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
   51 | 		return ApplicationReadyEvent.class.isAssignableFrom(eventType)
   52 | 				|| RefreshEvent.class.isAssignableFrom(eventType);
   53 | 	}
   54 | 
   55 | 	@Override
   56 | 	public void onApplicationEvent(ApplicationEvent event) {
   57 | 		if (event instanceof ApplicationReadyEvent applicationReadyEvent) {
   58 | 			handle(applicationReadyEvent);
   59 | 		}
   60 | 		else if (event instanceof RefreshEvent refreshEvent) {
   61 | 			handle(refreshEvent);
   62 | 		}
   63 | 	}
   64 | 
   65 | 	public void handle(ApplicationReadyEvent event) {
   66 | 		this.ready.compareAndSet(false, true);
   67 | 	}
   68 | 
   69 | 	public void handle(RefreshEvent event) {
   70 | 		if (this.ready.get()) { // don't handle events before app is ready
   71 | 			log.debug("Event received " + event.getEventDesc());
   72 | 			Set<String> keys = this.refresh.refresh();
   73 | 			log.info("Refresh keys changed: " + keys);
   74 | 		}
   75 | 	}
   76 | 
   77 | }
```
