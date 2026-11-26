<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/BatchExecutor.java -->
<!-- 正文引用：[[源码:BatchExecutor.java#L起-L止]] ；代码块头部：(src: BatchExecutor.java#L起-L止) -->

# BatchExecutor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/BatchExecutor.java` |
| 行数 | 162 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:31.664Z |

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
   18 | import java.sql.BatchUpdateException;
   19 | import java.sql.Connection;
   20 | import java.sql.SQLException;
   21 | import java.sql.Statement;
   22 | import java.util.ArrayList;
   23 | import java.util.List;
   24 | 
   25 | import org.apache.ibatis.cursor.Cursor;
   26 | import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
   27 | import org.apache.ibatis.executor.keygen.KeyGenerator;
   28 | import org.apache.ibatis.executor.keygen.NoKeyGenerator;
   29 | import org.apache.ibatis.executor.statement.StatementHandler;
   30 | import org.apache.ibatis.mapping.BoundSql;
   31 | import org.apache.ibatis.mapping.MappedStatement;
   32 | import org.apache.ibatis.session.Configuration;
   33 | import org.apache.ibatis.session.ResultHandler;
   34 | import org.apache.ibatis.session.RowBounds;
   35 | import org.apache.ibatis.transaction.Transaction;
   36 | 
   37 | /**
   38 |  * @author Jeff Butler
   39 |  */
   40 | public class BatchExecutor extends BaseExecutor {
   41 | 
   42 |   public static final int BATCH_UPDATE_RETURN_VALUE = Integer.MIN_VALUE + 1002;
   43 | 
   44 |   private final List<Statement> statementList = new ArrayList<>();
   45 |   private final List<BatchResult> batchResultList = new ArrayList<>();
   46 |   private String currentSql;
   47 |   private MappedStatement currentStatement;
   48 | 
   49 |   public BatchExecutor(Configuration configuration, Transaction transaction) {
   50 |     super(configuration, transaction);
   51 |   }
   52 | 
   53 |   @Override
   54 |   public int doUpdate(MappedStatement ms, Object parameterObject) throws SQLException {
   55 |     final Configuration configuration = ms.getConfiguration();
   56 |     final StatementHandler handler = configuration.newStatementHandler(this, ms, parameterObject, RowBounds.DEFAULT,
   57 |         null, null);
   58 |     final BoundSql boundSql = handler.getBoundSql();
   59 |     final String sql = boundSql.getSql();
   60 |     final Statement stmt;
   61 |     if (sql.equals(currentSql) && ms.equals(currentStatement)) {
   62 |       int last = statementList.size() - 1;
   63 |       stmt = statementList.get(last);
   64 |       applyTransactionTimeout(stmt);
   65 |       handler.parameterize(stmt);// fix Issues 322
   66 |       BatchResult batchResult = batchResultList.get(last);
   67 |       batchResult.addParameterObject(parameterObject);
   68 |     } else {
   69 |       Connection connection = getConnection(ms.getStatementLog());
   70 |       stmt = handler.prepare(connection, transaction.getTimeout());
   71 |       handler.parameterize(stmt); // fix Issues 322
   72 |       currentSql = sql;
   73 |       currentStatement = ms;
   74 |       statementList.add(stmt);
   75 |       batchResultList.add(new BatchResult(ms, sql, parameterObject));
   76 |     }
   77 |     handler.batch(stmt);
   78 |     return BATCH_UPDATE_RETURN_VALUE;
   79 |   }
   80 | 
   81 |   @Override
   82 |   public <E> List<E> doQuery(MappedStatement ms, Object parameterObject, RowBounds rowBounds,
   83 |       ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
   84 |     Statement stmt = null;
   85 |     try {
   86 |       flushStatements();
   87 |       Configuration configuration = ms.getConfiguration();
   88 |       StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameterObject, rowBounds,
   89 |           resultHandler, boundSql);
   90 |       Connection connection = getConnection(ms.getStatementLog());
   91 |       stmt = handler.prepare(connection, transaction.getTimeout());
   92 |       handler.parameterize(stmt);
   93 |       return handler.query(stmt, resultHandler);
   94 |     } finally {
   95 |       closeStatement(stmt);
   96 |     }
   97 |   }
   98 | 
   99 |   @Override
  100 |   protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql)
  101 |       throws SQLException {
  102 |     flushStatements();
  103 |     Configuration configuration = ms.getConfiguration();
  104 |     StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
  105 |     Connection connection = getConnection(ms.getStatementLog());
  106 |     Statement stmt = handler.prepare(connection, transaction.getTimeout());
  107 |     handler.parameterize(stmt);
  108 |     Cursor<E> cursor = handler.queryCursor(stmt);
  109 |     stmt.closeOnCompletion();
  110 |     return cursor;
  111 |   }
  112 | 
  113 |   @Override
  114 |   public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
  115 |     try {
  116 |       List<BatchResult> results = new ArrayList<>();
  117 |       if (isRollback) {
  118 |         return List.of();
  119 |       }
  120 |       for (int i = 0, n = statementList.size(); i < n; i++) {
  121 |         Statement stmt = statementList.get(i);
  122 |         applyTransactionTimeout(stmt);
  123 |         BatchResult batchResult = batchResultList.get(i);
  124 |         try {
  125 |           batchResult.setUpdateCounts(stmt.executeBatch());
  126 |           MappedStatement ms = batchResult.getMappedStatement();
  127 |           List<Object> parameterObjects = batchResult.getParameterObjects();
  128 |           KeyGenerator keyGenerator = ms.getKeyGenerator();
  129 |           if (Jdbc3KeyGenerator.class.equals(keyGenerator.getClass())) {
  130 |             Jdbc3KeyGenerator jdbc3KeyGenerator = (Jdbc3KeyGenerator) keyGenerator;
  131 |             jdbc3KeyGenerator.processBatch(ms, stmt, parameterObjects);
  132 |           } else if (!NoKeyGenerator.class.equals(keyGenerator.getClass())) { // issue #141
  133 |             for (Object parameter : parameterObjects) {
  134 |               keyGenerator.processAfter(this, ms, stmt, parameter);
  135 |             }
  136 |           }
  137 |           // Close statement to close cursor #1109
  138 |           closeStatement(stmt);
  139 |         } catch (BatchUpdateException e) {
  140 |           StringBuilder message = new StringBuilder();
  141 |           message.append(batchResult.getMappedStatement().getId()).append(" (batch index #").append(i + 1).append(")")
  142 |               .append(" failed.");
  143 |           if (i > 0) {
  144 |             message.append(" ").append(i)
  145 |                 .append(" prior sub executor(s) completed successfully, but will be rolled back.");
  146 |           }
  147 |           throw new BatchExecutorException(message.toString(), e, results, batchResult);
  148 |         }
  149 |         results.add(batchResult);
  150 |       }
  151 |       return results;
  152 |     } finally {
  153 |       for (Statement stmt : statementList) {
  154 |         closeStatement(stmt);
  155 |       }
  156 |       currentSql = null;
  157 |       statementList.clear();
  158 |       batchResultList.clear();
  159 |     }
  160 |   }
  161 | 
  162 | }
```
