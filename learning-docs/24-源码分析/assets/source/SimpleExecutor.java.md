<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/SimpleExecutor.java -->
<!-- 正文引用：[[源码:SimpleExecutor.java#L起-L止]] ；代码块头部：(src: SimpleExecutor.java#L起-L止) -->

# SimpleExecutor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/SimpleExecutor.java` |
| 行数 | 94 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:21.547Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2026 the original author or authors.
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
   16 | package org.apache.ibatis.executor;
   17 | 
   18 | import java.sql.Connection;
   19 | import java.sql.SQLException;
   20 | import java.sql.Statement;
   21 | import java.util.List;
   22 | 
   23 | import org.apache.ibatis.cursor.Cursor;
   24 | import org.apache.ibatis.executor.statement.StatementHandler;
   25 | import org.apache.ibatis.logging.Log;
   26 | import org.apache.ibatis.mapping.BoundSql;
   27 | import org.apache.ibatis.mapping.MappedStatement;
   28 | import org.apache.ibatis.session.Configuration;
   29 | import org.apache.ibatis.session.ResultHandler;
   30 | import org.apache.ibatis.session.RowBounds;
   31 | import org.apache.ibatis.transaction.Transaction;
   32 | 
   33 | /**
   34 |  * @author Clinton Begin
   35 |  */
   36 | public class SimpleExecutor extends BaseExecutor {
   37 | 
   38 |   public SimpleExecutor(Configuration configuration, Transaction transaction) {
   39 |     super(configuration, transaction);
   40 |   }
   41 | 
   42 |   @Override
   43 |   public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
   44 |     Statement stmt = null;
   45 |     try {
   46 |       Configuration configuration = ms.getConfiguration();
   47 |       StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
   48 |       stmt = prepareStatement(handler, ms.getStatementLog());
   49 |       return handler.update(stmt);
   50 |     } finally {
   51 |       closeStatement(stmt);
   52 |     }
   53 |   }
   54 | 
   55 |   @Override
   56 |   public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler,
   57 |       BoundSql boundSql) throws SQLException {
   58 |     Statement stmt = null;
   59 |     try {
   60 |       Configuration configuration = ms.getConfiguration();
   61 |       StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler,
   62 |           boundSql);
   63 |       stmt = prepareStatement(handler, ms.getStatementLog());
   64 |       return handler.query(stmt, resultHandler);
   65 |     } finally {
   66 |       closeStatement(stmt);
   67 |     }
   68 |   }
   69 | 
   70 |   @Override
   71 |   protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql)
   72 |       throws SQLException {
   73 |     Configuration configuration = ms.getConfiguration();
   74 |     StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
   75 |     Statement stmt = prepareStatement(handler, ms.getStatementLog());
   76 |     Cursor<E> cursor = handler.queryCursor(stmt);
   77 |     stmt.closeOnCompletion();
   78 |     return cursor;
   79 |   }
   80 | 
   81 |   @Override
   82 |   public List<BatchResult> doFlushStatements(boolean isRollback) {
   83 |     return List.of();
   84 |   }
   85 | 
   86 |   private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
   87 |     Statement stmt;
   88 |     Connection connection = getConnection(statementLog);
   89 |     stmt = handler.prepare(connection, transaction.getTimeout());
   90 |     handler.parameterize(stmt);
   91 |     return stmt;
   92 |   }
   93 | 
   94 | }
```
