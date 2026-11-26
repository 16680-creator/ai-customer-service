<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/session/Configuration.java -->
<!-- 正文引用：[[源码:Configuration.java#L起-L止]] ；代码块头部：(src: Configuration.java#L起-L止) -->

# Configuration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/session/Configuration.java` |
| 行数 | 1200 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:17.915Z |

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
   16 | package org.apache.ibatis.session;
   17 | 
   18 | import java.util.Arrays;
   19 | import java.util.Collection;
   20 | import java.util.HashMap;
   21 | import java.util.HashSet;
   22 | import java.util.Iterator;
   23 | import java.util.LinkedList;
   24 | import java.util.List;
   25 | import java.util.Map;
   26 | import java.util.Properties;
   27 | import java.util.Set;
   28 | import java.util.concurrent.ConcurrentHashMap;
   29 | import java.util.concurrent.locks.ReentrantLock;
   30 | import java.util.function.BiFunction;
   31 | 
   32 | import org.apache.ibatis.binding.MapperRegistry;
   33 | import org.apache.ibatis.builder.CacheRefResolver;
   34 | import org.apache.ibatis.builder.IncompleteElementException;
   35 | import org.apache.ibatis.builder.ResultMapResolver;
   36 | import org.apache.ibatis.builder.annotation.MethodResolver;
   37 | import org.apache.ibatis.builder.xml.XMLStatementBuilder;
   38 | import org.apache.ibatis.cache.Cache;
   39 | import org.apache.ibatis.cache.decorators.FifoCache;
   40 | import org.apache.ibatis.cache.decorators.LruCache;
   41 | import org.apache.ibatis.cache.decorators.SoftCache;
   42 | import org.apache.ibatis.cache.decorators.WeakCache;
   43 | import org.apache.ibatis.cache.impl.PerpetualCache;
   44 | import org.apache.ibatis.datasource.jndi.JndiDataSourceFactory;
   45 | import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;
   46 | import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
   47 | import org.apache.ibatis.executor.BatchExecutor;
   48 | import org.apache.ibatis.executor.CachingExecutor;
   49 | import org.apache.ibatis.executor.Executor;
   50 | import org.apache.ibatis.executor.ReuseExecutor;
   51 | import org.apache.ibatis.executor.SimpleExecutor;
   52 | import org.apache.ibatis.executor.keygen.KeyGenerator;
   53 | import org.apache.ibatis.executor.loader.ProxyFactory;
   54 | import org.apache.ibatis.executor.loader.cglib.CglibProxyFactory;
   55 | import org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory;
   56 | import org.apache.ibatis.executor.parameter.ParameterHandler;
   57 | import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
   58 | import org.apache.ibatis.executor.resultset.ResultSetHandler;
   59 | import org.apache.ibatis.executor.statement.RoutingStatementHandler;
   60 | import org.apache.ibatis.executor.statement.StatementHandler;
   61 | import org.apache.ibatis.io.VFS;
   62 | import org.apache.ibatis.logging.Log;
   63 | import org.apache.ibatis.logging.LogFactory;
   64 | import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
   65 | import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
   66 | import org.apache.ibatis.logging.log4j.Log4jImpl;
   67 | import org.apache.ibatis.logging.log4j2.Log4j2Impl;
   68 | import org.apache.ibatis.logging.nologging.NoLoggingImpl;
   69 | import org.apache.ibatis.logging.slf4j.Slf4jImpl;
   70 | import org.apache.ibatis.logging.stdout.StdOutImpl;
   71 | import org.apache.ibatis.mapping.BoundSql;
   72 | import org.apache.ibatis.mapping.Environment;
   73 | import org.apache.ibatis.mapping.MappedStatement;
   74 | import org.apache.ibatis.mapping.ParameterMap;
   75 | import org.apache.ibatis.mapping.ResultMap;
   76 | import org.apache.ibatis.mapping.ResultSetType;
   77 | import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
   78 | import org.apache.ibatis.parsing.XNode;
   79 | import org.apache.ibatis.plugin.Interceptor;
   80 | import org.apache.ibatis.plugin.InterceptorChain;
   81 | import org.apache.ibatis.reflection.DefaultReflectorFactory;
   82 | import org.apache.ibatis.reflection.MetaObject;
   83 | import org.apache.ibatis.reflection.ReflectorFactory;
   84 | import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
   85 | import org.apache.ibatis.reflection.factory.ObjectFactory;
   86 | import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
   87 | import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
   88 | import org.apache.ibatis.scripting.LanguageDriver;
   89 | import org.apache.ibatis.scripting.LanguageDriverRegistry;
   90 | import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
   91 | import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
   92 | import org.apache.ibatis.transaction.Transaction;
   93 | import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
   94 | import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
   95 | import org.apache.ibatis.type.JdbcType;
   96 | import org.apache.ibatis.type.TypeAliasRegistry;
   97 | import org.apache.ibatis.type.TypeHandler;
   98 | import org.apache.ibatis.type.TypeHandlerRegistry;
   99 | 
  100 | /**
  101 |  * @author Clinton Begin
  102 |  */
  103 | public class Configuration {
  104 | 
  105 |   protected Environment environment;
  106 | 
  107 |   protected boolean safeRowBoundsEnabled;
  108 |   protected boolean safeResultHandlerEnabled = true;
  109 |   protected boolean mapUnderscoreToCamelCase;
  110 |   protected boolean aggressiveLazyLoading;
  111 |   protected boolean useGeneratedKeys;
  112 |   protected boolean useColumnLabel = true;
  113 |   protected boolean cacheEnabled = true;
  114 |   protected boolean callSettersOnNulls;
  115 |   protected boolean useActualParamName = true;
  116 |   protected boolean returnInstanceForEmptyRow;
  117 |   protected boolean shrinkWhitespacesInSql;
  118 |   protected boolean nullableOnForEach;
  119 |   protected boolean argNameBasedConstructorAutoMapping;
  120 | 
  121 |   protected String logPrefix;
  122 |   protected Class<? extends Log> logImpl;
  123 |   protected Class<? extends VFS> vfsImpl;
  124 |   protected Class<?> defaultSqlProviderType;
  125 |   protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION;
  126 |   protected JdbcType jdbcTypeForNull = JdbcType.OTHER;
  127 |   protected Set<String> lazyLoadTriggerMethods = new HashSet<>(
  128 |       Arrays.asList("equals", "clone", "hashCode", "toString"));
  129 |   protected Integer defaultStatementTimeout;
  130 |   protected Integer defaultFetchSize;
  131 |   protected ResultSetType defaultResultSetType;
  132 |   protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;
  133 |   protected AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;
  134 |   protected AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior = AutoMappingUnknownColumnBehavior.NONE;
  135 | 
  136 |   protected Properties variables = new Properties();
  137 |   protected ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
  138 |   protected ObjectFactory objectFactory = new DefaultObjectFactory();
  139 |   protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();
  140 | 
  141 |   protected boolean lazyLoadingEnabled;
  142 |   protected ProxyFactory proxyFactory = new JavassistProxyFactory(); // #224 Using internal Javassist instead of OGNL
  143 | 
  144 |   protected String databaseId;
  145 |   /**
  146 |    * Configuration factory class. Used to create Configuration for loading deserialized unread properties.
  147 |    *
  148 |    * @see <a href='https://github.com/mybatis/old-google-code-issues/issues/300'>Issue 300 (google code)</a>
  149 |    */
  150 |   protected Class<?> configurationFactory;
  151 | 
  152 |   protected final MapperRegistry mapperRegistry = new MapperRegistry(this);
  153 |   protected final InterceptorChain interceptorChain = new InterceptorChain();
  154 |   protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry(this);
  155 |   protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
  156 |   protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();
  157 | 
  158 |   protected final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>(
  159 |       "Mapped Statements collection")
  160 |           .conflictMessageProducer((savedValue, targetValue) -> ". please check " + savedValue.getResource() + " and "
  161 |               + targetValue.getResource());
  162 |   protected final Map<String, Cache> caches = new StrictMap<>("Caches collection");
  163 |   protected final Map<String, ResultMap> resultMaps = new StrictMap<>("Result Maps collection");
  164 |   protected final Map<String, ParameterMap> parameterMaps = new StrictMap<>("Parameter Maps collection");
  165 |   protected final Map<String, KeyGenerator> keyGenerators = new StrictMap<>("Key Generators collection");
  166 | 
  167 |   protected final Set<String> loadedResources = new HashSet<>();
  168 |   protected final Map<String, XNode> sqlFragments = new StrictMap<>("XML fragments parsed from previous mappers");
  169 |   protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<>();
  170 |   protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<>();
  171 |   protected final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList<>();
  172 |   protected final Collection<MethodResolver> incompleteMethods = new LinkedList<>();
  173 | 
  174 |   private final ReentrantLock incompleteResultMapsLock = new ReentrantLock();
  175 |   private final ReentrantLock incompleteCacheRefsLock = new ReentrantLock();
  176 |   private final ReentrantLock incompleteStatementsLock = new ReentrantLock();
  177 |   private final ReentrantLock incompleteMethodsLock = new ReentrantLock();
  178 | 
  179 |   /*
  180 |    * A map holds cache-ref relationship. The key is the namespace that references a cache bound to another namespace and
  181 |    * the value is the namespace which the actual cache is bound to.
  182 |    */
  183 |   protected final Map<String, String> cacheRefMap = new HashMap<>();
  184 | 
  185 |   public Configuration(Environment environment) {
  186 |     this();
  187 |     this.environment = environment;
  188 |   }
  189 | 
  190 |   public Configuration() {
  191 |     typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
  192 |     typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);
  193 | 
  194 |     typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
  195 |     typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
  196 |     typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);
  197 | 
  198 |     typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
  199 |     typeAliasRegistry.registerAlias("FIFO", FifoCache.class);
  200 |     typeAliasRegistry.registerAlias("LRU", LruCache.class);
  201 |     typeAliasRegistry.registerAlias("SOFT", SoftCache.class);
  202 |     typeAliasRegistry.registerAlias("WEAK", WeakCache.class);
  203 | 
  204 |     typeAliasRegistry.registerAlias("DB_VENDOR", VendorDatabaseIdProvider.class);
  205 | 
  206 |     typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);
  207 |     typeAliasRegistry.registerAlias("RAW", RawLanguageDriver.class);
  208 | 
  209 |     typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
  210 |     typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
  211 |     typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
  212 |     typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
  213 |     typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
  214 |     typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
  215 |     typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);
  216 | 
  217 |     typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
  218 |     typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);
  219 | 
  220 |     languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
  221 |     languageRegistry.register(RawLanguageDriver.class);
  222 |   }
  223 | 
  224 |   public String getLogPrefix() {
  225 |     return logPrefix;
  226 |   }
  227 | 
  228 |   public void setLogPrefix(String logPrefix) {
  229 |     this.logPrefix = logPrefix;
  230 |   }
  231 | 
  232 |   public Class<? extends Log> getLogImpl() {
  233 |     return logImpl;
  234 |   }
  235 | 
  236 |   public void setLogImpl(Class<? extends Log> logImpl) {
  237 |     if (logImpl != null) {
  238 |       this.logImpl = logImpl;
  239 |       LogFactory.useCustomLogging(this.logImpl);
  240 |     }
  241 |   }
  242 | 
  243 |   public Class<? extends VFS> getVfsImpl() {
  244 |     return this.vfsImpl;
  245 |   }
  246 | 
  247 |   public void setVfsImpl(Class<? extends VFS> vfsImpl) {
  248 |     if (vfsImpl != null) {
  249 |       this.vfsImpl = vfsImpl;
  250 |       VFS.addImplClass(this.vfsImpl);
  251 |     }
  252 |   }
  253 | 
  254 |   /**
  255 |    * Gets an applying type when omit a type on sql provider annotation(e.g.
  256 |    * {@link org.apache.ibatis.annotations.SelectProvider}).
  257 |    *
  258 |    * @return the default type for sql provider annotation
  259 |    *
  260 |    * @since 3.5.6
  261 |    */
  262 |   public Class<?> getDefaultSqlProviderType() {
  263 |     return defaultSqlProviderType;
  264 |   }
  265 | 
  266 |   /**
  267 |    * Sets an applying type when omit a type on sql provider annotation(e.g.
  268 |    * {@link org.apache.ibatis.annotations.SelectProvider}).
  269 |    *
  270 |    * @param defaultSqlProviderType
  271 |    *          the default type for sql provider annotation
  272 |    *
  273 |    * @since 3.5.6
  274 |    */
  275 |   public void setDefaultSqlProviderType(Class<?> defaultSqlProviderType) {
  276 |     this.defaultSqlProviderType = defaultSqlProviderType;
  277 |   }
  278 | 
  279 |   public boolean isCallSettersOnNulls() {
  280 |     return callSettersOnNulls;
  281 |   }
  282 | 
  283 |   public void setCallSettersOnNulls(boolean callSettersOnNulls) {
  284 |     this.callSettersOnNulls = callSettersOnNulls;
  285 |   }
  286 | 
  287 |   public boolean isUseActualParamName() {
  288 |     return useActualParamName;
  289 |   }
  290 | 
  291 |   public void setUseActualParamName(boolean useActualParamName) {
  292 |     this.useActualParamName = useActualParamName;
  293 |   }
  294 | 
  295 |   public boolean isReturnInstanceForEmptyRow() {
  296 |     return returnInstanceForEmptyRow;
  297 |   }
  298 | 
  299 |   public void setReturnInstanceForEmptyRow(boolean returnEmptyInstance) {
  300 |     this.returnInstanceForEmptyRow = returnEmptyInstance;
  301 |   }
  302 | 
  303 |   public boolean isShrinkWhitespacesInSql() {
  304 |     return shrinkWhitespacesInSql;
  305 |   }
  306 | 
  307 |   public void setShrinkWhitespacesInSql(boolean shrinkWhitespacesInSql) {
  308 |     this.shrinkWhitespacesInSql = shrinkWhitespacesInSql;
  309 |   }
  310 | 
  311 |   /**
  312 |    * Sets the default value of 'nullable' attribute on 'foreach' tag.
  313 |    *
  314 |    * @param nullableOnForEach
  315 |    *          If nullable, set to {@code true}
  316 |    *
  317 |    * @since 3.5.9
  318 |    */
  319 |   public void setNullableOnForEach(boolean nullableOnForEach) {
  320 |     this.nullableOnForEach = nullableOnForEach;
  321 |   }
  322 | 
  323 |   /**
  324 |    * Returns the default value of 'nullable' attribute on 'foreach' tag.
  325 |    * <p>
  326 |    * Default is {@code false}.
  327 |    *
  328 |    * @return If nullable, set to {@code true}
  329 |    *
  330 |    * @since 3.5.9
  331 |    */
  332 |   public boolean isNullableOnForEach() {
  333 |     return nullableOnForEach;
  334 |   }
  335 | 
  336 |   public boolean isArgNameBasedConstructorAutoMapping() {
  337 |     return argNameBasedConstructorAutoMapping;
  338 |   }
  339 | 
  340 |   public void setArgNameBasedConstructorAutoMapping(boolean argNameBasedConstructorAutoMapping) {
  341 |     this.argNameBasedConstructorAutoMapping = argNameBasedConstructorAutoMapping;
  342 |   }
  343 | 
  344 |   public String getDatabaseId() {
  345 |     return databaseId;
  346 |   }
  347 | 
  348 |   public void setDatabaseId(String databaseId) {
  349 |     this.databaseId = databaseId;
  350 |   }
  351 | 
  352 |   public Class<?> getConfigurationFactory() {
  353 |     return configurationFactory;
  354 |   }
  355 | 
  356 |   public void setConfigurationFactory(Class<?> configurationFactory) {
  357 |     this.configurationFactory = configurationFactory;
  358 |   }
  359 | 
  360 |   public boolean isSafeResultHandlerEnabled() {
  361 |     return safeResultHandlerEnabled;
  362 |   }
  363 | 
  364 |   public void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled) {
  365 |     this.safeResultHandlerEnabled = safeResultHandlerEnabled;
  366 |   }
  367 | 
  368 |   public boolean isSafeRowBoundsEnabled() {
  369 |     return safeRowBoundsEnabled;
  370 |   }
  371 | 
  372 |   public void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled) {
  373 |     this.safeRowBoundsEnabled = safeRowBoundsEnabled;
  374 |   }
  375 | 
  376 |   public boolean isMapUnderscoreToCamelCase() {
  377 |     return mapUnderscoreToCamelCase;
  378 |   }
  379 | 
  380 |   public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
  381 |     this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
  382 |   }
  383 | 
  384 |   public void addLoadedResource(String resource) {
  385 |     loadedResources.add(resource);
  386 |   }
  387 | 
  388 |   public boolean isResourceLoaded(String resource) {
  389 |     return loadedResources.contains(resource);
  390 |   }
  391 | 
  392 |   public Environment getEnvironment() {
  393 |     return environment;
  394 |   }
  395 | 
  396 |   public void setEnvironment(Environment environment) {
  397 |     this.environment = environment;
  398 |   }
  399 | 
  400 |   public AutoMappingBehavior getAutoMappingBehavior() {
  401 |     return autoMappingBehavior;
  402 |   }
  403 | 
  404 |   public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior) {
  405 |     this.autoMappingBehavior = autoMappingBehavior;
  406 |   }
  407 | 
  408 |   /**
  409 |    * Gets the auto mapping unknown column behavior.
  410 |    *
  411 |    * @return the auto mapping unknown column behavior
  412 |    *
  413 |    * @since 3.4.0
  414 |    */
  415 |   public AutoMappingUnknownColumnBehavior getAutoMappingUnknownColumnBehavior() {
  416 |     return autoMappingUnknownColumnBehavior;
  417 |   }
  418 | 
  419 |   /**
  420 |    * Sets the auto mapping unknown column behavior.
  421 |    *
  422 |    * @param autoMappingUnknownColumnBehavior
  423 |    *          the new auto mapping unknown column behavior
  424 |    *
  425 |    * @since 3.4.0
  426 |    */
  427 |   public void setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior) {
  428 |     this.autoMappingUnknownColumnBehavior = autoMappingUnknownColumnBehavior;
  429 |   }
  430 | 
  431 |   public boolean isLazyLoadingEnabled() {
  432 |     return lazyLoadingEnabled;
  433 |   }
  434 | 
  435 |   public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
  436 |     this.lazyLoadingEnabled = lazyLoadingEnabled;
  437 |   }
  438 | 
  439 |   public ProxyFactory getProxyFactory() {
  440 |     return proxyFactory;
  441 |   }
  442 | 
  443 |   public void setProxyFactory(ProxyFactory proxyFactory) {
  444 |     if (proxyFactory == null) {
  445 |       proxyFactory = new JavassistProxyFactory();
  446 |     }
  447 |     this.proxyFactory = proxyFactory;
  448 |   }
  449 | 
  450 |   public boolean isAggressiveLazyLoading() {
  451 |     return aggressiveLazyLoading;
  452 |   }
  453 | 
  454 |   public void setAggressiveLazyLoading(boolean aggressiveLazyLoading) {
  455 |     this.aggressiveLazyLoading = aggressiveLazyLoading;
  456 |   }
  457 | 
  458 |   /**
  459 |    * @deprecated You can safely remove the call to this method as this option had no effect.
  460 |    */
  461 |   @Deprecated
  462 |   public boolean isMultipleResultSetsEnabled() {
  463 |     return true;
  464 |   }
  465 | 
  466 |   /**
  467 |    * @deprecated You can safely remove the call to this method as this option had no effect.
  468 |    */
  469 |   @Deprecated
  470 |   public void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled) {
  471 |     // nop
  472 |   }
  473 | 
  474 |   public Set<String> getLazyLoadTriggerMethods() {
  475 |     return lazyLoadTriggerMethods;
  476 |   }
  477 | 
  478 |   public void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods) {
  479 |     this.lazyLoadTriggerMethods = lazyLoadTriggerMethods;
  480 |   }
  481 | 
  482 |   public boolean isUseGeneratedKeys() {
  483 |     return useGeneratedKeys;
  484 |   }
  485 | 
  486 |   public void setUseGeneratedKeys(boolean useGeneratedKeys) {
  487 |     this.useGeneratedKeys = useGeneratedKeys;
  488 |   }
  489 | 
  490 |   public ExecutorType getDefaultExecutorType() {
  491 |     return defaultExecutorType;
  492 |   }
  493 | 
  494 |   public void setDefaultExecutorType(ExecutorType defaultExecutorType) {
  495 |     this.defaultExecutorType = defaultExecutorType;
  496 |   }
  497 | 
  498 |   public boolean isCacheEnabled() {
  499 |     return cacheEnabled;
  500 |   }
  501 | 
  502 |   public void setCacheEnabled(boolean cacheEnabled) {
  503 |     this.cacheEnabled = cacheEnabled;
  504 |   }
  505 | 
  506 |   public Integer getDefaultStatementTimeout() {
  507 |     return defaultStatementTimeout;
  508 |   }
  509 | 
  510 |   public void setDefaultStatementTimeout(Integer defaultStatementTimeout) {
  511 |     this.defaultStatementTimeout = defaultStatementTimeout;
  512 |   }
  513 | 
  514 |   /**
  515 |    * Gets the default fetch size.
  516 |    *
  517 |    * @return the default fetch size
  518 |    *
  519 |    * @since 3.3.0
  520 |    */
  521 |   public Integer getDefaultFetchSize() {
  522 |     return defaultFetchSize;
  523 |   }
  524 | 
  525 |   /**
  526 |    * Sets the default fetch size.
  527 |    *
  528 |    * @param defaultFetchSize
  529 |    *          the new default fetch size
  530 |    *
  531 |    * @since 3.3.0
  532 |    */
  533 |   public void setDefaultFetchSize(Integer defaultFetchSize) {
  534 |     this.defaultFetchSize = defaultFetchSize;
  535 |   }
  536 | 
  537 |   /**
  538 |    * Gets the default result set type.
  539 |    *
  540 |    * @return the default result set type
  541 |    *
  542 |    * @since 3.5.2
  543 |    */
  544 |   public ResultSetType getDefaultResultSetType() {
  545 |     return defaultResultSetType;
  546 |   }
  547 | 
  548 |   /**
  549 |    * Sets the default result set type.
  550 |    *
  551 |    * @param defaultResultSetType
  552 |    *          the new default result set type
  553 |    *
  554 |    * @since 3.5.2
  555 |    */
  556 |   public void setDefaultResultSetType(ResultSetType defaultResultSetType) {
  557 |     this.defaultResultSetType = defaultResultSetType;
  558 |   }
  559 | 
  560 |   public boolean isUseColumnLabel() {
  561 |     return useColumnLabel;
  562 |   }
  563 | 
  564 |   public void setUseColumnLabel(boolean useColumnLabel) {
  565 |     this.useColumnLabel = useColumnLabel;
  566 |   }
  567 | 
  568 |   public LocalCacheScope getLocalCacheScope() {
  569 |     return localCacheScope;
  570 |   }
  571 | 
  572 |   public void setLocalCacheScope(LocalCacheScope localCacheScope) {
  573 |     this.localCacheScope = localCacheScope;
  574 |   }
  575 | 
  576 |   public JdbcType getJdbcTypeForNull() {
  577 |     return jdbcTypeForNull;
  578 |   }
  579 | 
  580 |   public void setJdbcTypeForNull(JdbcType jdbcTypeForNull) {
  581 |     this.jdbcTypeForNull = jdbcTypeForNull;
  582 |   }
  583 | 
  584 |   public Properties getVariables() {
  585 |     return variables;
  586 |   }
  587 | 
  588 |   public void setVariables(Properties variables) {
  589 |     this.variables = variables;
  590 |   }
  591 | 
  592 |   public TypeHandlerRegistry getTypeHandlerRegistry() {
  593 |     return typeHandlerRegistry;
  594 |   }
  595 | 
  596 |   /**
  597 |    * Set a default {@link TypeHandler} class for {@link Enum}. A default {@link TypeHandler} is
  598 |    * {@link org.apache.ibatis.type.EnumTypeHandler}.
  599 |    *
  600 |    * @param typeHandler
  601 |    *          a type handler class for {@link Enum}
  602 |    *
  603 |    * @since 3.4.5
  604 |    */
  605 |   public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
  606 |     if (typeHandler != null) {
  607 |       getTypeHandlerRegistry().setDefaultEnumTypeHandler(typeHandler);
  608 |     }
  609 |   }
  610 | 
  611 |   public TypeAliasRegistry getTypeAliasRegistry() {
  612 |     return typeAliasRegistry;
  613 |   }
  614 | 
  615 |   /**
  616 |    * Gets the mapper registry.
  617 |    *
  618 |    * @return the mapper registry
  619 |    *
  620 |    * @since 3.2.2
  621 |    */
  622 |   public MapperRegistry getMapperRegistry() {
  623 |     return mapperRegistry;
  624 |   }
  625 | 
  626 |   public ReflectorFactory getReflectorFactory() {
  627 |     return reflectorFactory;
  628 |   }
  629 | 
  630 |   public void setReflectorFactory(ReflectorFactory reflectorFactory) {
  631 |     this.reflectorFactory = reflectorFactory;
  632 |   }
  633 | 
  634 |   public ObjectFactory getObjectFactory() {
  635 |     return objectFactory;
  636 |   }
  637 | 
  638 |   public void setObjectFactory(ObjectFactory objectFactory) {
  639 |     this.objectFactory = objectFactory;
  640 |   }
  641 | 
  642 |   public ObjectWrapperFactory getObjectWrapperFactory() {
  643 |     return objectWrapperFactory;
  644 |   }
  645 | 
  646 |   public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
  647 |     this.objectWrapperFactory = objectWrapperFactory;
  648 |   }
  649 | 
  650 |   /**
  651 |    * Gets the interceptors.
  652 |    *
  653 |    * @return the interceptors
  654 |    *
  655 |    * @since 3.2.2
  656 |    */
  657 |   public List<Interceptor> getInterceptors() {
  658 |     return interceptorChain.getInterceptors();
  659 |   }
  660 | 
  661 |   public LanguageDriverRegistry getLanguageRegistry() {
  662 |     return languageRegistry;
  663 |   }
  664 | 
  665 |   public void setDefaultScriptingLanguage(Class<? extends LanguageDriver> driver) {
  666 |     if (driver == null) {
  667 |       driver = XMLLanguageDriver.class;
  668 |     }
  669 |     getLanguageRegistry().setDefaultDriverClass(driver);
  670 |   }
  671 | 
  672 |   public LanguageDriver getDefaultScriptingLanguageInstance() {
  673 |     return languageRegistry.getDefaultDriver();
  674 |   }
  675 | 
  676 |   /**
  677 |    * Gets the language driver.
  678 |    *
  679 |    * @param langClass
  680 |    *          the lang class
  681 |    *
  682 |    * @return the language driver
  683 |    *
  684 |    * @since 3.5.1
  685 |    */
  686 |   public LanguageDriver getLanguageDriver(Class<? extends LanguageDriver> langClass) {
  687 |     if (langClass == null) {
  688 |       return languageRegistry.getDefaultDriver();
  689 |     }
  690 |     languageRegistry.register(langClass);
  691 |     return languageRegistry.getDriver(langClass);
  692 |   }
  693 | 
  694 |   /**
  695 |    * Gets the default scripting language instance.
  696 |    *
  697 |    * @return the default scripting language instance
  698 |    *
  699 |    * @deprecated Use {@link #getDefaultScriptingLanguageInstance()}
  700 |    */
  701 |   @Deprecated
  702 |   public LanguageDriver getDefaultScriptingLanuageInstance() {
  703 |     return getDefaultScriptingLanguageInstance();
  704 |   }
  705 | 
  706 |   public MetaObject newMetaObject(Object object) {
  707 |     return MetaObject.forObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
  708 |   }
  709 | 
  710 |   public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject,
  711 |       BoundSql boundSql) {
  712 |     ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement,
  713 |         parameterObject, boundSql);
  714 |     return (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
  715 |   }
  716 | 
  717 |   public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds,
  718 |       ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql) {
  719 |     ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler,
  720 |         resultHandler, boundSql, rowBounds);
  721 |     return (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
  722 |   }
  723 | 
  724 |   public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement,
  725 |       Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
  726 |     StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject,
  727 |         rowBounds, resultHandler, boundSql);
  728 |     return (StatementHandler) interceptorChain.pluginAll(statementHandler);
  729 |   }
  730 | 
  731 |   public Executor newExecutor(Transaction transaction) {
  732 |     return newExecutor(transaction, defaultExecutorType);
  733 |   }
  734 | 
  735 |   public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
  736 |     executorType = executorType == null ? defaultExecutorType : executorType;
  737 |     Executor executor;
  738 |     if (ExecutorType.BATCH == executorType) {
  739 |       executor = new BatchExecutor(this, transaction);
  740 |     } else if (ExecutorType.REUSE == executorType) {
  741 |       executor = new ReuseExecutor(this, transaction);
  742 |     } else {
  743 |       executor = new SimpleExecutor(this, transaction);
  744 |     }
  745 |     if (cacheEnabled) {
  746 |       executor = new CachingExecutor(executor);
  747 |     }
  748 |     return (Executor) interceptorChain.pluginAll(executor);
  749 |   }
  750 | 
  751 |   public void addKeyGenerator(String id, KeyGenerator keyGenerator) {
  752 |     keyGenerators.put(id, keyGenerator);
  753 |   }
  754 | 
  755 |   public Collection<String> getKeyGeneratorNames() {
  756 |     return keyGenerators.keySet();
  757 |   }
  758 | 
  759 |   public Collection<KeyGenerator> getKeyGenerators() {
  760 |     return keyGenerators.values();
  761 |   }
  762 | 
  763 |   public KeyGenerator getKeyGenerator(String id) {
  764 |     return keyGenerators.get(id);
  765 |   }
  766 | 
  767 |   public boolean hasKeyGenerator(String id) {
  768 |     return keyGenerators.containsKey(id);
  769 |   }
  770 | 
  771 |   public void addCache(Cache cache) {
  772 |     caches.put(cache.getId(), cache);
  773 |   }
  774 | 
  775 |   public Collection<String> getCacheNames() {
  776 |     return caches.keySet();
  777 |   }
  778 | 
  779 |   public Collection<Cache> getCaches() {
  780 |     return caches.values();
  781 |   }
  782 | 
  783 |   public Cache getCache(String id) {
  784 |     return caches.get(id);
  785 |   }
  786 | 
  787 |   public boolean hasCache(String id) {
  788 |     return caches.containsKey(id);
  789 |   }
  790 | 
  791 |   public void addResultMap(ResultMap rm) {
  792 |     resultMaps.put(rm.getId(), rm);
  793 |     checkLocallyForDiscriminatedNestedResultMaps(rm);
  794 |     checkGloballyForDiscriminatedNestedResultMaps(rm);
  795 |   }
  796 | 
  797 |   public Collection<String> getResultMapNames() {
  798 |     return resultMaps.keySet();
  799 |   }
  800 | 
  801 |   public Collection<ResultMap> getResultMaps() {
  802 |     return resultMaps.values();
  803 |   }
  804 | 
  805 |   public ResultMap getResultMap(String id) {
  806 |     return resultMaps.get(id);
  807 |   }
  808 | 
  809 |   public boolean hasResultMap(String id) {
  810 |     return resultMaps.containsKey(id);
  811 |   }
  812 | 
  813 |   public void addParameterMap(ParameterMap pm) {
  814 |     parameterMaps.put(pm.getId(), pm);
  815 |   }
  816 | 
  817 |   public Collection<String> getParameterMapNames() {
  818 |     return parameterMaps.keySet();
  819 |   }
  820 | 
  821 |   public Collection<ParameterMap> getParameterMaps() {
  822 |     return parameterMaps.values();
  823 |   }
  824 | 
  825 |   public ParameterMap getParameterMap(String id) {
  826 |     return parameterMaps.get(id);
  827 |   }
  828 | 
  829 |   public boolean hasParameterMap(String id) {
  830 |     return parameterMaps.containsKey(id);
  831 |   }
  832 | 
  833 |   public void addMappedStatement(MappedStatement ms) {
  834 |     mappedStatements.put(ms.getId(), ms);
  835 |   }
  836 | 
  837 |   public Collection<String> getMappedStatementNames() {
  838 |     buildAllStatements();
  839 |     return mappedStatements.keySet();
  840 |   }
  841 | 
  842 |   public Collection<MappedStatement> getMappedStatements() {
  843 |     buildAllStatements();
  844 |     return mappedStatements.values();
  845 |   }
  846 | 
  847 |   /**
  848 |    * @deprecated call {@link #parsePendingStatements(boolean)}
  849 |    */
  850 |   @Deprecated
  851 |   public Collection<XMLStatementBuilder> getIncompleteStatements() {
  852 |     return incompleteStatements;
  853 |   }
  854 | 
  855 |   public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {
  856 |     incompleteStatementsLock.lock();
  857 |     try {
  858 |       incompleteStatements.add(incompleteStatement);
  859 |     } finally {
  860 |       incompleteStatementsLock.unlock();
  861 |     }
  862 |   }
  863 | 
  864 |   /**
  865 |    * @deprecated call {@link #parsePendingCacheRefs(boolean)}
  866 |    */
  867 |   @Deprecated
  868 |   public Collection<CacheRefResolver> getIncompleteCacheRefs() {
  869 |     return incompleteCacheRefs;
  870 |   }
  871 | 
  872 |   public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {
  873 |     incompleteCacheRefsLock.lock();
  874 |     try {
  875 |       incompleteCacheRefs.add(incompleteCacheRef);
  876 |     } finally {
  877 |       incompleteCacheRefsLock.unlock();
  878 |     }
  879 |   }
  880 | 
  881 |   /**
  882 |    * @deprecated call {@link #parsePendingResultMaps(boolean)}
  883 |    */
  884 |   @Deprecated
  885 |   public Collection<ResultMapResolver> getIncompleteResultMaps() {
  886 |     return incompleteResultMaps;
  887 |   }
  888 | 
  889 |   public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {
  890 |     incompleteResultMapsLock.lock();
  891 |     try {
  892 |       incompleteResultMaps.add(resultMapResolver);
  893 |     } finally {
  894 |       incompleteResultMapsLock.unlock();
  895 |     }
  896 |   }
  897 | 
  898 |   public void addIncompleteMethod(MethodResolver builder) {
  899 |     incompleteMethodsLock.lock();
  900 |     try {
  901 |       incompleteMethods.add(builder);
  902 |     } finally {
  903 |       incompleteMethodsLock.unlock();
  904 |     }
  905 |   }
  906 | 
  907 |   /**
  908 |    * @deprecated call {@link #parsePendingMethods(boolean)}
  909 |    */
  910 |   @Deprecated
  911 |   public Collection<MethodResolver> getIncompleteMethods() {
  912 |     return incompleteMethods;
  913 |   }
  914 | 
  915 |   public MappedStatement getMappedStatement(String id) {
  916 |     return this.getMappedStatement(id, true);
  917 |   }
  918 | 
  919 |   public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
  920 |     if (validateIncompleteStatements) {
  921 |       buildAllStatements();
  922 |     }
  923 |     return mappedStatements.get(id);
  924 |   }
  925 | 
  926 |   public Map<String, XNode> getSqlFragments() {
  927 |     return sqlFragments;
  928 |   }
  929 | 
  930 |   public void addInterceptor(Interceptor interceptor) {
  931 |     interceptorChain.addInterceptor(interceptor);
  932 |   }
  933 | 
  934 |   public void addMappers(String packageName, Class<?> superType) {
  935 |     mapperRegistry.addMappers(packageName, superType);
  936 |   }
  937 | 
  938 |   public void addMappers(String packageName) {
  939 |     mapperRegistry.addMappers(packageName);
  940 |   }
  941 | 
  942 |   public <T> void addMapper(Class<T> type) {
  943 |     mapperRegistry.addMapper(type);
  944 |   }
  945 | 
  946 |   public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
  947 |     return mapperRegistry.getMapper(type, sqlSession);
  948 |   }
  949 | 
  950 |   public boolean hasMapper(Class<?> type) {
  951 |     return mapperRegistry.hasMapper(type);
  952 |   }
  953 | 
  954 |   public boolean hasStatement(String statementName) {
  955 |     return hasStatement(statementName, true);
  956 |   }
  957 | 
  958 |   public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
  959 |     if (validateIncompleteStatements) {
  960 |       buildAllStatements();
  961 |     }
  962 |     return mappedStatements.containsKey(statementName);
  963 |   }
  964 | 
  965 |   public void addCacheRef(String namespace, String referencedNamespace) {
  966 |     cacheRefMap.put(namespace, referencedNamespace);
  967 |   }
  968 | 
  969 |   /*
  970 |    * Parses all the unprocessed statement nodes in the cache. It is recommended to call this method once all the mappers
  971 |    * are added as it provides fail-fast statement validation.
  972 |    */
  973 |   protected void buildAllStatements() {
  974 |     parsePendingResultMaps(true);
  975 |     parsePendingCacheRefs(true);
  976 |     parsePendingStatements(true);
  977 |     parsePendingMethods(true);
  978 |   }
  979 | 
  980 |   public void parsePendingMethods(boolean reportUnresolved) {
  981 |     if (incompleteMethods.isEmpty()) {
  982 |       return;
  983 |     }
  984 |     incompleteMethodsLock.lock();
  985 |     try {
  986 |       incompleteMethods.removeIf(x -> {
  987 |         x.resolve();
  988 |         return true;
  989 |       });
  990 |     } catch (IncompleteElementException e) {
  991 |       if (reportUnresolved) {
  992 |         throw e;
  993 |       }
  994 |     } finally {
  995 |       incompleteMethodsLock.unlock();
  996 |     }
  997 |   }
  998 | 
  999 |   public void parsePendingStatements(boolean reportUnresolved) {
 1000 |     if (incompleteStatements.isEmpty()) {
 1001 |       return;
 1002 |     }
 1003 |     incompleteStatementsLock.lock();
 1004 |     try {
 1005 |       incompleteStatements.removeIf(x -> {
 1006 |         x.parseStatementNode();
 1007 |         return true;
 1008 |       });
 1009 |     } catch (IncompleteElementException e) {
 1010 |       if (reportUnresolved) {
 1011 |         throw e;
 1012 |       }
 1013 |     } finally {
 1014 |       incompleteStatementsLock.unlock();
 1015 |     }
 1016 |   }
 1017 | 
 1018 |   public void parsePendingCacheRefs(boolean reportUnresolved) {
 1019 |     if (incompleteCacheRefs.isEmpty()) {
 1020 |       return;
 1021 |     }
 1022 |     incompleteCacheRefsLock.lock();
 1023 |     try {
 1024 |       incompleteCacheRefs.removeIf(x -> x.resolveCacheRef() != null);
 1025 |     } catch (IncompleteElementException e) {
 1026 |       if (reportUnresolved) {
 1027 |         throw e;
 1028 |       }
 1029 |     } finally {
 1030 |       incompleteCacheRefsLock.unlock();
 1031 |     }
 1032 |   }
 1033 | 
 1034 |   public void parsePendingResultMaps(boolean reportUnresolved) {
 1035 |     if (incompleteResultMaps.isEmpty()) {
 1036 |       return;
 1037 |     }
 1038 |     incompleteResultMapsLock.lock();
 1039 |     try {
 1040 |       boolean resolved;
 1041 |       IncompleteElementException ex = null;
 1042 |       do {
 1043 |         resolved = false;
 1044 |         Iterator<ResultMapResolver> iterator = incompleteResultMaps.iterator();
 1045 |         while (iterator.hasNext()) {
 1046 |           try {
 1047 |             iterator.next().resolve();
 1048 |             iterator.remove();
 1049 |             resolved = true;
 1050 |           } catch (IncompleteElementException e) {
 1051 |             ex = e;
 1052 |           }
 1053 |         }
 1054 |       } while (resolved);
 1055 |       if (reportUnresolved && !incompleteResultMaps.isEmpty() && ex != null) {
 1056 |         // At least one result map is unresolvable.
 1057 |         throw ex;
 1058 |       }
 1059 |     } finally {
 1060 |       incompleteResultMapsLock.unlock();
 1061 |     }
 1062 |   }
 1063 | 
 1064 |   /**
 1065 |    * Extracts namespace from fully qualified statement id.
 1066 |    *
 1067 |    * @param statementId
 1068 |    *          the statement id
 1069 |    *
 1070 |    * @return namespace or null when id does not contain period.
 1071 |    */
 1072 |   protected String extractNamespace(String statementId) {
 1073 |     int lastPeriod = statementId.lastIndexOf('.');
 1074 |     return lastPeriod > 0 ? statementId.substring(0, lastPeriod) : null;
 1075 |   }
 1076 | 
 1077 |   // Slow but a one time cost. A better solution is welcome.
 1078 |   protected void checkGloballyForDiscriminatedNestedResultMaps(ResultMap rm) {
 1079 |     if (rm.hasNestedResultMaps()) {
 1080 |       final String resultMapId = rm.getId();
 1081 |       for (Object resultMapObject : resultMaps.values()) {
 1082 |         if (resultMapObject instanceof ResultMap) {
 1083 |           ResultMap entryResultMap = (ResultMap) resultMapObject;
 1084 |           if (!entryResultMap.hasNestedResultMaps() && entryResultMap.getDiscriminator() != null) {
 1085 |             Collection<String> discriminatedResultMapNames = entryResultMap.getDiscriminator().getDiscriminatorMap()
 1086 |                 .values();
 1087 |             if (discriminatedResultMapNames.contains(resultMapId)) {
 1088 |               entryResultMap.forceNestedResultMaps();
 1089 |             }
 1090 |           }
 1091 |         }
 1092 |       }
 1093 |     }
 1094 |   }
 1095 | 
 1096 |   // Slow but a one time cost. A better solution is welcome.
 1097 |   protected void checkLocallyForDiscriminatedNestedResultMaps(ResultMap rm) {
 1098 |     if (!rm.hasNestedResultMaps() && rm.getDiscriminator() != null) {
 1099 |       for (String discriminatedResultMapName : rm.getDiscriminator().getDiscriminatorMap().values()) {
 1100 |         if (hasResultMap(discriminatedResultMapName)) {
 1101 |           ResultMap discriminatedResultMap = resultMaps.get(discriminatedResultMapName);
 1102 |           if (discriminatedResultMap.hasNestedResultMaps()) {
 1103 |             rm.forceNestedResultMaps();
 1104 |             break;
 1105 |           }
 1106 |         }
 1107 |       }
 1108 |     }
 1109 |   }
 1110 | 
 1111 |   protected static class StrictMap<V> extends ConcurrentHashMap<String, V> {
 1112 | 
 1113 |     private static final long serialVersionUID = -4950446264854982944L;
 1114 |     private final String name;
 1115 |     private BiFunction<V, V, String> conflictMessageProducer;
 1116 |     private static final Object AMBIGUITY_INSTANCE = new Object();
 1117 | 
 1118 |     public StrictMap(String name, int initialCapacity, float loadFactor) {
 1119 |       super(initialCapacity, loadFactor);
 1120 |       this.name = name;
 1121 |     }
 1122 | 
 1123 |     public StrictMap(String name, int initialCapacity) {
 1124 |       super(initialCapacity);
 1125 |       this.name = name;
 1126 |     }
 1127 | 
 1128 |     public StrictMap(String name) {
 1129 |       this.name = name;
 1130 |     }
 1131 | 
 1132 |     public StrictMap(String name, Map<String, ? extends V> m) {
 1133 |       super(m);
 1134 |       this.name = name;
 1135 |     }
 1136 | 
 1137 |     /**
 1138 |      * Assign a function for producing a conflict error message when contains value with the same key.
 1139 |      * <p>
 1140 |      * function arguments are 1st is saved value and 2nd is target value.
 1141 |      *
 1142 |      * @param conflictMessageProducer
 1143 |      *          A function for producing a conflict error message
 1144 |      *
 1145 |      * @return a conflict error message
 1146 |      *
 1147 |      * @since 3.5.0
 1148 |      */
 1149 |     public StrictMap<V> conflictMessageProducer(BiFunction<V, V, String> conflictMessageProducer) {
 1150 |       this.conflictMessageProducer = conflictMessageProducer;
 1151 |       return this;
 1152 |     }
 1153 | 
 1154 |     @Override
 1155 |     @SuppressWarnings("unchecked")
 1156 |     public V put(String key, V value) {
 1157 |       if (containsKey(key)) {
 1158 |         throw new IllegalArgumentException(name + " already contains key " + key
 1159 |             + (conflictMessageProducer == null ? "" : conflictMessageProducer.apply(super.get(key), value)));
 1160 |       }
 1161 |       if (key.contains(".")) {
 1162 |         final String shortKey = getShortName(key);
 1163 |         if (super.get(shortKey) == null) {
 1164 |           super.put(shortKey, value);
 1165 |         } else {
 1166 |           super.put(shortKey, (V) AMBIGUITY_INSTANCE);
 1167 |         }
 1168 |       }
 1169 |       return super.put(key, value);
 1170 |     }
 1171 | 
 1172 |     @Override
 1173 |     public boolean containsKey(Object key) {
 1174 |       if (key == null) {
 1175 |         return false;
 1176 |       }
 1177 | 
 1178 |       return super.get(key) != null;
 1179 |     }
 1180 | 
 1181 |     @Override
 1182 |     public V get(Object key) {
 1183 |       V value = super.get(key);
 1184 |       if (value == null) {
 1185 |         throw new IllegalArgumentException(name + " does not contain value for " + key);
 1186 |       }
 1187 |       if (AMBIGUITY_INSTANCE == value) {
 1188 |         throw new IllegalArgumentException(key + " is ambiguous in " + name
 1189 |             + " (try using the full name including the namespace, or rename one of the entries)");
 1190 |       }
 1191 |       return value;
 1192 |     }
 1193 | 
 1194 |     private String getShortName(String key) {
 1195 |       final String[] keyParts = key.split("\\.");
 1196 |       return keyParts[keyParts.length - 1];
 1197 |     }
 1198 |   }
 1199 | 
 1200 | }
```
