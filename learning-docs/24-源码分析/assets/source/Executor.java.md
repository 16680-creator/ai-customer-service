<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/Executor.java -->
<!-- 正文引用：[[源码:Executor.java#L起-L止]] ；代码块头部：(src: Executor.java#L起-L止) -->

# Executor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/Executor.java` |
| 行数 | 69 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:20.719Z |

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
   16 | package org.apache.ibatis.executor;
   17 | 
   18 | import java.sql.SQLException;
   19 | import java.util.List;
   20 | 
   21 | import org.apache.ibatis.cache.CacheKey;
   22 | import org.apache.ibatis.cursor.Cursor;
   23 | import org.apache.ibatis.mapping.BoundSql;
   24 | import org.apache.ibatis.mapping.MappedStatement;
   25 | import org.apache.ibatis.reflection.MetaObject;
   26 | import org.apache.ibatis.session.ResultHandler;
   27 | import org.apache.ibatis.session.RowBounds;
   28 | import org.apache.ibatis.transaction.Transaction;
   29 | 
   30 | /**
   31 |  * @author Clinton Begin
   32 |  */
   33 | public interface Executor {
   34 | 
   35 |   ResultHandler NO_RESULT_HANDLER = null;
   36 | 
   37 |   int update(MappedStatement ms, Object parameter) throws SQLException;
   38 | 
   39 |   <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler,
   40 |       CacheKey cacheKey, BoundSql boundSql) throws SQLException;
   41 | 
   42 |   <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler)
   43 |       throws SQLException;
   44 | 
   45 |   <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException;
   46 | 
   47 |   List<BatchResult> flushStatements() throws SQLException;
   48 | 
   49 |   void commit(boolean required) throws SQLException;
   50 | 
   51 |   void rollback(boolean required) throws SQLException;
   52 | 
   53 |   CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql);
   54 | 
   55 |   boolean isCached(MappedStatement ms, CacheKey key);
   56 | 
   57 |   void clearLocalCache();
   58 | 
   59 |   void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType);
   60 | 
   61 |   Transaction getTransaction();
   62 | 
   63 |   void close(boolean forceRollback);
   64 | 
   65 |   boolean isClosed();
   66 | 
   67 |   void setExecutorWrapper(Executor executor);
   68 | 
   69 | }
```
