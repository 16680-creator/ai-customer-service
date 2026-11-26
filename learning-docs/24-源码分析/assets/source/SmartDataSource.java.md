<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/datasource/SmartDataSource.java -->
<!-- 正文引用：[[源码:SmartDataSource.java#L起-L止]] ；代码块头部：(src: SmartDataSource.java#L起-L止) -->

# SmartDataSource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/datasource/SmartDataSource.java` |
| 行数 | 52 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:04.432Z |

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
   17 | package org.springframework.jdbc.datasource;
   18 | 
   19 | import java.sql.Connection;
   20 | 
   21 | import javax.sql.DataSource;
   22 | 
   23 | /**
   24 |  * Extension of the {@code javax.sql.DataSource} interface, to be
   25 |  * implemented by special DataSources that return JDBC Connections
   26 |  * in an unwrapped fashion.
   27 |  *
   28 |  * <p>Classes using this interface can query whether the Connection
   29 |  * should be closed after an operation. Spring's DataSourceUtils and
   30 |  * JdbcTemplate classes automatically perform such a check.
   31 |  *
   32 |  * @author Rod Johnson
   33 |  * @author Juergen Hoeller
   34 |  * @see SingleConnectionDataSource#shouldClose
   35 |  * @see DataSourceUtils#releaseConnection
   36 |  * @see org.springframework.jdbc.core.JdbcTemplate
   37 |  */
   38 | public interface SmartDataSource extends DataSource {
   39 | 
   40 | 	/**
   41 | 	 * Should we close this Connection, obtained from this DataSource?
   42 | 	 * <p>Code that uses Connections from a SmartDataSource should always
   43 | 	 * perform a check via this method before invoking {@code close()}.
   44 | 	 * <p>Note that the JdbcTemplate class in the 'jdbc.core' package takes care of
   45 | 	 * releasing JDBC Connections, freeing application code of this responsibility.
   46 | 	 * @param con the Connection to check
   47 | 	 * @return whether the given Connection should be closed
   48 | 	 * @see java.sql.Connection#close()
   49 | 	 */
   50 | 	boolean shouldClose(Connection con);
   51 | 
   52 | }
```
