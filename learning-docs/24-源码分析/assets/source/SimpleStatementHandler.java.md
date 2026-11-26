<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/statement/SimpleStatementHandler.java -->
<!-- 正文引用：[[源码:SimpleStatementHandler.java#L起-L止]] ；代码块头部：(src: SimpleStatementHandler.java#L起-L止) -->

# SimpleStatementHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/statement/SimpleStatementHandler.java` |
| 行数 | 99 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:32.345Z |

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
   19 | import java.sql.ResultSet;
   20 | import java.sql.SQLException;
   21 | import java.sql.Statement;
   22 | import java.util.List;
   23 | 
   24 | import org.apache.ibatis.cursor.Cursor;
   25 | import org.apache.ibatis.executor.Executor;
   26 | import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
   27 | import org.apache.ibatis.executor.keygen.KeyGenerator;
   28 | import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
   29 | import org.apache.ibatis.mapping.BoundSql;
   30 | import org.apache.ibatis.mapping.MappedStatement;
   31 | import org.apache.ibatis.mapping.ResultSetType;
   32 | import org.apache.ibatis.session.ResultHandler;
   33 | import org.apache.ibatis.session.RowBounds;
   34 | 
   35 | /**
   36 |  * @author Clinton Begin
   37 |  */
   38 | public class SimpleStatementHandler extends BaseStatementHandler {
   39 | 
   40 |   public SimpleStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter,
   41 |       RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
   42 |     super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
   43 |   }
   44 | 
   45 |   @Override
   46 |   public int update(Statement statement) throws SQLException {
   47 |     String sql = boundSql.getSql();
   48 |     Object parameterObject = boundSql.getParameterObject();
   49 |     KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
   50 |     int rows;
   51 |     if (keyGenerator instanceof Jdbc3KeyGenerator) {
   52 |       statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
   53 |       rows = statement.getUpdateCount();
   54 |       keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
   55 |     } else if (keyGenerator instanceof SelectKeyGenerator) {
   56 |       statement.execute(sql);
   57 |       rows = statement.getUpdateCount();
   58 |       keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
   59 |     } else {
   60 |       statement.execute(sql);
   61 |       rows = statement.getUpdateCount();
   62 |     }
   63 |     return rows;
   64 |   }
   65 | 
   66 |   @Override
   67 |   public void batch(Statement statement) throws SQLException {
   68 |     String sql = boundSql.getSql();
   69 |     statement.addBatch(sql);
   70 |   }
   71 | 
   72 |   @Override
   73 |   public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
   74 |     String sql = boundSql.getSql();
   75 |     statement.execute(sql);
   76 |     return resultSetHandler.handleResultSets(statement);
   77 |   }
   78 | 
   79 |   @Override
   80 |   public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
   81 |     String sql = boundSql.getSql();
   82 |     statement.execute(sql);
   83 |     return resultSetHandler.handleCursorResultSets(statement);
   84 |   }
   85 | 
   86 |   @Override
   87 |   protected Statement instantiateStatement(Connection connection) throws SQLException {
   88 |     if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
   89 |       return connection.createStatement();
   90 |     }
   91 |     return connection.createStatement(mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
   92 |   }
   93 | 
   94 |   @Override
   95 |   public void parameterize(Statement statement) {
   96 |     // N/A
   97 |   }
   98 | 
   99 | }
```
