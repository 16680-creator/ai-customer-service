<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/dao/EmptyResultDataAccessException.java -->
<!-- 正文引用：[[源码:EmptyResultDataAccessException.java#L起-L止]] ；代码块头部：(src: EmptyResultDataAccessException.java#L起-L止) -->

# EmptyResultDataAccessException.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/dao/EmptyResultDataAccessException.java` |
| 行数 | 59 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:08:12.258Z |

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
   17 | package org.springframework.dao;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | /**
   22 |  * Data access exception thrown when a result was expected to have at least
   23 |  * one row (or element) but zero rows (or elements) were actually returned.
   24 |  *
   25 |  * @author Juergen Hoeller
   26 |  * @since 2.0
   27 |  * @see IncorrectResultSizeDataAccessException
   28 |  */
   29 | @SuppressWarnings("serial")
   30 | public class EmptyResultDataAccessException extends IncorrectResultSizeDataAccessException {
   31 | 
   32 | 	/**
   33 | 	 * Constructor for EmptyResultDataAccessException.
   34 | 	 * @param expectedSize the expected result size
   35 | 	 */
   36 | 	public EmptyResultDataAccessException(int expectedSize) {
   37 | 		super(expectedSize, 0);
   38 | 	}
   39 | 
   40 | 	/**
   41 | 	 * Constructor for EmptyResultDataAccessException.
   42 | 	 * @param msg the detail message
   43 | 	 * @param expectedSize the expected result size
   44 | 	 */
   45 | 	public EmptyResultDataAccessException(@Nullable String msg, int expectedSize) {
   46 | 		super(msg, expectedSize, 0);
   47 | 	}
   48 | 
   49 | 	/**
   50 | 	 * Constructor for EmptyResultDataAccessException.
   51 | 	 * @param msg the detail message
   52 | 	 * @param expectedSize the expected result size
   53 | 	 * @param ex the wrapped exception
   54 | 	 */
   55 | 	public EmptyResultDataAccessException(@Nullable String msg, int expectedSize, Throwable ex) {
   56 | 		super(msg, expectedSize, 0, ex);
   57 | 	}
   58 | 
   59 | }
```
