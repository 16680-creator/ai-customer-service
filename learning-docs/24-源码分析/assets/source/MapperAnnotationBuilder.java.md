<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/builder/annotation/MapperAnnotationBuilder.java -->
<!-- 正文引用：[[源码:MapperAnnotationBuilder.java#L起-L止]] ；代码块头部：(src: MapperAnnotationBuilder.java#L起-L止) -->

# MapperAnnotationBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/builder/annotation/MapperAnnotationBuilder.java` |
| 行数 | 773 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:09:17.910Z |

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
   16 | package org.apache.ibatis.builder.annotation;
   17 | 
   18 | import java.io.IOException;
   19 | import java.io.InputStream;
   20 | import java.lang.annotation.Annotation;
   21 | import java.lang.reflect.Array;
   22 | import java.lang.reflect.GenericArrayType;
   23 | import java.lang.reflect.Method;
   24 | import java.lang.reflect.Parameter;
   25 | import java.lang.reflect.ParameterizedType;
   26 | import java.lang.reflect.Type;
   27 | import java.util.ArrayList;
   28 | import java.util.Arrays;
   29 | import java.util.Collection;
   30 | import java.util.HashMap;
   31 | import java.util.List;
   32 | import java.util.Map;
   33 | import java.util.Optional;
   34 | import java.util.Properties;
   35 | import java.util.Set;
   36 | import java.util.stream.Collectors;
   37 | import java.util.stream.Stream;
   38 | 
   39 | import org.apache.ibatis.annotations.AnnotationConstants;
   40 | import org.apache.ibatis.annotations.Arg;
   41 | import org.apache.ibatis.annotations.CacheNamespace;
   42 | import org.apache.ibatis.annotations.CacheNamespaceRef;
   43 | import org.apache.ibatis.annotations.Case;
   44 | import org.apache.ibatis.annotations.Delete;
   45 | import org.apache.ibatis.annotations.DeleteProvider;
   46 | import org.apache.ibatis.annotations.Insert;
   47 | import org.apache.ibatis.annotations.InsertProvider;
   48 | import org.apache.ibatis.annotations.Lang;
   49 | import org.apache.ibatis.annotations.MapKey;
   50 | import org.apache.ibatis.annotations.NamedResultMap;
   51 | import org.apache.ibatis.annotations.NamedResultMaps;
   52 | import org.apache.ibatis.annotations.Options;
   53 | import org.apache.ibatis.annotations.Options.FlushCachePolicy;
   54 | import org.apache.ibatis.annotations.Param;
   55 | import org.apache.ibatis.annotations.Property;
   56 | import org.apache.ibatis.annotations.Result;
   57 | import org.apache.ibatis.annotations.ResultMap;
   58 | import org.apache.ibatis.annotations.ResultOrdered;
   59 | import org.apache.ibatis.annotations.ResultType;
   60 | import org.apache.ibatis.annotations.Results;
   61 | import org.apache.ibatis.annotations.Select;
   62 | import org.apache.ibatis.annotations.SelectKey;
   63 | import org.apache.ibatis.annotations.SelectProvider;
   64 | import org.apache.ibatis.annotations.TypeDiscriminator;
   65 | import org.apache.ibatis.annotations.Update;
   66 | import org.apache.ibatis.annotations.UpdateProvider;
   67 | import org.apache.ibatis.binding.MapperMethod.ParamMap;
   68 | import org.apache.ibatis.builder.BuilderException;
   69 | import org.apache.ibatis.builder.CacheRefResolver;
   70 | import org.apache.ibatis.builder.IncompleteElementException;
   71 | import org.apache.ibatis.builder.MapperBuilderAssistant;
   72 | import org.apache.ibatis.builder.ResultMappingConstructorResolver;
   73 | import org.apache.ibatis.builder.xml.XMLMapperBuilder;
   74 | import org.apache.ibatis.cursor.Cursor;
   75 | import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
   76 | import org.apache.ibatis.executor.keygen.KeyGenerator;
   77 | import org.apache.ibatis.executor.keygen.NoKeyGenerator;
   78 | import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
   79 | import org.apache.ibatis.io.Resources;
   80 | import org.apache.ibatis.mapping.Discriminator;
   81 | import org.apache.ibatis.mapping.FetchType;
   82 | import org.apache.ibatis.mapping.MappedStatement;
   83 | import org.apache.ibatis.mapping.ResultFlag;
   84 | import org.apache.ibatis.mapping.ResultMapping;
   85 | import org.apache.ibatis.mapping.ResultSetType;
   86 | import org.apache.ibatis.mapping.SqlCommandType;
   87 | import org.apache.ibatis.mapping.SqlSource;
   88 | import org.apache.ibatis.mapping.StatementType;
   89 | import org.apache.ibatis.parsing.PropertyParser;
   90 | import org.apache.ibatis.reflection.ParamNameResolver;
   91 | import org.apache.ibatis.reflection.TypeParameterResolver;
   92 | import org.apache.ibatis.scripting.LanguageDriver;
   93 | import org.apache.ibatis.session.Configuration;
   94 | import org.apache.ibatis.session.ResultHandler;
   95 | import org.apache.ibatis.session.RowBounds;
   96 | import org.apache.ibatis.type.JdbcType;
   97 | import org.apache.ibatis.type.TypeHandler;
   98 | import org.apache.ibatis.type.UnknownTypeHandler;
   99 | 
  100 | /**
  101 |  * @author Clinton Begin
  102 |  * @author Kazuki Shimizu
  103 |  */
  104 | public class MapperAnnotationBuilder {
  105 | 
  106 |   private static final Set<Class<? extends Annotation>> statementAnnotationTypes = Stream
  107 |       .of(Select.class, Update.class, Insert.class, Delete.class, SelectProvider.class, UpdateProvider.class,
  108 |           InsertProvider.class, DeleteProvider.class)
  109 |       .collect(Collectors.toSet());
  110 | 
  111 |   private final Configuration configuration;
  112 |   private final MapperBuilderAssistant assistant;
  113 |   private final Class<?> type;
  114 | 
  115 |   public MapperAnnotationBuilder(Configuration configuration, Class<?> type) {
  116 |     String resource = type.getName().replace('.', '/') + ".java (best guess)";
  117 |     this.assistant = new MapperBuilderAssistant(configuration, resource);
  118 |     this.configuration = configuration;
  119 |     this.type = type;
  120 |   }
  121 | 
  122 |   public void parse() {
  123 |     String resource = type.toString();
  124 |     if (!configuration.isResourceLoaded(resource)) {
  125 |       loadXmlResource();
  126 |       configuration.addLoadedResource(resource);
  127 |       assistant.setCurrentNamespace(type.getName());
  128 |       parseCache();
  129 |       parseCacheRef();
  130 | 
  131 |       if (type.isAnnotationPresent(NamedResultMap.class)) {
  132 |         parseNamedResultMap(type.getAnnotation(NamedResultMap.class));
  133 |       }
  134 | 
  135 |       if (type.isAnnotationPresent(NamedResultMaps.class)) {
  136 |         parseNamedResultMaps();
  137 |       }
  138 | 
  139 |       for (Method method : type.getMethods()) {
  140 |         if (!canHaveStatement(method)) {
  141 |           continue;
  142 |         }
  143 |         if (getAnnotationWrapper(method, false, Select.class, SelectProvider.class).isPresent()
  144 |             && method.getAnnotation(ResultMap.class) == null) {
  145 |           parseResultMap(method);
  146 |         }
  147 |         try {
  148 |           parseStatement(method);
  149 |         } catch (IncompleteElementException e) {
  150 |           configuration.addIncompleteMethod(new MethodResolver(this, method));
  151 |         }
  152 |       }
  153 |     }
  154 |     configuration.parsePendingMethods(false);
  155 |   }
  156 | 
  157 |   private static boolean canHaveStatement(Method method) {
  158 |     // issue #237
  159 |     return !method.isBridge() && !method.isDefault();
  160 |   }
  161 | 
  162 |   private void loadXmlResource() {
  163 |     // Spring may not know the real resource name so we check a flag
  164 |     // to prevent loading again a resource twice
  165 |     // this flag is set at XMLMapperBuilder#bindMapperForNamespace
  166 |     if (!configuration.isResourceLoaded("namespace:" + type.getName())) {
  167 |       String xmlResource = type.getName().replace('.', '/') + ".xml";
  168 |       // #1347
  169 |       InputStream inputStream = type.getResourceAsStream("/" + xmlResource);
  170 |       if (inputStream == null) {
  171 |         // Search XML mapper that is not in the module but in the classpath.
  172 |         try {
  173 |           inputStream = Resources.getResourceAsStream(type.getClassLoader(), xmlResource);
  174 |         } catch (IOException e2) {
  175 |           // ignore, resource is not required
  176 |         }
  177 |       }
  178 |       if (inputStream != null) {
  179 |         XMLMapperBuilder xmlParser = new XMLMapperBuilder(inputStream, assistant.getConfiguration(), xmlResource,
  180 |             configuration.getSqlFragments(), type);
  181 |         xmlParser.parse();
  182 |       }
  183 |     }
  184 |   }
  185 | 
  186 |   private void parseCache() {
  187 |     CacheNamespace cacheDomain = type.getAnnotation(CacheNamespace.class);
  188 |     if (cacheDomain != null) {
  189 |       Integer size = cacheDomain.size() == 0 ? null : cacheDomain.size();
  190 |       Long flushInterval = cacheDomain.flushInterval() == 0 ? null : cacheDomain.flushInterval();
  191 |       Properties props = convertToProperties(cacheDomain.properties());
  192 |       assistant.useNewCache(cacheDomain.implementation(), cacheDomain.eviction(), flushInterval, size,
  193 |           cacheDomain.readWrite(), cacheDomain.blocking(), props);
  194 |     }
  195 |   }
  196 | 
  197 |   private Properties convertToProperties(Property[] properties) {
  198 |     if (properties.length == 0) {
  199 |       return null;
  200 |     }
  201 |     Properties props = new Properties();
  202 |     for (Property property : properties) {
  203 |       props.setProperty(property.name(), PropertyParser.parse(property.value(), configuration.getVariables()));
  204 |     }
  205 |     return props;
  206 |   }
  207 | 
  208 |   private void parseCacheRef() {
  209 |     CacheNamespaceRef cacheDomainRef = type.getAnnotation(CacheNamespaceRef.class);
  210 |     if (cacheDomainRef != null) {
  211 |       Class<?> refType = cacheDomainRef.value();
  212 |       String refName = cacheDomainRef.name();
  213 |       if (refType == void.class && refName.isEmpty()) {
  214 |         throw new BuilderException("Should be specified either value() or name() attribute in the @CacheNamespaceRef");
  215 |       }
  216 |       if (refType != void.class && !refName.isEmpty()) {
  217 |         throw new BuilderException("Cannot use both value() and name() attribute in the @CacheNamespaceRef");
  218 |       }
  219 |       String namespace = refType != void.class ? refType.getName() : refName;
  220 |       try {
  221 |         assistant.useCacheRef(namespace);
  222 |       } catch (IncompleteElementException e) {
  223 |         configuration.addIncompleteCacheRef(new CacheRefResolver(assistant, namespace));
  224 |       }
  225 |     }
  226 |   }
  227 | 
  228 |   private String parseResultMap(Method method) {
  229 |     Class<?> returnType = getReturnType(method, type);
  230 |     Arg[] args = method.getAnnotationsByType(Arg.class);
  231 |     Result[] results = method.getAnnotationsByType(Result.class);
  232 |     TypeDiscriminator typeDiscriminator = method.getAnnotation(TypeDiscriminator.class);
  233 |     String resultMapId = generateResultMapName(method);
  234 |     applyResultMap(resultMapId, returnType, args, results, typeDiscriminator);
  235 |     return resultMapId;
  236 |   }
  237 | 
  238 |   private void parseNamedResultMaps() {
  239 |     NamedResultMaps namedResultMaps = type.getAnnotation(NamedResultMaps.class);
  240 |     for (NamedResultMap namedResultMap : namedResultMaps.value()) {
  241 |       parseNamedResultMap(namedResultMap);
  242 |     }
  243 |   }
  244 | 
  245 |   private void parseNamedResultMap(NamedResultMap namedResultMap) {
  246 |     validateNamedResultMap(namedResultMap);
  247 | 
  248 |     String resultMapId = type.getName() + '.' + namedResultMap.id();
  249 |     Class<?> returnType = namedResultMap.javaType();
  250 |     Arg[] args = namedResultMap.constructorArguments();
  251 |     Result[] results = namedResultMap.propertyMappings();
  252 |     TypeDiscriminator typeDiscriminator = namedResultMap.typeDiscriminator();
  253 |     if (AnnotationConstants.NULL_TYPE_DISCRIMINATOR.equals(typeDiscriminator.column())) {
  254 |       typeDiscriminator = null;
  255 |     }
  256 | 
  257 |     applyResultMap(resultMapId, returnType, args, results, typeDiscriminator);
  258 |   }
  259 | 
  260 |   private void validateNamedResultMap(NamedResultMap namedResultMap) {
  261 |     if (!AnnotationConstants.NULL_TYPE_DISCRIMINATOR.equals(namedResultMap.typeDiscriminator().column())) {
  262 |       return;
  263 |     }
  264 | 
  265 |     if (namedResultMap.constructorArguments().length == 0 && namedResultMap.propertyMappings().length == 0) {
  266 |       throw new BuilderException("If there is no type discriminator, then the NamedResultMap annotation "
  267 |           + "requires at least one constructor argument or property mapping");
  268 |     }
  269 |   }
  270 | 
  271 |   private String generateResultMapName(Method method) {
  272 |     Results results = method.getAnnotation(Results.class);
  273 |     if (results != null && !results.id().isEmpty()) {
  274 |       return type.getName() + "." + results.id();
  275 |     }
  276 |     StringBuilder suffix = new StringBuilder();
  277 |     for (Class<?> c : method.getParameterTypes()) {
  278 |       suffix.append("-");
  279 |       suffix.append(c.getSimpleName());
  280 |     }
  281 |     if (suffix.length() < 1) {
  282 |       suffix.append("-void");
  283 |     }
  284 |     return type.getName() + "." + method.getName() + suffix;
  285 |   }
  286 | 
  287 |   private void applyResultMap(String resultMapId, Class<?> returnType, Arg[] args, Result[] results,
  288 |       TypeDiscriminator discriminator) {
  289 |     List<ResultMapping> resultMappings = new ArrayList<>();
  290 |     applyConstructorArgs(args, returnType, resultMappings, resultMapId);
  291 |     applyResults(results, returnType, resultMappings);
  292 |     Discriminator disc = applyDiscriminator(resultMapId, returnType, discriminator);
  293 |     // TODO add AutoMappingBehaviour
  294 |     assistant.addResultMap(resultMapId, returnType, null, disc, resultMappings, null);
  295 |     createDiscriminatorResultMaps(resultMapId, returnType, discriminator);
  296 |   }
  297 | 
  298 |   private void createDiscriminatorResultMaps(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
  299 |     if (discriminator != null) {
  300 |       for (Case c : discriminator.cases()) {
  301 |         String caseResultMapId = resultMapId + "-" + c.value();
  302 |         List<ResultMapping> resultMappings = new ArrayList<>();
  303 |         // issue #136
  304 |         applyConstructorArgs(c.constructArgs(), resultType, resultMappings, resultMapId);
  305 |         applyResults(c.results(), resultType, resultMappings);
  306 |         // TODO add AutoMappingBehaviour
  307 |         assistant.addResultMap(caseResultMapId, c.type(), resultMapId, null, resultMappings, null);
  308 |       }
  309 |     }
  310 |   }
  311 | 
  312 |   private Discriminator applyDiscriminator(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
  313 |     if (discriminator != null) {
  314 |       String column = discriminator.column();
  315 |       Class<?> javaType = discriminator.javaType() == void.class ? String.class : discriminator.javaType();
  316 |       JdbcType jdbcType = discriminator.jdbcType() == JdbcType.UNDEFINED ? null : discriminator.jdbcType();
  317 |       @SuppressWarnings("unchecked")
  318 |       Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (discriminator
  319 |           .typeHandler() == UnknownTypeHandler.class ? null : discriminator.typeHandler());
  320 |       Case[] cases = discriminator.cases();
  321 |       Map<String, String> discriminatorMap = new HashMap<>();
  322 |       for (Case c : cases) {
  323 |         String value = c.value();
  324 |         String caseResultMapId = resultMapId + "-" + value;
  325 |         discriminatorMap.put(value, caseResultMapId);
  326 |       }
  327 |       return assistant.buildDiscriminator(resultType, column, javaType, jdbcType, typeHandler, discriminatorMap);
  328 |     }
  329 |     return null;
  330 |   }
  331 | 
  332 |   void parseStatement(Method method) {
  333 |     final Class<?> parameterTypeClass = getParameterType(method);
  334 |     final ParamNameResolver paramNameResolver = new ParamNameResolver(configuration, method, type);
  335 |     final LanguageDriver languageDriver = getLanguageDriver(method);
  336 | 
  337 |     getAnnotationWrapper(method, true, statementAnnotationTypes).ifPresent(statementAnnotation -> {
  338 |       final SqlSource sqlSource = buildSqlSource(statementAnnotation.getAnnotation(), parameterTypeClass,
  339 |           paramNameResolver, languageDriver, method);
  340 |       final SqlCommandType sqlCommandType = statementAnnotation.getSqlCommandType();
  341 |       final Options options = getAnnotationWrapper(method, false, Options.class).map(x -> (Options) x.getAnnotation())
  342 |           .orElse(null);
  343 |       final ResultOrdered resultOrderedAnnotation = getAnnotationWrapper(method, false, ResultOrdered.class)
  344 |           .map(x -> (ResultOrdered) x.getAnnotation()).orElse(null);
  345 |       final String mappedStatementId = type.getName() + "." + method.getName();
  346 | 
  347 |       final KeyGenerator keyGenerator;
  348 |       String keyProperty = null;
  349 |       String keyColumn = null;
  350 |       if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
  351 |         // first check for SelectKey annotation - that overrides everything else
  352 |         SelectKey selectKey = getAnnotationWrapper(method, false, SelectKey.class)
  353 |             .map(x -> (SelectKey) x.getAnnotation()).orElse(null);
  354 |         if (selectKey != null) {
  355 |           keyGenerator = handleSelectKeyAnnotation(selectKey, mappedStatementId, getParameterType(method),
  356 |               paramNameResolver, languageDriver);
  357 |           keyProperty = selectKey.keyProperty();
  358 |         } else if (options == null) {
  359 |           keyGenerator = configuration.isUseGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
  360 |         } else {
  361 |           keyGenerator = options.useGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
  362 |           keyProperty = options.keyProperty();
  363 |           keyColumn = options.keyColumn();
  364 |         }
  365 |       } else {
  366 |         keyGenerator = NoKeyGenerator.INSTANCE;
  367 |       }
  368 | 
  369 |       Integer fetchSize = null;
  370 |       Integer timeout = null;
  371 |       StatementType statementType = StatementType.PREPARED;
  372 |       ResultSetType resultSetType = configuration.getDefaultResultSetType();
  373 |       boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
  374 |       boolean flushCache = !isSelect;
  375 |       boolean useCache = isSelect;
  376 |       if (options != null) {
  377 |         if (FlushCachePolicy.TRUE.equals(options.flushCache())) {
  378 |           flushCache = true;
  379 |         } else if (FlushCachePolicy.FALSE.equals(options.flushCache())) {
  380 |           flushCache = false;
  381 |         }
  382 |         useCache = options.useCache();
  383 |         // issue #348
  384 |         fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ? options.fetchSize() : null;
  385 |         timeout = options.timeout() > -1 ? options.timeout() : null;
  386 |         statementType = options.statementType();
  387 |         if (options.resultSetType() != ResultSetType.DEFAULT) {
  388 |           resultSetType = options.resultSetType();
  389 |         }
  390 |       }
  391 | 
  392 |       boolean isResultOrdered = false;
  393 |       if (resultOrderedAnnotation != null) {
  394 |         isResultOrdered = resultOrderedAnnotation.value();
  395 |       }
  396 | 
  397 |       String resultMapId = null;
  398 |       if (isSelect) {
  399 |         ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
  400 |         if (resultMapAnnotation != null) {
  401 |           resultMapId = String.join(",", resultMapAnnotation.value());
  402 |         } else {
  403 |           resultMapId = generateResultMapName(method);
  404 |         }
  405 |       }
  406 | 
  407 |       assistant.addMappedStatement(mappedStatementId, sqlSource, statementType, sqlCommandType, fetchSize, timeout,
  408 |           // ParameterMapID
  409 |           null, parameterTypeClass, resultMapId, getReturnType(method, type), resultSetType, flushCache, useCache,
  410 |           // TODO gcode issue #577
  411 |           isResultOrdered, keyGenerator, keyProperty, keyColumn, statementAnnotation.getDatabaseId(), languageDriver,
  412 |           // ResultSets
  413 |           options != null ? nullOrEmpty(options.resultSets()) : null, statementAnnotation.isDirtySelect(),
  414 |           paramNameResolver);
  415 |     });
  416 |   }
  417 | 
  418 |   private LanguageDriver getLanguageDriver(Method method) {
  419 |     Lang lang = method.getAnnotation(Lang.class);
  420 |     Class<? extends LanguageDriver> langClass = null;
  421 |     if (lang != null) {
  422 |       langClass = lang.value();
  423 |     }
  424 |     return configuration.getLanguageDriver(langClass);
  425 |   }
  426 | 
  427 |   private Class<?> getParameterType(Method method) {
  428 |     Class<?> parameterType = null;
  429 |     Parameter[] parameters = method.getParameters();
  430 |     for (Parameter param : parameters) {
  431 |       Class<?> paramType = param.getType();
  432 |       if (RowBounds.class.isAssignableFrom(paramType) || ResultHandler.class.isAssignableFrom(paramType)) {
  433 |         continue;
  434 |       }
  435 |       if (parameterType == null && param.getAnnotation(Param.class) == null) {
  436 |         parameterType = paramType;
  437 |       } else {
  438 |         return ParamMap.class;
  439 |       }
  440 |     }
  441 |     return parameterType;
  442 |   }
  443 | 
  444 |   private static Class<?> getReturnType(Method method, Class<?> type) {
  445 |     Class<?> returnType = method.getReturnType();
  446 |     Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, type);
  447 |     if (resolvedReturnType instanceof Class) {
  448 |       returnType = (Class<?>) resolvedReturnType;
  449 |       if (returnType.isArray()) {
  450 |         returnType = returnType.getComponentType();
  451 |       }
  452 |       // gcode issue #508
  453 |       if (void.class.equals(returnType)) {
  454 |         ResultType rt = method.getAnnotation(ResultType.class);
  455 |         if (rt != null) {
  456 |           returnType = rt.value();
  457 |         }
  458 |       }
  459 |     } else if (resolvedReturnType instanceof ParameterizedType) {
  460 |       ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
  461 |       Class<?> rawType = (Class<?>) parameterizedType.getRawType();
  462 |       if (Collection.class.isAssignableFrom(rawType) || Cursor.class.isAssignableFrom(rawType)) {
  463 |         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
  464 |         if (actualTypeArguments != null && actualTypeArguments.length == 1) {
  465 |           Type returnTypeParameter = actualTypeArguments[0];
  466 |           if (returnTypeParameter instanceof Class<?>) {
  467 |             returnType = (Class<?>) returnTypeParameter;
  468 |           } else if (returnTypeParameter instanceof ParameterizedType) {
  469 |             // (gcode issue #443) actual type can be a also a parameterized type
  470 |             returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
  471 |           } else if (returnTypeParameter instanceof GenericArrayType) {
  472 |             Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter).getGenericComponentType();
  473 |             // (gcode issue #525) support List<byte[]>
  474 |             returnType = Array.newInstance(componentType, 0).getClass();
  475 |           }
  476 |         }
  477 |       } else if (method.isAnnotationPresent(MapKey.class) && Map.class.isAssignableFrom(rawType)) {
  478 |         // (gcode issue 504) Do not look into Maps if there is not MapKey annotation
  479 |         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
  480 |         if (actualTypeArguments != null && actualTypeArguments.length == 2) {
  481 |           Type returnTypeParameter = actualTypeArguments[1];
  482 |           if (returnTypeParameter instanceof Class<?>) {
  483 |             returnType = (Class<?>) returnTypeParameter;
  484 |           } else if (returnTypeParameter instanceof ParameterizedType) {
  485 |             // (gcode issue 443) actual type can be a also a parameterized type
  486 |             returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
  487 |           }
  488 |         }
  489 |       } else if (Optional.class.equals(rawType)) {
  490 |         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
  491 |         Type returnTypeParameter = actualTypeArguments[0];
  492 |         if (returnTypeParameter instanceof Class<?>) {
  493 |           returnType = (Class<?>) returnTypeParameter;
  494 |         }
  495 |       }
  496 |     }
  497 | 
  498 |     return returnType;
  499 |   }
  500 | 
  501 |   private void applyResults(Result[] results, Class<?> resultType, List<ResultMapping> resultMappings) {
  502 |     for (Result result : results) {
  503 |       List<ResultFlag> flags = new ArrayList<>();
  504 |       if (result.id()) {
  505 |         flags.add(ResultFlag.ID);
  506 |       }
  507 |       @SuppressWarnings("unchecked")
  508 |       Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (result
  509 |           .typeHandler() == UnknownTypeHandler.class ? null : result.typeHandler());
  510 |       boolean hasNestedResultMap = hasNestedResultMap(result);
  511 |       ResultMapping resultMapping = assistant.buildResultMapping(resultType, nullOrEmpty(result.property()),
  512 |           nullOrEmpty(result.column()), result.javaType() == void.class ? null : result.javaType(),
  513 |           result.jdbcType() == JdbcType.UNDEFINED ? null : result.jdbcType(),
  514 |           hasNestedSelect(result) ? nestedSelectId(result) : null,
  515 |           hasNestedResultMap ? nestedResultMapId(result) : null, null,
  516 |           hasNestedResultMap ? findColumnPrefix(result) : null, typeHandler, flags, null, null, isLazy(result));
  517 |       resultMappings.add(resultMapping);
  518 |     }
  519 |   }
  520 | 
  521 |   private String findColumnPrefix(Result result) {
  522 |     String columnPrefix = result.one().columnPrefix();
  523 |     if (columnPrefix.isEmpty()) {
  524 |       columnPrefix = result.many().columnPrefix();
  525 |     }
  526 |     return columnPrefix;
  527 |   }
  528 | 
  529 |   private String nestedResultMapId(Result result) {
  530 |     String resultMapId = result.one().resultMap();
  531 |     if (resultMapId.isEmpty()) {
  532 |       resultMapId = result.many().resultMap();
  533 |     }
  534 |     if (!resultMapId.contains(".")) {
  535 |       resultMapId = type.getName() + "." + resultMapId;
  536 |     }
  537 |     return resultMapId;
  538 |   }
  539 | 
  540 |   private boolean hasNestedResultMap(Result result) {
  541 |     if (!result.one().resultMap().isEmpty() && !result.many().resultMap().isEmpty()) {
  542 |       throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
  543 |     }
  544 |     return !result.one().resultMap().isEmpty() || !result.many().resultMap().isEmpty();
  545 |   }
  546 | 
  547 |   private String nestedSelectId(Result result) {
  548 |     String nestedSelect = result.one().select();
  549 |     if (nestedSelect.isEmpty()) {
  550 |       nestedSelect = result.many().select();
  551 |     }
  552 |     if (!nestedSelect.contains(".")) {
  553 |       nestedSelect = type.getName() + "." + nestedSelect;
  554 |     }
  555 |     return nestedSelect;
  556 |   }
  557 | 
  558 |   private boolean isLazy(Result result) {
  559 |     boolean isLazy = configuration.isLazyLoadingEnabled();
  560 |     if (!result.one().select().isEmpty() && FetchType.DEFAULT != result.one().fetchType()) {
  561 |       isLazy = result.one().fetchType() == FetchType.LAZY;
  562 |     } else if (!result.many().select().isEmpty() && FetchType.DEFAULT != result.many().fetchType()) {
  563 |       isLazy = result.many().fetchType() == FetchType.LAZY;
  564 |     }
  565 |     return isLazy;
  566 |   }
  567 | 
  568 |   private boolean hasNestedSelect(Result result) {
  569 |     if (!result.one().select().isEmpty() && !result.many().select().isEmpty()) {
  570 |       throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
  571 |     }
  572 |     return !result.one().select().isEmpty() || !result.many().select().isEmpty();
  573 |   }
  574 | 
  575 |   private void applyConstructorArgs(Arg[] args, Class<?> resultType, List<ResultMapping> resultMappings,
  576 |       String resultMapId) {
  577 |     final List<ResultMapping> mappings = new ArrayList<>();
  578 |     for (Arg arg : args) {
  579 |       List<ResultFlag> flags = new ArrayList<>();
  580 |       flags.add(ResultFlag.CONSTRUCTOR);
  581 |       if (arg.id()) {
  582 |         flags.add(ResultFlag.ID);
  583 |       }
  584 |       @SuppressWarnings("unchecked")
  585 |       Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (arg
  586 |           .typeHandler() == UnknownTypeHandler.class ? null : arg.typeHandler());
  587 |       ResultMapping resultMapping = assistant.buildResultMapping(resultType, nullOrEmpty(arg.name()),
  588 |           nullOrEmpty(arg.column()), arg.javaType() == void.class ? null : arg.javaType(),
  589 |           arg.jdbcType() == JdbcType.UNDEFINED ? null : arg.jdbcType(), nullOrEmpty(arg.select()),
  590 |           nullOrEmpty(arg.resultMap()), null, nullOrEmpty(arg.columnPrefix()), typeHandler, flags, null, null, false);
  591 |       mappings.add(resultMapping);
  592 |     }
  593 | 
  594 |     final ResultMappingConstructorResolver resolver = new ResultMappingConstructorResolver(configuration, mappings,
  595 |         resultType, resultMapId);
  596 |     resultMappings.addAll(resolver.resolveWithConstructor());
  597 |   }
  598 | 
  599 |   private String nullOrEmpty(String value) {
  600 |     return value == null || value.trim().isEmpty() ? null : value;
  601 |   }
  602 | 
  603 |   private KeyGenerator handleSelectKeyAnnotation(SelectKey selectKeyAnnotation, String baseStatementId,
  604 |       Class<?> parameterTypeClass, ParamNameResolver paramNameResolver, LanguageDriver languageDriver) {
  605 |     String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
  606 |     Class<?> resultTypeClass = selectKeyAnnotation.resultType();
  607 |     StatementType statementType = selectKeyAnnotation.statementType();
  608 |     String keyProperty = selectKeyAnnotation.keyProperty();
  609 |     String keyColumn = selectKeyAnnotation.keyColumn();
  610 |     boolean executeBefore = selectKeyAnnotation.before();
  611 | 
  612 |     // defaults
  613 |     boolean useCache = false;
  614 |     KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
  615 |     Integer fetchSize = null;
  616 |     Integer timeout = null;
  617 |     boolean flushCache = false;
  618 |     String parameterMap = null;
  619 |     String resultMap = null;
  620 |     ResultSetType resultSetTypeEnum = null;
  621 |     String databaseId = selectKeyAnnotation.databaseId().isEmpty() ? null : selectKeyAnnotation.databaseId();
  622 | 
  623 |     SqlSource sqlSource = buildSqlSourceFromStrings(selectKeyAnnotation.statement(), parameterTypeClass,
  624 |         paramNameResolver, languageDriver);
  625 |     SqlCommandType sqlCommandType = SqlCommandType.SELECT;
  626 | 
  627 |     assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
  628 |         parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum, flushCache, useCache, false, keyGenerator,
  629 |         keyProperty, keyColumn, databaseId, languageDriver, null, false, paramNameResolver);
  630 | 
  631 |     id = assistant.applyCurrentNamespace(id, false);
  632 | 
  633 |     MappedStatement keyStatement = configuration.getMappedStatement(id, false);
  634 |     SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
  635 |     configuration.addKeyGenerator(id, answer);
  636 |     return answer;
  637 |   }
  638 | 
  639 |   private SqlSource buildSqlSource(Annotation annotation, Class<?> parameterType, ParamNameResolver paramNameResolver,
  640 |       LanguageDriver languageDriver, Method method) {
  641 |     if (annotation instanceof Select) {
  642 |       return buildSqlSourceFromStrings(((Select) annotation).value(), parameterType, paramNameResolver, languageDriver);
  643 |     } else if (annotation instanceof Update) {
  644 |       return buildSqlSourceFromStrings(((Update) annotation).value(), parameterType, paramNameResolver, languageDriver);
  645 |     } else if (annotation instanceof Insert) {
  646 |       return buildSqlSourceFromStrings(((Insert) annotation).value(), parameterType, paramNameResolver, languageDriver);
  647 |     } else if (annotation instanceof Delete) {
  648 |       return buildSqlSourceFromStrings(((Delete) annotation).value(), parameterType, paramNameResolver, languageDriver);
  649 |     } else if (annotation instanceof SelectKey) {
  650 |       return buildSqlSourceFromStrings(((SelectKey) annotation).statement(), parameterType, paramNameResolver,
  651 |           languageDriver);
  652 |     }
  653 |     return new ProviderSqlSource(assistant.getConfiguration(), annotation, type, method);
  654 |   }
  655 | 
  656 |   private SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass,
  657 |       ParamNameResolver paramNameResolver, LanguageDriver languageDriver) {
  658 |     return languageDriver.createSqlSource(configuration, String.join(" ", strings).trim(), parameterTypeClass,
  659 |         paramNameResolver);
  660 |   }
  661 | 
  662 |   @SafeVarargs
  663 |   private final Optional<AnnotationWrapper> getAnnotationWrapper(Method method, boolean errorIfNoMatch,
  664 |       Class<? extends Annotation>... targetTypes) {
  665 |     return getAnnotationWrapper(method, errorIfNoMatch, Arrays.asList(targetTypes));
  666 |   }
  667 | 
  668 |   private Optional<AnnotationWrapper> getAnnotationWrapper(Method method, boolean errorIfNoMatch,
  669 |       Collection<Class<? extends Annotation>> targetTypes) {
  670 |     String databaseId = configuration.getDatabaseId();
  671 |     Map<String, AnnotationWrapper> statementAnnotations = targetTypes.stream()
  672 |         .flatMap(x -> Arrays.stream(method.getAnnotationsByType(x))).map(AnnotationWrapper::new)
  673 |         .collect(Collectors.toMap(AnnotationWrapper::getDatabaseId, x -> x, (existing, duplicate) -> {
  674 |           throw new BuilderException(
  675 |               String.format("Detected conflicting annotations '%s' and '%s' on '%s'.", existing.getAnnotation(),
  676 |                   duplicate.getAnnotation(), method.getDeclaringClass().getName() + "." + method.getName()));
  677 |         }));
  678 |     AnnotationWrapper annotationWrapper = null;
  679 |     if (databaseId != null) {
  680 |       annotationWrapper = statementAnnotations.get(databaseId);
  681 |     }
  682 |     if (annotationWrapper == null) {
  683 |       annotationWrapper = statementAnnotations.get("");
  684 |     }
  685 |     if (errorIfNoMatch && annotationWrapper == null && !statementAnnotations.isEmpty()) {
  686 |       // Annotations exist, but there is no matching one for the specified databaseId
  687 |       throw new BuilderException(String.format(
  688 |           "Could not find a statement annotation that correspond a current database or default statement on method '%s.%s'. Current database id is [%s].",
  689 |           method.getDeclaringClass().getName(), method.getName(), databaseId));
  690 |     }
  691 |     return Optional.ofNullable(annotationWrapper);
  692 |   }
  693 | 
  694 |   public static Class<?> getMethodReturnType(String mapperFqn, String localStatementId) {
  695 |     if (mapperFqn == null || localStatementId == null) {
  696 |       return null;
  697 |     }
  698 |     try {
  699 |       Class<?> mapperClass = Resources.classForName(mapperFqn);
  700 |       for (Method method : mapperClass.getMethods()) {
  701 |         if (method.getName().equals(localStatementId) && canHaveStatement(method)) {
  702 |           return getReturnType(method, mapperClass);
  703 |         }
  704 |       }
  705 |     } catch (ClassNotFoundException e) {
  706 |       // No corresponding mapper interface which is OK
  707 |     }
  708 |     return null;
  709 |   }
  710 | 
  711 |   private static class AnnotationWrapper {
  712 |     private final Annotation annotation;
  713 |     private final String databaseId;
  714 |     private final SqlCommandType sqlCommandType;
  715 |     private boolean dirtySelect;
  716 | 
  717 |     AnnotationWrapper(Annotation annotation) {
  718 |       this.annotation = annotation;
  719 |       if (annotation instanceof Select) {
  720 |         databaseId = ((Select) annotation).databaseId();
  721 |         sqlCommandType = SqlCommandType.SELECT;
  722 |         dirtySelect = ((Select) annotation).affectData();
  723 |       } else if (annotation instanceof Update) {
  724 |         databaseId = ((Update) annotation).databaseId();
  725 |         sqlCommandType = SqlCommandType.UPDATE;
  726 |       } else if (annotation instanceof Insert) {
  727 |         databaseId = ((Insert) annotation).databaseId();
  728 |         sqlCommandType = SqlCommandType.INSERT;
  729 |       } else if (annotation instanceof Delete) {
  730 |         databaseId = ((Delete) annotation).databaseId();
  731 |         sqlCommandType = SqlCommandType.DELETE;
  732 |       } else if (annotation instanceof SelectProvider) {
  733 |         databaseId = ((SelectProvider) annotation).databaseId();
  734 |         sqlCommandType = SqlCommandType.SELECT;
  735 |         dirtySelect = ((SelectProvider) annotation).affectData();
  736 |       } else if (annotation instanceof UpdateProvider) {
  737 |         databaseId = ((UpdateProvider) annotation).databaseId();
  738 |         sqlCommandType = SqlCommandType.UPDATE;
  739 |       } else if (annotation instanceof InsertProvider) {
  740 |         databaseId = ((InsertProvider) annotation).databaseId();
  741 |         sqlCommandType = SqlCommandType.INSERT;
  742 |       } else if (annotation instanceof DeleteProvider) {
  743 |         databaseId = ((DeleteProvider) annotation).databaseId();
  744 |         sqlCommandType = SqlCommandType.DELETE;
  745 |       } else {
  746 |         sqlCommandType = SqlCommandType.UNKNOWN;
  747 |         if (annotation instanceof Options) {
  748 |           databaseId = ((Options) annotation).databaseId();
  749 |         } else if (annotation instanceof SelectKey) {
  750 |           databaseId = ((SelectKey) annotation).databaseId();
  751 |         } else {
  752 |           databaseId = "";
  753 |         }
  754 |       }
  755 |     }
  756 | 
  757 |     Annotation getAnnotation() {
  758 |       return annotation;
  759 |     }
  760 | 
  761 |     SqlCommandType getSqlCommandType() {
  762 |       return sqlCommandType;
  763 |     }
  764 | 
  765 |     String getDatabaseId() {
  766 |       return databaseId;
  767 |     }
  768 | 
  769 |     boolean isDirtySelect() {
  770 |       return dirtySelect;
  771 |     }
  772 |   }
  773 | }
```
