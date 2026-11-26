<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/builder/xml/XMLConfigBuilder.java -->
<!-- 正文引用：[[源码:XMLConfigBuilder.java#L起-L止]] ；代码块头部：(src: XMLConfigBuilder.java#L起-L止) -->

# XMLConfigBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/builder/xml/XMLConfigBuilder.java` |
| 行数 | 443 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:18.646Z |

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
   16 | package org.apache.ibatis.builder.xml;
   17 | 
   18 | import java.io.InputStream;
   19 | import java.io.Reader;
   20 | import java.util.Properties;
   21 | 
   22 | import javax.sql.DataSource;
   23 | 
   24 | import org.apache.ibatis.builder.BaseBuilder;
   25 | import org.apache.ibatis.builder.BuilderException;
   26 | import org.apache.ibatis.datasource.DataSourceFactory;
   27 | import org.apache.ibatis.executor.ErrorContext;
   28 | import org.apache.ibatis.executor.loader.ProxyFactory;
   29 | import org.apache.ibatis.io.Resources;
   30 | import org.apache.ibatis.io.VFS;
   31 | import org.apache.ibatis.logging.Log;
   32 | import org.apache.ibatis.mapping.DatabaseIdProvider;
   33 | import org.apache.ibatis.mapping.Environment;
   34 | import org.apache.ibatis.parsing.XNode;
   35 | import org.apache.ibatis.parsing.XPathParser;
   36 | import org.apache.ibatis.plugin.Interceptor;
   37 | import org.apache.ibatis.reflection.DefaultReflectorFactory;
   38 | import org.apache.ibatis.reflection.MetaClass;
   39 | import org.apache.ibatis.reflection.ReflectorFactory;
   40 | import org.apache.ibatis.reflection.factory.ObjectFactory;
   41 | import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
   42 | import org.apache.ibatis.session.AutoMappingBehavior;
   43 | import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
   44 | import org.apache.ibatis.session.Configuration;
   45 | import org.apache.ibatis.session.ExecutorType;
   46 | import org.apache.ibatis.session.LocalCacheScope;
   47 | import org.apache.ibatis.transaction.TransactionFactory;
   48 | import org.apache.ibatis.type.JdbcType;
   49 | 
   50 | /**
   51 |  * @author Clinton Begin
   52 |  * @author Kazuki Shimizu
   53 |  */
   54 | public class XMLConfigBuilder extends BaseBuilder {
   55 | 
   56 |   private boolean parsed;
   57 |   private final XPathParser parser;
   58 |   private String environment;
   59 |   private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();
   60 | 
   61 |   public XMLConfigBuilder(Reader reader) {
   62 |     this(reader, null, null);
   63 |   }
   64 | 
   65 |   public XMLConfigBuilder(Reader reader, String environment) {
   66 |     this(reader, environment, null);
   67 |   }
   68 | 
   69 |   public XMLConfigBuilder(Reader reader, String environment, Properties props) {
   70 |     this(Configuration.class, reader, environment, props);
   71 |   }
   72 | 
   73 |   public XMLConfigBuilder(Class<? extends Configuration> configClass, Reader reader, String environment,
   74 |       Properties props) {
   75 |     this(configClass, new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
   76 |   }
   77 | 
   78 |   public XMLConfigBuilder(InputStream inputStream) {
   79 |     this(inputStream, null, null);
   80 |   }
   81 | 
   82 |   public XMLConfigBuilder(InputStream inputStream, String environment) {
   83 |     this(inputStream, environment, null);
   84 |   }
   85 | 
   86 |   public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
   87 |     this(Configuration.class, inputStream, environment, props);
   88 |   }
   89 | 
   90 |   public XMLConfigBuilder(Class<? extends Configuration> configClass, InputStream inputStream, String environment,
   91 |       Properties props) {
   92 |     this(configClass, new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
   93 |   }
   94 | 
   95 |   private XMLConfigBuilder(Class<? extends Configuration> configClass, XPathParser parser, String environment,
   96 |       Properties props) {
   97 |     super(newConfig(configClass));
   98 |     ErrorContext.instance().resource("SQL Mapper Configuration");
   99 |     this.configuration.setVariables(props);
  100 |     this.parsed = false;
  101 |     this.environment = environment;
  102 |     this.parser = parser;
  103 |   }
  104 | 
  105 |   public Configuration parse() {
  106 |     if (parsed) {
  107 |       throw new BuilderException("Each XMLConfigBuilder can only be used once.");
  108 |     }
  109 |     parsed = true;
  110 |     parseConfiguration(parser.evalNode("/configuration"));
  111 |     return configuration;
  112 |   }
  113 | 
  114 |   private void parseConfiguration(XNode root) {
  115 |     try {
  116 |       // issue #117 read properties first
  117 |       propertiesElement(root.evalNode("properties"));
  118 |       Properties settings = settingsAsProperties(root.evalNode("settings"));
  119 |       loadCustomVfsImpl(settings);
  120 |       loadCustomLogImpl(settings);
  121 |       typeAliasesElement(root.evalNode("typeAliases"));
  122 |       pluginsElement(root.evalNode("plugins"));
  123 |       objectFactoryElement(root.evalNode("objectFactory"));
  124 |       objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
  125 |       reflectorFactoryElement(root.evalNode("reflectorFactory"));
  126 |       settingsElement(settings);
  127 |       // read it after objectFactory and objectWrapperFactory issue #631
  128 |       environmentsElement(root.evalNode("environments"));
  129 |       databaseIdProviderElement(root.evalNode("databaseIdProvider"));
  130 |       typeHandlersElement(root.evalNode("typeHandlers"));
  131 |       mappersElement(root.evalNode("mappers"));
  132 |     } catch (Exception e) {
  133 |       throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
  134 |     }
  135 |   }
  136 | 
  137 |   private Properties settingsAsProperties(XNode context) {
  138 |     if (context == null) {
  139 |       return new Properties();
  140 |     }
  141 |     Properties props = context.getChildrenAsProperties();
  142 |     // Check that all settings are known to the configuration class
  143 |     MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
  144 |     for (Object key : props.keySet()) {
  145 |       if (!metaConfig.hasSetter(String.valueOf(key))) {
  146 |         throw new BuilderException(
  147 |             "The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
  148 |       }
  149 |     }
  150 |     return props;
  151 |   }
  152 | 
  153 |   private void loadCustomVfsImpl(Properties props) throws ClassNotFoundException {
  154 |     String value = props.getProperty("vfsImpl");
  155 |     if (value == null) {
  156 |       return;
  157 |     }
  158 |     String[] clazzes = value.split(",");
  159 |     for (String clazz : clazzes) {
  160 |       if (!clazz.isEmpty()) {
  161 |         @SuppressWarnings("unchecked")
  162 |         Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources.classForName(clazz);
  163 |         configuration.setVfsImpl(vfsImpl);
  164 |       }
  165 |     }
  166 |   }
  167 | 
  168 |   private void loadCustomLogImpl(Properties props) {
  169 |     Class<? extends Log> logImpl = resolveClass(props.getProperty("logImpl"));
  170 |     configuration.setLogImpl(logImpl);
  171 |   }
  172 | 
  173 |   private void typeAliasesElement(XNode context) {
  174 |     if (context == null) {
  175 |       return;
  176 |     }
  177 |     for (XNode child : context.getChildren()) {
  178 |       if ("package".equals(child.getName())) {
  179 |         String typeAliasPackage = child.getStringAttribute("name");
  180 |         configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
  181 |       } else {
  182 |         String alias = child.getStringAttribute("alias");
  183 |         String type = child.getStringAttribute("type");
  184 |         try {
  185 |           Class<?> clazz = Resources.classForName(type);
  186 |           if (alias == null) {
  187 |             typeAliasRegistry.registerAlias(clazz);
  188 |           } else {
  189 |             typeAliasRegistry.registerAlias(alias, clazz);
  190 |           }
  191 |         } catch (ClassNotFoundException e) {
  192 |           throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
  193 |         }
  194 |       }
  195 |     }
  196 |   }
  197 | 
  198 |   private void pluginsElement(XNode context) throws Exception {
  199 |     if (context != null) {
  200 |       for (XNode child : context.getChildren()) {
  201 |         String interceptor = child.getStringAttribute("interceptor");
  202 |         Properties properties = child.getChildrenAsProperties();
  203 |         Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).getDeclaredConstructor()
  204 |             .newInstance();
  205 |         interceptorInstance.setProperties(properties);
  206 |         configuration.addInterceptor(interceptorInstance);
  207 |       }
  208 |     }
  209 |   }
  210 | 
  211 |   private void objectFactoryElement(XNode context) throws Exception {
  212 |     if (context != null) {
  213 |       String type = context.getStringAttribute("type");
  214 |       Properties properties = context.getChildrenAsProperties();
  215 |       ObjectFactory factory = (ObjectFactory) resolveClass(type).getDeclaredConstructor().newInstance();
  216 |       factory.setProperties(properties);
  217 |       configuration.setObjectFactory(factory);
  218 |     }
  219 |   }
  220 | 
  221 |   private void objectWrapperFactoryElement(XNode context) throws Exception {
  222 |     if (context != null) {
  223 |       String type = context.getStringAttribute("type");
  224 |       ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).getDeclaredConstructor().newInstance();
  225 |       configuration.setObjectWrapperFactory(factory);
  226 |     }
  227 |   }
  228 | 
  229 |   private void reflectorFactoryElement(XNode context) throws Exception {
  230 |     if (context != null) {
  231 |       String type = context.getStringAttribute("type");
  232 |       ReflectorFactory factory = (ReflectorFactory) resolveClass(type).getDeclaredConstructor().newInstance();
  233 |       configuration.setReflectorFactory(factory);
  234 |     }
  235 |   }
  236 | 
  237 |   private void propertiesElement(XNode context) throws Exception {
  238 |     if (context == null) {
  239 |       return;
  240 |     }
  241 |     Properties defaults = context.getChildrenAsProperties();
  242 |     String resource = context.getStringAttribute("resource");
  243 |     String url = context.getStringAttribute("url");
  244 |     if (resource != null && url != null) {
  245 |       throw new BuilderException(
  246 |           "The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
  247 |     }
  248 |     if (resource != null) {
  249 |       defaults.putAll(Resources.getResourceAsProperties(resource));
  250 |     } else if (url != null) {
  251 |       defaults.putAll(Resources.getUrlAsProperties(url));
  252 |     }
  253 |     Properties vars = configuration.getVariables();
  254 |     if (vars != null) {
  255 |       defaults.putAll(vars);
  256 |     }
  257 |     parser.setVariables(defaults);
  258 |     configuration.setVariables(defaults);
  259 |   }
  260 | 
  261 |   private void settingsElement(Properties props) {
  262 |     configuration
  263 |         .setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
  264 |     configuration.setAutoMappingUnknownColumnBehavior(
  265 |         AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
  266 |     configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
  267 |     configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
  268 |     configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
  269 |     configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
  270 |     configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
  271 |     configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
  272 |     configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
  273 |     configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
  274 |     configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
  275 |     configuration.setDefaultResultSetType(resolveResultSetType(props.getProperty("defaultResultSetType")));
  276 |     configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
  277 |     configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
  278 |     configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
  279 |     configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
  280 |     configuration.setLazyLoadTriggerMethods(
  281 |         stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
  282 |     configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
  283 |     configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
  284 |     configuration.setDefaultEnumTypeHandler(resolveClass(props.getProperty("defaultEnumTypeHandler")));
  285 |     configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
  286 |     configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
  287 |     configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
  288 |     configuration.setLogPrefix(props.getProperty("logPrefix"));
  289 |     configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
  290 |     configuration.setShrinkWhitespacesInSql(booleanValueOf(props.getProperty("shrinkWhitespacesInSql"), false));
  291 |     configuration.setArgNameBasedConstructorAutoMapping(
  292 |         booleanValueOf(props.getProperty("argNameBasedConstructorAutoMapping"), false));
  293 |     configuration.setDefaultSqlProviderType(resolveClass(props.getProperty("defaultSqlProviderType")));
  294 |     configuration.setNullableOnForEach(booleanValueOf(props.getProperty("nullableOnForEach"), false));
  295 |   }
  296 | 
  297 |   private void environmentsElement(XNode context) throws Exception {
  298 |     if (context == null) {
  299 |       return;
  300 |     }
  301 |     if (environment == null) {
  302 |       environment = context.getStringAttribute("default");
  303 |     }
  304 |     for (XNode child : context.getChildren()) {
  305 |       String id = child.getStringAttribute("id");
  306 |       if (isSpecifiedEnvironment(id)) {
  307 |         TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
  308 |         DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
  309 |         DataSource dataSource = dsFactory.getDataSource();
  310 |         Environment.Builder environmentBuilder = new Environment.Builder(id).transactionFactory(txFactory)
  311 |             .dataSource(dataSource);
  312 |         configuration.setEnvironment(environmentBuilder.build());
  313 |         break;
  314 |       }
  315 |     }
  316 |   }
  317 | 
  318 |   private void databaseIdProviderElement(XNode context) throws Exception {
  319 |     if (context == null) {
  320 |       return;
  321 |     }
  322 |     String type = context.getStringAttribute("type");
  323 |     // awful patch to keep backward compatibility
  324 |     if ("VENDOR".equals(type)) {
  325 |       type = "DB_VENDOR";
  326 |     }
  327 |     Properties properties = context.getChildrenAsProperties();
  328 |     DatabaseIdProvider databaseIdProvider = (DatabaseIdProvider) resolveClass(type).getDeclaredConstructor()
  329 |         .newInstance();
  330 |     databaseIdProvider.setProperties(properties);
  331 |     Environment environment = configuration.getEnvironment();
  332 |     if (environment != null) {
  333 |       String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
  334 |       configuration.setDatabaseId(databaseId);
  335 |     }
  336 |   }
  337 | 
  338 |   private TransactionFactory transactionManagerElement(XNode context) throws Exception {
  339 |     if (context != null) {
  340 |       String type = context.getStringAttribute("type");
  341 |       Properties props = context.getChildrenAsProperties();
  342 |       TransactionFactory factory = (TransactionFactory) resolveClass(type).getDeclaredConstructor().newInstance();
  343 |       factory.setProperties(props);
  344 |       return factory;
  345 |     }
  346 |     throw new BuilderException("Environment declaration requires a TransactionFactory.");
  347 |   }
  348 | 
  349 |   private DataSourceFactory dataSourceElement(XNode context) throws Exception {
  350 |     if (context != null) {
  351 |       String type = context.getStringAttribute("type");
  352 |       Properties props = context.getChildrenAsProperties();
  353 |       DataSourceFactory factory = (DataSourceFactory) resolveClass(type).getDeclaredConstructor().newInstance();
  354 |       factory.setProperties(props);
  355 |       return factory;
  356 |     }
  357 |     throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  358 |   }
  359 | 
  360 |   private void typeHandlersElement(XNode context) {
  361 |     if (context == null) {
  362 |       return;
  363 |     }
  364 |     for (XNode child : context.getChildren()) {
  365 |       if ("package".equals(child.getName())) {
  366 |         String typeHandlerPackage = child.getStringAttribute("name");
  367 |         typeHandlerRegistry.register(typeHandlerPackage);
  368 |       } else {
  369 |         String javaTypeName = child.getStringAttribute("javaType");
  370 |         String jdbcTypeName = child.getStringAttribute("jdbcType");
  371 |         String handlerTypeName = child.getStringAttribute("handler");
  372 |         Class<?> javaTypeClass = resolveClass(javaTypeName);
  373 |         JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
  374 |         Class<?> typeHandlerClass = resolveClass(handlerTypeName);
  375 |         if (javaTypeClass != null) {
  376 |           if (jdbcType == null) {
  377 |             typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
  378 |           } else {
  379 |             typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
  380 |           }
  381 |         } else {
  382 |           typeHandlerRegistry.register(typeHandlerClass);
  383 |         }
  384 |       }
  385 |     }
  386 |   }
  387 | 
  388 |   private void mappersElement(XNode context) throws Exception {
  389 |     if (context == null) {
  390 |       return;
  391 |     }
  392 |     for (XNode child : context.getChildren()) {
  393 |       if ("package".equals(child.getName())) {
  394 |         String mapperPackage = child.getStringAttribute("name");
  395 |         configuration.addMappers(mapperPackage);
  396 |       } else {
  397 |         String resource = child.getStringAttribute("resource");
  398 |         String url = child.getStringAttribute("url");
  399 |         String mapperClass = child.getStringAttribute("class");
  400 |         if (resource != null && url == null && mapperClass == null) {
  401 |           ErrorContext.instance().resource(resource);
  402 |           try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
  403 |             XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource,
  404 |                 configuration.getSqlFragments());
  405 |             mapperParser.parse();
  406 |           }
  407 |         } else if (resource == null && url != null && mapperClass == null) {
  408 |           ErrorContext.instance().resource(url);
  409 |           try (InputStream inputStream = Resources.getUrlAsStream(url)) {
  410 |             XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url,
  411 |                 configuration.getSqlFragments());
  412 |             mapperParser.parse();
  413 |           }
  414 |         } else if (resource == null && url == null && mapperClass != null) {
  415 |           Class<?> mapperInterface = Resources.classForName(mapperClass);
  416 |           configuration.addMapper(mapperInterface);
  417 |         } else {
  418 |           throw new BuilderException(
  419 |               "A mapper element may only specify a url, resource or class, but not more than one.");
  420 |         }
  421 |       }
  422 |     }
  423 |   }
  424 | 
  425 |   private boolean isSpecifiedEnvironment(String id) {
  426 |     if (environment == null) {
  427 |       throw new BuilderException("No environment specified.");
  428 |     }
  429 |     if (id == null) {
  430 |       throw new BuilderException("Environment requires an id attribute.");
  431 |     }
  432 |     return environment.equals(id);
  433 |   }
  434 | 
  435 |   private static Configuration newConfig(Class<? extends Configuration> configClass) {
  436 |     try {
  437 |       return configClass.getDeclaredConstructor().newInstance();
  438 |     } catch (Exception ex) {
  439 |       throw new BuilderException("Failed to create a new Configuration instance.", ex);
  440 |     }
  441 |   }
  442 | 
  443 | }
```
