<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/MyBatisExceptionTranslator.java -->
<!-- 正文引用：[[源码:MyBatisExceptionTranslator.java#L起-L止]] ；代码块头部：(src: MyBatisExceptionTranslator.java#L起-L止) -->

# MyBatisExceptionTranslator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/MyBatisExceptionTranslator.java` |
| 行数 | 120 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:01:26.381Z |

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
   16 | package org.mybatis.spring;
   17 | 
   18 | import java.sql.SQLException;
   19 | import java.util.concurrent.locks.ReentrantLock;
   20 | import java.util.function.Supplier;
   21 | 
   22 | import javax.sql.DataSource;
   23 | 
   24 | import org.apache.ibatis.exceptions.PersistenceException;
   25 | import org.springframework.dao.DataAccessException;
   26 | import org.springframework.dao.support.PersistenceExceptionTranslator;
   27 | import org.springframework.jdbc.UncategorizedSQLException;
   28 | import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
   29 | import org.springframework.jdbc.support.SQLExceptionTranslator;
   30 | import org.springframework.transaction.TransactionException;
   31 | 
   32 | /**
   33 |  * Default exception translator.
   34 |  * <p>
   35 |  * Translates MyBatis SqlSession returned exception into a Spring {@code DataAccessException} using Spring's
   36 |  * {@code SQLExceptionTranslator} Can load {@code SQLExceptionTranslator} eagerly or when the first exception is
   37 |  * translated.
   38 |  *
   39 |  * @author Eduardo Macarron
   40 |  */
   41 | public class MyBatisExceptionTranslator implements PersistenceExceptionTranslator {
   42 | 
   43 |   private final Supplier<SQLExceptionTranslator> exceptionTranslatorSupplier;
   44 |   private SQLExceptionTranslator exceptionTranslator;
   45 |   private ReentrantLock lock = new ReentrantLock();
   46 | 
   47 |   /**
   48 |    * Creates a new {@code PersistenceExceptionTranslator} instance with {@code SQLErrorCodeSQLExceptionTranslator}.
   49 |    *
   50 |    * @param dataSource
   51 |    *          DataSource to use to find metadata and establish which error codes are usable.
   52 |    * @param exceptionTranslatorLazyInit
   53 |    *          if true, the translator instantiates internal stuff only the first time will have the need to translate
   54 |    *          exceptions.
   55 |    */
   56 |   public MyBatisExceptionTranslator(DataSource dataSource, boolean exceptionTranslatorLazyInit) {
   57 |     this(() -> new SQLErrorCodeSQLExceptionTranslator(dataSource), exceptionTranslatorLazyInit);
   58 |   }
   59 | 
   60 |   /**
   61 |    * Creates a new {@code PersistenceExceptionTranslator} instance with specified {@code SQLExceptionTranslator}.
   62 |    *
   63 |    * @param exceptionTranslatorSupplier
   64 |    *          Supplier for creating a {@code SQLExceptionTranslator} instance
   65 |    * @param exceptionTranslatorLazyInit
   66 |    *          if true, the translator instantiates internal stuff only the first time will have the need to translate
   67 |    *          exceptions.
   68 |    *
   69 |    * @since 2.0.3
   70 |    */
   71 |   public MyBatisExceptionTranslator(Supplier<SQLExceptionTranslator> exceptionTranslatorSupplier,
   72 |       boolean exceptionTranslatorLazyInit) {
   73 |     this.exceptionTranslatorSupplier = exceptionTranslatorSupplier;
   74 |     if (!exceptionTranslatorLazyInit) {
   75 |       this.initExceptionTranslator();
   76 |     }
   77 |   }
   78 | 
   79 |   @Override
   80 |   public DataAccessException translateExceptionIfPossible(RuntimeException e) {
   81 |     if (e instanceof PersistenceException) {
   82 |       // Batch exceptions come inside another PersistenceException
   83 |       // recursion has a risk of infinite loop so better make another if
   84 |       var msg = e.getMessage();
   85 |       if (e.getCause() instanceof PersistenceException) {
   86 |         e = (PersistenceException) e.getCause();
   87 |         if (msg == null) {
   88 |           msg = e.getMessage();
   89 |         }
   90 |       }
   91 |       if (e.getCause() instanceof SQLException) {
   92 |         this.initExceptionTranslator();
   93 |         var task = e.getMessage() + "\n";
   94 |         var se = (SQLException) e.getCause();
   95 |         var dae = this.exceptionTranslator.translate(task, null, se);
   96 |         return dae != null ? dae : new UncategorizedSQLException(task, null, se);
   97 |       }
   98 |       if (e.getCause() instanceof TransactionException) {
   99 |         throw (TransactionException) e.getCause();
  100 |       }
  101 |       return new MyBatisSystemException(msg, e);
  102 |     }
  103 |     return null;
  104 |   }
  105 | 
  106 |   /**
  107 |    * Initializes the internal translator reference.
  108 |    */
  109 |   private void initExceptionTranslator() {
  110 |     lock.lock();
  111 |     try {
  112 |       if (this.exceptionTranslator == null) {
  113 |         this.exceptionTranslator = exceptionTranslatorSupplier.get();
  114 |       }
  115 |     } finally {
  116 |       lock.unlock();
  117 |     }
  118 |   }
  119 | 
  120 | }
```
