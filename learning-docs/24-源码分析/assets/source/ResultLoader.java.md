<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/loader/ResultLoader.java -->
<!-- 正文引用：[[源码:ResultLoader.java#L起-L止]] ；代码块头部：(src: ResultLoader.java#L起-L止) -->

# ResultLoader.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/loader/ResultLoader.java` |
| 行数 | 109 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:00:16.088Z |

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
   16 | package org.apache.ibatis.executor.loader;
   17 | 
   18 | import java.sql.SQLException;
   19 | import java.util.List;
   20 | 
   21 | import javax.sql.DataSource;
   22 | 
   23 | import org.apache.ibatis.cache.CacheKey;
   24 | import org.apache.ibatis.executor.Executor;
   25 | import org.apache.ibatis.executor.ExecutorException;
   26 | import org.apache.ibatis.executor.ResultExtractor;
   27 | import org.apache.ibatis.mapping.BoundSql;
   28 | import org.apache.ibatis.mapping.Environment;
   29 | import org.apache.ibatis.mapping.MappedStatement;
   30 | import org.apache.ibatis.reflection.factory.ObjectFactory;
   31 | import org.apache.ibatis.session.Configuration;
   32 | import org.apache.ibatis.session.ExecutorType;
   33 | import org.apache.ibatis.session.RowBounds;
   34 | import org.apache.ibatis.transaction.Transaction;
   35 | import org.apache.ibatis.transaction.TransactionFactory;
   36 | 
   37 | /**
   38 |  * @author Clinton Begin
   39 |  */
   40 | public class ResultLoader {
   41 | 
   42 |   protected final Configuration configuration;
   43 |   protected final Executor executor;
   44 |   protected final MappedStatement mappedStatement;
   45 |   protected final Object parameterObject;
   46 |   protected final Class<?> targetType;
   47 |   protected final ObjectFactory objectFactory;
   48 |   protected final CacheKey cacheKey;
   49 |   protected final BoundSql boundSql;
   50 |   protected final ResultExtractor resultExtractor;
   51 |   protected final long creatorThreadId;
   52 | 
   53 |   protected boolean loaded;
   54 |   protected Object resultObject;
   55 | 
   56 |   public ResultLoader(Configuration config, Executor executor, MappedStatement mappedStatement, Object parameterObject,
   57 |       Class<?> targetType, CacheKey cacheKey, BoundSql boundSql) {
   58 |     this.configuration = config;
   59 |     this.executor = executor;
   60 |     this.mappedStatement = mappedStatement;
   61 |     this.parameterObject = parameterObject;
   62 |     this.targetType = targetType;
   63 |     this.objectFactory = configuration.getObjectFactory();
   64 |     this.cacheKey = cacheKey;
   65 |     this.boundSql = boundSql;
   66 |     this.resultExtractor = new ResultExtractor(configuration, objectFactory);
   67 |     this.creatorThreadId = Thread.currentThread().getId();
   68 |   }
   69 | 
   70 |   public Object loadResult() throws SQLException {
   71 |     List<Object> list = selectList();
   72 |     resultObject = resultExtractor.extractObjectFromList(list, targetType);
   73 |     return resultObject;
   74 |   }
   75 | 
   76 |   private <E> List<E> selectList() throws SQLException {
   77 |     Executor localExecutor = executor;
   78 |     if (Thread.currentThread().getId() != this.creatorThreadId || localExecutor.isClosed()) {
   79 |       localExecutor = newExecutor();
   80 |     }
   81 |     try {
   82 |       return localExecutor.query(mappedStatement, parameterObject, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER,
   83 |           cacheKey, boundSql);
   84 |     } finally {
   85 |       if (localExecutor != executor) {
   86 |         localExecutor.close(false);
   87 |       }
   88 |     }
   89 |   }
   90 | 
   91 |   private Executor newExecutor() {
   92 |     final Environment environment = configuration.getEnvironment();
   93 |     if (environment == null) {
   94 |       throw new ExecutorException("ResultLoader could not load lazily.  Environment was not configured.");
   95 |     }
   96 |     final DataSource ds = environment.getDataSource();
   97 |     if (ds == null) {
   98 |       throw new ExecutorException("ResultLoader could not load lazily.  DataSource was not configured.");
   99 |     }
  100 |     final TransactionFactory transactionFactory = environment.getTransactionFactory();
  101 |     final Transaction tx = transactionFactory.newTransaction(ds, null, false);
  102 |     return configuration.newExecutor(tx, ExecutorType.SIMPLE);
  103 |   }
  104 | 
  105 |   public boolean wasNull() {
  106 |     return resultObject == null;
  107 |   }
  108 | 
  109 | }
```
