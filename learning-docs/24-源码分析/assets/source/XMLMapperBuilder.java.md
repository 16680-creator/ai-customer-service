<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/builder/xml/XMLMapperBuilder.java -->
<!-- 正文引用：[[源码:XMLMapperBuilder.java#L起-L止]] ；代码块头部：(src: XMLMapperBuilder.java#L起-L止) -->

# XMLMapperBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/builder/xml/XMLMapperBuilder.java` |
| 行数 | 420 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:00:25.629Z |

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
   16 | package org.apache.ibatis.builder.xml;
   17 | 
   18 | import java.io.InputStream;
   19 | import java.io.Reader;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.HashMap;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | import java.util.Properties;
   26 | 
   27 | import org.apache.ibatis.builder.BaseBuilder;
   28 | import org.apache.ibatis.builder.BuilderException;
   29 | import org.apache.ibatis.builder.CacheRefResolver;
   30 | import org.apache.ibatis.builder.IncompleteElementException;
   31 | import org.apache.ibatis.builder.MapperBuilderAssistant;
   32 | import org.apache.ibatis.builder.ResultMapResolver;
   33 | import org.apache.ibatis.builder.ResultMappingConstructorResolver;
   34 | import org.apache.ibatis.cache.Cache;
   35 | import org.apache.ibatis.executor.ErrorContext;
   36 | import org.apache.ibatis.io.Resources;
   37 | import org.apache.ibatis.mapping.Discriminator;
   38 | import org.apache.ibatis.mapping.ParameterMapping;
   39 | import org.apache.ibatis.mapping.ParameterMode;
   40 | import org.apache.ibatis.mapping.ResultFlag;
   41 | import org.apache.ibatis.mapping.ResultMap;
   42 | import org.apache.ibatis.mapping.ResultMapping;
   43 | import org.apache.ibatis.parsing.XNode;
   44 | import org.apache.ibatis.parsing.XPathParser;
   45 | import org.apache.ibatis.reflection.MetaClass;
   46 | import org.apache.ibatis.session.Configuration;
   47 | import org.apache.ibatis.type.JdbcType;
   48 | import org.apache.ibatis.type.TypeHandler;
   49 | 
   50 | /**
   51 |  * @author Clinton Begin
   52 |  * @author Kazuki Shimizu
   53 |  */
   54 | public class XMLMapperBuilder extends BaseBuilder {
   55 | 
   56 |   private final XPathParser parser;
   57 |   private final MapperBuilderAssistant builderAssistant;
   58 |   private final Map<String, XNode> sqlFragments;
   59 |   private final String resource;
   60 |   private Class<?> mapperClass;
   61 | 
   62 |   @Deprecated(since = "3.6.0", forRemoval = true)
   63 |   public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments,
   64 |       String namespace) {
   65 |     this(reader, configuration, resource, sqlFragments);
   66 |     this.builderAssistant.setCurrentNamespace(namespace);
   67 |   }
   68 | 
   69 |   @Deprecated
   70 |   public XMLMapperBuilder(Reader reader, Configuration configuration, String resource,
   71 |       Map<String, XNode> sqlFragments) {
   72 |     this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()), configuration,
   73 |         resource, sqlFragments);
   74 |   }
   75 | 
   76 |   public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
   77 |       Map<String, XNode> sqlFragments, Class<?> mapperClass) {
   78 |     this(inputStream, configuration, resource, sqlFragments, mapperClass.getName());
   79 |     this.mapperClass = mapperClass;
   80 |   }
   81 | 
   82 |   public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
   83 |       Map<String, XNode> sqlFragments, String namespace) {
   84 |     this(inputStream, configuration, resource, sqlFragments);
   85 |     this.builderAssistant.setCurrentNamespace(namespace);
   86 |   }
   87 | 
   88 |   public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
   89 |       Map<String, XNode> sqlFragments) {
   90 |     this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()), configuration,
   91 |         resource, sqlFragments);
   92 |   }
   93 | 
   94 |   private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource,
   95 |       Map<String, XNode> sqlFragments) {
   96 |     super(configuration);
   97 |     this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
   98 |     this.parser = parser;
   99 |     this.sqlFragments = sqlFragments;
  100 |     this.resource = resource;
  101 |   }
  102 | 
  103 |   public void parse() {
  104 |     if (!configuration.isResourceLoaded(resource)) {
  105 |       configurationElement(parser.evalNode("/mapper"));
  106 |       configuration.addLoadedResource(resource);
  107 |       bindMapperForNamespace();
  108 |     }
  109 |     configuration.parsePendingResultMaps(false);
  110 |     configuration.parsePendingCacheRefs(false);
  111 |     configuration.parsePendingStatements(false);
  112 |   }
  113 | 
  114 |   public XNode getSqlFragment(String refid) {
  115 |     return sqlFragments.get(refid);
  116 |   }
  117 | 
  118 |   private void configurationElement(XNode context) {
  119 |     try {
  120 |       String namespace = context.getStringAttribute("namespace");
  121 |       if (namespace == null || namespace.isEmpty()) {
  122 |         throw new BuilderException("Mapper's namespace cannot be empty");
  123 |       }
  124 |       builderAssistant.setCurrentNamespace(namespace);
  125 |       cacheRefElement(context.evalNode("cache-ref"));
  126 |       cacheElement(context.evalNode("cache"));
  127 |       parameterMapElement(context.evalNodes("/mapper/parameterMap"));
  128 |       resultMapElements(context.evalNodes("/mapper/resultMap"));
  129 |       sqlElement(context.evalNodes("/mapper/sql"));
  130 |       buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
  131 |     } catch (Exception e) {
  132 |       throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
  133 |     }
  134 |   }
  135 | 
  136 |   private void buildStatementFromContext(List<XNode> list) {
  137 |     if (configuration.getDatabaseId() != null) {
  138 |       buildStatementFromContext(list, configuration.getDatabaseId());
  139 |     }
  140 |     buildStatementFromContext(list, null);
  141 |   }
  142 | 
  143 |   private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
  144 |     for (XNode context : list) {
  145 |       final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context,
  146 |           requiredDatabaseId, mapperClass);
  147 |       try {
  148 |         statementParser.parseStatementNode();
  149 |       } catch (IncompleteElementException e) {
  150 |         configuration.addIncompleteStatement(statementParser);
  151 |       }
  152 |     }
  153 |   }
  154 | 
  155 |   private void cacheRefElement(XNode context) {
  156 |     if (context != null) {
  157 |       configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
  158 |       CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant,
  159 |           context.getStringAttribute("namespace"));
  160 |       try {
  161 |         cacheRefResolver.resolveCacheRef();
  162 |       } catch (IncompleteElementException e) {
  163 |         configuration.addIncompleteCacheRef(cacheRefResolver);
  164 |       }
  165 |     }
  166 |   }
  167 | 
  168 |   private void cacheElement(XNode context) {
  169 |     if (context != null) {
  170 |       String type = context.getStringAttribute("type", "PERPETUAL");
  171 |       Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
  172 |       String eviction = context.getStringAttribute("eviction", "LRU");
  173 |       Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
  174 |       Long flushInterval = context.getLongAttribute("flushInterval");
  175 |       Integer size = context.getIntAttribute("size");
  176 |       boolean readWrite = !context.getBooleanAttribute("readOnly", false);
  177 |       boolean blocking = context.getBooleanAttribute("blocking", false);
  178 |       Properties props = context.getChildrenAsProperties();
  179 |       builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
  180 |     }
  181 |   }
  182 | 
  183 |   private void parameterMapElement(List<XNode> list) {
  184 |     for (XNode parameterMapNode : list) {
  185 |       String id = parameterMapNode.getStringAttribute("id");
  186 |       String type = parameterMapNode.getStringAttribute("type");
  187 |       Class<?> parameterClass = resolveClass(type);
  188 |       List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
  189 |       List<ParameterMapping> parameterMappings = new ArrayList<>();
  190 |       for (XNode parameterNode : parameterNodes) {
  191 |         String property = parameterNode.getStringAttribute("property");
  192 |         String javaType = parameterNode.getStringAttribute("javaType");
  193 |         String jdbcType = parameterNode.getStringAttribute("jdbcType");
  194 |         String resultMap = parameterNode.getStringAttribute("resultMap");
  195 |         String mode = parameterNode.getStringAttribute("mode");
  196 |         String typeHandler = parameterNode.getStringAttribute("typeHandler");
  197 |         Integer numericScale = parameterNode.getIntAttribute("numericScale");
  198 |         ParameterMode modeEnum = resolveParameterMode(mode);
  199 |         Class<?> javaTypeClass = resolveClass(javaType);
  200 |         JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
  201 |         Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
  202 |         ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property,
  203 |             javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
  204 |         parameterMappings.add(parameterMapping);
  205 |       }
  206 |       builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
  207 |     }
  208 |   }
  209 | 
  210 |   private void resultMapElements(List<XNode> list) {
  211 |     for (XNode resultMapNode : list) {
  212 |       try {
  213 |         resultMapElement(resultMapNode);
  214 |       } catch (IncompleteElementException e) {
  215 |         // ignore, it will be retried
  216 |       }
  217 |     }
  218 |   }
  219 | 
  220 |   private ResultMap resultMapElement(XNode resultMapNode) {
  221 |     return resultMapElement(resultMapNode, List.of(), null);
  222 |   }
  223 | 
  224 |   private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings,
  225 |       Class<?> enclosingType) {
  226 |     ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
  227 |     String type = resultMapNode.getStringAttribute("type", resultMapNode.getStringAttribute("ofType",
  228 |         resultMapNode.getStringAttribute("resultType", resultMapNode.getStringAttribute("javaType"))));
  229 |     Class<?> typeClass = resolveClass(type);
  230 |     if (typeClass == null) {
  231 |       typeClass = inheritEnclosingType(resultMapNode, enclosingType);
  232 |     }
  233 | 
  234 |     String id = resultMapNode.getStringAttribute("id", resultMapNode::getValueBasedIdentifier);
  235 |     String extend = resultMapNode.getStringAttribute("extends");
  236 |     Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
  237 | 
  238 |     Discriminator discriminator = null;
  239 |     List<ResultMapping> resultMappings = new ArrayList<>(additionalResultMappings);
  240 |     List<XNode> resultChildren = resultMapNode.getChildren();
  241 |     for (XNode resultChild : resultChildren) {
  242 |       if ("constructor".equals(resultChild.getName())) {
  243 |         processConstructorElement(resultChild, typeClass, resultMappings, id);
  244 |       } else if ("discriminator".equals(resultChild.getName())) {
  245 |         discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
  246 |       } else {
  247 |         List<ResultFlag> flags = new ArrayList<>();
  248 |         if ("id".equals(resultChild.getName())) {
  249 |           flags.add(ResultFlag.ID);
  250 |         }
  251 |         resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
  252 |       }
  253 |     }
  254 | 
  255 |     ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator,
  256 |         resultMappings, autoMapping);
  257 |     try {
  258 |       return resultMapResolver.resolve();
  259 |     } catch (IncompleteElementException e) {
  260 |       configuration.addIncompleteResultMap(resultMapResolver);
  261 |       throw e;
  262 |     }
  263 |   }
  264 | 
  265 |   protected Class<?> inheritEnclosingType(XNode resultMapNode, Class<?> enclosingType) {
  266 |     if ("association".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
  267 |       String property = resultMapNode.getStringAttribute("property");
  268 |       if (property != null && enclosingType != null) {
  269 |         MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
  270 |         return metaResultType.getSetterType(property);
  271 |       }
  272 |     } else if ("case".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
  273 |       return enclosingType;
  274 |     }
  275 |     return null;
  276 |   }
  277 | 
  278 |   private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings,
  279 |       String id) {
  280 |     List<XNode> argChildren = resultChild.getChildren();
  281 | 
  282 |     final List<ResultMapping> mappings = new ArrayList<>();
  283 |     for (XNode argChild : argChildren) {
  284 |       List<ResultFlag> flags = new ArrayList<>();
  285 |       flags.add(ResultFlag.CONSTRUCTOR);
  286 |       if ("idArg".equals(argChild.getName())) {
  287 |         flags.add(ResultFlag.ID);
  288 |       }
  289 | 
  290 |       mappings.add(buildResultMappingFromContext(argChild, resultType, flags));
  291 |     }
  292 | 
  293 |     final ResultMappingConstructorResolver resolver = new ResultMappingConstructorResolver(configuration, mappings,
  294 |         resultType, id);
  295 |     resultMappings.addAll(resolver.resolveWithConstructor());
  296 |   }
  297 | 
  298 |   private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType,
  299 |       List<ResultMapping> resultMappings) {
  300 |     String column = context.getStringAttribute("column");
  301 |     String javaType = context.getStringAttribute("javaType");
  302 |     String jdbcType = context.getStringAttribute("jdbcType");
  303 |     String typeHandler = context.getStringAttribute("typeHandler");
  304 |     Class<?> javaTypeClass = resolveClass(javaType);
  305 |     Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
  306 |     JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
  307 |     Map<String, String> discriminatorMap = new HashMap<>();
  308 |     for (XNode caseChild : context.getChildren()) {
  309 |       String value = caseChild.getStringAttribute("value");
  310 |       String resultMap = caseChild.getStringAttribute("resultMap",
  311 |           () -> processNestedResultMappings(caseChild, resultMappings, resultType));
  312 |       discriminatorMap.put(value, resultMap);
  313 |     }
  314 |     return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass,
  315 |         discriminatorMap);
  316 |   }
  317 | 
  318 |   private void sqlElement(List<XNode> list) {
  319 |     if (configuration.getDatabaseId() != null) {
  320 |       sqlElement(list, configuration.getDatabaseId());
  321 |     }
  322 |     sqlElement(list, null);
  323 |   }
  324 | 
  325 |   private void sqlElement(List<XNode> list, String requiredDatabaseId) {
  326 |     for (XNode context : list) {
  327 |       String databaseId = context.getStringAttribute("databaseId");
  328 |       String id = context.getStringAttribute("id");
  329 |       id = builderAssistant.applyCurrentNamespace(id, false);
  330 |       if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
  331 |         sqlFragments.put(id, context);
  332 |       }
  333 |     }
  334 |   }
  335 | 
  336 |   private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
  337 |     if (requiredDatabaseId != null) {
  338 |       return requiredDatabaseId.equals(databaseId);
  339 |     }
  340 |     if (databaseId != null) {
  341 |       return false;
  342 |     }
  343 |     if (!this.sqlFragments.containsKey(id)) {
  344 |       return true;
  345 |     }
  346 |     // skip this fragment if there is a previous one with a not null databaseId
  347 |     XNode context = this.sqlFragments.get(id);
  348 |     return context.getStringAttribute("databaseId") == null;
  349 |   }
  350 | 
  351 |   private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) {
  352 |     String property;
  353 |     if (flags.contains(ResultFlag.CONSTRUCTOR)) {
  354 |       property = context.getStringAttribute("name");
  355 |     } else {
  356 |       property = context.getStringAttribute("property");
  357 |     }
  358 |     String column = context.getStringAttribute("column");
  359 |     String javaType = context.getStringAttribute("javaType");
  360 |     String jdbcType = context.getStringAttribute("jdbcType");
  361 |     String nestedSelect = context.getStringAttribute("select");
  362 |     String nestedResultMap = context.getStringAttribute("resultMap",
  363 |         () -> processNestedResultMappings(context, List.of(), resultType));
  364 |     String notNullColumn = context.getStringAttribute("notNullColumn");
  365 |     String columnPrefix = context.getStringAttribute("columnPrefix");
  366 |     String typeHandler = context.getStringAttribute("typeHandler");
  367 |     String resultSet = context.getStringAttribute("resultSet");
  368 |     String foreignColumn = context.getStringAttribute("foreignColumn");
  369 |     boolean lazy = "lazy"
  370 |         .equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
  371 |     Class<?> javaTypeClass = resolveClass(javaType);
  372 |     Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
  373 |     JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
  374 |     return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect,
  375 |         nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
  376 |   }
  377 | 
  378 |   private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings,
  379 |       Class<?> enclosingType) {
  380 |     if (Arrays.asList("association", "collection", "case").contains(context.getName())
  381 |         && context.getStringAttribute("select") == null) {
  382 |       validateCollection(context, enclosingType);
  383 |       ResultMap resultMap = resultMapElement(context, resultMappings, enclosingType);
  384 |       return resultMap.getId();
  385 |     }
  386 |     return null;
  387 |   }
  388 | 
  389 |   protected void validateCollection(XNode context, Class<?> enclosingType) {
  390 |     if ("collection".equals(context.getName()) && context.getStringAttribute("resultMap") == null
  391 |         && context.getStringAttribute("javaType") == null) {
  392 |       MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
  393 |       String property = context.getStringAttribute("property");
  394 |       if (!metaResultType.hasSetter(property)) {
  395 |         throw new BuilderException(
  396 |             "Ambiguous collection type for property '" + property + "'. You must specify 'javaType' or 'resultMap'.");
  397 |       }
  398 |     }
  399 |   }
  400 | 
  401 |   private void bindMapperForNamespace() {
  402 |     String namespace = builderAssistant.getCurrentNamespace();
  403 |     if (namespace != null) {
  404 |       Class<?> boundType = null;
  405 |       try {
  406 |         boundType = Resources.classForName(namespace);
  407 |       } catch (ClassNotFoundException e) {
  408 |         // ignore, bound type is not required
  409 |       }
  410 |       if (boundType != null && !configuration.hasMapper(boundType)) {
  411 |         // Spring may not know the real resource name so we set a flag
  412 |         // to prevent loading again this resource from the mapper interface
  413 |         // look at MapperAnnotationBuilder#loadXmlResource
  414 |         configuration.addLoadedResource("namespace:" + namespace);
  415 |         configuration.addMapper(boundType);
  416 |       }
  417 |     }
  418 |   }
  419 | 
  420 | }
```
