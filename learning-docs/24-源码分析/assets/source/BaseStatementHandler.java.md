<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/statement/BaseStatementHandler.java -->
<!-- 正文引用：[[源码:BaseStatementHandler.java#L起-L止]] ；代码块头部：(src: BaseStatementHandler.java#L起-L止) -->

# BaseStatementHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/statement/BaseStatementHandler.java` |
| 行数 | 147 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:30.707Z |

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
   21 | 
   22 | import org.apache.ibatis.executor.ErrorContext;
   23 | import org.apache.ibatis.executor.Executor;
   24 | import org.apache.ibatis.executor.ExecutorException;
   25 | import org.apache.ibatis.executor.keygen.KeyGenerator;
   26 | import org.apache.ibatis.executor.parameter.ParameterHandler;
   27 | import org.apache.ibatis.executor.resultset.ResultSetHandler;
   28 | import org.apache.ibatis.mapping.BoundSql;
   29 | import org.apache.ibatis.mapping.MappedStatement;
   30 | import org.apache.ibatis.reflection.factory.ObjectFactory;
   31 | import org.apache.ibatis.session.Configuration;
   32 | import org.apache.ibatis.session.ResultHandler;
   33 | import org.apache.ibatis.session.RowBounds;
   34 | import org.apache.ibatis.type.TypeHandlerRegistry;
   35 | 
   36 | /**
   37 |  * @author Clinton Begin
   38 |  */
   39 | public abstract class BaseStatementHandler implements StatementHandler {
   40 | 
   41 |   protected final Configuration configuration;
   42 |   protected final ObjectFactory objectFactory;
   43 |   protected final TypeHandlerRegistry typeHandlerRegistry;
   44 |   protected final ResultSetHandler resultSetHandler;
   45 |   protected final ParameterHandler parameterHandler;
   46 | 
   47 |   protected final Executor executor;
   48 |   protected final MappedStatement mappedStatement;
   49 |   protected final RowBounds rowBounds;
   50 | 
   51 |   protected BoundSql boundSql;
   52 | 
   53 |   protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject,
   54 |       RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
   55 |     this.configuration = mappedStatement.getConfiguration();
   56 |     this.executor = executor;
   57 |     this.mappedStatement = mappedStatement;
   58 |     this.rowBounds = rowBounds;
   59 | 
   60 |     this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
   61 |     this.objectFactory = configuration.getObjectFactory();
   62 | 
   63 |     if (boundSql == null) { // issue #435, get the key before calculating the statement
   64 |       generateKeys(parameterObject);
   65 |       boundSql = mappedStatement.getBoundSql(parameterObject);
   66 |     }
   67 | 
   68 |     this.boundSql = boundSql;
   69 | 
   70 |     this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
   71 |     this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler,
   72 |         resultHandler, boundSql);
   73 |   }
   74 | 
   75 |   @Override
   76 |   public BoundSql getBoundSql() {
   77 |     return boundSql;
   78 |   }
   79 | 
   80 |   @Override
   81 |   public ParameterHandler getParameterHandler() {
   82 |     return parameterHandler;
   83 |   }
   84 | 
   85 |   @Override
   86 |   public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
   87 |     ErrorContext.instance().sql(boundSql.getSql());
   88 |     Statement statement = null;
   89 |     try {
   90 |       statement = instantiateStatement(connection);
   91 |       setStatementTimeout(statement, transactionTimeout);
   92 |       setFetchSize(statement);
   93 |       return statement;
   94 |     } catch (SQLException e) {
   95 |       closeStatement(statement);
   96 |       throw e;
   97 |     } catch (Exception e) {
   98 |       closeStatement(statement);
   99 |       throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
  100 |     }
  101 |   }
  102 | 
  103 |   protected abstract Statement instantiateStatement(Connection connection) throws SQLException;
  104 | 
  105 |   protected void setStatementTimeout(Statement stmt, Integer transactionTimeout) throws SQLException {
  106 |     Integer queryTimeout = null;
  107 |     if (mappedStatement.getTimeout() != null) {
  108 |       queryTimeout = mappedStatement.getTimeout();
  109 |     } else if (configuration.getDefaultStatementTimeout() != null) {
  110 |       queryTimeout = configuration.getDefaultStatementTimeout();
  111 |     }
  112 |     if (queryTimeout != null) {
  113 |       stmt.setQueryTimeout(queryTimeout);
  114 |     }
  115 |     StatementUtil.applyTransactionTimeout(stmt, queryTimeout, transactionTimeout);
  116 |   }
  117 | 
  118 |   protected void setFetchSize(Statement stmt) throws SQLException {
  119 |     Integer fetchSize = mappedStatement.getFetchSize();
  120 |     if (fetchSize != null) {
  121 |       stmt.setFetchSize(fetchSize);
  122 |       return;
  123 |     }
  124 |     Integer defaultFetchSize = configuration.getDefaultFetchSize();
  125 |     if (defaultFetchSize != null) {
  126 |       stmt.setFetchSize(defaultFetchSize);
  127 |     }
  128 |   }
  129 | 
  130 |   protected void closeStatement(Statement statement) {
  131 |     try {
  132 |       if (statement != null) {
  133 |         statement.close();
  134 |       }
  135 |     } catch (SQLException e) {
  136 |       // ignore
  137 |     }
  138 |   }
  139 | 
  140 |   protected void generateKeys(Object parameter) {
  141 |     KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
  142 |     ErrorContext.instance().store();
  143 |     keyGenerator.processBefore(executor, mappedStatement, null, parameter);
  144 |     ErrorContext.instance().recall();
  145 |   }
  146 | 
  147 | }
```
