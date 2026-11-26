<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/core/PreparedStatementCallback.java -->
<!-- 正文引用：[[源码:PreparedStatementCallback.java#L起-L止]] ；代码块头部：(src: PreparedStatementCallback.java#L起-L止) -->

# PreparedStatementCallback.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/core/PreparedStatementCallback.java` |
| 行数 | 80 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:09.614Z |

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
   19 | import java.sql.PreparedStatement;
   20 | import java.sql.SQLException;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.dao.DataAccessException;
   25 | 
   26 | /**
   27 |  * Generic callback interface for code that operates on a PreparedStatement.
   28 |  * Allows to execute any number of operations on a single PreparedStatement,
   29 |  * for example a single {@code executeUpdate} call or repeated
   30 |  * {@code executeUpdate} calls with varying parameters.
   31 |  *
   32 |  * <p>Used internally by JdbcTemplate, but also useful for application code.
   33 |  * Note that the passed-in PreparedStatement can have been created by the
   34 |  * framework or by a custom PreparedStatementCreator. However, the latter is
   35 |  * hardly ever necessary, as most custom callback actions will perform updates
   36 |  * in which case a standard PreparedStatement is fine. Custom actions will
   37 |  * always set parameter values themselves, so that PreparedStatementCreator
   38 |  * capability is not needed either.
   39 |  *
   40 |  * @author Juergen Hoeller
   41 |  * @since 16.03.2004
   42 |  * @param <T> the result type
   43 |  * @see JdbcTemplate#execute(String, PreparedStatementCallback)
   44 |  * @see JdbcTemplate#execute(PreparedStatementCreator, PreparedStatementCallback)
   45 |  */
   46 | @FunctionalInterface
   47 | public interface PreparedStatementCallback<T extends @Nullable Object> {
   48 | 
   49 | 	/**
   50 | 	 * Gets called by {@code JdbcTemplate.execute} with an active JDBC
   51 | 	 * PreparedStatement. Does not need to care about closing the Statement
   52 | 	 * or the Connection, or about handling transactions: this will all be
   53 | 	 * handled by Spring's JdbcTemplate.
   54 | 	 * <p><b>NOTE:</b> Any ResultSets opened should be closed in finally blocks
   55 | 	 * within the callback implementation. Spring will close the Statement
   56 | 	 * object after the callback returned, but this does not necessarily imply
   57 | 	 * that the ResultSet resources will be closed: the Statement objects might
   58 | 	 * get pooled by the connection pool, with {@code close} calls only
   59 | 	 * returning the object to the pool but not physically closing the resources.
   60 | 	 * <p>If called without a thread-bound JDBC transaction (initiated by
   61 | 	 * DataSourceTransactionManager), the code will simply get executed on the
   62 | 	 * JDBC connection with its transactional semantics. If JdbcTemplate is
   63 | 	 * configured to use a JTA-aware DataSource, the JDBC connection and thus
   64 | 	 * the callback code will be transactional if a JTA transaction is active.
   65 | 	 * <p>Allows for returning a result object created within the callback, i.e.
   66 | 	 * a domain object or a collection of domain objects. Note that there's
   67 | 	 * special support for single step actions: see JdbcTemplate.queryForObject etc.
   68 | 	 * A thrown RuntimeException is treated as application exception, it gets
   69 | 	 * propagated to the caller of the template.
   70 | 	 * @param ps active JDBC PreparedStatement
   71 | 	 * @return a result object, or {@code null} if none
   72 | 	 * @throws SQLException if thrown by a JDBC method, to be auto-converted
   73 | 	 * to a DataAccessException by an SQLExceptionTranslator
   74 | 	 * @throws DataAccessException in case of custom exceptions
   75 | 	 * @see JdbcTemplate#queryForObject(String, Class, Object...)
   76 | 	 * @see JdbcTemplate#queryForList(String, Object...)
   77 | 	 */
   78 | 	T doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException;
   79 | 
   80 | }
```
