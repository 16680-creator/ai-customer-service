<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/BaseExecutor.java -->
<!-- 正文引用：[[源码:BaseExecutor.java#L起-L止]] ；代码块头部：(src: BaseExecutor.java#L起-L止) -->

# BaseExecutor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/BaseExecutor.java` |
| 行数 | 405 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:21.082Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2026 the original author or authors.
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
   18 | import static org.apache.ibatis.executor.ExecutionPlaceholder.EXECUTION_PLACEHOLDER;
   19 | 
   20 | import java.sql.Connection;
   21 | import java.sql.SQLException;
   22 | import java.sql.Statement;
   23 | import java.util.List;
   24 | import java.util.concurrent.ConcurrentLinkedQueue;
   25 | 
   26 | import org.apache.ibatis.cache.CacheKey;
   27 | import org.apache.ibatis.cache.impl.PerpetualCache;
   28 | import org.apache.ibatis.cursor.Cursor;
   29 | import org.apache.ibatis.executor.statement.StatementUtil;
   30 | import org.apache.ibatis.logging.Log;
   31 | import org.apache.ibatis.logging.LogFactory;
   32 | import org.apache.ibatis.logging.jdbc.ConnectionLogger;
   33 | import org.apache.ibatis.mapping.BoundSql;
   34 | import org.apache.ibatis.mapping.MappedStatement;
   35 | import org.apache.ibatis.mapping.ParameterMapping;
   36 | import org.apache.ibatis.mapping.ParameterMode;
   37 | import org.apache.ibatis.mapping.StatementType;
   38 | import org.apache.ibatis.reflection.MetaObject;
   39 | import org.apache.ibatis.reflection.ParamNameResolver;
   40 | import org.apache.ibatis.reflection.factory.ObjectFactory;
   41 | import org.apache.ibatis.session.Configuration;
   42 | import org.apache.ibatis.session.LocalCacheScope;
   43 | import org.apache.ibatis.session.ResultHandler;
   44 | import org.apache.ibatis.session.RowBounds;
   45 | import org.apache.ibatis.transaction.Transaction;
   46 | import org.apache.ibatis.type.TypeHandlerRegistry;
   47 | 
   48 | /**
   49 |  * @author Clinton Begin
   50 |  */
   51 | public abstract class BaseExecutor implements Executor {
   52 | 
   53 |   private static final Log log = LogFactory.getLog(BaseExecutor.class);
   54 | 
   55 |   protected Transaction transaction;
   56 |   protected Executor wrapper;
   57 | 
   58 |   protected ConcurrentLinkedQueue<DeferredLoad> deferredLoads;
   59 |   protected PerpetualCache localCache;
   60 |   protected PerpetualCache localOutputParameterCache;
   61 |   protected Configuration configuration;
   62 | 
   63 |   protected int queryStack;
   64 |   private boolean closed;
   65 | 
   66 |   protected BaseExecutor(Configuration configuration, Transaction transaction) {
   67 |     this.transaction = transaction;
   68 |     this.deferredLoads = new ConcurrentLinkedQueue<>();
   69 |     this.localCache = new PerpetualCache("LocalCache");
   70 |     this.localOutputParameterCache = new PerpetualCache("LocalOutputParameterCache");
   71 |     this.closed = false;
   72 |     this.configuration = configuration;
   73 |     this.wrapper = this;
   74 |   }
   75 | 
   76 |   @Override
   77 |   public Transaction getTransaction() {
   78 |     if (closed) {
   79 |       throw new ExecutorException("Executor was closed.");
   80 |     }
   81 |     return transaction;
   82 |   }
   83 | 
   84 |   @Override
   85 |   public void close(boolean forceRollback) {
   86 |     try {
   87 |       try {
   88 |         rollback(forceRollback);
   89 |       } finally {
   90 |         if (transaction != null) {
   91 |           transaction.close();
   92 |         }
   93 |       }
   94 |     } catch (SQLException e) {
   95 |       // Ignore. There's nothing that can be done at this point.
   96 |       log.warn("Unexpected exception on closing transaction.  Cause: " + e);
   97 |     } finally {
   98 |       transaction = null;
   99 |       deferredLoads = null;
  100 |       localCache = null;
  101 |       localOutputParameterCache = null;
  102 |       closed = true;
  103 |     }
  104 |   }
  105 | 
  106 |   @Override
  107 |   public boolean isClosed() {
  108 |     return closed;
  109 |   }
  110 | 
  111 |   @Override
  112 |   public int update(MappedStatement ms, Object parameter) throws SQLException {
  113 |     ErrorContext.instance().resource(ms.getResource()).activity("executing an update").object(ms.getId());
  114 |     if (closed) {
  115 |       throw new ExecutorException("Executor was closed.");
  116 |     }
  117 |     clearLocalCache();
  118 |     return doUpdate(ms, parameter);
  119 |   }
  120 | 
  121 |   @Override
  122 |   public List<BatchResult> flushStatements() throws SQLException {
  123 |     return flushStatements(false);
  124 |   }
  125 | 
  126 |   public List<BatchResult> flushStatements(boolean isRollBack) throws SQLException {
  127 |     if (closed) {
  128 |       throw new ExecutorException("Executor was closed.");
  129 |     }
  130 |     return doFlushStatements(isRollBack);
  131 |   }
  132 | 
  133 |   @Override
  134 |   public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler)
  135 |       throws SQLException {
  136 |     BoundSql boundSql = ms.getBoundSql(parameter);
  137 |     CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
  138 |     return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
  139 |   }
  140 | 
  141 |   @SuppressWarnings("unchecked")
  142 |   @Override
  143 |   public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler,
  144 |       CacheKey key, BoundSql boundSql) throws SQLException {
  145 |     ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
  146 |     if (closed) {
  147 |       throw new ExecutorException("Executor was closed.");
  148 |     }
  149 |     if (queryStack == 0 && ms.isFlushCacheRequired()) {
  150 |       clearLocalCache();
  151 |     }
  152 |     List<E> list;
  153 |     try {
  154 |       queryStack++;
  155 |       list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
  156 |       if (list != null) {
  157 |         handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
  158 |       } else {
  159 |         list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
  160 |       }
  161 |     } finally {
  162 |       queryStack--;
  163 |     }
  164 |     if (queryStack == 0) {
  165 |       for (DeferredLoad deferredLoad : deferredLoads) {
  166 |         deferredLoad.load();
  167 |       }
  168 |       // issue #601
  169 |       deferredLoads.clear();
  170 |       if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
  171 |         // issue #482
  172 |         clearLocalCache();
  173 |       }
  174 |     }
  175 |     return list;
  176 |   }
  177 | 
  178 |   @Override
  179 |   public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
  180 |     BoundSql boundSql = ms.getBoundSql(parameter);
  181 |     return doQueryCursor(ms, parameter, rowBounds, boundSql);
  182 |   }
  183 | 
  184 |   @Override
  185 |   public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key,
  186 |       Class<?> targetType) {
  187 |     if (closed) {
  188 |       throw new ExecutorException("Executor was closed.");
  189 |     }
  190 |     DeferredLoad deferredLoad = new DeferredLoad(resultObject, property, key, localCache, configuration, targetType);
  191 |     if (deferredLoad.canLoad()) {
  192 |       deferredLoad.load();
  193 |     } else {
  194 |       deferredLoads.add(new DeferredLoad(resultObject, property, key, localCache, configuration, targetType));
  195 |     }
  196 |   }
  197 | 
  198 |   @Override
  199 |   public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
  200 |     if (closed) {
  201 |       throw new ExecutorException("Executor was closed.");
  202 |     }
  203 |     CacheKey cacheKey = new CacheKey();
  204 |     cacheKey.update(ms.getId());
  205 |     cacheKey.update(rowBounds.getOffset());
  206 |     cacheKey.update(rowBounds.getLimit());
  207 |     cacheKey.update(boundSql.getSql());
  208 |     List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
  209 |     TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();
  210 |     // mimic DefaultParameterHandler logic
  211 |     MetaObject metaObject = null;
  212 |     for (ParameterMapping parameterMapping : parameterMappings) {
  213 |       if (parameterMapping.getMode() != ParameterMode.OUT) {
  214 |         Object value;
  215 |         String propertyName = parameterMapping.getProperty();
  216 |         if (parameterMapping.hasValue()) {
  217 |           value = parameterMapping.getValue();
  218 |         } else if (boundSql.hasAdditionalParameter(propertyName)) {
  219 |           value = boundSql.getAdditionalParameter(propertyName);
  220 |         } else if (parameterObject == null) {
  221 |           value = null;
  222 |         } else {
  223 |           ParamNameResolver paramNameResolver = ms.getParamNameResolver();
  224 |           if (paramNameResolver != null
  225 |               && typeHandlerRegistry.hasTypeHandler(paramNameResolver.getType(paramNameResolver.getNames()[0]))
  226 |               || typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
  227 |             value = parameterObject;
  228 |           } else {
  229 |             if (metaObject == null) {
  230 |               metaObject = configuration.newMetaObject(parameterObject);
  231 |             }
  232 |             value = metaObject.getValue(propertyName);
  233 |           }
  234 |         }
  235 |         cacheKey.update(value);
  236 |       }
  237 |     }
  238 |     if (configuration.getEnvironment() != null) {
  239 |       // issue #176
  240 |       cacheKey.update(configuration.getEnvironment().getId());
  241 |     }
  242 |     return cacheKey;
  243 |   }
  244 | 
  245 |   @Override
  246 |   public boolean isCached(MappedStatement ms, CacheKey key) {
  247 |     return localCache.getObject(key) != null;
  248 |   }
  249 | 
  250 |   @Override
  251 |   public void commit(boolean required) throws SQLException {
  252 |     if (closed) {
  253 |       throw new ExecutorException("Cannot commit, transaction is already closed");
  254 |     }
  255 |     clearLocalCache();
  256 |     flushStatements();
  257 |     if (required) {
  258 |       transaction.commit();
  259 |     }
  260 |   }
  261 | 
  262 |   @Override
  263 |   public void rollback(boolean required) throws SQLException {
  264 |     if (!closed) {
  265 |       try {
  266 |         clearLocalCache();
  267 |         flushStatements(true);
  268 |       } finally {
  269 |         if (required) {
  270 |           transaction.rollback();
  271 |         }
  272 |       }
  273 |     }
  274 |   }
  275 | 
  276 |   @Override
  277 |   public void clearLocalCache() {
  278 |     if (!closed) {
  279 |       localCache.clear();
  280 |       localOutputParameterCache.clear();
  281 |     }
  282 |   }
  283 | 
  284 |   protected abstract int doUpdate(MappedStatement ms, Object parameter) throws SQLException;
  285 | 
  286 |   protected abstract List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException;
  287 | 
  288 |   protected abstract <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds,
  289 |       ResultHandler resultHandler, BoundSql boundSql) throws SQLException;
  290 | 
  291 |   protected abstract <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds,
  292 |       BoundSql boundSql) throws SQLException;
  293 | 
  294 |   protected void closeStatement(Statement statement) {
  295 |     if (statement != null) {
  296 |       try {
  297 |         statement.close();
  298 |       } catch (SQLException e) {
  299 |         // ignore
  300 |       }
  301 |     }
  302 |   }
  303 | 
  304 |   /**
  305 |    * Apply a transaction timeout.
  306 |    *
  307 |    * @param statement
  308 |    *          a current statement
  309 |    *
  310 |    * @throws SQLException
  311 |    *           if a database access error occurs, this method is called on a closed <code>Statement</code>
  312 |    *
  313 |    * @since 3.4.0
  314 |    *
  315 |    * @see StatementUtil#applyTransactionTimeout(Statement, Integer, Integer)
  316 |    */
  317 |   protected void applyTransactionTimeout(Statement statement) throws SQLException {
  318 |     StatementUtil.applyTransactionTimeout(statement, statement.getQueryTimeout(), transaction.getTimeout());
  319 |   }
  320 | 
  321 |   private void handleLocallyCachedOutputParameters(MappedStatement ms, CacheKey key, Object parameter,
  322 |       BoundSql boundSql) {
  323 |     if (ms.getStatementType() == StatementType.CALLABLE) {
  324 |       final Object cachedParameter = localOutputParameterCache.getObject(key);
  325 |       if (cachedParameter != null && parameter != null) {
  326 |         final MetaObject metaCachedParameter = configuration.newMetaObject(cachedParameter);
  327 |         final MetaObject metaParameter = configuration.newMetaObject(parameter);
  328 |         for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
  329 |           if (parameterMapping.getMode() != ParameterMode.IN) {
  330 |             final String parameterName = parameterMapping.getProperty();
  331 |             final Object cachedValue = metaCachedParameter.getValue(parameterName);
  332 |             metaParameter.setValue(parameterName, cachedValue);
  333 |           }
  334 |         }
  335 |       }
  336 |     }
  337 |   }
  338 | 
  339 |   private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds,
  340 |       ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
  341 |     List<E> list;
  342 |     localCache.putObject(key, EXECUTION_PLACEHOLDER);
  343 |     try {
  344 |       list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
  345 |     } finally {
  346 |       localCache.removeObject(key);
  347 |     }
  348 |     localCache.putObject(key, list);
  349 |     if (ms.getStatementType() == StatementType.CALLABLE) {
  350 |       localOutputParameterCache.putObject(key, parameter);
  351 |     }
  352 |     return list;
  353 |   }
  354 | 
  355 |   protected Connection getConnection(Log statementLog) throws SQLException {
  356 |     Connection connection = transaction.getConnection();
  357 |     if (statementLog.isDebugEnabled()) {
  358 |       return ConnectionLogger.newInstance(connection, statementLog, queryStack);
  359 |     }
  360 |     return connection;
  361 |   }
  362 | 
  363 |   @Override
  364 |   public void setExecutorWrapper(Executor wrapper) {
  365 |     this.wrapper = wrapper;
  366 |   }
  367 | 
  368 |   private static class DeferredLoad {
  369 | 
  370 |     private final MetaObject resultObject;
  371 |     private final String property;
  372 |     private final Class<?> targetType;
  373 |     private final CacheKey key;
  374 |     private final PerpetualCache localCache;
  375 |     private final ObjectFactory objectFactory;
  376 |     private final ResultExtractor resultExtractor;
  377 | 
  378 |     // issue #781
  379 |     public DeferredLoad(MetaObject resultObject, String property, CacheKey key, PerpetualCache localCache,
  380 |         Configuration configuration, Class<?> targetType) {
  381 |       this.resultObject = resultObject;
  382 |       this.property = property;
  383 |       this.key = key;
  384 |       this.localCache = localCache;
  385 |       this.objectFactory = configuration.getObjectFactory();
  386 |       this.resultExtractor = new ResultExtractor(configuration, objectFactory);
  387 |       this.targetType = targetType;
  388 |     }
  389 | 
  390 |     public boolean canLoad() {
  391 |       Object cached = localCache.getObject(key);
  392 |       return cached != null && cached != EXECUTION_PLACEHOLDER;
  393 |     }
  394 | 
  395 |     public void load() {
  396 |       @SuppressWarnings("unchecked")
  397 |       // we suppose we get back a List
  398 |       List<Object> list = (List<Object>) localCache.getObject(key);
  399 |       Object value = resultExtractor.extractObjectFromList(list, targetType);
  400 |       resultObject.setValue(property, value);
  401 |     }
  402 | 
  403 |   }
  404 | 
  405 | }
```
