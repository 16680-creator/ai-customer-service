<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/session/defaults/DefaultSqlSessionFactory.java -->
<!-- 正文引用：[[源码:DefaultSqlSessionFactory.java#L起-L止]] ；代码块头部：(src: DefaultSqlSessionFactory.java#L起-L止) -->

# DefaultSqlSessionFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/session/defaults/DefaultSqlSessionFactory.java` |
| 行数 | 150 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:18.370Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2024 the original author or authors.
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
   16 | package org.apache.ibatis.session.defaults;
   17 | 
   18 | import java.sql.Connection;
   19 | import java.sql.SQLException;
   20 | 
   21 | import org.apache.ibatis.exceptions.ExceptionFactory;
   22 | import org.apache.ibatis.executor.ErrorContext;
   23 | import org.apache.ibatis.executor.Executor;
   24 | import org.apache.ibatis.mapping.Environment;
   25 | import org.apache.ibatis.session.Configuration;
   26 | import org.apache.ibatis.session.ExecutorType;
   27 | import org.apache.ibatis.session.SqlSession;
   28 | import org.apache.ibatis.session.SqlSessionFactory;
   29 | import org.apache.ibatis.session.TransactionIsolationLevel;
   30 | import org.apache.ibatis.transaction.Transaction;
   31 | import org.apache.ibatis.transaction.TransactionFactory;
   32 | import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
   33 | 
   34 | /**
   35 |  * @author Clinton Begin
   36 |  */
   37 | public class DefaultSqlSessionFactory implements SqlSessionFactory {
   38 | 
   39 |   private final Configuration configuration;
   40 | 
   41 |   public DefaultSqlSessionFactory(Configuration configuration) {
   42 |     this.configuration = configuration;
   43 |   }
   44 | 
   45 |   @Override
   46 |   public SqlSession openSession() {
   47 |     return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
   48 |   }
   49 | 
   50 |   @Override
   51 |   public SqlSession openSession(boolean autoCommit) {
   52 |     return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
   53 |   }
   54 | 
   55 |   @Override
   56 |   public SqlSession openSession(ExecutorType execType) {
   57 |     return openSessionFromDataSource(execType, null, false);
   58 |   }
   59 | 
   60 |   @Override
   61 |   public SqlSession openSession(TransactionIsolationLevel level) {
   62 |     return openSessionFromDataSource(configuration.getDefaultExecutorType(), level, false);
   63 |   }
   64 | 
   65 |   @Override
   66 |   public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
   67 |     return openSessionFromDataSource(execType, level, false);
   68 |   }
   69 | 
   70 |   @Override
   71 |   public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
   72 |     return openSessionFromDataSource(execType, null, autoCommit);
   73 |   }
   74 | 
   75 |   @Override
   76 |   public SqlSession openSession(Connection connection) {
   77 |     return openSessionFromConnection(configuration.getDefaultExecutorType(), connection);
   78 |   }
   79 | 
   80 |   @Override
   81 |   public SqlSession openSession(ExecutorType execType, Connection connection) {
   82 |     return openSessionFromConnection(execType, connection);
   83 |   }
   84 | 
   85 |   @Override
   86 |   public Configuration getConfiguration() {
   87 |     return configuration;
   88 |   }
   89 | 
   90 |   protected SqlSession createSqlSession(Configuration configuration, Executor executor, boolean autoCommit) {
   91 |     return new DefaultSqlSession(configuration, executor, autoCommit);
   92 |   }
   93 | 
   94 |   private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level,
   95 |       boolean autoCommit) {
   96 |     Transaction tx = null;
   97 |     try {
   98 |       final Environment environment = configuration.getEnvironment();
   99 |       final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
  100 |       tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
  101 |       final Executor executor = configuration.newExecutor(tx, execType);
  102 |       return createSqlSession(configuration, executor, autoCommit);
  103 |     } catch (Exception e) {
  104 |       closeTransaction(tx); // may have fetched a connection so lets call close()
  105 |       throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
  106 |     } finally {
  107 |       ErrorContext.instance().reset();
  108 |     }
  109 |   }
  110 | 
  111 |   private SqlSession openSessionFromConnection(ExecutorType execType, Connection connection) {
  112 |     try {
  113 |       boolean autoCommit;
  114 |       try {
  115 |         autoCommit = connection.getAutoCommit();
  116 |       } catch (SQLException e) {
  117 |         // Failover to true, as most poor drivers
  118 |         // or databases won't support transactions
  119 |         autoCommit = true;
  120 |       }
  121 |       final Environment environment = configuration.getEnvironment();
  122 |       final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
  123 |       final Transaction tx = transactionFactory.newTransaction(connection);
  124 |       final Executor executor = configuration.newExecutor(tx, execType);
  125 |       return createSqlSession(configuration, executor, autoCommit);
  126 |     } catch (Exception e) {
  127 |       throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
  128 |     } finally {
  129 |       ErrorContext.instance().reset();
  130 |     }
  131 |   }
  132 | 
  133 |   private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
  134 |     if (environment == null || environment.getTransactionFactory() == null) {
  135 |       return new ManagedTransactionFactory();
  136 |     }
  137 |     return environment.getTransactionFactory();
  138 |   }
  139 | 
  140 |   private void closeTransaction(Transaction tx) {
  141 |     if (tx != null) {
  142 |       try {
  143 |         tx.close();
  144 |       } catch (SQLException ignore) {
  145 |         // Intentionally ignore. Prefer previous error.
  146 |       }
  147 |     }
  148 |   }
  149 | 
  150 | }
```
