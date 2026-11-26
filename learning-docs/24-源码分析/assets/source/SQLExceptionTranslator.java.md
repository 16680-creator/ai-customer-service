<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/support/SQLExceptionTranslator.java -->
<!-- 正文引用：[[源码:SQLExceptionTranslator.java#L起-L止]] ；代码块头部：(src: SQLExceptionTranslator.java#L起-L止) -->

# SQLExceptionTranslator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/support/SQLExceptionTranslator.java` |
| 行数 | 58 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:09.785Z |

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
   17 | package org.springframework.jdbc.support;
   18 | 
   19 | import java.sql.SQLException;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.dao.DataAccessException;
   24 | 
   25 | /**
   26 |  * Strategy interface for translating between {@link SQLException SQLExceptions}
   27 |  * and Spring's data access strategy-agnostic {@link DataAccessException}
   28 |  * hierarchy.
   29 |  *
   30 |  * <p>Implementations can be generic (for example, using
   31 |  * {@link java.sql.SQLException#getSQLState() SQLState} codes for JDBC) or wholly
   32 |  * proprietary (for example, using Oracle error codes) for greater precision.
   33 |  *
   34 |  * @author Rod Johnson
   35 |  * @author Juergen Hoeller
   36 |  * @see org.springframework.dao.DataAccessException
   37 |  */
   38 | @FunctionalInterface
   39 | public interface SQLExceptionTranslator {
   40 | 
   41 | 	/**
   42 | 	 * Translate the given {@link SQLException} into a generic {@link DataAccessException}.
   43 | 	 * <p>The returned DataAccessException is supposed to contain the original
   44 | 	 * {@code SQLException} as root cause. However, client code may not generally
   45 | 	 * rely on this due to DataAccessExceptions possibly being caused by other resource
   46 | 	 * APIs as well. That said, a {@code getRootCause() instanceof SQLException}
   47 | 	 * check (and subsequent cast) is considered reliable when expecting JDBC-based
   48 | 	 * access to have happened.
   49 | 	 * @param task readable text describing the task being attempted
   50 | 	 * @param sql the SQL query or update that caused the problem (if known)
   51 | 	 * @param ex the offending {@code SQLException}
   52 | 	 * @return the DataAccessException wrapping the {@code SQLException},
   53 | 	 * or {@code null} if no specific translation could be applied
   54 | 	 * @see org.springframework.dao.DataAccessException#getRootCause()
   55 | 	 */
   56 | 	@Nullable DataAccessException translate(String task, @Nullable String sql, SQLException ex);
   57 | 
   58 | }
```
