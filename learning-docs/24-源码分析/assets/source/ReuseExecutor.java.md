<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/ReuseExecutor.java -->
<!-- 正文引用：[[源码:ReuseExecutor.java#L起-L止]] ；代码块头部：(src: ReuseExecutor.java#L起-L止) -->

# ReuseExecutor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/ReuseExecutor.java` |
| 行数 | 115 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:31.487Z |

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
   21 | import java.util.HashMap;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | 
   25 | import org.apache.ibatis.cursor.Cursor;
   26 | import org.apache.ibatis.executor.statement.StatementHandler;
   27 | import org.apache.ibatis.logging.Log;
   28 | import org.apache.ibatis.mapping.BoundSql;
   29 | import org.apache.ibatis.mapping.MappedStatement;
   30 | import org.apache.ibatis.session.Configuration;
   31 | import org.apache.ibatis.session.ResultHandler;
   32 | import org.apache.ibatis.session.RowBounds;
   33 | import org.apache.ibatis.transaction.Transaction;
   34 | 
   35 | /**
   36 |  * @author Clinton Begin
   37 |  */
   38 | public class ReuseExecutor extends BaseExecutor {
   39 | 
   40 |   private final Map<String, Statement> statementMap = new HashMap<>();
   41 | 
   42 |   public ReuseExecutor(Configuration configuration, Transaction transaction) {
   43 |     super(configuration, transaction);
   44 |   }
   45 | 
   46 |   @Override
   47 |   public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
   48 |     Configuration configuration = ms.getConfiguration();
   49 |     StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
   50 |     Statement stmt = prepareStatement(handler, ms.getStatementLog());
   51 |     return handler.update(stmt);
   52 |   }
   53 | 
   54 |   @Override
   55 |   public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler,
   56 |       BoundSql boundSql) throws SQLException {
   57 |     Configuration configuration = ms.getConfiguration();
   58 |     StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler,
   59 |         boundSql);
   60 |     Statement stmt = prepareStatement(handler, ms.getStatementLog());
   61 |     return handler.query(stmt, resultHandler);
   62 |   }
   63 | 
   64 |   @Override
   65 |   protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql)
   66 |       throws SQLException {
   67 |     Configuration configuration = ms.getConfiguration();
   68 |     StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
   69 |     Statement stmt = prepareStatement(handler, ms.getStatementLog());
   70 |     return handler.queryCursor(stmt);
   71 |   }
   72 | 
   73 |   @Override
   74 |   public List<BatchResult> doFlushStatements(boolean isRollback) {
   75 |     for (Statement stmt : statementMap.values()) {
   76 |       closeStatement(stmt);
   77 |     }
   78 |     statementMap.clear();
   79 |     return List.of();
   80 |   }
   81 | 
   82 |   private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
   83 |     Statement stmt;
   84 |     BoundSql boundSql = handler.getBoundSql();
   85 |     String sql = boundSql.getSql();
   86 |     if (hasStatementFor(sql)) {
   87 |       stmt = getStatement(sql);
   88 |       applyTransactionTimeout(stmt);
   89 |     } else {
   90 |       Connection connection = getConnection(statementLog);
   91 |       stmt = handler.prepare(connection, transaction.getTimeout());
   92 |       putStatement(sql, stmt);
   93 |     }
   94 |     handler.parameterize(stmt);
   95 |     return stmt;
   96 |   }
   97 | 
   98 |   private boolean hasStatementFor(String sql) {
   99 |     try {
  100 |       Statement statement = statementMap.get(sql);
  101 |       return statement != null && !statement.getConnection().isClosed();
  102 |     } catch (SQLException e) {
  103 |       return false;
  104 |     }
  105 |   }
  106 | 
  107 |   private Statement getStatement(String s) {
  108 |     return statementMap.get(s);
  109 |   }
  110 | 
  111 |   private void putStatement(String sql, Statement stmt) {
  112 |     statementMap.put(sql, stmt);
  113 |   }
  114 | 
  115 | }
```
