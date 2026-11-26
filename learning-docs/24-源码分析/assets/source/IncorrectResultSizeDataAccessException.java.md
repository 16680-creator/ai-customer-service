<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/dao/IncorrectResultSizeDataAccessException.java -->
<!-- 正文引用：[[源码:IncorrectResultSizeDataAccessException.java#L起-L止]] ；代码块头部：(src: IncorrectResultSizeDataAccessException.java#L起-L止) -->

# IncorrectResultSizeDataAccessException.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/dao/IncorrectResultSizeDataAccessException.java` |
| 行数 | 122 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:08:12.163Z |

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
   22 |  * Data access exception thrown when a result was not of the expected size,
   23 |  * for example when expecting a single row but getting 0 or more than 1 rows.
   24 |  *
   25 |  * @author Juergen Hoeller
   26 |  * @author Chris Beams
   27 |  * @since 1.0.2
   28 |  * @see EmptyResultDataAccessException
   29 |  */
   30 | @SuppressWarnings("serial")
   31 | public class IncorrectResultSizeDataAccessException extends DataRetrievalFailureException {
   32 | 
   33 | 	private final int expectedSize;
   34 | 
   35 | 	private final int actualSize;
   36 | 
   37 | 
   38 | 	/**
   39 | 	 * Constructor for IncorrectResultSizeDataAccessException.
   40 | 	 * @param expectedSize the expected result size
   41 | 	 */
   42 | 	public IncorrectResultSizeDataAccessException(int expectedSize) {
   43 | 		super("Incorrect result size: expected " + expectedSize);
   44 | 		this.expectedSize = expectedSize;
   45 | 		this.actualSize = -1;
   46 | 	}
   47 | 
   48 | 	/**
   49 | 	 * Constructor for IncorrectResultSizeDataAccessException.
   50 | 	 * @param expectedSize the expected result size
   51 | 	 * @param actualSize the actual result size (or -1 if unknown)
   52 | 	 */
   53 | 	public IncorrectResultSizeDataAccessException(int expectedSize, int actualSize) {
   54 | 		super("Incorrect result size: expected " + expectedSize + ", actual " + actualSize);
   55 | 		this.expectedSize = expectedSize;
   56 | 		this.actualSize = actualSize;
   57 | 	}
   58 | 
   59 | 	/**
   60 | 	 * Constructor for IncorrectResultSizeDataAccessException.
   61 | 	 * @param msg the detail message
   62 | 	 * @param expectedSize the expected result size
   63 | 	 */
   64 | 	public IncorrectResultSizeDataAccessException(String msg, int expectedSize) {
   65 | 		super(msg);
   66 | 		this.expectedSize = expectedSize;
   67 | 		this.actualSize = -1;
   68 | 	}
   69 | 
   70 | 	/**
   71 | 	 * Constructor for IncorrectResultSizeDataAccessException.
   72 | 	 * @param msg the detail message
   73 | 	 * @param expectedSize the expected result size
   74 | 	 * @param ex the wrapped exception
   75 | 	 */
   76 | 	public IncorrectResultSizeDataAccessException(@Nullable String msg, int expectedSize, @Nullable Throwable ex) {
   77 | 		super(msg, ex);
   78 | 		this.expectedSize = expectedSize;
   79 | 		this.actualSize = -1;
   80 | 	}
   81 | 
   82 | 	/**
   83 | 	 * Constructor for IncorrectResultSizeDataAccessException.
   84 | 	 * @param msg the detail message
   85 | 	 * @param expectedSize the expected result size
   86 | 	 * @param actualSize the actual result size (or -1 if unknown)
   87 | 	 */
   88 | 	public IncorrectResultSizeDataAccessException(@Nullable String msg, int expectedSize, int actualSize) {
   89 | 		super(msg);
   90 | 		this.expectedSize = expectedSize;
   91 | 		this.actualSize = actualSize;
   92 | 	}
   93 | 
   94 | 	/**
   95 | 	 * Constructor for IncorrectResultSizeDataAccessException.
   96 | 	 * @param msg the detail message
   97 | 	 * @param expectedSize the expected result size
   98 | 	 * @param actualSize the actual result size (or -1 if unknown)
   99 | 	 * @param ex the wrapped exception
  100 | 	 */
  101 | 	public IncorrectResultSizeDataAccessException(@Nullable String msg, int expectedSize, int actualSize, @Nullable Throwable ex) {
  102 | 		super(msg, ex);
  103 | 		this.expectedSize = expectedSize;
  104 | 		this.actualSize = actualSize;
  105 | 	}
  106 | 
  107 | 
  108 | 	/**
  109 | 	 * Return the expected result size.
  110 | 	 */
  111 | 	public int getExpectedSize() {
  112 | 		return this.expectedSize;
  113 | 	}
  114 | 
  115 | 	/**
  116 | 	 * Return the actual result size (or -1 if unknown).
  117 | 	 */
  118 | 	public int getActualSize() {
  119 | 		return this.actualSize;
  120 | 	}
  121 | 
  122 | }
```
