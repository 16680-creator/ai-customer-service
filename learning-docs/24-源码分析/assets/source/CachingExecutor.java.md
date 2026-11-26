<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/CachingExecutor.java -->
<!-- 正文引用：[[源码:CachingExecutor.java#L起-L止]] ；代码块头部：(src: CachingExecutor.java#L起-L止) -->

# CachingExecutor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/CachingExecutor.java` |
| 行数 | 180 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:21.303Z |

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
   16 | package org.apache.ibatis.executor;
   17 | 
   18 | import java.sql.SQLException;
   19 | import java.util.List;
   20 | 
   21 | import org.apache.ibatis.cache.Cache;
   22 | import org.apache.ibatis.cache.CacheKey;
   23 | import org.apache.ibatis.cache.TransactionalCacheManager;
   24 | import org.apache.ibatis.cursor.Cursor;
   25 | import org.apache.ibatis.mapping.BoundSql;
   26 | import org.apache.ibatis.mapping.MappedStatement;
   27 | import org.apache.ibatis.mapping.ParameterMapping;
   28 | import org.apache.ibatis.mapping.ParameterMode;
   29 | import org.apache.ibatis.mapping.StatementType;
   30 | import org.apache.ibatis.reflection.MetaObject;
   31 | import org.apache.ibatis.session.ResultHandler;
   32 | import org.apache.ibatis.session.RowBounds;
   33 | import org.apache.ibatis.transaction.Transaction;
   34 | 
   35 | /**
   36 |  * @author Clinton Begin
   37 |  * @author Eduardo Macarron
   38 |  */
   39 | public class CachingExecutor implements Executor {
   40 | 
   41 |   private final Executor delegate;
   42 |   private final TransactionalCacheManager tcm = new TransactionalCacheManager();
   43 | 
   44 |   public CachingExecutor(Executor delegate) {
   45 |     this.delegate = delegate;
   46 |     delegate.setExecutorWrapper(this);
   47 |   }
   48 | 
   49 |   @Override
   50 |   public Transaction getTransaction() {
   51 |     return delegate.getTransaction();
   52 |   }
   53 | 
   54 |   @Override
   55 |   public void close(boolean forceRollback) {
   56 |     try {
   57 |       // issues #499, #524 and #573
   58 |       if (forceRollback) {
   59 |         tcm.rollback();
   60 |       } else {
   61 |         tcm.commit();
   62 |       }
   63 |     } finally {
   64 |       delegate.close(forceRollback);
   65 |     }
   66 |   }
   67 | 
   68 |   @Override
   69 |   public boolean isClosed() {
   70 |     return delegate.isClosed();
   71 |   }
   72 | 
   73 |   @Override
   74 |   public int update(MappedStatement ms, Object parameterObject) throws SQLException {
   75 |     flushCacheIfRequired(ms);
   76 |     return delegate.update(ms, parameterObject);
   77 |   }
   78 | 
   79 |   @Override
   80 |   public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
   81 |     flushCacheIfRequired(ms);
   82 |     return delegate.queryCursor(ms, parameter, rowBounds);
   83 |   }
   84 | 
   85 |   @Override
   86 |   public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler)
   87 |       throws SQLException {
   88 |     BoundSql boundSql = ms.getBoundSql(parameterObject);
   89 |     CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
   90 |     return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
   91 |   }
   92 | 
   93 |   @Override
   94 |   public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler,
   95 |       CacheKey key, BoundSql boundSql) throws SQLException {
   96 |     Cache cache = ms.getCache();
   97 |     if (cache != null) {
   98 |       flushCacheIfRequired(ms);
   99 |       if (ms.isUseCache() && resultHandler == null) {
  100 |         ensureNoOutParams(ms, boundSql);
  101 |         @SuppressWarnings("unchecked")
  102 |         List<E> list = (List<E>) tcm.getObject(cache, key);
  103 |         if (list == null) {
  104 |           list = delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  105 |           tcm.putObject(cache, key, list); // issue #578 and #116
  106 |         }
  107 |         return list;
  108 |       }
  109 |     }
  110 |     return delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  111 |   }
  112 | 
  113 |   @Override
  114 |   public List<BatchResult> flushStatements() throws SQLException {
  115 |     return delegate.flushStatements();
  116 |   }
  117 | 
  118 |   @Override
  119 |   public void commit(boolean required) throws SQLException {
  120 |     delegate.commit(required);
  121 |     tcm.commit();
  122 |   }
  123 | 
  124 |   @Override
  125 |   public void rollback(boolean required) throws SQLException {
  126 |     try {
  127 |       delegate.rollback(required);
  128 |     } finally {
  129 |       if (required) {
  130 |         tcm.rollback();
  131 |       }
  132 |     }
  133 |   }
  134 | 
  135 |   private void ensureNoOutParams(MappedStatement ms, BoundSql boundSql) {
  136 |     if (ms.getStatementType() == StatementType.CALLABLE) {
  137 |       for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
  138 |         if (parameterMapping.getMode() != ParameterMode.IN) {
  139 |           throw new ExecutorException(
  140 |               "Caching stored procedures with OUT params is not supported.  Please configure useCache=false in "
  141 |                   + ms.getId() + " statement.");
  142 |         }
  143 |       }
  144 |     }
  145 |   }
  146 | 
  147 |   @Override
  148 |   public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
  149 |     return delegate.createCacheKey(ms, parameterObject, rowBounds, boundSql);
  150 |   }
  151 | 
  152 |   @Override
  153 |   public boolean isCached(MappedStatement ms, CacheKey key) {
  154 |     return delegate.isCached(ms, key);
  155 |   }
  156 | 
  157 |   @Override
  158 |   public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key,
  159 |       Class<?> targetType) {
  160 |     delegate.deferLoad(ms, resultObject, property, key, targetType);
  161 |   }
  162 | 
  163 |   @Override
  164 |   public void clearLocalCache() {
  165 |     delegate.clearLocalCache();
  166 |   }
  167 | 
  168 |   private void flushCacheIfRequired(MappedStatement ms) {
  169 |     Cache cache = ms.getCache();
  170 |     if (cache != null && ms.isFlushCacheRequired()) {
  171 |       tcm.clear(cache);
  172 |     }
  173 |   }
  174 | 
  175 |   @Override
  176 |   public void setExecutorWrapper(Executor executor) {
  177 |     throw new UnsupportedOperationException("This method should not be called");
  178 |   }
  179 | 
  180 | }
```
