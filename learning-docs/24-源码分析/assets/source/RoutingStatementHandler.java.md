<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/statement/RoutingStatementHandler.java -->
<!-- 正文引用：[[源码:RoutingStatementHandler.java#L起-L止]] ；代码块头部：(src: RoutingStatementHandler.java#L起-L止) -->

# RoutingStatementHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/statement/RoutingStatementHandler.java` |
| 行数 | 97 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:21.752Z |

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
   24 | import org.apache.ibatis.executor.Executor;
   25 | import org.apache.ibatis.executor.ExecutorException;
   26 | import org.apache.ibatis.executor.parameter.ParameterHandler;
   27 | import org.apache.ibatis.mapping.BoundSql;
   28 | import org.apache.ibatis.mapping.MappedStatement;
   29 | import org.apache.ibatis.session.ResultHandler;
   30 | import org.apache.ibatis.session.RowBounds;
   31 | 
   32 | /**
   33 |  * @author Clinton Begin
   34 |  */
   35 | public class RoutingStatementHandler implements StatementHandler {
   36 | 
   37 |   private final StatementHandler delegate;
   38 | 
   39 |   public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
   40 |       ResultHandler resultHandler, BoundSql boundSql) {
   41 | 
   42 |     switch (ms.getStatementType()) {
   43 |       case STATEMENT:
   44 |         delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
   45 |         break;
   46 |       case PREPARED:
   47 |         delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
   48 |         break;
   49 |       case CALLABLE:
   50 |         delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
   51 |         break;
   52 |       default:
   53 |         throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
   54 |     }
   55 | 
   56 |   }
   57 | 
   58 |   @Override
   59 |   public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
   60 |     return delegate.prepare(connection, transactionTimeout);
   61 |   }
   62 | 
   63 |   @Override
   64 |   public void parameterize(Statement statement) throws SQLException {
   65 |     delegate.parameterize(statement);
   66 |   }
   67 | 
   68 |   @Override
   69 |   public void batch(Statement statement) throws SQLException {
   70 |     delegate.batch(statement);
   71 |   }
   72 | 
   73 |   @Override
   74 |   public int update(Statement statement) throws SQLException {
   75 |     return delegate.update(statement);
   76 |   }
   77 | 
   78 |   @Override
   79 |   public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
   80 |     return delegate.query(statement, resultHandler);
   81 |   }
   82 | 
   83 |   @Override
   84 |   public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
   85 |     return delegate.queryCursor(statement);
   86 |   }
   87 | 
   88 |   @Override
   89 |   public BoundSql getBoundSql() {
   90 |     return delegate.getBoundSql();
   91 |   }
   92 | 
   93 |   @Override
   94 |   public ParameterHandler getParameterHandler() {
   95 |     return delegate.getParameterHandler();
   96 |   }
   97 | }
```
