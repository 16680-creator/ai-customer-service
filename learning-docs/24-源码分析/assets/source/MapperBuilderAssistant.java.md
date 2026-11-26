<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/builder/MapperBuilderAssistant.java -->
<!-- 正文引用：[[源码:MapperBuilderAssistant.java#L起-L止]] ；代码块头部：(src: MapperBuilderAssistant.java#L起-L止) -->

# MapperBuilderAssistant.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/builder/MapperBuilderAssistant.java` |
| 行数 | 469 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:55:31.568Z |

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
   16 | package org.apache.ibatis.builder;
   17 | 
   18 | import java.lang.reflect.Type;
   19 | import java.sql.ResultSet;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.HashMap;
   23 | import java.util.HashSet;
   24 | import java.util.List;
   25 | import java.util.Map;
   26 | import java.util.Map.Entry;
   27 | import java.util.Properties;
   28 | import java.util.Set;
   29 | import java.util.regex.Pattern;
   30 | import java.util.stream.Collectors;
   31 | 
   32 | import org.apache.ibatis.cache.Cache;
   33 | import org.apache.ibatis.cache.decorators.LruCache;
   34 | import org.apache.ibatis.cache.impl.PerpetualCache;
   35 | import org.apache.ibatis.executor.ErrorContext;
   36 | import org.apache.ibatis.executor.keygen.KeyGenerator;
   37 | import org.apache.ibatis.mapping.CacheBuilder;
   38 | import org.apache.ibatis.mapping.Discriminator;
   39 | import org.apache.ibatis.mapping.MappedStatement;
   40 | import org.apache.ibatis.mapping.ParameterMap;
   41 | import org.apache.ibatis.mapping.ParameterMapping;
   42 | import org.apache.ibatis.mapping.ParameterMode;
   43 | import org.apache.ibatis.mapping.ResultFlag;
   44 | import org.apache.ibatis.mapping.ResultMap;
   45 | import org.apache.ibatis.mapping.ResultMapping;
   46 | import org.apache.ibatis.mapping.ResultSetType;
   47 | import org.apache.ibatis.mapping.SqlCommandType;
   48 | import org.apache.ibatis.mapping.SqlSource;
   49 | import org.apache.ibatis.mapping.StatementType;
   50 | import org.apache.ibatis.reflection.MetaClass;
   51 | import org.apache.ibatis.reflection.ParamNameResolver;
   52 | import org.apache.ibatis.scripting.LanguageDriver;
   53 | import org.apache.ibatis.session.Configuration;
   54 | import org.apache.ibatis.type.JdbcType;
   55 | import org.apache.ibatis.type.TypeHandler;
   56 | 
   57 | /**
   58 |  * @author Clinton Begin
   59 |  */
   60 | public class MapperBuilderAssistant extends BaseBuilder {
   61 | 
   62 |   private String currentNamespace;
   63 |   private final String resource;
   64 |   private Cache currentCache;
   65 |   private boolean unresolvedCacheRef; // issue #676
   66 | 
   67 |   public MapperBuilderAssistant(Configuration configuration, String resource) {
   68 |     super(configuration);
   69 |     ErrorContext.instance().resource(resource);
   70 |     this.resource = resource;
   71 |   }
   72 | 
   73 |   public String getCurrentNamespace() {
   74 |     return currentNamespace;
   75 |   }
   76 | 
   77 |   public void setCurrentNamespace(String currentNamespace) {
   78 |     if (currentNamespace == null) {
   79 |       throw new BuilderException("The mapper element requires a namespace attribute to be specified.");
   80 |     }
   81 | 
   82 |     if (this.currentNamespace != null && !this.currentNamespace.equals(currentNamespace)) {
   83 |       throw new BuilderException(
   84 |           "Wrong namespace. Expected '" + this.currentNamespace + "' but found '" + currentNamespace + "'.");
   85 |     }
   86 | 
   87 |     this.currentNamespace = currentNamespace;
   88 |   }
   89 | 
   90 |   public String applyCurrentNamespace(String base, boolean isReference) {
   91 |     if (base == null) {
   92 |       return null;
   93 |     }
   94 |     if (isReference) {
   95 |       // is it qualified with any namespace yet?
   96 |       if (base.contains(".")) {
   97 |         return base;
   98 |       }
   99 |     } else {
  100 |       // is it qualified with this namespace yet?
  101 |       if (base.startsWith(currentNamespace + ".")) {
  102 |         return base;
  103 |       }
  104 |       if (base.contains(".")) {
  105 |         throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
  106 |       }
  107 |     }
  108 |     return currentNamespace + "." + base;
  109 |   }
  110 | 
  111 |   public Cache useCacheRef(String namespace) {
  112 |     if (namespace == null) {
  113 |       throw new BuilderException("cache-ref element requires a namespace attribute.");
  114 |     }
  115 |     try {
  116 |       unresolvedCacheRef = true;
  117 |       Cache cache = configuration.getCache(namespace);
  118 |       if (cache == null) {
  119 |         throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
  120 |       }
  121 |       currentCache = cache;
  122 |       unresolvedCacheRef = false;
  123 |       return cache;
  124 |     } catch (IllegalArgumentException e) {
  125 |       throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
  126 |     }
  127 |   }
  128 | 
  129 |   public Cache useNewCache(Class<? extends Cache> typeClass, Class<? extends Cache> evictionClass, Long flushInterval,
  130 |       Integer size, boolean readWrite, boolean blocking, Properties props) {
  131 |     Cache cache = new CacheBuilder(currentNamespace).implementation(valueOrDefault(typeClass, PerpetualCache.class))
  132 |         .addDecorator(valueOrDefault(evictionClass, LruCache.class)).clearInterval(flushInterval).size(size)
  133 |         .readWrite(readWrite).blocking(blocking).properties(props).build();
  134 |     configuration.addCache(cache);
  135 |     currentCache = cache;
  136 |     return cache;
  137 |   }
  138 | 
  139 |   public ParameterMap addParameterMap(String id, Class<?> parameterClass, List<ParameterMapping> parameterMappings) {
  140 |     id = applyCurrentNamespace(id, false);
  141 |     ParameterMap parameterMap = new ParameterMap.Builder(configuration, id, parameterClass, parameterMappings).build();
  142 |     configuration.addParameterMap(parameterMap);
  143 |     return parameterMap;
  144 |   }
  145 | 
  146 |   public ParameterMapping buildParameterMapping(Class<?> parameterType, String property, Class<?> javaType,
  147 |       JdbcType jdbcType, String resultMap, ParameterMode parameterMode, Class<? extends TypeHandler<?>> typeHandler,
  148 |       Integer numericScale) {
  149 |     resultMap = applyCurrentNamespace(resultMap, true);
  150 | 
  151 |     // Class parameterType = parameterMapBuilder.type();
  152 |     Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType);
  153 |     TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, jdbcType, typeHandler);
  154 | 
  155 |     return new ParameterMapping.Builder(configuration, property, javaTypeClass).jdbcType(jdbcType)
  156 |         .resultMapId(resultMap).mode(parameterMode).numericScale(numericScale).typeHandler(typeHandlerInstance).build();
  157 |   }
  158 | 
  159 |   public ResultMap addResultMap(String id, Class<?> type, String extend, Discriminator discriminator,
  160 |       List<ResultMapping> resultMappings, Boolean autoMapping) {
  161 |     id = applyCurrentNamespace(id, false);
  162 |     extend = applyCurrentNamespace(extend, true);
  163 | 
  164 |     if (extend != null) {
  165 |       if (!configuration.hasResultMap(extend)) {
  166 |         throw new IncompleteElementException("Could not find a parent resultMap with id '" + extend + "'");
  167 |       }
  168 |       ResultMap resultMap = configuration.getResultMap(extend);
  169 |       List<ResultMapping> extendedResultMappings = new ArrayList<>(resultMap.getResultMappings());
  170 |       extendedResultMappings.removeAll(resultMappings);
  171 |       // Remove parent constructor if this resultMap declares a constructor.
  172 |       boolean declaresConstructor = false;
  173 |       for (ResultMapping resultMapping : resultMappings) {
  174 |         if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
  175 |           declaresConstructor = true;
  176 |           break;
  177 |         }
  178 |       }
  179 |       if (declaresConstructor) {
  180 |         extendedResultMappings.removeIf(resultMapping -> resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR));
  181 |       }
  182 |       resultMappings.addAll(extendedResultMappings);
  183 |     }
  184 |     ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
  185 |         .discriminator(discriminator).build();
  186 |     configuration.addResultMap(resultMap);
  187 |     return resultMap;
  188 |   }
  189 | 
  190 |   public Discriminator buildDiscriminator(Class<?> resultType, String column, Class<?> javaType, JdbcType jdbcType,
  191 |       Class<? extends TypeHandler<?>> typeHandler, Map<String, String> discriminatorMap) {
  192 |     ResultMapping resultMapping = buildResultMapping(resultType, null, column, javaType, jdbcType, null, null, null,
  193 |         null, typeHandler, new ArrayList<>(), null, null, false);
  194 |     Map<String, String> namespaceDiscriminatorMap = new HashMap<>();
  195 |     for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
  196 |       String resultMap = e.getValue();
  197 |       resultMap = applyCurrentNamespace(resultMap, true);
  198 |       namespaceDiscriminatorMap.put(e.getKey(), resultMap);
  199 |     }
  200 |     return new Discriminator.Builder(configuration, resultMapping, namespaceDiscriminatorMap).build();
  201 |   }
  202 | 
  203 |   public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
  204 |       SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType,
  205 |       String resultMap, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache,
  206 |       boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
  207 |       LanguageDriver lang, String resultSets, boolean dirtySelect, ParamNameResolver paramNameResolver) {
  208 | 
  209 |     if (unresolvedCacheRef) {
  210 |       throw new IncompleteElementException("Cache-ref not yet resolved");
  211 |     }
  212 | 
  213 |     id = applyCurrentNamespace(id, false);
  214 | 
  215 |     MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType)
  216 |         .resource(resource).fetchSize(fetchSize).timeout(timeout).statementType(statementType)
  217 |         .keyGenerator(keyGenerator).keyProperty(keyProperty).keyColumn(keyColumn).databaseId(databaseId).lang(lang)
  218 |         .resultOrdered(resultOrdered).resultSets(resultSets)
  219 |         .resultMaps(getStatementResultMaps(resultMap, resultType, id)).resultSetType(resultSetType)
  220 |         .flushCacheRequired(flushCache).useCache(useCache).cache(currentCache).dirtySelect(dirtySelect)
  221 |         .paramNameResolver(paramNameResolver);
  222 | 
  223 |     ParameterMap statementParameterMap = getStatementParameterMap(parameterMap, parameterType, id);
  224 |     if (statementParameterMap != null) {
  225 |       statementBuilder.parameterMap(statementParameterMap);
  226 |     }
  227 | 
  228 |     MappedStatement statement = statementBuilder.build();
  229 |     configuration.addMappedStatement(statement);
  230 |     return statement;
  231 |   }
  232 | 
  233 |   /**
  234 |    * Backward compatibility signature 'addMappedStatement'.
  235 |    *
  236 |    * @param id
  237 |    *          the id
  238 |    * @param sqlSource
  239 |    *          the sql source
  240 |    * @param statementType
  241 |    *          the statement type
  242 |    * @param sqlCommandType
  243 |    *          the sql command type
  244 |    * @param fetchSize
  245 |    *          the fetch size
  246 |    * @param timeout
  247 |    *          the timeout
  248 |    * @param parameterMap
  249 |    *          the parameter map
  250 |    * @param parameterType
  251 |    *          the parameter type
  252 |    * @param resultMap
  253 |    *          the result map
  254 |    * @param resultType
  255 |    *          the result type
  256 |    * @param resultSetType
  257 |    *          the result set type
  258 |    * @param flushCache
  259 |    *          the flush cache
  260 |    * @param useCache
  261 |    *          the use cache
  262 |    * @param resultOrdered
  263 |    *          the result ordered
  264 |    * @param keyGenerator
  265 |    *          the key generator
  266 |    * @param keyProperty
  267 |    *          the key property
  268 |    * @param keyColumn
  269 |    *          the key column
  270 |    * @param databaseId
  271 |    *          the database id
  272 |    * @param lang
  273 |    *          the lang
  274 |    *
  275 |    * @return the mapped statement
  276 |    */
  277 |   public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
  278 |       SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType,
  279 |       String resultMap, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache,
  280 |       boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
  281 |       LanguageDriver lang, String resultSets) {
  282 |     return addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
  283 |         parameterType, resultMap, resultType, resultSetType, flushCache, useCache, resultOrdered, keyGenerator,
  284 |         keyProperty, keyColumn, databaseId, lang, null, false, null);
  285 |   }
  286 | 
  287 |   public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
  288 |       SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType,
  289 |       String resultMap, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache,
  290 |       boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
  291 |       LanguageDriver lang) {
  292 |     return addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
  293 |         parameterType, resultMap, resultType, resultSetType, flushCache, useCache, resultOrdered, keyGenerator,
  294 |         keyProperty, keyColumn, databaseId, lang, null);
  295 |   }
  296 | 
  297 |   private <T> T valueOrDefault(T value, T defaultValue) {
  298 |     return value == null ? defaultValue : value;
  299 |   }
  300 | 
  301 |   private ParameterMap getStatementParameterMap(String parameterMapName, Class<?> parameterTypeClass,
  302 |       String statementId) {
  303 |     parameterMapName = applyCurrentNamespace(parameterMapName, true);
  304 |     ParameterMap parameterMap = null;
  305 |     if (parameterMapName != null) {
  306 |       try {
  307 |         parameterMap = configuration.getParameterMap(parameterMapName);
  308 |       } catch (IllegalArgumentException e) {
  309 |         throw new IncompleteElementException("Could not find parameter map " + parameterMapName, e);
  310 |       }
  311 |     } else if (parameterTypeClass != null) {
  312 |       parameterMap = ParameterMap.buildEmpty(statementId + "-Inline", parameterTypeClass);
  313 |     }
  314 |     return parameterMap;
  315 |   }
  316 | 
  317 |   private List<ResultMap> getStatementResultMaps(String resultMap, Class<?> resultType, String statementId) {
  318 |     resultMap = applyCurrentNamespace(resultMap, true);
  319 | 
  320 |     List<ResultMap> resultMaps = new ArrayList<>();
  321 |     if (resultMap != null) {
  322 |       String[] resultMapNames = resultMap.split(",");
  323 |       for (String resultMapName : resultMapNames) {
  324 |         try {
  325 |           resultMaps.add(configuration.getResultMap(resultMapName.trim()));
  326 |         } catch (IllegalArgumentException e) {
  327 |           throw new IncompleteElementException(
  328 |               "Could not find result map '" + resultMapName + "' referenced from '" + statementId + "'", e);
  329 |         }
  330 |       }
  331 |     } else if (resultType != null) {
  332 |       resultMaps.add(ResultMap.buildEmpty(configuration, statementId + "-Inline", resultType));
  333 |     }
  334 |     return resultMaps;
  335 |   }
  336 | 
  337 |   public ResultMapping buildResultMapping(Class<?> resultType, String property, String column, Class<?> javaType,
  338 |       JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
  339 |       Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags, String resultSet, String foreignColumn,
  340 |       boolean lazy) {
  341 |     Entry<Type, Class<?>> setterType = resolveSetterType(resultType, property, javaType);
  342 |     TypeHandler<?> typeHandlerInstance = resolveTypeHandler(setterType.getKey(), jdbcType, typeHandler);
  343 |     List<ResultMapping> composites;
  344 |     if ((nestedSelect == null || nestedSelect.isEmpty()) && (foreignColumn == null || foreignColumn.isEmpty())) {
  345 |       composites = List.of();
  346 |     } else {
  347 |       composites = parseCompositeColumnName(column);
  348 |     }
  349 |     return new ResultMapping.Builder(configuration, property, column, setterType.getValue()).jdbcType(jdbcType)
  350 |         .nestedQueryId(applyCurrentNamespace(nestedSelect, true))
  351 |         .nestedResultMapId(applyCurrentNamespace(nestedResultMap, true)).resultSet(resultSet)
  352 |         .typeHandler(typeHandlerInstance).flags(flags == null ? new ArrayList<>() : flags).composites(composites)
  353 |         .notNullColumns(parseMultipleColumnNames(notNullColumn)).columnPrefix(columnPrefix).foreignColumn(foreignColumn)
  354 |         .lazy(lazy).build();
  355 |   }
  356 | 
  357 |   /**
  358 |    * Backward compatibility signature 'buildResultMapping'.
  359 |    *
  360 |    * @param resultType
  361 |    *          the result type
  362 |    * @param property
  363 |    *          the property
  364 |    * @param column
  365 |    *          the column
  366 |    * @param javaType
  367 |    *          the java type
  368 |    * @param jdbcType
  369 |    *          the jdbc type
  370 |    * @param nestedSelect
  371 |    *          the nested select
  372 |    * @param nestedResultMap
  373 |    *          the nested result map
  374 |    * @param notNullColumn
  375 |    *          the not null column
  376 |    * @param columnPrefix
  377 |    *          the column prefix
  378 |    * @param typeHandler
  379 |    *          the type handler
  380 |    * @param flags
  381 |    *          the flags
  382 |    *
  383 |    * @return the result mapping
  384 |    */
  385 |   public ResultMapping buildResultMapping(Class<?> resultType, String property, String column, Class<?> javaType,
  386 |       JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
  387 |       Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags) {
  388 |     return buildResultMapping(resultType, property, column, javaType, jdbcType, nestedSelect, nestedResultMap,
  389 |         notNullColumn, columnPrefix, typeHandler, flags, null, null, configuration.isLazyLoadingEnabled());
  390 |   }
  391 | 
  392 |   /**
  393 |    * Gets the language driver.
  394 |    *
  395 |    * @param langClass
  396 |    *          the lang class
  397 |    *
  398 |    * @return the language driver
  399 |    *
  400 |    * @deprecated Use {@link Configuration#getLanguageDriver(Class)}
  401 |    */
  402 |   @Deprecated
  403 |   public LanguageDriver getLanguageDriver(Class<? extends LanguageDriver> langClass) {
  404 |     return configuration.getLanguageDriver(langClass);
  405 |   }
  406 | 
  407 |   private Set<String> parseMultipleColumnNames(final String columnName) {
  408 |     final Set<String> columns = new HashSet<>();
  409 |     if (columnName != null) {
  410 |       if (columnName.contains(",")) {
  411 |         Pattern.compile("[{}, ]+").splitAsStream(columnName).filter(column -> !column.isEmpty()).forEach(columns::add);
  412 |       } else {
  413 |         columns.add(columnName);
  414 |       }
  415 |     }
  416 |     return columns;
  417 |   }
  418 | 
  419 |   private List<ResultMapping> parseCompositeColumnName(final String columnName) {
  420 |     final List<ResultMapping> composites = new ArrayList<>();
  421 |     if (columnName != null && (columnName.contains("=") || columnName.contains(","))) {
  422 |       final String[] columns = Pattern.compile("[{},= ]+").split(columnName);
  423 |       final List<String> tokens = Arrays.stream(columns).filter(column -> !column.isEmpty())
  424 |           .collect(Collectors.toList());
  425 | 
  426 |       for (int i = 0; i + 1 < tokens.size(); i += 2) {
  427 |         final String property = tokens.get(i);
  428 |         final String column = tokens.get(i + 1);
  429 |         final ResultMapping complexResultMapping = new ResultMapping.Builder(configuration, property, column,
  430 |             (TypeHandler<?>) null).build();
  431 |         composites.add(complexResultMapping);
  432 |       }
  433 |     }
  434 |     return composites;
  435 |   }
  436 | 
  437 |   private Entry<Type, Class<?>> resolveSetterType(Class<?> resultType, String property, Class<?> javaType) {
  438 |     if (javaType != null) {
  439 |       return Map.entry(javaType, javaType);
  440 |     }
  441 |     if (property != null) {
  442 |       MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
  443 |       try {
  444 |         return metaResultType.getGenericSetterType(property);
  445 |       } catch (Exception e) {
  446 |         // Not all property types are resolvable.
  447 |       }
  448 |     }
  449 |     return Map.entry(Object.class, Object.class);
  450 |   }
  451 | 
  452 |   private Class<?> resolveParameterJavaType(Class<?> resultType, String property, Class<?> javaType,
  453 |       JdbcType jdbcType) {
  454 |     if (javaType == null) {
  455 |       if (JdbcType.CURSOR.equals(jdbcType)) {
  456 |         javaType = ResultSet.class;
  457 |       } else if (Map.class.isAssignableFrom(resultType)) {
  458 |         javaType = Object.class;
  459 |       } else {
  460 |         MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
  461 |         javaType = metaResultType.getGetterType(property);
  462 |       }
  463 |     }
  464 |     if (javaType == null) {
  465 |       javaType = Object.class;
  466 |     }
  467 |     return javaType;
  468 |   }
  469 | }
```
