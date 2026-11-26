<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/core/PreparedStatementCreator.java -->
<!-- 正文引用：[[源码:PreparedStatementCreator.java#L起-L止]] ；代码块头部：(src: PreparedStatementCreator.java#L起-L止) -->

# PreparedStatementCreator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/core/PreparedStatementCreator.java` |
| 行数 | 57 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:10.186Z |

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
   19 | import java.sql.Connection;
   20 | import java.sql.PreparedStatement;
   21 | import java.sql.SQLException;
   22 | 
   23 | /**
   24 |  * One of the two central callback interfaces used by the JdbcTemplate class.
   25 |  * This interface creates a PreparedStatement given a connection, provided
   26 |  * by the JdbcTemplate class. Implementations are responsible for providing
   27 |  * SQL and any necessary parameters.
   28 |  *
   29 |  * <p>Implementations <i>do not</i> need to concern themselves with
   30 |  * SQLExceptions that may be thrown from operations they attempt.
   31 |  * The JdbcTemplate class will catch and handle SQLExceptions appropriately.
   32 |  *
   33 |  * <p>A PreparedStatementCreator should also implement the SqlProvider interface
   34 |  * if it is able to provide the SQL it uses for PreparedStatement creation.
   35 |  * This allows for better contextual information in case of exceptions.
   36 |  *
   37 |  * @author Rod Johnson
   38 |  * @see JdbcTemplate#execute(PreparedStatementCreator, PreparedStatementCallback)
   39 |  * @see JdbcTemplate#query(PreparedStatementCreator, RowCallbackHandler)
   40 |  * @see JdbcTemplate#update(PreparedStatementCreator)
   41 |  * @see SqlProvider
   42 |  */
   43 | @FunctionalInterface
   44 | public interface PreparedStatementCreator {
   45 | 
   46 | 	/**
   47 | 	 * Create a statement in this connection. Allows implementations to use
   48 | 	 * PreparedStatements. The JdbcTemplate will close the created statement.
   49 | 	 * @param con the connection used to create statement
   50 | 	 * @return a prepared statement
   51 | 	 * @throws SQLException there is no need to catch SQLExceptions
   52 | 	 * that may be thrown in the implementation of this method.
   53 | 	 * The JdbcTemplate class will handle them.
   54 | 	 */
   55 | 	PreparedStatement createPreparedStatement(Connection con) throws SQLException;
   56 | 
   57 | }
```
