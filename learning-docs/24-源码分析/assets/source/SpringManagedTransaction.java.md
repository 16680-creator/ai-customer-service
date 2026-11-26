<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/transaction/SpringManagedTransaction.java -->
<!-- 正文引用：[[源码:SpringManagedTransaction.java#L起-L止]] ；代码块头部：(src: SpringManagedTransaction.java#L起-L止) -->

# SpringManagedTransaction.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/transaction/SpringManagedTransaction.java` |
| 行数 | 115 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:01:25.910Z |

```java
    1 | /*
    2 |  * Copyright 2010-2024 the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *    https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | package org.mybatis.spring.transaction;
   17 | 
   18 | import static org.springframework.util.Assert.notNull;
   19 | 
   20 | import java.sql.Connection;
   21 | import java.sql.SQLException;
   22 | 
   23 | import javax.sql.DataSource;
   24 | 
   25 | import org.apache.ibatis.transaction.Transaction;
   26 | import org.mybatis.logging.Logger;
   27 | import org.mybatis.logging.LoggerFactory;
   28 | import org.springframework.jdbc.datasource.ConnectionHolder;
   29 | import org.springframework.jdbc.datasource.DataSourceUtils;
   30 | import org.springframework.transaction.support.TransactionSynchronizationManager;
   31 | 
   32 | /**
   33 |  * {@code SpringManagedTransaction} handles the lifecycle of a JDBC connection. It retrieves a connection from Spring's
   34 |  * transaction manager and returns it back to it when it is no longer needed.
   35 |  * <p>
   36 |  * If Spring's transaction handling is active it will no-op all commit/rollback/close calls assuming that the Spring
   37 |  * transaction manager will do the job.
   38 |  * <p>
   39 |  * If it is not it will behave like {@code JdbcTransaction}.
   40 |  *
   41 |  * @author Hunter Presnall
   42 |  * @author Eduardo Macarron
   43 |  */
   44 | public class SpringManagedTransaction implements Transaction {
   45 | 
   46 |   private static final Logger LOGGER = LoggerFactory.getLogger(SpringManagedTransaction.class);
   47 | 
   48 |   private final DataSource dataSource;
   49 | 
   50 |   private Connection connection;
   51 | 
   52 |   private boolean isConnectionTransactional;
   53 | 
   54 |   private boolean autoCommit;
   55 | 
   56 |   public SpringManagedTransaction(DataSource dataSource) {
   57 |     notNull(dataSource, "No DataSource specified");
   58 |     this.dataSource = dataSource;
   59 |   }
   60 | 
   61 |   @Override
   62 |   public Connection getConnection() throws SQLException {
   63 |     if (this.connection == null) {
   64 |       openConnection();
   65 |     }
   66 |     return this.connection;
   67 |   }
   68 | 
   69 |   /**
   70 |    * Gets a connection from Spring transaction manager and discovers if this {@code Transaction} should manage
   71 |    * connection or let it to Spring.
   72 |    * <p>
   73 |    * It also reads autocommit setting because when using Spring Transaction MyBatis thinks that autocommit is always
   74 |    * false and will always call commit/rollback so we need to no-op that calls.
   75 |    */
   76 |   private void openConnection() throws SQLException {
   77 |     this.connection = DataSourceUtils.getConnection(this.dataSource);
   78 |     this.autoCommit = this.connection.getAutoCommit();
   79 |     this.isConnectionTransactional = DataSourceUtils.isConnectionTransactional(this.connection, this.dataSource);
   80 | 
   81 |     LOGGER.debug(() -> "JDBC Connection [" + this.connection + "] will"
   82 |         + (this.isConnectionTransactional ? " " : " not ") + "be managed by Spring");
   83 |   }
   84 | 
   85 |   @Override
   86 |   public void commit() throws SQLException {
   87 |     if (this.connection != null && !this.isConnectionTransactional && !this.autoCommit) {
   88 |       LOGGER.debug(() -> "Committing JDBC Connection [" + this.connection + "]");
   89 |       this.connection.commit();
   90 |     }
   91 |   }
   92 | 
   93 |   @Override
   94 |   public void rollback() throws SQLException {
   95 |     if (this.connection != null && !this.isConnectionTransactional && !this.autoCommit) {
   96 |       LOGGER.debug(() -> "Rolling back JDBC Connection [" + this.connection + "]");
   97 |       this.connection.rollback();
   98 |     }
   99 |   }
  100 | 
  101 |   @Override
  102 |   public void close() throws SQLException {
  103 |     DataSourceUtils.releaseConnection(this.connection, this.dataSource);
  104 |   }
  105 | 
  106 |   @Override
  107 |   public Integer getTimeout() throws SQLException {
  108 |     var holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
  109 |     if (holder != null && holder.hasTimeout()) {
  110 |       return holder.getTimeToLiveInSeconds();
  111 |     }
  112 |     return null;
  113 |   }
  114 | 
  115 | }
```
