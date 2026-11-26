<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/resultset/DefaultResultSetHandler.java -->
<!-- 正文引用：[[源码:DefaultResultSetHandler.java#L起-L止]] ；代码块头部：(src: DefaultResultSetHandler.java#L起-L止) -->

# DefaultResultSetHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/resultset/DefaultResultSetHandler.java` |
| 行数 | 1662 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:20.798Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2025 the original author or authors.
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
   16 | package org.apache.ibatis.executor.resultset;
   17 | 
   18 | import java.lang.reflect.Constructor;
   19 | import java.lang.reflect.Parameter;
   20 | import java.lang.reflect.Type;
   21 | import java.sql.CallableStatement;
   22 | import java.sql.ResultSet;
   23 | import java.sql.SQLException;
   24 | import java.sql.Statement;
   25 | import java.text.MessageFormat;
   26 | import java.util.ArrayList;
   27 | import java.util.Arrays;
   28 | import java.util.Collection;
   29 | import java.util.HashMap;
   30 | import java.util.HashSet;
   31 | import java.util.IdentityHashMap;
   32 | import java.util.List;
   33 | import java.util.Locale;
   34 | import java.util.Map;
   35 | import java.util.Optional;
   36 | import java.util.Set;
   37 | 
   38 | import org.apache.ibatis.annotations.AutomapConstructor;
   39 | import org.apache.ibatis.annotations.Param;
   40 | import org.apache.ibatis.binding.MapperMethod.ParamMap;
   41 | import org.apache.ibatis.cache.CacheKey;
   42 | import org.apache.ibatis.cursor.Cursor;
   43 | import org.apache.ibatis.cursor.defaults.DefaultCursor;
   44 | import org.apache.ibatis.executor.ErrorContext;
   45 | import org.apache.ibatis.executor.Executor;
   46 | import org.apache.ibatis.executor.ExecutorException;
   47 | import org.apache.ibatis.executor.loader.ResultLoader;
   48 | import org.apache.ibatis.executor.loader.ResultLoaderMap;
   49 | import org.apache.ibatis.executor.parameter.ParameterHandler;
   50 | import org.apache.ibatis.executor.result.DefaultResultContext;
   51 | import org.apache.ibatis.executor.result.DefaultResultHandler;
   52 | import org.apache.ibatis.executor.result.ResultMapException;
   53 | import org.apache.ibatis.mapping.BoundSql;
   54 | import org.apache.ibatis.mapping.Discriminator;
   55 | import org.apache.ibatis.mapping.MappedStatement;
   56 | import org.apache.ibatis.mapping.ParameterMapping;
   57 | import org.apache.ibatis.mapping.ParameterMode;
   58 | import org.apache.ibatis.mapping.ResultMap;
   59 | import org.apache.ibatis.mapping.ResultMapping;
   60 | import org.apache.ibatis.reflection.MetaClass;
   61 | import org.apache.ibatis.reflection.MetaObject;
   62 | import org.apache.ibatis.reflection.ReflectorFactory;
   63 | import org.apache.ibatis.reflection.factory.ObjectFactory;
   64 | import org.apache.ibatis.session.AutoMappingBehavior;
   65 | import org.apache.ibatis.session.Configuration;
   66 | import org.apache.ibatis.session.ResultContext;
   67 | import org.apache.ibatis.session.ResultHandler;
   68 | import org.apache.ibatis.session.RowBounds;
   69 | import org.apache.ibatis.type.JdbcType;
   70 | import org.apache.ibatis.type.ObjectTypeHandler;
   71 | import org.apache.ibatis.type.TypeHandler;
   72 | import org.apache.ibatis.type.TypeHandlerRegistry;
   73 | 
   74 | /**
   75 |  * @author Clinton Begin
   76 |  * @author Eduardo Macarron
   77 |  * @author Iwao AVE!
   78 |  * @author Kazuki Shimizu
   79 |  * @author Willie Scholtz
   80 |  */
   81 | public class DefaultResultSetHandler implements ResultSetHandler {
   82 | 
   83 |   private static final Object DEFERRED = new Object();
   84 | 
   85 |   private final Executor executor;
   86 |   private final Configuration configuration;
   87 |   private final MappedStatement mappedStatement;
   88 |   private final RowBounds rowBounds;
   89 |   private final ParameterHandler parameterHandler;
   90 |   private final ResultHandler<?> resultHandler;
   91 |   private final BoundSql boundSql;
   92 |   private final TypeHandlerRegistry typeHandlerRegistry;
   93 |   private final ObjectFactory objectFactory;
   94 |   private final ReflectorFactory reflectorFactory;
   95 | 
   96 |   // pending creations property tracker
   97 |   private final Map<Object, PendingRelation> pendingPccRelations = new IdentityHashMap<>();
   98 | 
   99 |   // nested resultMaps
  100 |   private final Map<CacheKey, Object> nestedResultObjects = new HashMap<>();
  101 |   private final Map<String, Object> ancestorObjects = new HashMap<>();
  102 |   private Object previousRowValue;
  103 | 
  104 |   // multiple resultSets
  105 |   private final Map<String, ResultMapping> nextResultMaps = new HashMap<>();
  106 |   private final Map<CacheKey, List<PendingRelation>> pendingRelations = new HashMap<>();
  107 | 
  108 |   // Cached AutoMappings
  109 |   private final Map<String, List<UnMappedColumnAutoMapping>> autoMappingsCache = new HashMap<>();
  110 |   private final Map<String, List<String>> constructorAutoMappingColumns = new HashMap<>();
  111 | 
  112 |   // temporary marking flag that indicate using constructor mapping (use field to reduce memory usage)
  113 |   private boolean useConstructorMappings;
  114 | 
  115 |   private static class PendingRelation {
  116 |     public MetaObject metaObject;
  117 |     public ResultMapping propertyMapping;
  118 |   }
  119 | 
  120 |   private static class UnMappedColumnAutoMapping {
  121 |     private final String column;
  122 |     private final String property;
  123 |     private final TypeHandler<?> typeHandler;
  124 |     private final boolean primitive;
  125 | 
  126 |     public UnMappedColumnAutoMapping(String column, String property, TypeHandler<?> typeHandler, boolean primitive) {
  127 |       this.column = column;
  128 |       this.property = property;
  129 |       this.typeHandler = typeHandler;
  130 |       this.primitive = primitive;
  131 |     }
  132 |   }
  133 | 
  134 |   public DefaultResultSetHandler(Executor executor, MappedStatement mappedStatement, ParameterHandler parameterHandler,
  135 |       ResultHandler<?> resultHandler, BoundSql boundSql, RowBounds rowBounds) {
  136 |     this.executor = executor;
  137 |     this.configuration = mappedStatement.getConfiguration();
  138 |     this.mappedStatement = mappedStatement;
  139 |     this.rowBounds = rowBounds;
  140 |     this.parameterHandler = parameterHandler;
  141 |     this.boundSql = boundSql;
  142 |     this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
  143 |     this.objectFactory = configuration.getObjectFactory();
  144 |     this.reflectorFactory = configuration.getReflectorFactory();
  145 |     this.resultHandler = resultHandler;
  146 |   }
  147 | 
  148 |   //
  149 |   // HANDLE OUTPUT PARAMETER
  150 |   //
  151 | 
  152 |   @Override
  153 |   public void handleOutputParameters(CallableStatement cs) throws SQLException {
  154 |     final Object parameterObject = parameterHandler.getParameterObject();
  155 |     final MetaObject metaParam = configuration.newMetaObject(parameterObject);
  156 |     final List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
  157 |     for (int i = 0; i < parameterMappings.size(); i++) {
  158 |       final ParameterMapping parameterMapping = parameterMappings.get(i);
  159 |       if (parameterMapping.getMode() == ParameterMode.OUT || parameterMapping.getMode() == ParameterMode.INOUT) {
  160 |         if (ResultSet.class.equals(parameterMapping.getJavaType())) {
  161 |           handleRefCursorOutputParameter((ResultSet) cs.getObject(i + 1), parameterMapping, metaParam);
  162 |         } else {
  163 |           final String property = parameterMapping.getProperty();
  164 |           TypeHandler<?> typeHandler = parameterMapping.getTypeHandler();
  165 |           if (typeHandler == null) {
  166 |             Type javaType = parameterMapping.getJavaType();
  167 |             if (javaType == null || javaType == Object.class) {
  168 |               javaType = metaParam.getGenericSetterType(property).getKey();
  169 |             }
  170 |             JdbcType jdbcType = parameterMapping.getJdbcType();
  171 |             typeHandler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType, null);
  172 |             if (typeHandler == null) {
  173 |               typeHandler = typeHandlerRegistry.getTypeHandler(jdbcType);
  174 |               if (typeHandler == null) {
  175 |                 typeHandler = ObjectTypeHandler.INSTANCE;
  176 |               }
  177 |             }
  178 |           }
  179 |           metaParam.setValue(property, typeHandler.getResult(cs, i + 1));
  180 |         }
  181 |       }
  182 |     }
  183 |   }
  184 | 
  185 |   private void handleRefCursorOutputParameter(ResultSet rs, ParameterMapping parameterMapping, MetaObject metaParam)
  186 |       throws SQLException {
  187 |     if (rs == null) {
  188 |       return;
  189 |     }
  190 |     try {
  191 |       final String resultMapId = parameterMapping.getResultMapId();
  192 |       final ResultMap resultMap = configuration.getResultMap(resultMapId);
  193 |       final ResultSetWrapper rsw = new ResultSetWrapper(rs, configuration);
  194 |       if (this.resultHandler == null) {
  195 |         final DefaultResultHandler resultHandler = new DefaultResultHandler(objectFactory);
  196 |         handleRowValues(rsw, resultMap, resultHandler, new RowBounds(), null);
  197 |         metaParam.setValue(parameterMapping.getProperty(), resultHandler.getResultList());
  198 |       } else {
  199 |         handleRowValues(rsw, resultMap, resultHandler, new RowBounds(), null);
  200 |       }
  201 |     } finally {
  202 |       // issue #228 (close resultSets)
  203 |       closeResultSet(rs);
  204 |     }
  205 |   }
  206 | 
  207 |   //
  208 |   // HANDLE RESULT SETS
  209 |   //
  210 |   @Override
  211 |   public List<Object> handleResultSets(Statement stmt) throws SQLException {
  212 |     ErrorContext.instance().activity("handling results").object(mappedStatement.getId());
  213 | 
  214 |     final List<Object> multipleResults = new ArrayList<>();
  215 | 
  216 |     int resultSetCount = 0;
  217 |     ResultSetWrapper rsw = getFirstResultSet(stmt);
  218 | 
  219 |     List<ResultMap> resultMaps = mappedStatement.getResultMaps();
  220 |     int resultMapCount = resultMaps.size();
  221 |     validateResultMapsCount(rsw, resultMapCount);
  222 |     while (rsw != null && resultMapCount > resultSetCount) {
  223 |       ResultMap resultMap = resultMaps.get(resultSetCount);
  224 |       handleResultSet(rsw, resultMap, multipleResults, null);
  225 |       rsw = getNextResultSet(stmt);
  226 |       cleanUpAfterHandlingResultSet();
  227 |       resultSetCount++;
  228 |     }
  229 | 
  230 |     String[] resultSets = mappedStatement.getResultSets();
  231 |     if (resultSets != null) {
  232 |       while (rsw != null && resultSetCount < resultSets.length) {
  233 |         ResultMapping parentMapping = nextResultMaps.get(resultSets[resultSetCount]);
  234 |         if (parentMapping != null) {
  235 |           String nestedResultMapId = parentMapping.getNestedResultMapId();
  236 |           ResultMap resultMap = configuration.getResultMap(nestedResultMapId);
  237 |           handleResultSet(rsw, resultMap, null, parentMapping);
  238 |         }
  239 |         rsw = getNextResultSet(stmt);
  240 |         cleanUpAfterHandlingResultSet();
  241 |         resultSetCount++;
  242 |       }
  243 |     }
  244 | 
  245 |     return collapseSingleResultList(multipleResults);
  246 |   }
  247 | 
  248 |   @Override
  249 |   public <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException {
  250 |     ErrorContext.instance().activity("handling cursor results").object(mappedStatement.getId());
  251 | 
  252 |     ResultSetWrapper rsw = getFirstResultSet(stmt);
  253 | 
  254 |     List<ResultMap> resultMaps = mappedStatement.getResultMaps();
  255 | 
  256 |     int resultMapCount = resultMaps.size();
  257 |     validateResultMapsCount(rsw, resultMapCount);
  258 |     if (resultMapCount != 1) {
  259 |       throw new ExecutorException("Cursor results cannot be mapped to multiple resultMaps");
  260 |     }
  261 | 
  262 |     ResultMap resultMap = resultMaps.get(0);
  263 |     return new DefaultCursor<>(this, resultMap, rsw, rowBounds);
  264 |   }
  265 | 
  266 |   private ResultSetWrapper getFirstResultSet(Statement stmt) throws SQLException {
  267 |     ResultSet rs = null;
  268 |     SQLException e1 = null;
  269 | 
  270 |     try {
  271 |       rs = stmt.getResultSet();
  272 |     } catch (SQLException e) {
  273 |       // Oracle throws ORA-17283 for implicit cursor
  274 |       e1 = e;
  275 |     }
  276 | 
  277 |     try {
  278 |       while (rs == null) {
  279 |         // move forward to get the first resultSet in case the driver
  280 |         // doesn't return the resultSet as the first result (HSQLDB)
  281 |         if (stmt.getMoreResults()) {
  282 |           rs = stmt.getResultSet();
  283 |         } else if (stmt.getUpdateCount() == -1) {
  284 |           // no more results. Must be no resultSet
  285 |           break;
  286 |         }
  287 |       }
  288 |     } catch (SQLException e) {
  289 |       throw e1 != null ? e1 : e;
  290 |     }
  291 | 
  292 |     return rs != null ? new ResultSetWrapper(rs, configuration) : null;
  293 |   }
  294 | 
  295 |   private ResultSetWrapper getNextResultSet(Statement stmt) {
  296 |     // Making this method tolerant of bad JDBC drivers
  297 |     try {
  298 |       // We stopped checking DatabaseMetaData#supportsMultipleResultSets()
  299 |       // because Oracle driver (incorrectly) returns false
  300 | 
  301 |       // Crazy Standard JDBC way of determining if there are more results
  302 |       // DO NOT try to 'improve' the condition even if IDE tells you to!
  303 |       // It's important that getUpdateCount() is called here.
  304 |       if (!(!stmt.getMoreResults() && stmt.getUpdateCount() == -1)) {
  305 |         ResultSet rs = stmt.getResultSet();
  306 |         if (rs == null) {
  307 |           return getNextResultSet(stmt);
  308 |         } else {
  309 |           return new ResultSetWrapper(rs, configuration);
  310 |         }
  311 |       }
  312 |     } catch (Exception e) {
  313 |       // Intentionally ignored.
  314 |     }
  315 |     return null;
  316 |   }
  317 | 
  318 |   private void closeResultSet(ResultSet rs) {
  319 |     try {
  320 |       if (rs != null) {
  321 |         rs.close();
  322 |       }
  323 |     } catch (SQLException e) {
  324 |       // ignore
  325 |     }
  326 |   }
  327 | 
  328 |   private void cleanUpAfterHandlingResultSet() {
  329 |     nestedResultObjects.clear();
  330 |   }
  331 | 
  332 |   private void validateResultMapsCount(ResultSetWrapper rsw, int resultMapCount) {
  333 |     if (rsw != null && resultMapCount < 1) {
  334 |       throw new ExecutorException(
  335 |           "A query was run and no Result Maps were found for the Mapped Statement '" + mappedStatement.getId()
  336 |               + "'. 'resultType' or 'resultMap' must be specified when there is no corresponding method.");
  337 |     }
  338 |   }
  339 | 
  340 |   private void handleResultSet(ResultSetWrapper rsw, ResultMap resultMap, List<Object> multipleResults,
  341 |       ResultMapping parentMapping) throws SQLException {
  342 |     try {
  343 |       if (parentMapping != null) {
  344 |         handleRowValues(rsw, resultMap, null, RowBounds.DEFAULT, parentMapping);
  345 |       } else if (resultHandler == null) {
  346 |         DefaultResultHandler defaultResultHandler = new DefaultResultHandler(objectFactory);
  347 |         handleRowValues(rsw, resultMap, defaultResultHandler, rowBounds, null);
  348 |         multipleResults.add(defaultResultHandler.getResultList());
  349 |       } else {
  350 |         handleRowValues(rsw, resultMap, resultHandler, rowBounds, null);
  351 |       }
  352 |     } finally {
  353 |       // issue #228 (close resultSets)
  354 |       closeResultSet(rsw.getResultSet());
  355 |     }
  356 |   }
  357 | 
  358 |   @SuppressWarnings("unchecked")
  359 |   private List<Object> collapseSingleResultList(List<Object> multipleResults) {
  360 |     return multipleResults.size() == 1 ? (List<Object>) multipleResults.get(0) : multipleResults;
  361 |   }
  362 | 
  363 |   //
  364 |   // HANDLE ROWS FOR SIMPLE RESULTMAP
  365 |   //
  366 | 
  367 |   public void handleRowValues(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler,
  368 |       RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
  369 |     if (resultMap.hasNestedResultMaps()) {
  370 |       ensureNoRowBounds();
  371 |       checkResultHandler();
  372 |       handleRowValuesForNestedResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
  373 |     } else {
  374 |       handleRowValuesForSimpleResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
  375 |     }
  376 |   }
  377 | 
  378 |   private void ensureNoRowBounds() {
  379 |     if (configuration.isSafeRowBoundsEnabled() && rowBounds != null
  380 |         && (rowBounds.getLimit() < RowBounds.NO_ROW_LIMIT || rowBounds.getOffset() > RowBounds.NO_ROW_OFFSET)) {
  381 |       throw new ExecutorException(
  382 |           "Mapped Statements with nested result mappings cannot be safely constrained by RowBounds. "
  383 |               + "Use safeRowBoundsEnabled=false setting to bypass this check.");
  384 |     }
  385 |   }
  386 | 
  387 |   protected void checkResultHandler() {
  388 |     if (resultHandler != null && configuration.isSafeResultHandlerEnabled() && !mappedStatement.isResultOrdered()) {
  389 |       throw new ExecutorException(
  390 |           "Mapped Statements with nested result mappings cannot be safely used with a custom ResultHandler. "
  391 |               + "Use safeResultHandlerEnabled=false setting to bypass this check "
  392 |               + "or ensure your statement returns ordered data and set resultOrdered=true on it.");
  393 |     }
  394 |   }
  395 | 
  396 |   private void handleRowValuesForSimpleResultMap(ResultSetWrapper rsw, ResultMap resultMap,
  397 |       ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
  398 |     final boolean useCollectionConstructorInjection = resultMap.hasResultMapsUsingConstructorCollection();
  399 | 
  400 |     DefaultResultContext<Object> resultContext = new DefaultResultContext<>();
  401 |     ResultSet resultSet = rsw.getResultSet();
  402 |     skipRows(resultSet, rowBounds);
  403 |     while (shouldProcessMoreRows(resultContext, rowBounds) && !resultSet.isClosed() && resultSet.next()) {
  404 |       ResultMap discriminatedResultMap = resolveDiscriminatedResultMap(rsw, resultMap, null);
  405 |       Object rowValue = getRowValue(rsw, discriminatedResultMap, null, null);
  406 |       if (!useCollectionConstructorInjection) {
  407 |         storeObject(resultHandler, resultContext, rowValue, parentMapping, resultSet);
  408 |       } else {
  409 |         if (!(rowValue instanceof PendingConstructorCreation)) {
  410 |           throw new ExecutorException("Expected result object to be a pending constructor creation!");
  411 |         }
  412 | 
  413 |         createAndStorePendingCreation(resultHandler, resultSet, resultContext, (PendingConstructorCreation) rowValue);
  414 |       }
  415 |     }
  416 |   }
  417 | 
  418 |   private void storeObject(ResultHandler<?> resultHandler, DefaultResultContext<Object> resultContext, Object rowValue,
  419 |       ResultMapping parentMapping, ResultSet rs) throws SQLException {
  420 |     if (parentMapping != null) {
  421 |       linkToParents(rs, parentMapping, rowValue);
  422 |       return;
  423 |     }
  424 | 
  425 |     if (pendingPccRelations.containsKey(rowValue)) {
  426 |       createPendingConstructorCreations(rowValue);
  427 |     }
  428 | 
  429 |     callResultHandler(resultHandler, resultContext, rowValue);
  430 |   }
  431 | 
  432 |   @SuppressWarnings("unchecked" /* because ResultHandler<?> is always ResultHandler<Object> */)
  433 |   private void callResultHandler(ResultHandler<?> resultHandler, DefaultResultContext<Object> resultContext,
  434 |       Object rowValue) {
  435 |     resultContext.nextResultObject(rowValue);
  436 |     ((ResultHandler<Object>) resultHandler).handleResult(resultContext);
  437 |   }
  438 | 
  439 |   private boolean shouldProcessMoreRows(ResultContext<?> context, RowBounds rowBounds) {
  440 |     return !context.isStopped() && context.getResultCount() < rowBounds.getLimit();
  441 |   }
  442 | 
  443 |   private void skipRows(ResultSet rs, RowBounds rowBounds) throws SQLException {
  444 |     if (rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {
  445 |       if (rowBounds.getOffset() != RowBounds.NO_ROW_OFFSET) {
  446 |         rs.absolute(rowBounds.getOffset());
  447 |       }
  448 |     } else {
  449 |       for (int i = 0; i < rowBounds.getOffset(); i++) {
  450 |         if (!rs.next()) {
  451 |           break;
  452 |         }
  453 |       }
  454 |     }
  455 |   }
  456 | 
  457 |   //
  458 |   // GET VALUE FROM ROW FOR SIMPLE RESULT MAP
  459 |   //
  460 | 
  461 |   private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix, CacheKey parentRowKey)
  462 |       throws SQLException {
  463 |     final ResultLoaderMap lazyLoader = new ResultLoaderMap();
  464 |     Object rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix, parentRowKey);
  465 |     if (rowValue != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
  466 |       final MetaObject metaObject = configuration.newMetaObject(rowValue);
  467 |       boolean foundValues = this.useConstructorMappings;
  468 |       if (shouldApplyAutomaticMappings(resultMap, false)) {
  469 |         foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues;
  470 |       }
  471 |       foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
  472 |       foundValues = lazyLoader.size() > 0 || foundValues;
  473 |       rowValue = foundValues || configuration.isReturnInstanceForEmptyRow() ? rowValue : null;
  474 |     }
  475 | 
  476 |     if (parentRowKey != null) {
  477 |       // found a simple object/primitive in pending constructor creation that will need linking later
  478 |       final CacheKey rowKey = createRowKey(resultMap, rsw, columnPrefix);
  479 |       final CacheKey combinedKey = combineKeys(rowKey, parentRowKey);
  480 | 
  481 |       if (combinedKey != CacheKey.NULL_CACHE_KEY) {
  482 |         nestedResultObjects.put(combinedKey, rowValue);
  483 |       }
  484 |     }
  485 | 
  486 |     return rowValue;
  487 |   }
  488 | 
  489 |   //
  490 |   // GET VALUE FROM ROW FOR NESTED RESULT MAP
  491 |   //
  492 | 
  493 |   private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, CacheKey combinedKey, String columnPrefix,
  494 |       Object partialObject) throws SQLException {
  495 |     final String resultMapId = resultMap.getId();
  496 |     Object rowValue = partialObject;
  497 |     if (rowValue != null) {
  498 |       final MetaObject metaObject = configuration.newMetaObject(rowValue);
  499 |       putAncestor(rowValue, resultMapId);
  500 |       applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, false);
  501 |       ancestorObjects.remove(resultMapId);
  502 |     } else {
  503 |       final ResultLoaderMap lazyLoader = new ResultLoaderMap();
  504 |       rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix, combinedKey);
  505 |       if (rowValue != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
  506 |         final MetaObject metaObject = configuration.newMetaObject(rowValue);
  507 |         boolean foundValues = this.useConstructorMappings;
  508 |         if (shouldApplyAutomaticMappings(resultMap, true)) {
  509 |           foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues;
  510 |         }
  511 |         foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
  512 |         putAncestor(rowValue, resultMapId);
  513 |         foundValues = applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, true)
  514 |             || foundValues;
  515 |         ancestorObjects.remove(resultMapId);
  516 |         foundValues = lazyLoader.size() > 0 || foundValues;
  517 |         rowValue = foundValues || configuration.isReturnInstanceForEmptyRow() ? rowValue : null;
  518 |       }
  519 |       if (combinedKey != CacheKey.NULL_CACHE_KEY) {
  520 |         nestedResultObjects.put(combinedKey, rowValue);
  521 |       }
  522 |     }
  523 |     return rowValue;
  524 |   }
  525 | 
  526 |   private void putAncestor(Object resultObject, String resultMapId) {
  527 |     ancestorObjects.put(resultMapId, resultObject);
  528 |   }
  529 | 
  530 |   private boolean shouldApplyAutomaticMappings(ResultMap resultMap, boolean isNested) {
  531 |     if (resultMap.getAutoMapping() != null) {
  532 |       return resultMap.getAutoMapping();
  533 |     }
  534 |     if (isNested) {
  535 |       return AutoMappingBehavior.FULL == configuration.getAutoMappingBehavior();
  536 |     } else {
  537 |       return AutoMappingBehavior.NONE != configuration.getAutoMappingBehavior();
  538 |     }
  539 |   }
  540 | 
  541 |   //
  542 |   // PROPERTY MAPPINGS
  543 |   //
  544 | 
  545 |   private boolean applyPropertyMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject,
  546 |       ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {
  547 |     final Set<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
  548 |     boolean foundValues = false;
  549 |     final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
  550 |     for (ResultMapping propertyMapping : propertyMappings) {
  551 |       String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
  552 |       if (propertyMapping.getNestedResultMapId() != null && !JdbcType.CURSOR.equals(propertyMapping.getJdbcType())) {
  553 |         // the user added a column attribute to a nested result map, ignore it
  554 |         column = null;
  555 |       }
  556 |       if (propertyMapping.isCompositeResult()
  557 |           || column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH))
  558 |           || propertyMapping.getResultSet() != null) {
  559 |         Object value = getPropertyMappingValue(rsw, metaObject, propertyMapping, lazyLoader, columnPrefix);
  560 |         // issue #541 make property optional
  561 |         final String property = propertyMapping.getProperty();
  562 |         if (property == null) {
  563 |           continue;
  564 |         }
  565 |         if (value == DEFERRED) {
  566 |           foundValues = true;
  567 |           continue;
  568 |         }
  569 |         if (value != null) {
  570 |           foundValues = true;
  571 |         }
  572 |         if (value != null
  573 |             || configuration.isCallSettersOnNulls() && !metaObject.getSetterType(property).isPrimitive()) {
  574 |           // gcode issue #377, call setter on nulls (value is not 'found')
  575 |           metaObject.setValue(property, value);
  576 |         }
  577 |       }
  578 |     }
  579 |     return foundValues;
  580 |   }
  581 | 
  582 |   private Object getPropertyMappingValue(ResultSetWrapper rsw, MetaObject metaResultObject,
  583 |       ResultMapping propertyMapping, ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {
  584 |     final ResultSet rs = rsw.getResultSet();
  585 |     if (propertyMapping.getNestedQueryId() != null) {
  586 |       return getNestedQueryMappingValue(rsw, metaResultObject, propertyMapping, lazyLoader, columnPrefix);
  587 |     }
  588 |     if (JdbcType.CURSOR.equals(propertyMapping.getJdbcType())) {
  589 |       List<Object> results = getNestedCursorValue(rsw, propertyMapping, columnPrefix);
  590 |       linkObjects(metaResultObject, propertyMapping, results.get(0), true);
  591 |       return metaResultObject.getValue(propertyMapping.getProperty());
  592 |     }
  593 |     if (propertyMapping.getResultSet() != null) {
  594 |       addPendingChildRelation(rs, metaResultObject, propertyMapping); // TODO is that OK?
  595 |       return DEFERRED;
  596 |     } else {
  597 |       final String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
  598 |       TypeHandler<?> typeHandler = propertyMapping.getTypeHandler();
  599 |       if (typeHandler == null) {
  600 |         final String property = propertyMapping.getProperty();
  601 |         final Type javaType = property == null ? null : metaResultObject.getGenericSetterType(property).getKey();
  602 |         typeHandler = rsw.getTypeHandler(javaType, column);
  603 |         if (typeHandler == null) {
  604 |           throw new ExecutorException(
  605 |               "No type handler found for '" + javaType + "' and JDBC type '" + rsw.getJdbcType(column) + "'");
  606 |         }
  607 |       }
  608 |       return typeHandler.getResult(rs, column);
  609 |     }
  610 |   }
  611 | 
  612 |   private List<Object> getNestedCursorValue(ResultSetWrapper rsw, ResultMapping propertyMapping,
  613 |       String parentColumnPrefix) throws SQLException {
  614 |     final String column = prependPrefix(propertyMapping.getColumn(), parentColumnPrefix);
  615 |     ResultMap nestedResultMap = resolveDiscriminatedResultMap(rsw,
  616 |         configuration.getResultMap(propertyMapping.getNestedResultMapId()),
  617 |         getColumnPrefix(parentColumnPrefix, propertyMapping));
  618 |     ResultSetWrapper nestedRsw = new ResultSetWrapper(rsw.getResultSet().getObject(column, ResultSet.class),
  619 |         configuration);
  620 |     List<Object> results = new ArrayList<>();
  621 |     handleResultSet(nestedRsw, nestedResultMap, results, null);
  622 |     return results;
  623 |   }
  624 | 
  625 |   private List<UnMappedColumnAutoMapping> createAutomaticMappings(ResultSetWrapper rsw, ResultMap resultMap,
  626 |       MetaObject metaObject, String columnPrefix) throws SQLException {
  627 |     final String mapKey = resultMap.getId() + ":" + columnPrefix;
  628 |     List<UnMappedColumnAutoMapping> autoMapping = autoMappingsCache.get(mapKey);
  629 |     if (autoMapping == null) {
  630 |       autoMapping = new ArrayList<>();
  631 |       final List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);
  632 |       // Remove the entry to release the memory
  633 |       List<String> mappedInConstructorAutoMapping = constructorAutoMappingColumns.remove(mapKey);
  634 |       if (mappedInConstructorAutoMapping != null) {
  635 |         unmappedColumnNames.removeAll(mappedInConstructorAutoMapping);
  636 |       }
  637 |       for (String columnName : unmappedColumnNames) {
  638 |         String propertyName = columnName;
  639 |         if (columnPrefix != null && !columnPrefix.isEmpty()) {
  640 |           // When columnPrefix is specified,
  641 |           // ignore columns without the prefix.
  642 |           if (!columnName.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
  643 |             continue;
  644 |           }
  645 |           propertyName = columnName.substring(columnPrefix.length());
  646 |         }
  647 |         final String property = metaObject.findProperty(propertyName, configuration.isMapUnderscoreToCamelCase());
  648 |         if (property != null && metaObject.hasSetter(property)) {
  649 |           if (resultMap.getMappedProperties().contains(property)) {
  650 |             continue;
  651 |           }
  652 |           final Type propertyType = metaObject.getGenericSetterType(property).getKey();
  653 |           TypeHandler<?> typeHandler = rsw.getTypeHandler(propertyType, columnName);
  654 |           if (typeHandler != null) {
  655 |             autoMapping.add(new UnMappedColumnAutoMapping(columnName, property, typeHandler,
  656 |                 propertyType instanceof Class && ((Class<?>) propertyType).isPrimitive()));
  657 |           } else {
  658 |             configuration.getAutoMappingUnknownColumnBehavior().doAction(mappedStatement, columnName, property,
  659 |                 propertyType);
  660 |           }
  661 |         } else {
  662 |           configuration.getAutoMappingUnknownColumnBehavior().doAction(mappedStatement, columnName,
  663 |               property != null ? property : propertyName, null);
  664 |         }
  665 |       }
  666 |       autoMappingsCache.put(mapKey, autoMapping);
  667 |     }
  668 |     return autoMapping;
  669 |   }
  670 | 
  671 |   private boolean applyAutomaticMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject,
  672 |       String columnPrefix) throws SQLException {
  673 |     List<UnMappedColumnAutoMapping> autoMapping = createAutomaticMappings(rsw, resultMap, metaObject, columnPrefix);
  674 |     boolean foundValues = false;
  675 |     if (!autoMapping.isEmpty()) {
  676 |       for (UnMappedColumnAutoMapping mapping : autoMapping) {
  677 |         final Object value = mapping.typeHandler.getResult(rsw.getResultSet(), mapping.column);
  678 |         if (value != null) {
  679 |           foundValues = true;
  680 |         }
  681 |         if (value != null || configuration.isCallSettersOnNulls() && !mapping.primitive) {
  682 |           // gcode issue #377, call setter on nulls (value is not 'found')
  683 |           metaObject.setValue(mapping.property, value);
  684 |         }
  685 |       }
  686 |     }
  687 |     return foundValues;
  688 |   }
  689 | 
  690 |   // MULTIPLE RESULT SETS
  691 | 
  692 |   private void linkToParents(ResultSet rs, ResultMapping parentMapping, Object rowValue) throws SQLException {
  693 |     CacheKey parentKey = createKeyForMultipleResults(rs, parentMapping, parentMapping.getColumn(),
  694 |         parentMapping.getForeignColumn());
  695 |     List<PendingRelation> parents = pendingRelations.get(parentKey);
  696 |     if (parents != null) {
  697 |       for (PendingRelation parent : parents) {
  698 |         if (parent != null && rowValue != null) {
  699 |           linkObjects(parent.metaObject, parent.propertyMapping, rowValue);
  700 |         }
  701 |       }
  702 |     }
  703 |   }
  704 | 
  705 |   private void addPendingChildRelation(ResultSet rs, MetaObject metaResultObject, ResultMapping parentMapping)
  706 |       throws SQLException {
  707 |     CacheKey cacheKey = createKeyForMultipleResults(rs, parentMapping, parentMapping.getColumn(),
  708 |         parentMapping.getColumn());
  709 |     PendingRelation deferLoad = new PendingRelation();
  710 |     deferLoad.metaObject = metaResultObject;
  711 |     deferLoad.propertyMapping = parentMapping;
  712 |     List<PendingRelation> relations = pendingRelations.computeIfAbsent(cacheKey, k -> new ArrayList<>());
  713 |     // issue #255
  714 |     relations.add(deferLoad);
  715 |     ResultMapping previous = nextResultMaps.get(parentMapping.getResultSet());
  716 |     if (previous == null) {
  717 |       nextResultMaps.put(parentMapping.getResultSet(), parentMapping);
  718 |     } else if (!previous.equals(parentMapping)) {
  719 |       throw new ExecutorException("Two different properties are mapped to the same resultSet");
  720 |     }
  721 |   }
  722 | 
  723 |   private CacheKey createKeyForMultipleResults(ResultSet rs, ResultMapping resultMapping, String names, String columns)
  724 |       throws SQLException {
  725 |     CacheKey cacheKey = new CacheKey();
  726 |     cacheKey.update(resultMapping);
  727 |     if (columns != null && names != null) {
  728 |       String[] columnsArray = columns.split(",");
  729 |       String[] namesArray = names.split(",");
  730 |       for (int i = 0; i < columnsArray.length; i++) {
  731 |         Object value = rs.getString(columnsArray[i]);
  732 |         if (value != null) {
  733 |           cacheKey.update(namesArray[i]);
  734 |           cacheKey.update(value);
  735 |         }
  736 |       }
  737 |     }
  738 |     return cacheKey;
  739 |   }
  740 | 
  741 |   //
  742 |   // INSTANTIATION & CONSTRUCTOR MAPPING
  743 |   //
  744 | 
  745 |   private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, ResultLoaderMap lazyLoader,
  746 |       String columnPrefix, CacheKey parentRowKey) throws SQLException {
  747 |     this.useConstructorMappings = false; // reset previous mapping result
  748 |     final List<Class<?>> constructorArgTypes = new ArrayList<>();
  749 |     final List<Object> constructorArgs = new ArrayList<>();
  750 | 
  751 |     Object resultObject = createResultObject(rsw, resultMap, constructorArgTypes, constructorArgs, columnPrefix,
  752 |         parentRowKey);
  753 |     if (resultObject != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
  754 |       final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
  755 |       for (ResultMapping propertyMapping : propertyMappings) {
  756 |         // issue gcode #109 && issue #149
  757 |         if (propertyMapping.getNestedQueryId() != null && propertyMapping.isLazy()) {
  758 |           resultObject = configuration.getProxyFactory().createProxy(resultObject, lazyLoader, configuration,
  759 |               objectFactory, constructorArgTypes, constructorArgs);
  760 |           break;
  761 |         }
  762 |       }
  763 | 
  764 |       // (issue #101)
  765 |       if (resultMap.hasResultMapsUsingConstructorCollection() && resultObject instanceof PendingConstructorCreation) {
  766 |         linkNestedPendingCreations(rsw, resultMap, columnPrefix, parentRowKey,
  767 |             (PendingConstructorCreation) resultObject, constructorArgs);
  768 |       }
  769 |     }
  770 | 
  771 |     this.useConstructorMappings = resultObject != null && !constructorArgTypes.isEmpty(); // set current mapping result
  772 |     return resultObject;
  773 |   }
  774 | 
  775 |   private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, List<Class<?>> constructorArgTypes,
  776 |       List<Object> constructorArgs, String columnPrefix, CacheKey parentRowKey) throws SQLException {
  777 | 
  778 |     final Class<?> resultType = resultMap.getType();
  779 |     final MetaClass metaType = MetaClass.forClass(resultType, reflectorFactory);
  780 |     final List<ResultMapping> constructorMappings = resultMap.getConstructorResultMappings();
  781 |     if (hasTypeHandlerForResultObject(rsw, resultType)) {
  782 |       return createPrimitiveResultObject(rsw, resultMap, columnPrefix);
  783 |     }
  784 |     if (!constructorMappings.isEmpty()) {
  785 |       return createParameterizedResultObject(rsw, resultType, constructorMappings, constructorArgTypes, constructorArgs,
  786 |           columnPrefix, resultMap.hasResultMapsUsingConstructorCollection(), parentRowKey);
  787 |     } else if (resultType.isInterface() || metaType.hasDefaultConstructor()) {
  788 |       return objectFactory.create(resultType);
  789 |     } else if (shouldApplyAutomaticMappings(resultMap, false)) {
  790 |       return createByConstructorSignature(rsw, resultMap, columnPrefix, resultType, constructorArgTypes,
  791 |           constructorArgs);
  792 |     }
  793 |     throw new ExecutorException("Do not know how to create an instance of " + resultType);
  794 |   }
  795 | 
  796 |   Object createParameterizedResultObject(ResultSetWrapper rsw, Class<?> resultType,
  797 |       List<ResultMapping> constructorMappings, List<Class<?>> constructorArgTypes, List<Object> constructorArgs,
  798 |       String columnPrefix, boolean useCollectionConstructorInjection, CacheKey parentRowKey) {
  799 |     boolean foundValues = false;
  800 | 
  801 |     for (ResultMapping constructorMapping : constructorMappings) {
  802 |       final Class<?> parameterType = constructorMapping.getJavaType();
  803 |       final String column = constructorMapping.getColumn();
  804 |       final Object value;
  805 |       try {
  806 |         if (constructorMapping.getNestedQueryId() != null) {
  807 |           value = getNestedQueryConstructorValue(rsw, constructorMapping, columnPrefix);
  808 |         } else if (JdbcType.CURSOR.equals(constructorMapping.getJdbcType())) {
  809 |           List<?> result = (List<?>) getNestedCursorValue(rsw, constructorMapping, columnPrefix).get(0);
  810 |           if (objectFactory.isCollection(parameterType)) {
  811 |             MetaObject collection = configuration.newMetaObject(objectFactory.create(parameterType));
  812 |             collection.addAll((List<?>) result);
  813 |             value = collection.getOriginalObject();
  814 |           } else {
  815 |             value = toSingleObj(result);
  816 |           }
  817 |         } else if (constructorMapping.getNestedResultMapId() != null) {
  818 |           final String constructorColumnPrefix = getColumnPrefix(columnPrefix, constructorMapping);
  819 |           final ResultMap resultMap = resolveDiscriminatedResultMap(rsw,
  820 |               configuration.getResultMap(constructorMapping.getNestedResultMapId()), constructorColumnPrefix);
  821 |           value = getRowValue(rsw, resultMap, constructorColumnPrefix,
  822 |               useCollectionConstructorInjection ? parentRowKey : null);
  823 |         } else {
  824 |           TypeHandler<?> typeHandler = constructorMapping.getTypeHandler();
  825 |           if (typeHandler == null) {
  826 |             typeHandler = typeHandlerRegistry.getTypeHandler(constructorMapping.getJavaType(), rsw.getJdbcType(column));
  827 |           }
  828 |           value = typeHandler.getResult(rsw.getResultSet(), prependPrefix(column, columnPrefix));
  829 |         }
  830 |       } catch (ResultMapException | SQLException e) {
  831 |         throw new ExecutorException("Could not process result for mapping: " + constructorMapping, e);
  832 |       }
  833 | 
  834 |       constructorArgTypes.add(parameterType);
  835 |       constructorArgs.add(value);
  836 | 
  837 |       foundValues = value != null || foundValues;
  838 |     }
  839 | 
  840 |     if (!foundValues) {
  841 |       return null;
  842 |     }
  843 | 
  844 |     if (useCollectionConstructorInjection) {
  845 |       // at least one of the nestedResultMaps contained a collection, we have to defer until later
  846 |       return new PendingConstructorCreation(resultType, constructorArgTypes, constructorArgs);
  847 |     }
  848 | 
  849 |     return objectFactory.create(resultType, constructorArgTypes, constructorArgs);
  850 |   }
  851 | 
  852 |   private Object createByConstructorSignature(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix,
  853 |       Class<?> resultType, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) throws SQLException {
  854 |     return applyConstructorAutomapping(rsw, resultMap, columnPrefix, resultType, constructorArgTypes, constructorArgs,
  855 |         findConstructorForAutomapping(resultType, rsw).orElseThrow(() -> new ExecutorException(
  856 |             "No constructor found in " + resultType.getName() + " matching " + rsw.getClassNames())));
  857 |   }
  858 | 
  859 |   private Optional<Constructor<?>> findConstructorForAutomapping(final Class<?> resultType, ResultSetWrapper rsw) {
  860 |     Constructor<?>[] constructors = resultType.getDeclaredConstructors();
  861 |     if (constructors.length == 1) {
  862 |       return Optional.of(constructors[0]);
  863 |     }
  864 |     Optional<Constructor<?>> annotated = Arrays.stream(constructors)
  865 |         .filter(x -> x.isAnnotationPresent(AutomapConstructor.class)).reduce((x, y) -> {
  866 |           throw new ExecutorException("@AutomapConstructor should be used in only one constructor.");
  867 |         });
  868 |     if (annotated.isPresent()) {
  869 |       return annotated;
  870 |     }
  871 |     if (configuration.isArgNameBasedConstructorAutoMapping()) {
  872 |       // Finding-best-match type implementation is possible,
  873 |       // but using @AutomapConstructor seems sufficient.
  874 |       throw new ExecutorException(MessageFormat.format(
  875 |           "'argNameBasedConstructorAutoMapping' is enabled and the class ''{0}'' has multiple constructors, so @AutomapConstructor must be added to one of the constructors.",
  876 |           resultType.getName()));
  877 |     } else {
  878 |       return Arrays.stream(constructors).filter(x -> findUsableConstructorByArgTypes(x, rsw.getJdbcTypes())).findAny();
  879 |     }
  880 |   }
  881 | 
  882 |   private boolean findUsableConstructorByArgTypes(final Constructor<?> constructor, final List<JdbcType> jdbcTypes) {
  883 |     final Class<?>[] parameterTypes = constructor.getParameterTypes();
  884 |     if (parameterTypes.length != jdbcTypes.size()) {
  885 |       return false;
  886 |     }
  887 |     for (int i = 0; i < parameterTypes.length; i++) {
  888 |       if (!typeHandlerRegistry.hasTypeHandler(parameterTypes[i], jdbcTypes.get(i))) {
  889 |         return false;
  890 |       }
  891 |     }
  892 |     return true;
  893 |   }
  894 | 
  895 |   private Object applyConstructorAutomapping(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix,
  896 |       Class<?> resultType, List<Class<?>> constructorArgTypes, List<Object> constructorArgs, Constructor<?> constructor)
  897 |       throws SQLException {
  898 |     boolean foundValues = false;
  899 |     if (configuration.isArgNameBasedConstructorAutoMapping()) {
  900 |       foundValues = applyArgNameBasedConstructorAutoMapping(rsw, resultMap, columnPrefix, constructorArgTypes,
  901 |           constructorArgs, constructor, foundValues);
  902 |     } else {
  903 |       foundValues = applyColumnOrderBasedConstructorAutomapping(rsw, constructorArgTypes, constructorArgs, constructor,
  904 |           foundValues);
  905 |     }
  906 |     return foundValues || configuration.isReturnInstanceForEmptyRow()
  907 |         ? objectFactory.create(resultType, constructorArgTypes, constructorArgs) : null;
  908 |   }
  909 | 
  910 |   private boolean applyColumnOrderBasedConstructorAutomapping(ResultSetWrapper rsw, List<Class<?>> constructorArgTypes,
  911 |       List<Object> constructorArgs, Constructor<?> constructor, boolean foundValues) throws SQLException {
  912 |     Class<?>[] parameterTypes = constructor.getParameterTypes();
  913 | 
  914 |     if (parameterTypes.length > rsw.getClassNames().size()) {
  915 |       throw new ExecutorException(MessageFormat.format(
  916 |           "Constructor auto-mapping of ''{0}'' failed. The constructor takes ''{1}'' arguments, but there are only ''{2}'' columns in the result set.",
  917 |           constructor, parameterTypes.length, rsw.getClassNames().size()));
  918 |     }
  919 | 
  920 |     for (int i = 0; i < parameterTypes.length; i++) {
  921 |       Class<?> parameterType = parameterTypes[i];
  922 |       String columnName = rsw.getColumnNames().get(i);
  923 |       TypeHandler<?> typeHandler = rsw.getTypeHandler(parameterType, columnName);
  924 |       Object value = typeHandler.getResult(rsw.getResultSet(), columnName);
  925 |       constructorArgTypes.add(parameterType);
  926 |       constructorArgs.add(value);
  927 |       foundValues = value != null || foundValues;
  928 |     }
  929 |     return foundValues;
  930 |   }
  931 | 
  932 |   private boolean applyArgNameBasedConstructorAutoMapping(ResultSetWrapper rsw, ResultMap resultMap,
  933 |       String columnPrefix, List<Class<?>> constructorArgTypes, List<Object> constructorArgs, Constructor<?> constructor,
  934 |       boolean foundValues) throws SQLException {
  935 |     List<String> missingArgs = null;
  936 |     Parameter[] params = constructor.getParameters();
  937 |     for (Parameter param : params) {
  938 |       boolean columnNotFound = true;
  939 |       Param paramAnno = param.getAnnotation(Param.class);
  940 |       String paramName = paramAnno == null ? param.getName() : paramAnno.value();
  941 |       for (String columnName : rsw.getColumnNames()) {
  942 |         if (columnMatchesParam(columnName, paramName, columnPrefix)) {
  943 |           Class<?> paramType = param.getType();
  944 |           TypeHandler<?> typeHandler = rsw.getTypeHandler(paramType, columnName);
  945 |           Object value = typeHandler.getResult(rsw.getResultSet(), columnName);
  946 |           constructorArgTypes.add(paramType);
  947 |           constructorArgs.add(value);
  948 |           final String mapKey = resultMap.getId() + ":" + columnPrefix;
  949 |           if (!autoMappingsCache.containsKey(mapKey)) {
  950 |             constructorAutoMappingColumns.computeIfAbsent(mapKey, k -> new ArrayList<>()).add(columnName);
  951 |           }
  952 |           columnNotFound = false;
  953 |           foundValues = value != null || foundValues;
  954 |         }
  955 |       }
  956 |       if (columnNotFound) {
  957 |         if (missingArgs == null) {
  958 |           missingArgs = new ArrayList<>();
  959 |         }
  960 |         missingArgs.add(paramName);
  961 |       }
  962 |     }
  963 |     if (foundValues && constructorArgs.size() < params.length) {
  964 |       throw new ExecutorException(MessageFormat.format(
  965 |           "Constructor auto-mapping of ''{1}'' failed " + "because ''{0}'' were not found in the result set; "
  966 |               + "Available columns are ''{2}'' and mapUnderscoreToCamelCase is ''{3}''.",
  967 |           missingArgs, constructor, rsw.getColumnNames(), configuration.isMapUnderscoreToCamelCase()));
  968 |     }
  969 |     return foundValues;
  970 |   }
  971 | 
  972 |   private boolean columnMatchesParam(String columnName, String paramName, String columnPrefix) {
  973 |     if (columnPrefix != null) {
  974 |       if (!columnName.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
  975 |         return false;
  976 |       }
  977 |       columnName = columnName.substring(columnPrefix.length());
  978 |     }
  979 |     return paramName
  980 |         .equalsIgnoreCase(configuration.isMapUnderscoreToCamelCase() ? columnName.replace("_", "") : columnName);
  981 |   }
  982 | 
  983 |   private Object createPrimitiveResultObject(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix)
  984 |       throws SQLException {
  985 |     final Class<?> resultType = resultMap.getType();
  986 |     final String columnName;
  987 |     if (!resultMap.getResultMappings().isEmpty()) {
  988 |       final List<ResultMapping> resultMappingList = resultMap.getResultMappings();
  989 |       final ResultMapping mapping = resultMappingList.get(0);
  990 |       columnName = prependPrefix(mapping.getColumn(), columnPrefix);
  991 |     } else {
  992 |       columnName = rsw.getColumnNames().get(0);
  993 |     }
  994 |     final TypeHandler<?> typeHandler = rsw.getTypeHandler(resultType, columnName);
  995 |     return typeHandler.getResult(rsw.getResultSet(), columnName);
  996 |   }
  997 | 
  998 |   //
  999 |   // NESTED QUERY
 1000 |   //
 1001 | 
 1002 |   private Object getNestedQueryConstructorValue(ResultSetWrapper rsw, ResultMapping constructorMapping,
 1003 |       String columnPrefix) throws SQLException {
 1004 |     final String nestedQueryId = constructorMapping.getNestedQueryId();
 1005 |     final MappedStatement nestedQuery = configuration.getMappedStatement(nestedQueryId);
 1006 |     final Class<?> nestedQueryParameterType = nestedQuery.getParameterMap().getType();
 1007 |     final Object nestedQueryParameterObject = prepareParameterForNestedQuery(rsw, constructorMapping,
 1008 |         nestedQueryParameterType, columnPrefix);
 1009 |     Object value = null;
 1010 |     if (nestedQueryParameterObject != null) {
 1011 |       final BoundSql nestedBoundSql = nestedQuery.getBoundSql(nestedQueryParameterObject);
 1012 |       final CacheKey key = executor.createCacheKey(nestedQuery, nestedQueryParameterObject, RowBounds.DEFAULT,
 1013 |           nestedBoundSql);
 1014 |       final Class<?> targetType = constructorMapping.getJavaType();
 1015 |       final ResultLoader resultLoader = new ResultLoader(configuration, executor, nestedQuery,
 1016 |           nestedQueryParameterObject, targetType, key, nestedBoundSql);
 1017 |       value = resultLoader.loadResult();
 1018 |     }
 1019 |     return value;
 1020 |   }
 1021 | 
 1022 |   private Object getNestedQueryMappingValue(ResultSetWrapper rsw, MetaObject metaResultObject,
 1023 |       ResultMapping propertyMapping, ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {
 1024 |     final String nestedQueryId = propertyMapping.getNestedQueryId();
 1025 |     final String property = propertyMapping.getProperty();
 1026 |     final MappedStatement nestedQuery = configuration.getMappedStatement(nestedQueryId);
 1027 |     final Class<?> nestedQueryParameterType = nestedQuery.getParameterMap().getType();
 1028 |     final Object nestedQueryParameterObject = prepareParameterForNestedQuery(rsw, propertyMapping,
 1029 |         nestedQueryParameterType, columnPrefix);
 1030 |     Object value = null;
 1031 |     if (nestedQueryParameterObject != null) {
 1032 |       final BoundSql nestedBoundSql = nestedQuery.getBoundSql(nestedQueryParameterObject);
 1033 |       final CacheKey key = executor.createCacheKey(nestedQuery, nestedQueryParameterObject, RowBounds.DEFAULT,
 1034 |           nestedBoundSql);
 1035 |       final Class<?> targetType = propertyMapping.getJavaType();
 1036 |       if (executor.isCached(nestedQuery, key)) {
 1037 |         executor.deferLoad(nestedQuery, metaResultObject, property, key, targetType);
 1038 |         value = DEFERRED;
 1039 |       } else {
 1040 |         final ResultLoader resultLoader = new ResultLoader(configuration, executor, nestedQuery,
 1041 |             nestedQueryParameterObject, targetType, key, nestedBoundSql);
 1042 |         if (propertyMapping.isLazy()) {
 1043 |           lazyLoader.addLoader(property, metaResultObject, resultLoader);
 1044 |           value = DEFERRED;
 1045 |         } else {
 1046 |           value = resultLoader.loadResult();
 1047 |         }
 1048 |       }
 1049 |     }
 1050 |     return value;
 1051 |   }
 1052 | 
 1053 |   private Object prepareParameterForNestedQuery(ResultSetWrapper rsw, ResultMapping resultMapping,
 1054 |       Class<?> parameterType, String columnPrefix) throws SQLException {
 1055 |     if (resultMapping.isCompositeResult()) {
 1056 |       return prepareCompositeKeyParameter(rsw, resultMapping, parameterType, columnPrefix);
 1057 |     }
 1058 |     return prepareSimpleKeyParameter(rsw, resultMapping, parameterType, columnPrefix);
 1059 |   }
 1060 | 
 1061 |   private Object prepareSimpleKeyParameter(ResultSetWrapper rsw, ResultMapping resultMapping, Class<?> parameterType,
 1062 |       String columnPrefix) throws SQLException {
 1063 |     // parameterType is ignored in this case
 1064 |     final String columnName = prependPrefix(resultMapping.getColumn(), columnPrefix);
 1065 |     final TypeHandler<?> typeHandler = rsw.getTypeHandler(null, columnName);
 1066 |     return typeHandler.getResult(rsw.getResultSet(), columnName);
 1067 |   }
 1068 | 
 1069 |   private Object prepareCompositeKeyParameter(ResultSetWrapper rsw, ResultMapping resultMapping, Class<?> parameterType,
 1070 |       String columnPrefix) throws SQLException {
 1071 |     // Map is used if parameterType is not specified
 1072 |     final Object parameterObject = instantiateParameterObject(parameterType);
 1073 |     final MetaObject metaObject = configuration.newMetaObject(parameterObject);
 1074 |     boolean foundValues = false;
 1075 |     for (ResultMapping innerResultMapping : resultMapping.getComposites()) {
 1076 |       final String columnName = prependPrefix(innerResultMapping.getColumn(), columnPrefix);
 1077 |       final TypeHandler<?> typeHandler = rsw
 1078 |           .getTypeHandler(metaObject.getGenericSetterType(innerResultMapping.getProperty()).getKey(), columnName);
 1079 |       final Object propValue = typeHandler.getResult(rsw.getResultSet(), columnName);
 1080 |       // issue #353 & #560 do not execute nested query if key is null
 1081 |       if (propValue != null) {
 1082 |         metaObject.setValue(innerResultMapping.getProperty(), propValue);
 1083 |         foundValues = true;
 1084 |       }
 1085 |     }
 1086 |     return foundValues ? parameterObject : null;
 1087 |   }
 1088 | 
 1089 |   private Object instantiateParameterObject(Class<?> parameterType) {
 1090 |     if (parameterType == null) {
 1091 |       return new HashMap<>();
 1092 |     }
 1093 |     if (ParamMap.class.equals(parameterType)) {
 1094 |       return new HashMap<>(); // issue #649
 1095 |     } else {
 1096 |       return objectFactory.create(parameterType);
 1097 |     }
 1098 |   }
 1099 | 
 1100 |   //
 1101 |   // DISCRIMINATOR
 1102 |   //
 1103 | 
 1104 |   public ResultMap resolveDiscriminatedResultMap(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix)
 1105 |       throws SQLException {
 1106 |     Set<String> pastDiscriminators = new HashSet<>();
 1107 |     Discriminator discriminator = resultMap.getDiscriminator();
 1108 |     while (discriminator != null) {
 1109 |       final Object value = getDiscriminatorValue(rsw, discriminator, columnPrefix);
 1110 |       final String discriminatedMapId = discriminator.getMapIdFor(String.valueOf(value));
 1111 |       if (!configuration.hasResultMap(discriminatedMapId)) {
 1112 |         break;
 1113 |       }
 1114 |       resultMap = configuration.getResultMap(discriminatedMapId);
 1115 |       Discriminator lastDiscriminator = discriminator;
 1116 |       discriminator = resultMap.getDiscriminator();
 1117 |       if (discriminator == lastDiscriminator || !pastDiscriminators.add(discriminatedMapId)) {
 1118 |         break;
 1119 |       }
 1120 |     }
 1121 |     return resultMap;
 1122 |   }
 1123 | 
 1124 |   private Object getDiscriminatorValue(ResultSetWrapper rsw, Discriminator discriminator, String columnPrefix)
 1125 |       throws SQLException {
 1126 |     final ResultMapping resultMapping = discriminator.getResultMapping();
 1127 |     String column = prependPrefix(resultMapping.getColumn(), columnPrefix);
 1128 |     TypeHandler<?> typeHandler = resultMapping.getTypeHandler();
 1129 |     if (typeHandler == null) {
 1130 |       typeHandler = typeHandlerRegistry.getTypeHandler(resultMapping.getJavaType(), rsw.getJdbcType(column));
 1131 |     }
 1132 |     return typeHandler.getResult(rsw.getResultSet(), column);
 1133 |   }
 1134 | 
 1135 |   private String prependPrefix(String columnName, String prefix) {
 1136 |     if (columnName == null || columnName.length() == 0 || prefix == null || prefix.length() == 0) {
 1137 |       return columnName;
 1138 |     }
 1139 |     return prefix + columnName;
 1140 |   }
 1141 | 
 1142 |   //
 1143 |   // HANDLE NESTED RESULT MAPS
 1144 |   //
 1145 | 
 1146 |   private void handleRowValuesForNestedResultMap(ResultSetWrapper rsw, ResultMap resultMap,
 1147 |       ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
 1148 |     final boolean useCollectionConstructorInjection = resultMap.hasResultMapsUsingConstructorCollection();
 1149 |     PendingConstructorCreation lastHandledCreation = null;
 1150 |     if (useCollectionConstructorInjection) {
 1151 |       verifyPendingCreationPreconditions(parentMapping);
 1152 |     }
 1153 | 
 1154 |     final DefaultResultContext<Object> resultContext = new DefaultResultContext<>();
 1155 |     ResultSet resultSet = rsw.getResultSet();
 1156 |     skipRows(resultSet, rowBounds);
 1157 |     Object rowValue = previousRowValue;
 1158 | 
 1159 |     while (shouldProcessMoreRows(resultContext, rowBounds) && !resultSet.isClosed() && resultSet.next()) {
 1160 |       final ResultMap discriminatedResultMap = resolveDiscriminatedResultMap(rsw, resultMap, null);
 1161 |       final CacheKey rowKey = createRowKey(discriminatedResultMap, rsw, null);
 1162 | 
 1163 |       final Object partialObject = nestedResultObjects.get(rowKey);
 1164 |       final boolean foundNewUniqueRow = partialObject == null;
 1165 | 
 1166 |       // issue #577, #542 && #101
 1167 |       if (useCollectionConstructorInjection) {
 1168 |         if (foundNewUniqueRow && lastHandledCreation != null) {
 1169 |           createAndStorePendingCreation(resultHandler, resultSet, resultContext, lastHandledCreation);
 1170 |           lastHandledCreation = null;
 1171 |         }
 1172 | 
 1173 |         rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, null, partialObject);
 1174 |         if (rowValue instanceof PendingConstructorCreation) {
 1175 |           lastHandledCreation = (PendingConstructorCreation) rowValue;
 1176 |         }
 1177 |       } else if (mappedStatement.isResultOrdered()) {
 1178 |         if (foundNewUniqueRow && rowValue != null) {
 1179 |           nestedResultObjects.clear();
 1180 |           storeObject(resultHandler, resultContext, rowValue, parentMapping, resultSet);
 1181 |         }
 1182 |         rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, null, partialObject);
 1183 |       } else {
 1184 |         rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, null, partialObject);
 1185 |         if (foundNewUniqueRow) {
 1186 |           storeObject(resultHandler, resultContext, rowValue, parentMapping, resultSet);
 1187 |         }
 1188 |       }
 1189 |     }
 1190 | 
 1191 |     if (useCollectionConstructorInjection && lastHandledCreation != null) {
 1192 |       createAndStorePendingCreation(resultHandler, resultSet, resultContext, lastHandledCreation);
 1193 |     } else if (rowValue != null && mappedStatement.isResultOrdered()
 1194 |         && shouldProcessMoreRows(resultContext, rowBounds)) {
 1195 |       storeObject(resultHandler, resultContext, rowValue, parentMapping, resultSet);
 1196 |       previousRowValue = null;
 1197 |     } else if (rowValue != null) {
 1198 |       previousRowValue = rowValue;
 1199 |     }
 1200 |   }
 1201 | 
 1202 |   //
 1203 |   // NESTED RESULT MAP (PENDING CONSTRUCTOR CREATIONS)
 1204 |   //
 1205 |   private void linkNestedPendingCreations(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix,
 1206 |       CacheKey parentRowKey, PendingConstructorCreation pendingCreation, List<Object> constructorArgs)
 1207 |       throws SQLException {
 1208 |     if (parentRowKey == null) {
 1209 |       // nothing to link, possibly due to simple (non-nested) result map
 1210 |       return;
 1211 |     }
 1212 | 
 1213 |     final CacheKey rowKey = createRowKey(resultMap, rsw, columnPrefix);
 1214 |     final CacheKey combinedKey = combineKeys(rowKey, parentRowKey);
 1215 | 
 1216 |     if (combinedKey != CacheKey.NULL_CACHE_KEY) {
 1217 |       nestedResultObjects.put(combinedKey, pendingCreation);
 1218 |     }
 1219 | 
 1220 |     final List<ResultMapping> constructorMappings = resultMap.getConstructorResultMappings();
 1221 |     for (int index = 0; index < constructorMappings.size(); index++) {
 1222 |       final ResultMapping constructorMapping = constructorMappings.get(index);
 1223 |       final String nestedResultMapId = constructorMapping.getNestedResultMapId();
 1224 | 
 1225 |       if (nestedResultMapId == null) {
 1226 |         continue;
 1227 |       }
 1228 | 
 1229 |       final Class<?> javaType = constructorMapping.getJavaType();
 1230 |       if (javaType == null || !objectFactory.isCollection(javaType)) {
 1231 |         continue;
 1232 |       }
 1233 | 
 1234 |       final String constructorColumnPrefix = getColumnPrefix(columnPrefix, constructorMapping);
 1235 |       final ResultMap nestedResultMap = resolveDiscriminatedResultMap(rsw,
 1236 |           configuration.getResultMap(constructorMapping.getNestedResultMapId()), constructorColumnPrefix);
 1237 | 
 1238 |       final Object actualValue = constructorArgs.get(index);
 1239 |       final boolean hasValue = actualValue != null;
 1240 |       final boolean isInnerCreation = actualValue instanceof PendingConstructorCreation;
 1241 |       final boolean alreadyCreatedCollection = hasValue && objectFactory.isCollection(actualValue.getClass());
 1242 | 
 1243 |       if (!isInnerCreation) {
 1244 |         final Collection<Object> value = pendingCreation.initializeCollectionForResultMapping(objectFactory,
 1245 |             nestedResultMap, constructorMapping, index);
 1246 |         if (!alreadyCreatedCollection) {
 1247 |           // override values with empty collection
 1248 |           constructorArgs.set(index, value);
 1249 |         }
 1250 | 
 1251 |         // since we are linking a new value, we need to let nested objects know we did that
 1252 |         final CacheKey nestedRowKey = createRowKey(nestedResultMap, rsw, constructorColumnPrefix);
 1253 |         final CacheKey nestedCombinedKey = combineKeys(nestedRowKey, combinedKey);
 1254 | 
 1255 |         if (nestedCombinedKey != CacheKey.NULL_CACHE_KEY) {
 1256 |           nestedResultObjects.put(nestedCombinedKey, pendingCreation);
 1257 |         }
 1258 | 
 1259 |         if (hasValue) {
 1260 |           pendingCreation.linkCollectionValue(constructorMapping, actualValue);
 1261 |         }
 1262 |       } else {
 1263 |         final PendingConstructorCreation innerCreation = (PendingConstructorCreation) actualValue;
 1264 |         final Collection<Object> value = pendingCreation.initializeCollectionForResultMapping(objectFactory,
 1265 |             nestedResultMap, constructorMapping, index);
 1266 |         // we will fill this collection when building the final object
 1267 |         constructorArgs.set(index, value);
 1268 |         // link the creation for building later
 1269 |         pendingCreation.linkCreation(constructorMapping, innerCreation);
 1270 |       }
 1271 |     }
 1272 |   }
 1273 | 
 1274 |   private boolean applyNestedPendingConstructorCreations(ResultSetWrapper rsw, ResultMap resultMap,
 1275 |       MetaObject metaObject, String parentPrefix, CacheKey parentRowKey, boolean newObject, boolean foundValues) {
 1276 |     if (newObject) {
 1277 |       // new objects are linked by createResultObject
 1278 |       return false;
 1279 |     }
 1280 | 
 1281 |     for (ResultMapping constructorMapping : resultMap.getConstructorResultMappings()) {
 1282 |       final String nestedResultMapId = constructorMapping.getNestedResultMapId();
 1283 |       final Class<?> parameterType = constructorMapping.getJavaType();
 1284 |       if (nestedResultMapId == null || constructorMapping.getResultSet() != null || parameterType == null
 1285 |           || !objectFactory.isCollection(parameterType)) {
 1286 |         continue;
 1287 |       }
 1288 | 
 1289 |       try {
 1290 |         final String columnPrefix = getColumnPrefix(parentPrefix, constructorMapping);
 1291 |         final ResultMap nestedResultMap = getNestedResultMap(rsw, nestedResultMapId, columnPrefix);
 1292 | 
 1293 |         final CacheKey rowKey = createRowKey(nestedResultMap, rsw, columnPrefix);
 1294 |         final CacheKey combinedKey = combineKeys(rowKey, parentRowKey);
 1295 | 
 1296 |         // should have inserted already as a nested result object
 1297 |         Object rowValue = nestedResultObjects.get(combinedKey);
 1298 | 
 1299 |         PendingConstructorCreation pendingConstructorCreation = null;
 1300 |         if (rowValue instanceof PendingConstructorCreation) {
 1301 |           pendingConstructorCreation = (PendingConstructorCreation) rowValue;
 1302 |         } else if (rowValue != null) {
 1303 |           // found a simple object that was already linked/handled
 1304 |           continue;
 1305 |         }
 1306 | 
 1307 |         final boolean newValueForNestedResultMap = pendingConstructorCreation == null;
 1308 |         if (newValueForNestedResultMap) {
 1309 |           final Object parentObject = metaObject.getOriginalObject();
 1310 |           if (!(parentObject instanceof PendingConstructorCreation)) {
 1311 |             throw new ExecutorException(
 1312 |                 "parentObject is not a pending creation, cannot continue linking! MyBatis internal error!");
 1313 |           }
 1314 | 
 1315 |           pendingConstructorCreation = (PendingConstructorCreation) parentObject;
 1316 |         }
 1317 | 
 1318 |         rowValue = getRowValue(rsw, nestedResultMap, combinedKey, columnPrefix,
 1319 |             newValueForNestedResultMap ? null : pendingConstructorCreation);
 1320 | 
 1321 |         if (rowValue == null) {
 1322 |           continue;
 1323 |         }
 1324 | 
 1325 |         if (rowValue instanceof PendingConstructorCreation) {
 1326 |           if (newValueForNestedResultMap) {
 1327 |             // we created a brand new pcc. this is a new collection value
 1328 |             pendingConstructorCreation.linkCreation(constructorMapping, (PendingConstructorCreation) rowValue);
 1329 |             foundValues = true;
 1330 |           }
 1331 |         } else {
 1332 |           pendingConstructorCreation.linkCollectionValue(constructorMapping, rowValue);
 1333 |           foundValues = true;
 1334 | 
 1335 |           if (combinedKey != CacheKey.NULL_CACHE_KEY) {
 1336 |             nestedResultObjects.put(combinedKey, pendingConstructorCreation);
 1337 |           }
 1338 |         }
 1339 |       } catch (SQLException e) {
 1340 |         throw new ExecutorException("Error getting constructor collection nested result map values for '"
 1341 |             + constructorMapping.getProperty() + "'.  Cause: " + e, e);
 1342 |       }
 1343 |     }
 1344 | 
 1345 |     return foundValues;
 1346 |   }
 1347 | 
 1348 |   private void createPendingConstructorCreations(Object rowValue) {
 1349 |     // handle possible pending creations within this object
 1350 |     // by now, the property mapping has been completely built, we can reconstruct it
 1351 |     final PendingRelation pendingRelation = pendingPccRelations.remove(rowValue);
 1352 |     final MetaObject metaObject = pendingRelation.metaObject;
 1353 |     final ResultMapping resultMapping = pendingRelation.propertyMapping;
 1354 | 
 1355 |     // get the list to be built
 1356 |     Object collectionProperty = instantiateCollectionPropertyIfAppropriate(resultMapping, metaObject);
 1357 |     if (collectionProperty != null) {
 1358 |       // we expect pending creations now
 1359 |       final Collection<Object> pendingCreations = (Collection<Object>) collectionProperty;
 1360 | 
 1361 |       // remove the link to the old collection
 1362 |       metaObject.setValue(resultMapping.getProperty(), null);
 1363 | 
 1364 |       // create new collection property
 1365 |       collectionProperty = instantiateCollectionPropertyIfAppropriate(resultMapping, metaObject);
 1366 |       final MetaObject targetMetaObject = configuration.newMetaObject(collectionProperty);
 1367 | 
 1368 |       // create the pending objects
 1369 |       for (Object pendingCreation : pendingCreations) {
 1370 |         if (pendingCreation instanceof PendingConstructorCreation) {
 1371 |           final PendingConstructorCreation pendingConstructorCreation = (PendingConstructorCreation) pendingCreation;
 1372 |           targetMetaObject.add(pendingConstructorCreation.create(objectFactory));
 1373 |         }
 1374 |       }
 1375 |     }
 1376 |   }
 1377 | 
 1378 |   private void verifyPendingCreationPreconditions(ResultMapping parentMapping) {
 1379 |     if (parentMapping != null) {
 1380 |       throw new ExecutorException(
 1381 |           "Cannot construct objects with collections in constructors using multiple result sets yet!");
 1382 |     }
 1383 | 
 1384 |     if (!mappedStatement.isResultOrdered()) {
 1385 |       throw new ExecutorException("Cannot reliably construct result if we are not sure the results are ordered "
 1386 |           + "so that no new previous rows would occur, set resultOrdered on your mapped statement if you have verified this");
 1387 |     }
 1388 |   }
 1389 | 
 1390 |   private void createAndStorePendingCreation(ResultHandler<?> resultHandler, ResultSet resultSet,
 1391 |       DefaultResultContext<Object> resultContext, PendingConstructorCreation pendingCreation) throws SQLException {
 1392 |     final Object result = pendingCreation.create(objectFactory);
 1393 |     storeObject(resultHandler, resultContext, result, null, resultSet);
 1394 |     nestedResultObjects.clear();
 1395 |   }
 1396 | 
 1397 |   //
 1398 |   // NESTED RESULT MAP (JOIN MAPPING)
 1399 |   //
 1400 | 
 1401 |   private boolean applyNestedResultMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject,
 1402 |       String parentPrefix, CacheKey parentRowKey, boolean newObject) {
 1403 |     boolean foundValues = false;
 1404 |     for (ResultMapping resultMapping : resultMap.getPropertyResultMappings()) {
 1405 |       final String nestedResultMapId = resultMapping.getNestedResultMapId();
 1406 |       if (nestedResultMapId != null && resultMapping.getResultSet() == null) {
 1407 |         try {
 1408 |           final String columnPrefix = getColumnPrefix(parentPrefix, resultMapping);
 1409 |           final ResultMap nestedResultMap = getNestedResultMap(rsw, nestedResultMapId, columnPrefix);
 1410 |           if (resultMapping.getColumnPrefix() == null) {
 1411 |             // try to fill circular reference only when columnPrefix
 1412 |             // is not specified for the nested result map (issue #215)
 1413 |             Object ancestorObject = ancestorObjects.get(nestedResultMapId);
 1414 |             if (ancestorObject != null) {
 1415 |               if (newObject) {
 1416 |                 linkObjects(metaObject, resultMapping, ancestorObject); // issue #385
 1417 |               }
 1418 |               continue;
 1419 |             }
 1420 |           }
 1421 |           final CacheKey rowKey = createRowKey(nestedResultMap, rsw, columnPrefix);
 1422 |           final CacheKey combinedKey = combineKeys(rowKey, parentRowKey);
 1423 |           Object rowValue = nestedResultObjects.get(combinedKey);
 1424 |           boolean knownValue = rowValue != null;
 1425 |           instantiateCollectionPropertyIfAppropriate(resultMapping, metaObject); // mandatory
 1426 |           if (anyNotNullColumnHasValue(resultMapping, columnPrefix, rsw)) {
 1427 |             rowValue = getRowValue(rsw, nestedResultMap, combinedKey, columnPrefix, rowValue);
 1428 |             if (rowValue != null && !knownValue) {
 1429 |               linkObjects(metaObject, resultMapping, rowValue);
 1430 |               foundValues = true;
 1431 |             }
 1432 |           }
 1433 |         } catch (SQLException e) {
 1434 |           throw new ExecutorException(
 1435 |               "Error getting nested result map values for '" + resultMapping.getProperty() + "'.  Cause: " + e, e);
 1436 |         }
 1437 |       }
 1438 |     }
 1439 | 
 1440 |     // (issue #101)
 1441 |     if (resultMap.hasResultMapsUsingConstructorCollection()) {
 1442 |       foundValues = applyNestedPendingConstructorCreations(rsw, resultMap, metaObject, parentPrefix, parentRowKey,
 1443 |           newObject, foundValues);
 1444 |     }
 1445 | 
 1446 |     return foundValues;
 1447 |   }
 1448 | 
 1449 |   private String getColumnPrefix(String parentPrefix, ResultMapping resultMapping) {
 1450 |     final StringBuilder columnPrefixBuilder = new StringBuilder();
 1451 |     if (parentPrefix != null) {
 1452 |       columnPrefixBuilder.append(parentPrefix);
 1453 |     }
 1454 |     if (resultMapping.getColumnPrefix() != null) {
 1455 |       columnPrefixBuilder.append(resultMapping.getColumnPrefix());
 1456 |     }
 1457 |     return columnPrefixBuilder.length() == 0 ? null : columnPrefixBuilder.toString().toUpperCase(Locale.ENGLISH);
 1458 |   }
 1459 | 
 1460 |   private boolean anyNotNullColumnHasValue(ResultMapping resultMapping, String columnPrefix, ResultSetWrapper rsw)
 1461 |       throws SQLException {
 1462 |     Set<String> notNullColumns = resultMapping.getNotNullColumns();
 1463 |     if (notNullColumns != null && !notNullColumns.isEmpty()) {
 1464 |       ResultSet rs = rsw.getResultSet();
 1465 |       for (String column : notNullColumns) {
 1466 |         rs.getObject(prependPrefix(column, columnPrefix));
 1467 |         if (!rs.wasNull()) {
 1468 |           return true;
 1469 |         }
 1470 |       }
 1471 |       return false;
 1472 |     }
 1473 |     if (columnPrefix != null) {
 1474 |       for (String columnName : rsw.getColumnNames()) {
 1475 |         if (columnName.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix.toUpperCase(Locale.ENGLISH))) {
 1476 |           return true;
 1477 |         }
 1478 |       }
 1479 |       return false;
 1480 |     }
 1481 |     return true;
 1482 |   }
 1483 | 
 1484 |   private ResultMap getNestedResultMap(ResultSetWrapper rsw, String nestedResultMapId, String columnPrefix)
 1485 |       throws SQLException {
 1486 |     ResultMap nestedResultMap = configuration.getResultMap(nestedResultMapId);
 1487 |     return resolveDiscriminatedResultMap(rsw, nestedResultMap, columnPrefix);
 1488 |   }
 1489 | 
 1490 |   //
 1491 |   // UNIQUE RESULT KEY
 1492 |   //
 1493 | 
 1494 |   private CacheKey createRowKey(ResultMap resultMap, ResultSetWrapper rsw, String columnPrefix) throws SQLException {
 1495 |     final CacheKey cacheKey = new CacheKey();
 1496 |     cacheKey.update(resultMap.getId());
 1497 |     List<ResultMapping> resultMappings = getResultMappingsForRowKey(resultMap);
 1498 |     if (resultMappings.isEmpty()) {
 1499 |       if (Map.class.isAssignableFrom(resultMap.getType())) {
 1500 |         createRowKeyForMap(rsw, cacheKey);
 1501 |       } else {
 1502 |         createRowKeyForUnmappedProperties(resultMap, rsw, cacheKey, columnPrefix);
 1503 |       }
 1504 |     } else {
 1505 |       createRowKeyForMappedProperties(resultMap, rsw, cacheKey, resultMappings, columnPrefix);
 1506 |     }
 1507 |     if (cacheKey.getUpdateCount() < 2) {
 1508 |       return CacheKey.NULL_CACHE_KEY;
 1509 |     }
 1510 |     return cacheKey;
 1511 |   }
 1512 | 
 1513 |   private CacheKey combineKeys(CacheKey rowKey, CacheKey parentRowKey) {
 1514 |     if (rowKey.getUpdateCount() > 1 && parentRowKey.getUpdateCount() > 1) {
 1515 |       CacheKey combinedKey;
 1516 |       try {
 1517 |         combinedKey = rowKey.clone();
 1518 |       } catch (CloneNotSupportedException e) {
 1519 |         throw new ExecutorException("Error cloning cache key.  Cause: " + e, e);
 1520 |       }
 1521 |       combinedKey.update(parentRowKey);
 1522 |       return combinedKey;
 1523 |     }
 1524 |     return CacheKey.NULL_CACHE_KEY;
 1525 |   }
 1526 | 
 1527 |   private List<ResultMapping> getResultMappingsForRowKey(ResultMap resultMap) {
 1528 |     List<ResultMapping> resultMappings = resultMap.getIdResultMappings();
 1529 |     if (resultMappings.isEmpty()) {
 1530 |       resultMappings = resultMap.getPropertyResultMappings();
 1531 |     }
 1532 |     return resultMappings;
 1533 |   }
 1534 | 
 1535 |   private void createRowKeyForMappedProperties(ResultMap resultMap, ResultSetWrapper rsw, CacheKey cacheKey,
 1536 |       List<ResultMapping> resultMappings, String columnPrefix) throws SQLException {
 1537 |     for (ResultMapping resultMapping : resultMappings) {
 1538 |       if (resultMapping.isSimple()) {
 1539 |         final String column = prependPrefix(resultMapping.getColumn(), columnPrefix);
 1540 |         TypeHandler<?> th = resultMapping.getTypeHandler();
 1541 |         Set<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
 1542 |         // Issue #114
 1543 |         if (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH))) {
 1544 |           if (th == null) {
 1545 |             th = typeHandlerRegistry.getTypeHandler(rsw.getJdbcType(column));
 1546 |           }
 1547 |           if (th == null) {
 1548 |             th = ObjectTypeHandler.INSTANCE;
 1549 |           }
 1550 |           final Object value = th.getResult(rsw.getResultSet(), column);
 1551 |           if (value != null || configuration.isReturnInstanceForEmptyRow()) {
 1552 |             cacheKey.update(column);
 1553 |             cacheKey.update(value);
 1554 |           }
 1555 |         }
 1556 |       }
 1557 |     }
 1558 |   }
 1559 | 
 1560 |   private void createRowKeyForUnmappedProperties(ResultMap resultMap, ResultSetWrapper rsw, CacheKey cacheKey,
 1561 |       String columnPrefix) throws SQLException {
 1562 |     final MetaClass metaType = MetaClass.forClass(resultMap.getType(), reflectorFactory);
 1563 |     List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);
 1564 |     for (String column : unmappedColumnNames) {
 1565 |       String property = column;
 1566 |       if (columnPrefix != null && !columnPrefix.isEmpty()) {
 1567 |         // When columnPrefix is specified, ignore columns without the prefix.
 1568 |         if (!column.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
 1569 |           continue;
 1570 |         }
 1571 |         property = column.substring(columnPrefix.length());
 1572 |       }
 1573 |       if (metaType.findProperty(property, configuration.isMapUnderscoreToCamelCase()) != null) {
 1574 |         String value = rsw.getResultSet().getString(column);
 1575 |         if (value != null) {
 1576 |           cacheKey.update(column);
 1577 |           cacheKey.update(value);
 1578 |         }
 1579 |       }
 1580 |     }
 1581 |   }
 1582 | 
 1583 |   private void createRowKeyForMap(ResultSetWrapper rsw, CacheKey cacheKey) throws SQLException {
 1584 |     List<String> columnNames = rsw.getColumnNames();
 1585 |     for (String columnName : columnNames) {
 1586 |       final String value = rsw.getResultSet().getString(columnName);
 1587 |       if (value != null) {
 1588 |         cacheKey.update(columnName);
 1589 |         cacheKey.update(value);
 1590 |       }
 1591 |     }
 1592 |   }
 1593 | 
 1594 |   private void linkObjects(MetaObject metaObject, ResultMapping resultMapping, Object rowValue) {
 1595 |     linkObjects(metaObject, resultMapping, rowValue, false);
 1596 |   }
 1597 | 
 1598 |   private void linkObjects(MetaObject metaObject, ResultMapping resultMapping, Object rowValue,
 1599 |       boolean isNestedCursorResult) {
 1600 |     final Object collectionProperty = instantiateCollectionPropertyIfAppropriate(resultMapping, metaObject);
 1601 |     if (collectionProperty != null) {
 1602 |       final MetaObject targetMetaObject = configuration.newMetaObject(collectionProperty);
 1603 |       if (isNestedCursorResult) {
 1604 |         targetMetaObject.addAll((List<?>) rowValue);
 1605 |       } else {
 1606 |         targetMetaObject.add(rowValue);
 1607 |       }
 1608 | 
 1609 |       // it is possible for pending creations to get set via property mappings,
 1610 |       // keep track of these, so we can rebuild them.
 1611 |       final Object originalObject = metaObject.getOriginalObject();
 1612 |       if (rowValue instanceof PendingConstructorCreation && !pendingPccRelations.containsKey(originalObject)) {
 1613 |         PendingRelation pendingRelation = new PendingRelation();
 1614 |         pendingRelation.propertyMapping = resultMapping;
 1615 |         pendingRelation.metaObject = metaObject;
 1616 | 
 1617 |         pendingPccRelations.put(originalObject, pendingRelation);
 1618 |       }
 1619 |     } else {
 1620 |       metaObject.setValue(resultMapping.getProperty(),
 1621 |           isNestedCursorResult ? toSingleObj((List<?>) rowValue) : rowValue);
 1622 |     }
 1623 |   }
 1624 | 
 1625 |   private Object toSingleObj(List<?> list) {
 1626 |     // Even if there are multiple elements, silently returns the first one.
 1627 |     return list.isEmpty() ? null : list.get(0);
 1628 |   }
 1629 | 
 1630 |   private Object instantiateCollectionPropertyIfAppropriate(ResultMapping resultMapping, MetaObject metaObject) {
 1631 |     final String propertyName = resultMapping.getProperty();
 1632 |     Object propertyValue = metaObject.getValue(propertyName);
 1633 |     if (propertyValue == null) {
 1634 |       Class<?> type = resultMapping.getJavaType();
 1635 |       if (type == null) {
 1636 |         type = metaObject.getSetterType(propertyName);
 1637 |       }
 1638 |       try {
 1639 |         if (objectFactory.isCollection(type)) {
 1640 |           propertyValue = objectFactory.create(type);
 1641 |           metaObject.setValue(propertyName, propertyValue);
 1642 |           return propertyValue;
 1643 |         }
 1644 |       } catch (Exception e) {
 1645 |         throw new ExecutorException(
 1646 |             "Error instantiating collection property for result '" + resultMapping.getProperty() + "'.  Cause: " + e,
 1647 |             e);
 1648 |       }
 1649 |     } else if (objectFactory.isCollection(propertyValue.getClass())) {
 1650 |       return propertyValue;
 1651 |     }
 1652 |     return null;
 1653 |   }
 1654 | 
 1655 |   private boolean hasTypeHandlerForResultObject(ResultSetWrapper rsw, Class<?> resultType) {
 1656 |     if (rsw.getColumnNames().size() == 1) {
 1657 |       return typeHandlerRegistry.hasTypeHandler(resultType, rsw.getJdbcType(rsw.getColumnNames().get(0)));
 1658 |     }
 1659 |     return typeHandlerRegistry.hasTypeHandler(resultType);
 1660 |   }
 1661 | 
 1662 | }
```
