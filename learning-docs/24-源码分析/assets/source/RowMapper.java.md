<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/core/RowMapper.java -->
<!-- 正文引用：[[源码:RowMapper.java#L起-L止]] ；代码块头部：(src: RowMapper.java#L起-L止) -->

# RowMapper.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/core/RowMapper.java` |
| 行数 | 66 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:09.429Z |

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
   24 | /**
   25 |  * An interface used by {@link JdbcTemplate} for mapping rows of a
   26 |  * {@link java.sql.ResultSet} on a per-row basis. Implementations of this
   27 |  * interface perform the actual work of mapping each row to a result object
   28 |  * but don't need to worry about exception handling.
   29 |  * {@link java.sql.SQLException SQLExceptions} will be caught and handled
   30 |  * by the calling {@code JdbcTemplate}.
   31 |  *
   32 |  * <p>Typically used either for {@code JdbcTemplate}'s query methods or for
   33 |  * {@code out} parameters of stored procedures. {@code RowMapper} objects are
   34 |  * typically stateless and thus reusable; they are an ideal choice for
   35 |  * implementing row-mapping logic in a single place.
   36 |  *
   37 |  * <p>Alternatively, consider subclassing
   38 |  * {@link org.springframework.jdbc.object.MappingSqlQuery} from the
   39 |  * {@code jdbc.object} package: instead of working with separate
   40 |  * {@code JdbcTemplate} and {@code RowMapper} objects, you can build executable
   41 |  * query objects (containing row-mapping logic) in that style.
   42 |  *
   43 |  * @author Thomas Risberg
   44 |  * @author Juergen Hoeller
   45 |  * @param <T> the result type
   46 |  * @see JdbcTemplate
   47 |  * @see RowCallbackHandler
   48 |  * @see ResultSetExtractor
   49 |  * @see org.springframework.jdbc.object.MappingSqlQuery
   50 |  */
   51 | @FunctionalInterface
   52 | public interface RowMapper<T extends @Nullable Object> {
   53 | 
   54 | 	/**
   55 | 	 * Implementations must implement this method to map each row of data in the
   56 | 	 * {@code ResultSet}. This method should not call {@code next()} on the
   57 | 	 * {@code ResultSet}; it is only supposed to map values of the current row.
   58 | 	 * @param rs the {@code ResultSet} to map (pre-initialized for the current row)
   59 | 	 * @param rowNum the number of the current row
   60 | 	 * @return the result object for the current row (may be {@code null})
   61 | 	 * @throws SQLException if an SQLException is encountered while getting
   62 | 	 * column values (that is, there's no need to catch SQLException)
   63 | 	 */
   64 | 	T mapRow(ResultSet rs, int rowNum) throws SQLException;
   65 | 
   66 | }
```
