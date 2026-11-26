<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/SqlSessionFactoryBean.java -->
<!-- 正文引用：[[源码:SqlSessionFactoryBean.java#L起-L止]] ；代码块头部：(src: SqlSessionFactoryBean.java#L起-L止) -->

# SqlSessionFactoryBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/SqlSessionFactoryBean.java` |
| 行数 | 758 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:01:23.953Z |

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
   18 | import static org.springframework.util.Assert.notNull;
   19 | import static org.springframework.util.Assert.state;
   20 | import static org.springframework.util.ObjectUtils.isEmpty;
   21 | import static org.springframework.util.StringUtils.hasLength;
   22 | import static org.springframework.util.StringUtils.tokenizeToStringArray;
   23 | 
   24 | import java.io.IOException;
   25 | import java.lang.reflect.Modifier;
   26 | import java.sql.SQLException;
   27 | import java.util.ArrayList;
   28 | import java.util.Arrays;
   29 | import java.util.HashSet;
   30 | import java.util.List;
   31 | import java.util.Optional;
   32 | import java.util.Properties;
   33 | import java.util.Set;
   34 | import java.util.function.IntFunction;
   35 | import java.util.stream.Stream;
   36 | 
   37 | import javax.sql.DataSource;
   38 | 
   39 | import org.apache.ibatis.builder.xml.XMLConfigBuilder;
   40 | import org.apache.ibatis.builder.xml.XMLMapperBuilder;
   41 | import org.apache.ibatis.cache.Cache;
   42 | import org.apache.ibatis.executor.ErrorContext;
   43 | import org.apache.ibatis.io.Resources;
   44 | import org.apache.ibatis.io.VFS;
   45 | import org.apache.ibatis.mapping.DatabaseIdProvider;
   46 | import org.apache.ibatis.mapping.Environment;
   47 | import org.apache.ibatis.plugin.Interceptor;
   48 | import org.apache.ibatis.reflection.factory.ObjectFactory;
   49 | import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
   50 | import org.apache.ibatis.scripting.LanguageDriver;
   51 | import org.apache.ibatis.session.Configuration;
   52 | import org.apache.ibatis.session.SqlSessionFactory;
   53 | import org.apache.ibatis.session.SqlSessionFactoryBuilder;
   54 | import org.apache.ibatis.transaction.TransactionFactory;
   55 | import org.apache.ibatis.type.TypeHandler;
   56 | import org.mybatis.logging.Logger;
   57 | import org.mybatis.logging.LoggerFactory;
   58 | import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
   59 | import org.springframework.beans.factory.FactoryBean;
   60 | import org.springframework.beans.factory.InitializingBean;
   61 | import org.springframework.context.ApplicationListener;
   62 | import org.springframework.context.ConfigurableApplicationContext;
   63 | import org.springframework.context.event.ContextRefreshedEvent;
   64 | import org.springframework.core.io.Resource;
   65 | import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
   66 | import org.springframework.core.io.support.ResourcePatternResolver;
   67 | import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
   68 | import org.springframework.core.type.classreading.MetadataReaderFactory;
   69 | import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
   70 | import org.springframework.util.ClassUtils;
   71 | 
   72 | /**
   73 |  * {@code FactoryBean} that creates a MyBatis {@code SqlSessionFactory}. This is the usual way to set up a shared
   74 |  * MyBatis {@code SqlSessionFactory} in a Spring application context; the SqlSessionFactory can then be passed to
   75 |  * MyBatis-based DAOs via dependency injection.
   76 |  * <p>
   77 |  * Either {@code DataSourceTransactionManager} or {@code JtaTransactionManager} can be used for transaction demarcation
   78 |  * in combination with a {@code SqlSessionFactory}. JTA should be used for transactions which span multiple databases or
   79 |  * when container managed transactions (CMT) are being used.
   80 |  *
   81 |  * @author Putthiphong Boonphong
   82 |  * @author Hunter Presnall
   83 |  * @author Eduardo Macarron
   84 |  * @author Eddú Meléndez
   85 |  * @author Kazuki Shimizu
   86 |  * @author Jens Schauder
   87 |  *
   88 |  * @see #setConfigLocation
   89 |  * @see #setDataSource
   90 |  */
   91 | public class SqlSessionFactoryBean
   92 |     implements FactoryBean<SqlSessionFactory>, InitializingBean, ApplicationListener<ContextRefreshedEvent> {
   93 | 
   94 |   private static final Logger LOGGER = LoggerFactory.getLogger(SqlSessionFactoryBean.class);
   95 | 
   96 |   private static final ResourcePatternResolver RESOURCE_PATTERN_RESOLVER = new PathMatchingResourcePatternResolver();
   97 |   private static final MetadataReaderFactory METADATA_READER_FACTORY = new CachingMetadataReaderFactory();
   98 | 
   99 |   private Resource configLocation;
  100 | 
  101 |   private Configuration configuration;
  102 | 
  103 |   private Resource[] mapperLocations;
  104 | 
  105 |   private DataSource dataSource;
  106 | 
  107 |   private TransactionFactory transactionFactory;
  108 | 
  109 |   private Properties configurationProperties;
  110 | 
  111 |   private SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
  112 | 
  113 |   private SqlSessionFactory sqlSessionFactory;
  114 | 
  115 |   // EnvironmentAware requires spring 3.1
  116 |   private String environment = SqlSessionFactoryBean.class.getSimpleName();
  117 | 
  118 |   private boolean failFast;
  119 | 
  120 |   private Interceptor[] plugins;
  121 | 
  122 |   private TypeHandler<?>[] typeHandlers;
  123 | 
  124 |   private String typeHandlersPackage;
  125 | 
  126 |   @SuppressWarnings("rawtypes")
  127 |   private Class<? extends TypeHandler> defaultEnumTypeHandler;
  128 | 
  129 |   private Class<?>[] typeAliases;
  130 | 
  131 |   private String typeAliasesPackage;
  132 | 
  133 |   private Class<?> typeAliasesSuperType;
  134 | 
  135 |   private LanguageDriver[] scriptingLanguageDrivers;
  136 | 
  137 |   private Class<? extends LanguageDriver> defaultScriptingLanguageDriver;
  138 | 
  139 |   // issue #19. No default provider.
  140 |   private DatabaseIdProvider databaseIdProvider;
  141 | 
  142 |   private Class<? extends VFS> vfs;
  143 | 
  144 |   private Cache cache;
  145 | 
  146 |   private ObjectFactory objectFactory;
  147 | 
  148 |   private ObjectWrapperFactory objectWrapperFactory;
  149 | 
  150 |   /**
  151 |    * Sets the ObjectFactory.
  152 |    *
  153 |    * @since 1.1.2
  154 |    *
  155 |    * @param objectFactory
  156 |    *          a custom ObjectFactory
  157 |    */
  158 |   public void setObjectFactory(ObjectFactory objectFactory) {
  159 |     this.objectFactory = objectFactory;
  160 |   }
  161 | 
  162 |   /**
  163 |    * Sets the ObjectWrapperFactory.
  164 |    *
  165 |    * @since 1.1.2
  166 |    *
  167 |    * @param objectWrapperFactory
  168 |    *          a specified ObjectWrapperFactory
  169 |    */
  170 |   public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
  171 |     this.objectWrapperFactory = objectWrapperFactory;
  172 |   }
  173 | 
  174 |   /**
  175 |    * Gets the DatabaseIdProvider
  176 |    *
  177 |    * @since 1.1.0
  178 |    *
  179 |    * @return a specified DatabaseIdProvider
  180 |    */
  181 |   public DatabaseIdProvider getDatabaseIdProvider() {
  182 |     return databaseIdProvider;
  183 |   }
  184 | 
  185 |   /**
  186 |    * Sets the DatabaseIdProvider. As of version 1.2.2 this variable is not initialized by default.
  187 |    *
  188 |    * @since 1.1.0
  189 |    *
  190 |    * @param databaseIdProvider
  191 |    *          a DatabaseIdProvider
  192 |    */
  193 |   public void setDatabaseIdProvider(DatabaseIdProvider databaseIdProvider) {
  194 |     this.databaseIdProvider = databaseIdProvider;
  195 |   }
  196 | 
  197 |   /**
  198 |    * Gets the VFS.
  199 |    *
  200 |    * @return a specified VFS
  201 |    */
  202 |   public Class<? extends VFS> getVfs() {
  203 |     return this.vfs;
  204 |   }
  205 | 
  206 |   /**
  207 |    * Sets the VFS.
  208 |    *
  209 |    * @param vfs
  210 |    *          a VFS
  211 |    */
  212 |   public void setVfs(Class<? extends VFS> vfs) {
  213 |     this.vfs = vfs;
  214 |   }
  215 | 
  216 |   /**
  217 |    * Gets the Cache.
  218 |    *
  219 |    * @return a specified Cache
  220 |    */
  221 |   public Cache getCache() {
  222 |     return this.cache;
  223 |   }
  224 | 
  225 |   /**
  226 |    * Sets the Cache.
  227 |    *
  228 |    * @param cache
  229 |    *          a Cache
  230 |    */
  231 |   public void setCache(Cache cache) {
  232 |     this.cache = cache;
  233 |   }
  234 | 
  235 |   /**
  236 |    * Mybatis plugin list.
  237 |    *
  238 |    * @since 1.0.1
  239 |    *
  240 |    * @param plugins
  241 |    *          list of plugins
  242 |    */
  243 |   public void setPlugins(Interceptor... plugins) {
  244 |     this.plugins = plugins;
  245 |   }
  246 | 
  247 |   /**
  248 |    * Packages to search for type aliases.
  249 |    * <p>
  250 |    * Since 2.0.1, allow to specify a wildcard such as {@code com.example.*.model}.
  251 |    *
  252 |    * @since 1.0.1
  253 |    *
  254 |    * @param typeAliasesPackage
  255 |    *          package to scan for domain objects
  256 |    */
  257 |   public void setTypeAliasesPackage(String typeAliasesPackage) {
  258 |     this.typeAliasesPackage = typeAliasesPackage;
  259 |   }
  260 | 
  261 |   /**
  262 |    * Super class which domain objects have to extend to have a type alias created. No effect if there is no package to
  263 |    * scan configured.
  264 |    *
  265 |    * @since 1.1.2
  266 |    *
  267 |    * @param typeAliasesSuperType
  268 |    *          super class for domain objects
  269 |    */
  270 |   public void setTypeAliasesSuperType(Class<?> typeAliasesSuperType) {
  271 |     this.typeAliasesSuperType = typeAliasesSuperType;
  272 |   }
  273 | 
  274 |   /**
  275 |    * Packages to search for type handlers.
  276 |    * <p>
  277 |    * Since 2.0.1, allow to specify a wildcard such as {@code com.example.*.typehandler}.
  278 |    *
  279 |    * @since 1.0.1
  280 |    *
  281 |    * @param typeHandlersPackage
  282 |    *          package to scan for type handlers
  283 |    */
  284 |   public void setTypeHandlersPackage(String typeHandlersPackage) {
  285 |     this.typeHandlersPackage = typeHandlersPackage;
  286 |   }
  287 | 
  288 |   /**
  289 |    * Set type handlers. They must be annotated with {@code MappedTypes} and optionally with {@code MappedJdbcTypes}
  290 |    *
  291 |    * @since 1.0.1
  292 |    *
  293 |    * @param typeHandlers
  294 |    *          Type handler list
  295 |    */
  296 |   public void setTypeHandlers(TypeHandler<?>... typeHandlers) {
  297 |     this.typeHandlers = typeHandlers;
  298 |   }
  299 | 
  300 |   /**
  301 |    * Set the default type handler class for enum.
  302 |    *
  303 |    * @since 2.0.5
  304 |    *
  305 |    * @param defaultEnumTypeHandler
  306 |    *          The default type handler class for enum
  307 |    */
  308 |   public void setDefaultEnumTypeHandler(
  309 |       @SuppressWarnings("rawtypes") Class<? extends TypeHandler> defaultEnumTypeHandler) {
  310 |     this.defaultEnumTypeHandler = defaultEnumTypeHandler;
  311 |   }
  312 | 
  313 |   /**
  314 |    * List of type aliases to register. They can be annotated with {@code Alias}
  315 |    *
  316 |    * @since 1.0.1
  317 |    *
  318 |    * @param typeAliases
  319 |    *          Type aliases list
  320 |    */
  321 |   public void setTypeAliases(Class<?>... typeAliases) {
  322 |     this.typeAliases = typeAliases;
  323 |   }
  324 | 
  325 |   /**
  326 |    * If true, a final check is done on Configuration to assure that all mapped statements are fully loaded and there is
  327 |    * no one still pending to resolve includes. Defaults to false.
  328 |    *
  329 |    * @since 1.0.1
  330 |    *
  331 |    * @param failFast
  332 |    *          enable failFast
  333 |    */
  334 |   public void setFailFast(boolean failFast) {
  335 |     this.failFast = failFast;
  336 |   }
  337 | 
  338 |   /**
  339 |    * Set the location of the MyBatis {@code SqlSessionFactory} config file. A typical value is
  340 |    * "WEB-INF/mybatis-configuration.xml".
  341 |    *
  342 |    * @param configLocation
  343 |    *          a location the MyBatis config file
  344 |    */
  345 |   public void setConfigLocation(Resource configLocation) {
  346 |     this.configLocation = configLocation;
  347 |   }
  348 | 
  349 |   /**
  350 |    * Set a customized MyBatis configuration.
  351 |    *
  352 |    * @param configuration
  353 |    *          MyBatis configuration
  354 |    *
  355 |    * @since 1.3.0
  356 |    */
  357 |   public void setConfiguration(Configuration configuration) {
  358 |     this.configuration = configuration;
  359 |   }
  360 | 
  361 |   /**
  362 |    * Set locations of MyBatis mapper files that are going to be merged into the {@code SqlSessionFactory} configuration
  363 |    * at runtime.
  364 |    * <p>
  365 |    * This is an alternative to specifying "&lt;sqlmapper&gt;" entries in an MyBatis config file. This property being
  366 |    * based on Spring's resource abstraction also allows for specifying resource patterns here: e.g.
  367 |    * "classpath*:sqlmap/*-mapper.xml".
  368 |    *
  369 |    * @param mapperLocations
  370 |    *          location of MyBatis mapper files
  371 |    */
  372 |   public void setMapperLocations(Resource... mapperLocations) {
  373 |     this.mapperLocations = mapperLocations;
  374 |   }
  375 | 
  376 |   /**
  377 |    * Set optional properties to be passed into the SqlSession configuration, as alternative to a
  378 |    * {@code &lt;properties&gt;} tag in the configuration xml file. This will be used to resolve placeholders in the
  379 |    * config file.
  380 |    *
  381 |    * @param sqlSessionFactoryProperties
  382 |    *          optional properties for the SqlSessionFactory
  383 |    */
  384 |   public void setConfigurationProperties(Properties sqlSessionFactoryProperties) {
  385 |     this.configurationProperties = sqlSessionFactoryProperties;
  386 |   }
  387 | 
  388 |   /**
  389 |    * Set the JDBC {@code DataSource} that this instance should manage transactions for. The {@code DataSource} should
  390 |    * match the one used by the {@code SqlSessionFactory}: for example, you could specify the same JNDI DataSource for
  391 |    * both.
  392 |    * <p>
  393 |    * A transactional JDBC {@code Connection} for this {@code DataSource} will be provided to application code accessing
  394 |    * this {@code DataSource} directly via {@code DataSourceUtils} or {@code DataSourceTransactionManager}.
  395 |    * <p>
  396 |    * The {@code DataSource} specified here should be the target {@code DataSource} to manage transactions for, not a
  397 |    * {@code TransactionAwareDataSourceProxy}. Only data access code may work with
  398 |    * {@code TransactionAwareDataSourceProxy}, while the transaction manager needs to work on the underlying target
  399 |    * {@code DataSource}. If there's nevertheless a {@code TransactionAwareDataSourceProxy} passed in, it will be
  400 |    * unwrapped to extract its target {@code DataSource}.
  401 |    *
  402 |    * @param dataSource
  403 |    *          a JDBC {@code DataSource}
  404 |    */
  405 |   public void setDataSource(DataSource dataSource) {
  406 |     if (dataSource instanceof TransactionAwareDataSourceProxy) {
  407 |       // If we got a TransactionAwareDataSourceProxy, we need to perform
  408 |       // transactions for its underlying target DataSource, else data
  409 |       // access code won't see properly exposed transactions (i.e.
  410 |       // transactions for the target DataSource).
  411 |       this.dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
  412 |     } else {
  413 |       this.dataSource = dataSource;
  414 |     }
  415 |   }
  416 | 
  417 |   /**
  418 |    * Sets the {@code SqlSessionFactoryBuilder} to use when creating the {@code SqlSessionFactory}.
  419 |    * <p>
  420 |    * This is mainly meant for testing so that mock SqlSessionFactory classes can be injected. By default,
  421 |    * {@code SqlSessionFactoryBuilder} creates {@code DefaultSqlSessionFactory} instances.
  422 |    *
  423 |    * @param sqlSessionFactoryBuilder
  424 |    *          a SqlSessionFactoryBuilder
  425 |    */
  426 |   public void setSqlSessionFactoryBuilder(SqlSessionFactoryBuilder sqlSessionFactoryBuilder) {
  427 |     this.sqlSessionFactoryBuilder = sqlSessionFactoryBuilder;
  428 |   }
  429 | 
  430 |   /**
  431 |    * Set the MyBatis TransactionFactory to use. Default is {@code SpringManagedTransactionFactory}.
  432 |    * <p>
  433 |    * The default {@code SpringManagedTransactionFactory} should be appropriate for all cases: be it Spring transaction
  434 |    * management, EJB CMT or plain JTA. If there is no active transaction, SqlSession operations will execute SQL
  435 |    * statements non-transactionally.
  436 |    * <p>
  437 |    * <b>It is strongly recommended to use the default {@code TransactionFactory}.</b> If not used, any attempt at
  438 |    * getting an SqlSession through Spring's MyBatis framework will throw an exception if a transaction is active.
  439 |    *
  440 |    * @see SpringManagedTransactionFactory
  441 |    *
  442 |    * @param transactionFactory
  443 |    *          the MyBatis TransactionFactory
  444 |    */
  445 |   public void setTransactionFactory(TransactionFactory transactionFactory) {
  446 |     this.transactionFactory = transactionFactory;
  447 |   }
  448 | 
  449 |   /**
  450 |    * <b>NOTE:</b> This class <em>overrides</em> any {@code Environment} you have set in the MyBatis config file. This is
  451 |    * used only as a placeholder name. The default value is {@code SqlSessionFactoryBean.class.getSimpleName()}.
  452 |    *
  453 |    * @param environment
  454 |    *          the environment name
  455 |    */
  456 |   public void setEnvironment(String environment) {
  457 |     this.environment = environment;
  458 |   }
  459 | 
  460 |   /**
  461 |    * Set scripting language drivers.
  462 |    *
  463 |    * @param scriptingLanguageDrivers
  464 |    *          scripting language drivers
  465 |    *
  466 |    * @since 2.0.2
  467 |    */
  468 |   public void setScriptingLanguageDrivers(LanguageDriver... scriptingLanguageDrivers) {
  469 |     this.scriptingLanguageDrivers = scriptingLanguageDrivers;
  470 |   }
  471 | 
  472 |   /**
  473 |    * Set a default scripting language driver class.
  474 |    *
  475 |    * @param defaultScriptingLanguageDriver
  476 |    *          A default scripting language driver class
  477 |    *
  478 |    * @since 2.0.2
  479 |    */
  480 |   public void setDefaultScriptingLanguageDriver(Class<? extends LanguageDriver> defaultScriptingLanguageDriver) {
  481 |     this.defaultScriptingLanguageDriver = defaultScriptingLanguageDriver;
  482 |   }
  483 | 
  484 |   /**
  485 |    * Add locations of MyBatis mapper files that are going to be merged into the {@code SqlSessionFactory} configuration
  486 |    * at runtime.
  487 |    * <p>
  488 |    * This is an alternative to specifying "&lt;sqlmapper&gt;" entries in an MyBatis config file. This property being
  489 |    * based on Spring's resource abstraction also allows for specifying resource patterns here: e.g.
  490 |    * "classpath*:sqlmap/*-mapper.xml".
  491 |    *
  492 |    * @param mapperLocations
  493 |    *          location of MyBatis mapper files
  494 |    *
  495 |    * @see #setMapperLocations(Resource...)
  496 |    *
  497 |    * @since 3.0.2
  498 |    */
  499 |   public void addMapperLocations(Resource... mapperLocations) {
  500 |     setMapperLocations(appendArrays(this.mapperLocations, mapperLocations, Resource[]::new));
  501 |   }
  502 | 
  503 |   /**
  504 |    * Add type handlers.
  505 |    *
  506 |    * @param typeHandlers
  507 |    *          Type handler list
  508 |    *
  509 |    * @since 3.0.2
  510 |    */
  511 |   public void addTypeHandlers(TypeHandler<?>... typeHandlers) {
  512 |     setTypeHandlers(appendArrays(this.typeHandlers, typeHandlers, TypeHandler[]::new));
  513 |   }
  514 | 
  515 |   /**
  516 |    * Add scripting language drivers.
  517 |    *
  518 |    * @param scriptingLanguageDrivers
  519 |    *          scripting language drivers
  520 |    *
  521 |    * @since 3.0.2
  522 |    */
  523 |   public void addScriptingLanguageDrivers(LanguageDriver... scriptingLanguageDrivers) {
  524 |     setScriptingLanguageDrivers(
  525 |         appendArrays(this.scriptingLanguageDrivers, scriptingLanguageDrivers, LanguageDriver[]::new));
  526 |   }
  527 | 
  528 |   /**
  529 |    * Add Mybatis plugins.
  530 |    *
  531 |    * @param plugins
  532 |    *          list of plugins
  533 |    *
  534 |    * @since 3.0.2
  535 |    */
  536 |   public void addPlugins(Interceptor... plugins) {
  537 |     setPlugins(appendArrays(this.plugins, plugins, Interceptor[]::new));
  538 |   }
  539 | 
  540 |   /**
  541 |    * Add type aliases.
  542 |    *
  543 |    * @param typeAliases
  544 |    *          Type aliases list
  545 |    *
  546 |    * @since 3.0.2
  547 |    */
  548 |   public void addTypeAliases(Class<?>... typeAliases) {
  549 |     setTypeAliases(appendArrays(this.typeAliases, typeAliases, Class[]::new));
  550 |   }
  551 | 
  552 |   private <T> T[] appendArrays(T[] oldArrays, T[] newArrays, IntFunction<T[]> generator) {
  553 |     if (oldArrays == null) {
  554 |       return newArrays;
  555 |     }
  556 |     if (newArrays == null) {
  557 |       return oldArrays;
  558 |     }
  559 |     List<T> newList = new ArrayList<>(Arrays.asList(oldArrays));
  560 |     newList.addAll(Arrays.asList(newArrays));
  561 |     return newList.toArray(generator.apply(0));
  562 |   }
  563 | 
  564 |   @Override
  565 |   public void afterPropertiesSet() throws Exception {
  566 |     notNull(dataSource, "Property 'dataSource' is required");
  567 |     notNull(sqlSessionFactoryBuilder, "Property 'sqlSessionFactoryBuilder' is required");
  568 |     // TODO Review this statement as it seems off!
  569 |     state((configuration == null && configLocation == null) || !(configuration != null && configLocation != null),
  570 |         "Property 'configuration' and 'configLocation' can not specified with together");
  571 | 
  572 |     this.sqlSessionFactory = buildSqlSessionFactory();
  573 |   }
  574 | 
  575 |   /**
  576 |    * Build a {@code SqlSessionFactory} instance.
  577 |    * <p>
  578 |    * The default implementation uses the standard MyBatis {@code XMLConfigBuilder} API to build a
  579 |    * {@code SqlSessionFactory} instance based on a Reader. Since 1.3.0, it can be specified a {@link Configuration}
  580 |    * instance directly(without config file).
  581 |    *
  582 |    * @return SqlSessionFactory
  583 |    *
  584 |    * @throws Exception
  585 |    *           if configuration is failed
  586 |    */
  587 |   protected SqlSessionFactory buildSqlSessionFactory() throws Exception {
  588 | 
  589 |     final Configuration targetConfiguration;
  590 | 
  591 |     XMLConfigBuilder xmlConfigBuilder = null;
  592 |     if (this.configuration != null) {
  593 |       targetConfiguration = this.configuration;
  594 |       if (targetConfiguration.getVariables() == null) {
  595 |         targetConfiguration.setVariables(this.configurationProperties);
  596 |       } else if (this.configurationProperties != null) {
  597 |         targetConfiguration.getVariables().putAll(this.configurationProperties);
  598 |       }
  599 |     } else if (this.configLocation != null) {
  600 |       xmlConfigBuilder = new XMLConfigBuilder(this.configLocation.getInputStream(), null, this.configurationProperties);
  601 |       targetConfiguration = xmlConfigBuilder.getConfiguration();
  602 |     } else {
  603 |       LOGGER.debug(
  604 |           () -> "Property 'configuration' or 'configLocation' not specified, using default MyBatis Configuration");
  605 |       targetConfiguration = new Configuration();
  606 |       Optional.ofNullable(this.configurationProperties).ifPresent(targetConfiguration::setVariables);
  607 |     }
  608 | 
  609 |     Optional.ofNullable(this.objectFactory).ifPresent(targetConfiguration::setObjectFactory);
  610 |     Optional.ofNullable(this.objectWrapperFactory).ifPresent(targetConfiguration::setObjectWrapperFactory);
  611 |     Optional.ofNullable(this.vfs).ifPresent(targetConfiguration::setVfsImpl);
  612 | 
  613 |     if (hasLength(this.typeAliasesPackage)) {
  614 |       scanClasses(this.typeAliasesPackage, this.typeAliasesSuperType).stream()
  615 |           .filter(clazz -> !clazz.isAnonymousClass()).filter(clazz -> !clazz.isInterface())
  616 |           .filter(clazz -> !clazz.isMemberClass()).forEach(targetConfiguration.getTypeAliasRegistry()::registerAlias);
  617 |     }
  618 | 
  619 |     if (!isEmpty(this.typeAliases)) {
  620 |       Stream.of(this.typeAliases).forEach(typeAlias -> {
  621 |         targetConfiguration.getTypeAliasRegistry().registerAlias(typeAlias);
  622 |         LOGGER.debug(() -> "Registered type alias: '" + typeAlias + "'");
  623 |       });
  624 |     }
  625 | 
  626 |     if (!isEmpty(this.plugins)) {
  627 |       Stream.of(this.plugins).forEach(plugin -> {
  628 |         targetConfiguration.addInterceptor(plugin);
  629 |         LOGGER.debug(() -> "Registered plugin: '" + plugin + "'");
  630 |       });
  631 |     }
  632 | 
  633 |     if (hasLength(this.typeHandlersPackage)) {
  634 |       scanClasses(this.typeHandlersPackage, TypeHandler.class).stream().filter(clazz -> !clazz.isAnonymousClass())
  635 |           .filter(clazz -> !clazz.isInterface()).filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
  636 |           .forEach(targetConfiguration.getTypeHandlerRegistry()::register);
  637 |     }
  638 | 
  639 |     if (!isEmpty(this.typeHandlers)) {
  640 |       Stream.of(this.typeHandlers).forEach(typeHandler -> {
  641 |         targetConfiguration.getTypeHandlerRegistry().register(typeHandler);
  642 |         LOGGER.debug(() -> "Registered type handler: '" + typeHandler + "'");
  643 |       });
  644 |     }
  645 | 
  646 |     targetConfiguration.setDefaultEnumTypeHandler(defaultEnumTypeHandler);
  647 | 
  648 |     if (!isEmpty(this.scriptingLanguageDrivers)) {
  649 |       Stream.of(this.scriptingLanguageDrivers).forEach(languageDriver -> {
  650 |         targetConfiguration.getLanguageRegistry().register(languageDriver);
  651 |         LOGGER.debug(() -> "Registered scripting language driver: '" + languageDriver + "'");
  652 |       });
  653 |     }
  654 |     Optional.ofNullable(this.defaultScriptingLanguageDriver)
  655 |         .ifPresent(targetConfiguration::setDefaultScriptingLanguage);
  656 | 
  657 |     if (this.databaseIdProvider != null) {// fix #64 set databaseId before parse mapper xmls
  658 |       try {
  659 |         targetConfiguration.setDatabaseId(this.databaseIdProvider.getDatabaseId(this.dataSource));
  660 |       } catch (SQLException e) {
  661 |         throw new IOException("Failed getting a databaseId", e);
  662 |       }
  663 |     }
  664 | 
  665 |     Optional.ofNullable(this.cache).ifPresent(targetConfiguration::addCache);
  666 | 
  667 |     if (xmlConfigBuilder != null) {
  668 |       try {
  669 |         xmlConfigBuilder.parse();
  670 |         LOGGER.debug(() -> "Parsed configuration file: '" + this.configLocation + "'");
  671 |       } catch (Exception ex) {
  672 |         throw new IOException("Failed to parse config resource: " + this.configLocation, ex);
  673 |       } finally {
  674 |         ErrorContext.instance().reset();
  675 |       }
  676 |     }
  677 | 
  678 |     targetConfiguration.setEnvironment(new Environment(this.environment,
  679 |         this.transactionFactory == null ? new SpringManagedTransactionFactory() : this.transactionFactory,
  680 |         this.dataSource));
  681 | 
  682 |     if (this.mapperLocations != null) {
  683 |       if (this.mapperLocations.length == 0) {
  684 |         LOGGER.warn(() -> "Property 'mapperLocations' was specified but matching resources are not found.");
  685 |       } else {
  686 |         for (Resource mapperLocation : this.mapperLocations) {
  687 |           if (mapperLocation == null) {
  688 |             continue;
  689 |           }
  690 |           try {
  691 |             var xmlMapperBuilder = new XMLMapperBuilder(mapperLocation.getInputStream(), targetConfiguration,
  692 |                 mapperLocation.toString(), targetConfiguration.getSqlFragments());
  693 |             xmlMapperBuilder.parse();
  694 |           } catch (Exception e) {
  695 |             throw new IOException("Failed to parse mapping resource: '" + mapperLocation + "'", e);
  696 |           } finally {
  697 |             ErrorContext.instance().reset();
  698 |           }
  699 |           LOGGER.debug(() -> "Parsed mapper file: '" + mapperLocation + "'");
  700 |         }
  701 |       }
  702 |     } else {
  703 |       LOGGER.debug(() -> "Property 'mapperLocations' was not specified.");
  704 |     }
  705 | 
  706 |     return this.sqlSessionFactoryBuilder.build(targetConfiguration);
  707 |   }
  708 | 
  709 |   @Override
  710 |   public SqlSessionFactory getObject() throws Exception {
  711 |     if (this.sqlSessionFactory == null) {
  712 |       afterPropertiesSet();
  713 |     }
  714 | 
  715 |     return this.sqlSessionFactory;
  716 |   }
  717 | 
  718 |   @Override
  719 |   public Class<? extends SqlSessionFactory> getObjectType() {
  720 |     return this.sqlSessionFactory == null ? SqlSessionFactory.class : this.sqlSessionFactory.getClass();
  721 |   }
  722 | 
  723 |   @Override
  724 |   public boolean isSingleton() {
  725 |     return true;
  726 |   }
  727 | 
  728 |   @Override
  729 |   public void onApplicationEvent(ContextRefreshedEvent event) {
  730 |     if (failFast) {
  731 |       // fail-fast -> check all statements are completed
  732 |       this.sqlSessionFactory.getConfiguration().getMappedStatementNames();
  733 |     }
  734 |   }
  735 | 
  736 |   private Set<Class<?>> scanClasses(String packagePatterns, Class<?> assignableType) throws IOException {
  737 |     Set<Class<?>> classes = new HashSet<>();
  738 |     var packagePatternArray = tokenizeToStringArray(packagePatterns,
  739 |         ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
  740 |     for (String packagePattern : packagePatternArray) {
  741 |       var resources = RESOURCE_PATTERN_RESOLVER.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
  742 |           + ClassUtils.convertClassNameToResourcePath(packagePattern) + "/**/*.class");
  743 |       for (Resource resource : resources) {
  744 |         try {
  745 |           var classMetadata = METADATA_READER_FACTORY.getMetadataReader(resource).getClassMetadata();
  746 |           Class<?> clazz = Resources.classForName(classMetadata.getClassName());
  747 |           if (assignableType == null || assignableType.isAssignableFrom(clazz)) {
  748 |             classes.add(clazz);
  749 |           }
  750 |         } catch (Throwable e) {
  751 |           LOGGER.warn(() -> "Cannot load the '" + resource + "'. Cause by " + e.toString());
  752 |         }
  753 |       }
  754 |     }
  755 |     return classes;
  756 |   }
  757 | 
  758 | }
```
