<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/interceptor/NoRollbackRuleAttribute.java -->
<!-- 正文引用：[[源码:NoRollbackRuleAttribute.java#L起-L止]] ；代码块头部：(src: NoRollbackRuleAttribute.java#L起-L止) -->

# NoRollbackRuleAttribute.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/interceptor/NoRollbackRuleAttribute.java` |
| 行数 | 61 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:57.261Z |

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
   17 | package org.springframework.transaction.interceptor;
   18 | 
   19 | /**
   20 |  * Tag subclass of {@link RollbackRuleAttribute} that has the opposite behavior
   21 |  * to the {@code RollbackRuleAttribute} superclass.
   22 |  *
   23 |  * @author Rod Johnson
   24 |  * @author Sam Brannen
   25 |  * @since 09.04.2003
   26 |  */
   27 | @SuppressWarnings("serial")
   28 | public class NoRollbackRuleAttribute extends RollbackRuleAttribute {
   29 | 
   30 | 	/**
   31 | 	 * Create a new instance of the {@code NoRollbackRuleAttribute} class
   32 | 	 * for the given {@code exceptionType}.
   33 | 	 * @param exceptionType exception type; must be {@link Throwable} or a subclass
   34 | 	 * of {@code Throwable}
   35 | 	 * @throws IllegalArgumentException if the supplied {@code exceptionType} is
   36 | 	 * not a {@code Throwable} type or is {@code null}
   37 | 	 * @see RollbackRuleAttribute#RollbackRuleAttribute(Class)
   38 | 	 */
   39 | 	public NoRollbackRuleAttribute(Class<?> exceptionType) {
   40 | 		super(exceptionType);
   41 | 	}
   42 | 
   43 | 	/**
   44 | 	 * Create a new instance of the {@code NoRollbackRuleAttribute} class
   45 | 	 * for the supplied {@code exceptionPattern}.
   46 | 	 * @param exceptionPattern the exception name pattern; can also be a fully
   47 | 	 * package-qualified class name
   48 | 	 * @throws IllegalArgumentException if the supplied {@code exceptionPattern}
   49 | 	 * is {@code null} or empty
   50 | 	 * @see RollbackRuleAttribute#RollbackRuleAttribute(String)
   51 | 	 */
   52 | 	public NoRollbackRuleAttribute(String exceptionPattern) {
   53 | 		super(exceptionPattern);
   54 | 	}
   55 | 
   56 | 	@Override
   57 | 	public String toString() {
   58 | 		return "No" + super.toString();
   59 | 	}
   60 | 
   61 | }
```
