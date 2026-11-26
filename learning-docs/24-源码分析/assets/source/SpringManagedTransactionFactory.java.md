<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/transaction/SpringManagedTransactionFactory.java -->
<!-- 正文引用：[[源码:SpringManagedTransactionFactory.java#L起-L止]] ；代码块头部：(src: SpringManagedTransactionFactory.java#L起-L止) -->

# SpringManagedTransactionFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/transaction/SpringManagedTransactionFactory.java` |
| 行数 | 49 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:20:11.469Z |

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
   18 | import java.sql.Connection;
   19 | import java.util.Properties;
   20 | 
   21 | import javax.sql.DataSource;
   22 | 
   23 | import org.apache.ibatis.session.TransactionIsolationLevel;
   24 | import org.apache.ibatis.transaction.Transaction;
   25 | import org.apache.ibatis.transaction.TransactionFactory;
   26 | 
   27 | /**
   28 |  * Creates a {@code SpringManagedTransaction}.
   29 |  *
   30 |  * @author Hunter Presnall
   31 |  */
   32 | public class SpringManagedTransactionFactory implements TransactionFactory {
   33 | 
   34 |   @Override
   35 |   public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
   36 |     return new SpringManagedTransaction(dataSource);
   37 |   }
   38 | 
   39 |   @Override
   40 |   public Transaction newTransaction(Connection conn) {
   41 |     throw new UnsupportedOperationException("New Spring transactions require a DataSource");
   42 |   }
   43 | 
   44 |   @Override
   45 |   public void setProperties(Properties props) {
   46 |     // not needed in this version
   47 |   }
   48 | 
   49 | }
```
