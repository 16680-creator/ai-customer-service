<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/support/ResourceHolder.java -->
<!-- 正文引用：[[源码:ResourceHolder.java#L起-L止]] ；代码块头部：(src: ResourceHolder.java#L起-L止) -->

# ResourceHolder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/support/ResourceHolder.java` |
| 行数 | 47 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:59.012Z |

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
   17 | package org.springframework.transaction.support;
   18 | 
   19 | /**
   20 |  * Generic interface to be implemented by resource holders.
   21 |  * Allows Spring's transaction infrastructure to introspect
   22 |  * and reset the holder when necessary.
   23 |  *
   24 |  * @author Juergen Hoeller
   25 |  * @since 2.5.5
   26 |  * @see ResourceHolderSupport
   27 |  * @see ResourceHolderSynchronization
   28 |  */
   29 | public interface ResourceHolder {
   30 | 
   31 | 	/**
   32 | 	 * Reset the transactional state of this holder.
   33 | 	 */
   34 | 	void reset();
   35 | 
   36 | 	/**
   37 | 	 * Notify this holder that it has been unbound from transaction synchronization.
   38 | 	 */
   39 | 	void unbound();
   40 | 
   41 | 	/**
   42 | 	 * Determine whether this holder is considered as 'void',
   43 | 	 * i.e. as a leftover from a previous thread.
   44 | 	 */
   45 | 	boolean isVoid();
   46 | 
   47 | }
```
