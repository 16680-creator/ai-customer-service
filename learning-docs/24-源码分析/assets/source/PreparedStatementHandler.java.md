<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/statement/PreparedStatementHandler.java -->
<!-- 正文引用：[[源码:PreparedStatementHandler.java#L起-L止]] ；代码块头部：(src: PreparedStatementHandler.java#L起-L止) -->

# PreparedStatementHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/statement/PreparedStatementHandler.java` |
| 行数 | 100 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:21.935Z |

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
   19 | import java.sql.PreparedStatement;
   20 | import java.sql.ResultSet;
   21 | import java.sql.SQLException;
   22 | import java.sql.Statement;
   23 | import java.util.List;
   24 | 
   25 | import org.apache.ibatis.cursor.Cursor;
   26 | import org.apache.ibatis.executor.Executor;
   27 | import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
   28 | import org.apache.ibatis.executor.keygen.KeyGenerator;
   29 | import org.apache.ibatis.mapping.BoundSql;
   30 | import org.apache.ibatis.mapping.MappedStatement;
   31 | import org.apache.ibatis.mapping.ResultSetType;
   32 | import org.apache.ibatis.session.ResultHandler;
   33 | import org.apache.ibatis.session.RowBounds;
   34 | 
   35 | /**
   36 |  * @author Clinton Begin
   37 |  */
   38 | public class PreparedStatementHandler extends BaseStatementHandler {
   39 | 
   40 |   public PreparedStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter,
   41 |       RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
   42 |     super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
   43 |   }
   44 | 
   45 |   @Override
   46 |   public int update(Statement statement) throws SQLException {
   47 |     PreparedStatement ps = (PreparedStatement) statement;
   48 |     ps.execute();
   49 |     int rows = ps.getUpdateCount();
   50 |     Object parameterObject = boundSql.getParameterObject();
   51 |     KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
   52 |     keyGenerator.processAfter(executor, mappedStatement, ps, parameterObject);
   53 |     return rows;
   54 |   }
   55 | 
   56 |   @Override
   57 |   public void batch(Statement statement) throws SQLException {
   58 |     PreparedStatement ps = (PreparedStatement) statement;
   59 |     ps.addBatch();
   60 |   }
   61 | 
   62 |   @Override
   63 |   public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
   64 |     PreparedStatement ps = (PreparedStatement) statement;
   65 |     ps.execute();
   66 |     return resultSetHandler.handleResultSets(ps);
   67 |   }
   68 | 
   69 |   @Override
   70 |   public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
   71 |     PreparedStatement ps = (PreparedStatement) statement;
   72 |     ps.execute();
   73 |     return resultSetHandler.handleCursorResultSets(ps);
   74 |   }
   75 | 
   76 |   @Override
   77 |   protected Statement instantiateStatement(Connection connection) throws SQLException {
   78 |     String sql = boundSql.getSql();
   79 |     if (mappedStatement.getKeyGenerator() instanceof Jdbc3KeyGenerator) {
   80 |       String[] keyColumnNames = mappedStatement.getKeyColumns();
   81 |       if (keyColumnNames == null) {
   82 |         return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
   83 |       } else {
   84 |         return connection.prepareStatement(sql, keyColumnNames);
   85 |       }
   86 |     }
   87 |     if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
   88 |       return connection.prepareStatement(sql);
   89 |     } else {
   90 |       return connection.prepareStatement(sql, mappedStatement.getResultSetType().getValue(),
   91 |           ResultSet.CONCUR_READ_ONLY);
   92 |     }
   93 |   }
   94 | 
   95 |   @Override
   96 |   public void parameterize(Statement statement) throws SQLException {
   97 |     parameterHandler.setParameters((PreparedStatement) statement);
   98 |   }
   99 | 
  100 | }
```
