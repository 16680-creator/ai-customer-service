<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/core/ResultSetExtractor.java -->
<!-- 正文引用：[[源码:ResultSetExtractor.java#L起-L止]] ；代码块头部：(src: ResultSetExtractor.java#L起-L止) -->

# ResultSetExtractor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/core/ResultSetExtractor.java` |
| 行数 | 67 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:23.178Z |

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
   17 | package org.springframework.jdbc.core;
   18 | 
   19 | import java.sql.ResultSet;
   20 | import java.sql.SQLException;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.dao.DataAccessException;
   25 | 
   26 | /**
   27 |  * Callback interface used by {@link JdbcTemplate}'s query methods.
   28 |  * Implementations of this interface perform the actual work of extracting
   29 |  * results from a {@link java.sql.ResultSet}, but don't need to worry
   30 |  * about exception handling. {@link java.sql.SQLException SQLExceptions}
   31 |  * will be caught and handled by the calling JdbcTemplate.
   32 |  *
   33 |  * <p>This interface is mainly used within the JDBC framework itself.
   34 |  * A {@link RowMapper} is usually a simpler choice for ResultSet processing,
   35 |  * mapping one result object per row instead of one result object for
   36 |  * the entire ResultSet.
   37 |  *
   38 |  * <p>Note: In contrast to a {@link RowCallbackHandler}, a ResultSetExtractor
   39 |  * object is typically stateless and thus reusable, as long as it doesn't
   40 |  * access stateful resources (such as output streams when streaming LOB
   41 |  * contents) or keep result state within the object.
   42 |  *
   43 |  * @author Rod Johnson
   44 |  * @author Juergen Hoeller
   45 |  * @since April 24, 2003
   46 |  * @param <T> the result type
   47 |  * @see JdbcTemplate
   48 |  * @see RowCallbackHandler
   49 |  * @see RowMapper
   50 |  * @see org.springframework.jdbc.core.support.AbstractLobStreamingResultSetExtractor
   51 |  */
   52 | @FunctionalInterface
   53 | public interface ResultSetExtractor<T extends @Nullable Object> {
   54 | 
   55 | 	/**
   56 | 	 * Implementations must implement this method to process the entire ResultSet.
   57 | 	 * @param rs the ResultSet to extract data from. Implementations should
   58 | 	 * not close this: it will be closed by the calling JdbcTemplate.
   59 | 	 * @return an arbitrary result object, or {@code null} if none
   60 | 	 * (the extractor will typically be stateful in the latter case).
   61 | 	 * @throws SQLException if an SQLException is encountered getting column
   62 | 	 * values or navigating (that is, there's no need to catch SQLException)
   63 | 	 * @throws DataAccessException in case of custom exceptions
   64 | 	 */
   65 | 	T extractData(ResultSet rs) throws SQLException, DataAccessException;
   66 | 
   67 | }
```
