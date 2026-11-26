<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/SqlSessionHolder.java -->
<!-- 正文引用：[[源码:SqlSessionHolder.java#L起-L止]] ；代码块头部：(src: SqlSessionHolder.java#L起-L止) -->

# SqlSessionHolder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/SqlSessionHolder.java` |
| 行数 | 74 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:01:24.864Z |

```java
    1 | /*
    2 |  * Copyright 2010-2022 the original author or authors.
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
   18 | import static org.springframework.util.Assert.notNull;
   19 | 
   20 | import org.apache.ibatis.session.ExecutorType;
   21 | import org.apache.ibatis.session.SqlSession;
   22 | import org.springframework.dao.support.PersistenceExceptionTranslator;
   23 | import org.springframework.transaction.support.ResourceHolderSupport;
   24 | 
   25 | /**
   26 |  * Used to keep current {@code SqlSession} in {@code TransactionSynchronizationManager}. The {@code SqlSessionFactory}
   27 |  * that created that {@code SqlSession} is used as a key. {@code ExecutorType} is also kept to be able to check if the
   28 |  * user is trying to change it during a TX (that is not allowed) and throw a Exception in that case.
   29 |  *
   30 |  * @author Hunter Presnall
   31 |  * @author Eduardo Macarron
   32 |  */
   33 | public final class SqlSessionHolder extends ResourceHolderSupport {
   34 | 
   35 |   private final SqlSession sqlSession;
   36 | 
   37 |   private final ExecutorType executorType;
   38 | 
   39 |   private final PersistenceExceptionTranslator exceptionTranslator;
   40 | 
   41 |   /**
   42 |    * Creates a new holder instance.
   43 |    *
   44 |    * @param sqlSession
   45 |    *          the {@code SqlSession} has to be hold.
   46 |    * @param executorType
   47 |    *          the {@code ExecutorType} has to be hold.
   48 |    * @param exceptionTranslator
   49 |    *          the {@code PersistenceExceptionTranslator} has to be hold.
   50 |    */
   51 |   public SqlSessionHolder(SqlSession sqlSession, ExecutorType executorType,
   52 |       PersistenceExceptionTranslator exceptionTranslator) {
   53 | 
   54 |     notNull(sqlSession, "SqlSession must not be null");
   55 |     notNull(executorType, "ExecutorType must not be null");
   56 | 
   57 |     this.sqlSession = sqlSession;
   58 |     this.executorType = executorType;
   59 |     this.exceptionTranslator = exceptionTranslator;
   60 |   }
   61 | 
   62 |   public SqlSession getSqlSession() {
   63 |     return sqlSession;
   64 |   }
   65 | 
   66 |   public ExecutorType getExecutorType() {
   67 |     return executorType;
   68 |   }
   69 | 
   70 |   public PersistenceExceptionTranslator getPersistenceExceptionTranslator() {
   71 |     return exceptionTranslator;
   72 |   }
   73 | 
   74 | }
```
