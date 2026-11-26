<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis.spring.boot:mybatis-spring-boot-autoconfigure:4.1.0 | path: org/mybatis/spring/boot/autoconfigure/MybatisProperties.java -->
<!-- 正文引用：[[源码:MybatisProperties.java#L起-L止]] ；代码块头部：(src: MybatisProperties.java#L起-L止) -->

# MybatisProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis.spring.boot:mybatis-spring-boot-autoconfigure:4.1.0` |
| 路径 | `org/mybatis/spring/boot/autoconfigure/MybatisProperties.java` |
| 行数 | 713 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/spring/boot/mybatis-spring-boot-autoconfigure/4.1.0/mybatis-spring-boot-autoconfigure-4.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:01:44.375Z |

```java
    1 | /*
    2 |  *    Copyright 2015-2025 the original author or authors.
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
   16 | package org.mybatis.spring.boot.autoconfigure;
   17 | 
   18 | import java.io.IOException;
   19 | import java.util.Optional;
   20 | import java.util.Properties;
   21 | import java.util.Set;
   22 | import java.util.stream.Stream;
   23 | 
   24 | import org.apache.ibatis.io.VFS;
   25 | import org.apache.ibatis.logging.Log;
   26 | import org.apache.ibatis.mapping.ResultSetType;
   27 | import org.apache.ibatis.scripting.LanguageDriver;
   28 | import org.apache.ibatis.session.AutoMappingBehavior;
   29 | import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
   30 | import org.apache.ibatis.session.Configuration;
   31 | import org.apache.ibatis.session.ExecutorType;
   32 | import org.apache.ibatis.session.LocalCacheScope;
   33 | import org.apache.ibatis.type.JdbcType;
   34 | import org.apache.ibatis.type.TypeHandler;
   35 | import org.springframework.boot.context.properties.ConfigurationProperties;
   36 | import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
   37 | import org.springframework.boot.context.properties.PropertyMapper;
   38 | import org.springframework.core.io.Resource;
   39 | import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
   40 | import org.springframework.core.io.support.ResourcePatternResolver;
   41 | 
   42 | /**
   43 |  * Configuration properties for MyBatis.
   44 |  *
   45 |  * @author Eddú Meléndez
   46 |  * @author Kazuki Shimizu
   47 |  */
   48 | @ConfigurationProperties(prefix = MybatisProperties.MYBATIS_PREFIX)
   49 | public class MybatisProperties {
   50 | 
   51 |   public static final String MYBATIS_PREFIX = "mybatis";
   52 | 
   53 |   private static final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
   54 | 
   55 |   /**
   56 |    * Location of MyBatis xml config file.
   57 |    */
   58 |   private String configLocation;
   59 | 
   60 |   /**
   61 |    * Locations of MyBatis mapper files.
   62 |    */
   63 |   private String[] mapperLocations;
   64 | 
   65 |   /**
   66 |    * Packages to search type aliases. (Package delimiters are ",; \t\n")
   67 |    */
   68 |   private String typeAliasesPackage;
   69 | 
   70 |   /**
   71 |    * The super class for filtering type alias. If this not specifies, the MyBatis deal as type alias all classes that
   72 |    * searched from typeAliasesPackage.
   73 |    */
   74 |   private Class<?> typeAliasesSuperType;
   75 | 
   76 |   /**
   77 |    * Packages to search for type handlers. (Package delimiters are ",; \t\n")
   78 |    */
   79 |   private String typeHandlersPackage;
   80 | 
   81 |   /**
   82 |    * Indicates whether perform presence check of the MyBatis xml config file.
   83 |    */
   84 |   private boolean checkConfigLocation = false;
   85 | 
   86 |   /**
   87 |    * Execution mode for {@link org.mybatis.spring.SqlSessionTemplate}.
   88 |    */
   89 |   private ExecutorType executorType;
   90 | 
   91 |   /**
   92 |    * The default scripting language driver class. (Available when use together with mybatis-spring 2.0.2+)
   93 |    */
   94 |   private Class<? extends LanguageDriver> defaultScriptingLanguageDriver;
   95 | 
   96 |   /**
   97 |    * Externalized properties for MyBatis configuration.
   98 |    */
   99 |   private Properties configurationProperties;
  100 | 
  101 |   /**
  102 |    * A Configuration object for customize default settings. If {@link #configLocation} is specified, this property is
  103 |    * not used.
  104 |    */
  105 |   private CoreConfiguration configuration;
  106 | 
  107 |   /**
  108 |    * @since 1.1.0
  109 |    */
  110 |   public String getConfigLocation() {
  111 |     return this.configLocation;
  112 |   }
  113 | 
  114 |   /**
  115 |    * @since 1.1.0
  116 |    */
  117 |   public void setConfigLocation(String configLocation) {
  118 |     this.configLocation = configLocation;
  119 |   }
  120 | 
  121 |   public String[] getMapperLocations() {
  122 |     return this.mapperLocations;
  123 |   }
  124 | 
  125 |   public void setMapperLocations(String[] mapperLocations) {
  126 |     this.mapperLocations = mapperLocations;
  127 |   }
  128 | 
  129 |   public String getTypeHandlersPackage() {
  130 |     return this.typeHandlersPackage;
  131 |   }
  132 | 
  133 |   public void setTypeHandlersPackage(String typeHandlersPackage) {
  134 |     this.typeHandlersPackage = typeHandlersPackage;
  135 |   }
  136 | 
  137 |   public String getTypeAliasesPackage() {
  138 |     return this.typeAliasesPackage;
  139 |   }
  140 | 
  141 |   public void setTypeAliasesPackage(String typeAliasesPackage) {
  142 |     this.typeAliasesPackage = typeAliasesPackage;
  143 |   }
  144 | 
  145 |   /**
  146 |    * @since 1.3.3
  147 |    */
  148 |   public Class<?> getTypeAliasesSuperType() {
  149 |     return typeAliasesSuperType;
  150 |   }
  151 | 
  152 |   /**
  153 |    * @since 1.3.3
  154 |    */
  155 |   public void setTypeAliasesSuperType(Class<?> typeAliasesSuperType) {
  156 |     this.typeAliasesSuperType = typeAliasesSuperType;
  157 |   }
  158 | 
  159 |   public boolean isCheckConfigLocation() {
  160 |     return this.checkConfigLocation;
  161 |   }
  162 | 
  163 |   public void setCheckConfigLocation(boolean checkConfigLocation) {
  164 |     this.checkConfigLocation = checkConfigLocation;
  165 |   }
  166 | 
  167 |   public ExecutorType getExecutorType() {
  168 |     return this.executorType;
  169 |   }
  170 | 
  171 |   public void setExecutorType(ExecutorType executorType) {
  172 |     this.executorType = executorType;
  173 |   }
  174 | 
  175 |   /**
  176 |    * @since 2.1.0
  177 |    */
  178 |   public Class<? extends LanguageDriver> getDefaultScriptingLanguageDriver() {
  179 |     return defaultScriptingLanguageDriver;
  180 |   }
  181 | 
  182 |   /**
  183 |    * @since 2.1.0
  184 |    */
  185 |   public void setDefaultScriptingLanguageDriver(Class<? extends LanguageDriver> defaultScriptingLanguageDriver) {
  186 |     this.defaultScriptingLanguageDriver = defaultScriptingLanguageDriver;
  187 |   }
  188 | 
  189 |   /**
  190 |    * @since 1.2.0
  191 |    */
  192 |   public Properties getConfigurationProperties() {
  193 |     return configurationProperties;
  194 |   }
  195 | 
  196 |   /**
  197 |    * @since 1.2.0
  198 |    */
  199 |   public void setConfigurationProperties(Properties configurationProperties) {
  200 |     this.configurationProperties = configurationProperties;
  201 |   }
  202 | 
  203 |   public CoreConfiguration getConfiguration() {
  204 |     return configuration;
  205 |   }
  206 | 
  207 |   public void setConfiguration(CoreConfiguration configuration) {
  208 |     this.configuration = configuration;
  209 |   }
  210 | 
  211 |   public Resource[] resolveMapperLocations() {
  212 |     return Stream.of(Optional.ofNullable(this.mapperLocations).orElse(new String[0]))
  213 |         .flatMap(location -> Stream.of(getResources(location))).toArray(Resource[]::new);
  214 |   }
  215 | 
  216 |   private Resource[] getResources(String location) {
  217 |     try {
  218 |       return resourceResolver.getResources(location);
  219 |     } catch (IOException e) {
  220 |       return new Resource[0];
  221 |     }
  222 |   }
  223 | 
  224 |   /**
  225 |    * The configuration properties for mybatis core module.
  226 |    *
  227 |    * @since 3.0.0
  228 |    */
  229 |   public static class CoreConfiguration {
  230 | 
  231 |     /**
  232 |      * Allows using RowBounds on nested statements. If allow, set the false. Default is false.
  233 |      */
  234 |     private Boolean safeRowBoundsEnabled;
  235 | 
  236 |     /**
  237 |      * Allows using ResultHandler on nested statements. If allow, set the false. Default is true.
  238 |      */
  239 |     private Boolean safeResultHandlerEnabled;
  240 | 
  241 |     /**
  242 |      * Enables automatic mapping from classic database column names A_COLUMN to camel case classic Java property names
  243 |      * aColumn. Default is false.
  244 |      */
  245 |     private Boolean mapUnderscoreToCamelCase;
  246 | 
  247 |     /**
  248 |      * When enabled, any method call will load all the lazy properties of the object. Otherwise, each property is loaded
  249 |      * on demand (see also lazyLoadTriggerMethods). Default is false.
  250 |      */
  251 |     private Boolean aggressiveLazyLoading;
  252 | 
  253 |     /**
  254 |      * Allows or disallows multiple ResultSets to be returned from a single statement (compatible driver required).
  255 |      * Default is true.
  256 |      */
  257 |     private Boolean multipleResultSetsEnabled;
  258 | 
  259 |     /**
  260 |      * Allows JDBC support for generated keys. A compatible driver is required. This setting forces generated keys to be
  261 |      * used if set to true, as some drivers deny compatibility but still work (e.g. Derby). Default is false.
  262 |      */
  263 |     private Boolean useGeneratedKeys;
  264 | 
  265 |     /**
  266 |      * Uses the column label instead of the column name. Different drivers behave differently in this respect. Refer to
  267 |      * the driver documentation, or test out both modes to determine how your driver behaves. Default is true.
  268 |      */
  269 |     private Boolean useColumnLabel;
  270 | 
  271 |     /**
  272 |      * Globally enables or disables any caches configured in any mapper under this configuration. Default is true.
  273 |      */
  274 |     private Boolean cacheEnabled;
  275 | 
  276 |     /**
  277 |      * Specifies if setters or map's put method will be called when a retrieved value is null. It is useful when you
  278 |      * rely on Map.keySet() or null value initialization. Note primitives such as (int,boolean,etc.) will not be set to
  279 |      * null. Default is false.
  280 |      */
  281 |     private Boolean callSettersOnNulls;
  282 | 
  283 |     /**
  284 |      * Allow referencing statement parameters by their actual names declared in the method signature. To use this
  285 |      * feature, your project must be compiled in Java 8 with -parameters option. Default is true.
  286 |      */
  287 |     private Boolean useActualParamName;
  288 | 
  289 |     /**
  290 |      * MyBatis, by default, returns null when all the columns of a returned row are NULL. When this setting is enabled,
  291 |      * MyBatis returns an empty instance instead. Note that it is also applied to nested results (i.e. collection and
  292 |      * association). Default is false.
  293 |      */
  294 |     private Boolean returnInstanceForEmptyRow;
  295 | 
  296 |     /**
  297 |      * Removes extra whitespace characters from the SQL. Note that this also affects literal strings in SQL. Default is
  298 |      * false.
  299 |      */
  300 |     private Boolean shrinkWhitespacesInSql;
  301 | 
  302 |     /**
  303 |      * Specifies the default value of 'nullable' attribute on 'foreach' tag. Default is false.
  304 |      */
  305 |     private Boolean nullableOnForEach;
  306 | 
  307 |     /**
  308 |      * When applying constructor auto-mapping, argument name is used to search the column to map instead of relying on
  309 |      * the column order. Default is false.
  310 |      */
  311 |     private Boolean argNameBasedConstructorAutoMapping;
  312 | 
  313 |     /**
  314 |      * Globally enables or disables lazy loading. When enabled, all relations will be lazily loaded. This value can be
  315 |      * superseded for a specific relation by using the fetchType attribute on it. Default is False.
  316 |      */
  317 |     private Boolean lazyLoadingEnabled;
  318 | 
  319 |     /**
  320 |      * Sets the number of seconds the driver will wait for a response from the database.
  321 |      */
  322 |     private Integer defaultStatementTimeout;
  323 | 
  324 |     /**
  325 |      * Sets the driver a hint as to control fetching size for return results. This parameter value can be override by a
  326 |      * query setting.
  327 |      */
  328 |     private Integer defaultFetchSize;
  329 | 
  330 |     /**
  331 |      * MyBatis uses local cache to prevent circular references and speed up repeated nested queries. By default
  332 |      * (SESSION) all queries executed during a session are cached. If localCacheScope=STATEMENT local session will be
  333 |      * used just for statement execution, no data will be shared between two different calls to the same SqlSession.
  334 |      * Default is SESSION.
  335 |      */
  336 |     private LocalCacheScope localCacheScope;
  337 | 
  338 |     /**
  339 |      * Specifies the JDBC type for null values when no specific JDBC type was provided for the parameter. Some drivers
  340 |      * require specifying the column JDBC type but others work with generic values like NULL, VARCHAR or OTHER. Default
  341 |      * is OTHER.
  342 |      */
  343 |     private JdbcType jdbcTypeForNull;
  344 | 
  345 |     /**
  346 |      * Specifies a scroll strategy when omit it per statement settings.
  347 |      */
  348 |     private ResultSetType defaultResultSetType;
  349 | 
  350 |     /**
  351 |      * Configures the default executor. SIMPLE executor does nothing special. REUSE executor reuses prepared statements.
  352 |      * BATCH executor reuses statements and batches updates. Default is SIMPLE.
  353 |      */
  354 |     private ExecutorType defaultExecutorType;
  355 | 
  356 |     /**
  357 |      * Specifies if and how MyBatis should automatically map columns to fields/properties. NONE disables auto-mapping.
  358 |      * PARTIAL will only auto-map results with no nested result mappings defined inside. FULL will auto-map result
  359 |      * mappings of any complexity (containing nested or otherwise). Default is PARTIAL.
  360 |      */
  361 |     private AutoMappingBehavior autoMappingBehavior;
  362 | 
  363 |     /**
  364 |      * Specify the behavior when detects an unknown column (or unknown property type) of automatic mapping target.
  365 |      * Default is NONE.
  366 |      */
  367 |     private AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior;
  368 | 
  369 |     /**
  370 |      * Specifies the prefix string that MyBatis will add to the logger names.
  371 |      */
  372 |     private String logPrefix;
  373 | 
  374 |     /**
  375 |      * Specifies which Object's methods trigger a lazy load. Default is [equals,clone,hashCode,toString].
  376 |      */
  377 |     private Set<String> lazyLoadTriggerMethods;
  378 | 
  379 |     /**
  380 |      * Specifies which logging implementation MyBatis should use. If this setting is not present logging implementation
  381 |      * will be autodiscovered.
  382 |      */
  383 |     private Class<? extends Log> logImpl;
  384 | 
  385 |     /**
  386 |      * Specifies VFS implementations.
  387 |      */
  388 |     private Class<? extends VFS> vfsImpl;
  389 | 
  390 |     /**
  391 |      * Specifies an sql provider class that holds provider method. This class apply to the type(or value) attribute on
  392 |      * sql provider annotation(e.g. @SelectProvider), when these attribute was omitted.
  393 |      */
  394 |     private Class<?> defaultSqlProviderType;
  395 | 
  396 |     /**
  397 |      * Specifies the TypeHandler used by default for Enum.
  398 |      */
  399 |     Class<? extends TypeHandler> defaultEnumTypeHandler;
  400 | 
  401 |     /**
  402 |      * Specifies the class that provides an instance of Configuration. The returned Configuration instance is used to
  403 |      * load lazy properties of deserialized objects. This class must have a method with a signature static Configuration
  404 |      * getConfiguration().
  405 |      */
  406 |     private Class<?> configurationFactory;
  407 | 
  408 |     /**
  409 |      * Specify any configuration variables.
  410 |      */
  411 |     private Properties variables;
  412 | 
  413 |     /**
  414 |      * Specifies the database identify value for switching query to use.
  415 |      */
  416 |     private String databaseId;
  417 | 
  418 |     public Boolean getSafeRowBoundsEnabled() {
  419 |       return safeRowBoundsEnabled;
  420 |     }
  421 | 
  422 |     public void setSafeRowBoundsEnabled(Boolean safeRowBoundsEnabled) {
  423 |       this.safeRowBoundsEnabled = safeRowBoundsEnabled;
  424 |     }
  425 | 
  426 |     public Boolean getSafeResultHandlerEnabled() {
  427 |       return safeResultHandlerEnabled;
  428 |     }
  429 | 
  430 |     public void setSafeResultHandlerEnabled(Boolean safeResultHandlerEnabled) {
  431 |       this.safeResultHandlerEnabled = safeResultHandlerEnabled;
  432 |     }
  433 | 
  434 |     public Boolean getMapUnderscoreToCamelCase() {
  435 |       return mapUnderscoreToCamelCase;
  436 |     }
  437 | 
  438 |     public void setMapUnderscoreToCamelCase(Boolean mapUnderscoreToCamelCase) {
  439 |       this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
  440 |     }
  441 | 
  442 |     public Boolean getAggressiveLazyLoading() {
  443 |       return aggressiveLazyLoading;
  444 |     }
  445 | 
  446 |     public void setAggressiveLazyLoading(Boolean aggressiveLazyLoading) {
  447 |       this.aggressiveLazyLoading = aggressiveLazyLoading;
  448 |     }
  449 | 
  450 |     @DeprecatedConfigurationProperty(since = "3.0.4", reason = "The option is not used at MyBatis core module. It will be removed in the future. See https://github.com/mybatis/mybatis-3/pull/3238")
  451 |     public Boolean getMultipleResultSetsEnabled() {
  452 |       return multipleResultSetsEnabled;
  453 |     }
  454 | 
  455 |     public void setMultipleResultSetsEnabled(Boolean multipleResultSetsEnabled) {
  456 |       this.multipleResultSetsEnabled = multipleResultSetsEnabled;
  457 |     }
  458 | 
  459 |     public Boolean getUseGeneratedKeys() {
  460 |       return useGeneratedKeys;
  461 |     }
  462 | 
  463 |     public void setUseGeneratedKeys(Boolean useGeneratedKeys) {
  464 |       this.useGeneratedKeys = useGeneratedKeys;
  465 |     }
  466 | 
  467 |     public Boolean getUseColumnLabel() {
  468 |       return useColumnLabel;
  469 |     }
  470 | 
  471 |     public void setUseColumnLabel(Boolean useColumnLabel) {
  472 |       this.useColumnLabel = useColumnLabel;
  473 |     }
  474 | 
  475 |     public Boolean getCacheEnabled() {
  476 |       return cacheEnabled;
  477 |     }
  478 | 
  479 |     public void setCacheEnabled(Boolean cacheEnabled) {
  480 |       this.cacheEnabled = cacheEnabled;
  481 |     }
  482 | 
  483 |     public Boolean getCallSettersOnNulls() {
  484 |       return callSettersOnNulls;
  485 |     }
  486 | 
  487 |     public void setCallSettersOnNulls(Boolean callSettersOnNulls) {
  488 |       this.callSettersOnNulls = callSettersOnNulls;
  489 |     }
  490 | 
  491 |     public Boolean getUseActualParamName() {
  492 |       return useActualParamName;
  493 |     }
  494 | 
  495 |     public void setUseActualParamName(Boolean useActualParamName) {
  496 |       this.useActualParamName = useActualParamName;
  497 |     }
  498 | 
  499 |     public Boolean getReturnInstanceForEmptyRow() {
  500 |       return returnInstanceForEmptyRow;
  501 |     }
  502 | 
  503 |     public void setReturnInstanceForEmptyRow(Boolean returnInstanceForEmptyRow) {
  504 |       this.returnInstanceForEmptyRow = returnInstanceForEmptyRow;
  505 |     }
  506 | 
  507 |     public Boolean getShrinkWhitespacesInSql() {
  508 |       return shrinkWhitespacesInSql;
  509 |     }
  510 | 
  511 |     public void setShrinkWhitespacesInSql(Boolean shrinkWhitespacesInSql) {
  512 |       this.shrinkWhitespacesInSql = shrinkWhitespacesInSql;
  513 |     }
  514 | 
  515 |     public Boolean getNullableOnForEach() {
  516 |       return nullableOnForEach;
  517 |     }
  518 | 
  519 |     public void setNullableOnForEach(Boolean nullableOnForEach) {
  520 |       this.nullableOnForEach = nullableOnForEach;
  521 |     }
  522 | 
  523 |     public Boolean getArgNameBasedConstructorAutoMapping() {
  524 |       return argNameBasedConstructorAutoMapping;
  525 |     }
  526 | 
  527 |     public void setArgNameBasedConstructorAutoMapping(Boolean argNameBasedConstructorAutoMapping) {
  528 |       this.argNameBasedConstructorAutoMapping = argNameBasedConstructorAutoMapping;
  529 |     }
  530 | 
  531 |     public String getLogPrefix() {
  532 |       return logPrefix;
  533 |     }
  534 | 
  535 |     public void setLogPrefix(String logPrefix) {
  536 |       this.logPrefix = logPrefix;
  537 |     }
  538 | 
  539 |     public Class<? extends Log> getLogImpl() {
  540 |       return logImpl;
  541 |     }
  542 | 
  543 |     public void setLogImpl(Class<? extends Log> logImpl) {
  544 |       this.logImpl = logImpl;
  545 |     }
  546 | 
  547 |     public Class<? extends VFS> getVfsImpl() {
  548 |       return vfsImpl;
  549 |     }
  550 | 
  551 |     public void setVfsImpl(Class<? extends VFS> vfsImpl) {
  552 |       this.vfsImpl = vfsImpl;
  553 |     }
  554 | 
  555 |     public Class<?> getDefaultSqlProviderType() {
  556 |       return defaultSqlProviderType;
  557 |     }
  558 | 
  559 |     public void setDefaultSqlProviderType(Class<?> defaultSqlProviderType) {
  560 |       this.defaultSqlProviderType = defaultSqlProviderType;
  561 |     }
  562 | 
  563 |     public LocalCacheScope getLocalCacheScope() {
  564 |       return localCacheScope;
  565 |     }
  566 | 
  567 |     public void setLocalCacheScope(LocalCacheScope localCacheScope) {
  568 |       this.localCacheScope = localCacheScope;
  569 |     }
  570 | 
  571 |     public JdbcType getJdbcTypeForNull() {
  572 |       return jdbcTypeForNull;
  573 |     }
  574 | 
  575 |     public void setJdbcTypeForNull(JdbcType jdbcTypeForNull) {
  576 |       this.jdbcTypeForNull = jdbcTypeForNull;
  577 |     }
  578 | 
  579 |     public Set<String> getLazyLoadTriggerMethods() {
  580 |       return lazyLoadTriggerMethods;
  581 |     }
  582 | 
  583 |     public void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods) {
  584 |       this.lazyLoadTriggerMethods = lazyLoadTriggerMethods;
  585 |     }
  586 | 
  587 |     public Integer getDefaultStatementTimeout() {
  588 |       return defaultStatementTimeout;
  589 |     }
  590 | 
  591 |     public void setDefaultStatementTimeout(Integer defaultStatementTimeout) {
  592 |       this.defaultStatementTimeout = defaultStatementTimeout;
  593 |     }
  594 | 
  595 |     public Integer getDefaultFetchSize() {
  596 |       return defaultFetchSize;
  597 |     }
  598 | 
  599 |     public void setDefaultFetchSize(Integer defaultFetchSize) {
  600 |       this.defaultFetchSize = defaultFetchSize;
  601 |     }
  602 | 
  603 |     public ResultSetType getDefaultResultSetType() {
  604 |       return defaultResultSetType;
  605 |     }
  606 | 
  607 |     public void setDefaultResultSetType(ResultSetType defaultResultSetType) {
  608 |       this.defaultResultSetType = defaultResultSetType;
  609 |     }
  610 | 
  611 |     public ExecutorType getDefaultExecutorType() {
  612 |       return defaultExecutorType;
  613 |     }
  614 | 
  615 |     public void setDefaultExecutorType(ExecutorType defaultExecutorType) {
  616 |       this.defaultExecutorType = defaultExecutorType;
  617 |     }
  618 | 
  619 |     public AutoMappingBehavior getAutoMappingBehavior() {
  620 |       return autoMappingBehavior;
  621 |     }
  622 | 
  623 |     public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior) {
  624 |       this.autoMappingBehavior = autoMappingBehavior;
  625 |     }
  626 | 
  627 |     public AutoMappingUnknownColumnBehavior getAutoMappingUnknownColumnBehavior() {
  628 |       return autoMappingUnknownColumnBehavior;
  629 |     }
  630 | 
  631 |     public void setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior) {
  632 |       this.autoMappingUnknownColumnBehavior = autoMappingUnknownColumnBehavior;
  633 |     }
  634 | 
  635 |     public Properties getVariables() {
  636 |       return variables;
  637 |     }
  638 | 
  639 |     public void setVariables(Properties variables) {
  640 |       this.variables = variables;
  641 |     }
  642 | 
  643 |     public Boolean getLazyLoadingEnabled() {
  644 |       return lazyLoadingEnabled;
  645 |     }
  646 | 
  647 |     public void setLazyLoadingEnabled(Boolean lazyLoadingEnabled) {
  648 |       this.lazyLoadingEnabled = lazyLoadingEnabled;
  649 |     }
  650 | 
  651 |     public Class<?> getConfigurationFactory() {
  652 |       return configurationFactory;
  653 |     }
  654 | 
  655 |     public void setConfigurationFactory(Class<?> configurationFactory) {
  656 |       this.configurationFactory = configurationFactory;
  657 |     }
  658 | 
  659 |     public Class<? extends TypeHandler> getDefaultEnumTypeHandler() {
  660 |       return defaultEnumTypeHandler;
  661 |     }
  662 | 
  663 |     public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> defaultEnumTypeHandler) {
  664 |       this.defaultEnumTypeHandler = defaultEnumTypeHandler;
  665 |     }
  666 | 
  667 |     public String getDatabaseId() {
  668 |       return databaseId;
  669 |     }
  670 | 
  671 |     public void setDatabaseId(String databaseId) {
  672 |       this.databaseId = databaseId;
  673 |     }
  674 | 
  675 |     public void applyTo(Configuration target) {
  676 |       PropertyMapper mapper = PropertyMapper.get();
  677 |       mapper.from(getSafeRowBoundsEnabled()).to(target::setSafeRowBoundsEnabled);
  678 |       mapper.from(getSafeResultHandlerEnabled()).to(target::setSafeResultHandlerEnabled);
  679 |       mapper.from(getMapUnderscoreToCamelCase()).to(target::setMapUnderscoreToCamelCase);
  680 |       mapper.from(getAggressiveLazyLoading()).to(target::setAggressiveLazyLoading);
  681 |       mapper.from(getMultipleResultSetsEnabled()).to(target::setMultipleResultSetsEnabled);
  682 |       mapper.from(getUseGeneratedKeys()).to(target::setUseGeneratedKeys);
  683 |       mapper.from(getUseColumnLabel()).to(target::setUseColumnLabel);
  684 |       mapper.from(getCacheEnabled()).to(target::setCacheEnabled);
  685 |       mapper.from(getCallSettersOnNulls()).to(target::setCallSettersOnNulls);
  686 |       mapper.from(getUseActualParamName()).to(target::setUseActualParamName);
  687 |       mapper.from(getReturnInstanceForEmptyRow()).to(target::setReturnInstanceForEmptyRow);
  688 |       mapper.from(getShrinkWhitespacesInSql()).to(target::setShrinkWhitespacesInSql);
  689 |       mapper.from(getNullableOnForEach()).to(target::setNullableOnForEach);
  690 |       mapper.from(getArgNameBasedConstructorAutoMapping()).to(target::setArgNameBasedConstructorAutoMapping);
  691 |       mapper.from(getLazyLoadingEnabled()).to(target::setLazyLoadingEnabled);
  692 |       mapper.from(getLogPrefix()).to(target::setLogPrefix);
  693 |       mapper.from(getLazyLoadTriggerMethods()).to(target::setLazyLoadTriggerMethods);
  694 |       mapper.from(getDefaultStatementTimeout()).to(target::setDefaultStatementTimeout);
  695 |       mapper.from(getDefaultFetchSize()).to(target::setDefaultFetchSize);
  696 |       mapper.from(getLocalCacheScope()).to(target::setLocalCacheScope);
  697 |       mapper.from(getJdbcTypeForNull()).to(target::setJdbcTypeForNull);
  698 |       mapper.from(getDefaultResultSetType()).to(target::setDefaultResultSetType);
  699 |       mapper.from(getDefaultExecutorType()).to(target::setDefaultExecutorType);
  700 |       mapper.from(getAutoMappingBehavior()).to(target::setAutoMappingBehavior);
  701 |       mapper.from(getAutoMappingUnknownColumnBehavior()).to(target::setAutoMappingUnknownColumnBehavior);
  702 |       mapper.from(getVariables()).to(target::setVariables);
  703 |       mapper.from(getLogImpl()).to(target::setLogImpl);
  704 |       mapper.from(getVfsImpl()).to(target::setVfsImpl);
  705 |       mapper.from(getDefaultSqlProviderType()).to(target::setDefaultSqlProviderType);
  706 |       mapper.from(getConfigurationFactory()).to(target::setConfigurationFactory);
  707 |       mapper.from(getDefaultEnumTypeHandler()).to(target::setDefaultEnumTypeHandler);
  708 |       mapper.from(getDatabaseId()).to(target::setDatabaseId);
  709 |     }
  710 | 
  711 |   }
  712 | 
  713 | }
```
