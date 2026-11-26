<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/statement/StatementHandler.java -->
<!-- 正文引用：[[源码:StatementHandler.java#L起-L止]] ；代码块头部：(src: StatementHandler.java#L起-L止) -->

# StatementHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/statement/StatementHandler.java` |
| 行数 | 49 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:30.494Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2023 the original author or authors.
    3 |  *
    4 |  *    Licensed under the Apache License, Version 2.0 (the "License");
    5 |  *    you may not use this file except in compliance with the License.
    6 |  *    You may obtain a copy of the License at
    7 |  *
    8 |  *       https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  *    Unless required by applicable law or agreed to in writing, software
   11 |  *    distributed under the License is distributed on an "AS IS" BASIS,
   12 |  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  *    See the License for the specific language governing permissions and
   14 |  *    limitations under the License.
   15 |  */
   16 | package org.apache.ibatis.executor.statement;
   17 | 
   18 | import java.sql.Connection;
   19 | import java.sql.SQLException;
   20 | import java.sql.Statement;
   21 | import java.util.List;
   22 | 
   23 | import org.apache.ibatis.cursor.Cursor;
   24 | import org.apache.ibatis.executor.parameter.ParameterHandler;
   25 | import org.apache.ibatis.mapping.BoundSql;
   26 | import org.apache.ibatis.session.ResultHandler;
   27 | 
   28 | /**
   29 |  * @author Clinton Begin
   30 |  */
   31 | public interface StatementHandler {
   32 | 
   33 |   Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException;
   34 | 
   35 |   void parameterize(Statement statement) throws SQLException;
   36 | 
   37 |   void batch(Statement statement) throws SQLException;
   38 | 
   39 |   int update(Statement statement) throws SQLException;
   40 | 
   41 |   <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException;
   42 | 
   43 |   <E> Cursor<E> queryCursor(Statement statement) throws SQLException;
   44 | 
   45 |   BoundSql getBoundSql();
   46 | 
   47 |   ParameterHandler getParameterHandler();
   48 | 
   49 | }
```
