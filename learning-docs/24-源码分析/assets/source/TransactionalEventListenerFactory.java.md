<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/event/TransactionalEventListenerFactory.java -->
<!-- 正文引用：[[源码:TransactionalEventListenerFactory.java#L起-L止]] ；代码块头部：(src: TransactionalEventListenerFactory.java#L起-L止) -->

# TransactionalEventListenerFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/event/TransactionalEventListenerFactory.java` |
| 行数 | 59 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:57.986Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.transaction.event;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | 
   21 | import org.springframework.context.ApplicationListener;
   22 | import org.springframework.context.event.EventListenerFactory;
   23 | import org.springframework.core.Ordered;
   24 | import org.springframework.core.annotation.AnnotatedElementUtils;
   25 | 
   26 | /**
   27 |  * {@link EventListenerFactory} implementation that handles {@link TransactionalEventListener}
   28 |  * annotated methods.
   29 |  *
   30 |  * @author Stephane Nicoll
   31 |  * @since 4.2
   32 |  * @see TransactionalApplicationListenerMethodAdapter
   33 |  */
   34 | public class TransactionalEventListenerFactory implements EventListenerFactory, Ordered {
   35 | 
   36 | 	private int order = 50;
   37 | 
   38 | 
   39 | 	public void setOrder(int order) {
   40 | 		this.order = order;
   41 | 	}
   42 | 
   43 | 	@Override
   44 | 	public int getOrder() {
   45 | 		return this.order;
   46 | 	}
   47 | 
   48 | 
   49 | 	@Override
   50 | 	public boolean supportsMethod(Method method) {
   51 | 		return AnnotatedElementUtils.hasAnnotation(method, TransactionalEventListener.class);
   52 | 	}
   53 | 
   54 | 	@Override
   55 | 	public ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method) {
   56 | 		return new TransactionalApplicationListenerMethodAdapter(beanName, type, method);
   57 | 	}
   58 | 
   59 | }
```
