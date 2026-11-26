<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/builder/xml/XMLStatementBuilder.java -->
<!-- 正文引用：[[源码:XMLStatementBuilder.java#L起-L止]] ；代码块头部：(src: XMLStatementBuilder.java#L起-L止) -->

# XMLStatementBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/builder/xml/XMLStatementBuilder.java` |
| 行数 | 241 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:18.973Z |

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
   18 | import java.lang.reflect.Method;
   19 | import java.lang.reflect.Type;
   20 | import java.util.Arrays;
   21 | import java.util.List;
   22 | import java.util.Locale;
   23 | import java.util.stream.Collectors;
   24 | 
   25 | import org.apache.ibatis.binding.MapperMethod.ParamMap;
   26 | import org.apache.ibatis.builder.BaseBuilder;
   27 | import org.apache.ibatis.builder.MapperBuilderAssistant;
   28 | import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
   29 | import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
   30 | import org.apache.ibatis.executor.keygen.KeyGenerator;
   31 | import org.apache.ibatis.executor.keygen.NoKeyGenerator;
   32 | import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
   33 | import org.apache.ibatis.mapping.MappedStatement;
   34 | import org.apache.ibatis.mapping.ResultSetType;
   35 | import org.apache.ibatis.mapping.SqlCommandType;
   36 | import org.apache.ibatis.mapping.SqlSource;
   37 | import org.apache.ibatis.mapping.StatementType;
   38 | import org.apache.ibatis.parsing.XNode;
   39 | import org.apache.ibatis.reflection.ParamNameResolver;
   40 | import org.apache.ibatis.scripting.LanguageDriver;
   41 | import org.apache.ibatis.session.Configuration;
   42 | 
   43 | /**
   44 |  * @author Clinton Begin
   45 |  */
   46 | public class XMLStatementBuilder extends BaseBuilder {
   47 | 
   48 |   private final MapperBuilderAssistant builderAssistant;
   49 |   private final XNode context;
   50 |   private final String requiredDatabaseId;
   51 |   private final Class<?> mapperClass;
   52 | 
   53 |   public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context) {
   54 |     this(configuration, builderAssistant, context, null);
   55 |   }
   56 | 
   57 |   public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context,
   58 |       String databaseId) {
   59 |     this(configuration, builderAssistant, context, databaseId, null);
   60 |   }
   61 | 
   62 |   public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context,
   63 |       String databaseId, Class<?> mapperClass) {
   64 |     super(configuration);
   65 |     this.builderAssistant = builderAssistant;
   66 |     this.context = context;
   67 |     this.requiredDatabaseId = databaseId;
   68 |     this.mapperClass = mapperClass;
   69 |   }
   70 | 
   71 |   public void parseStatementNode() {
   72 |     String id = context.getStringAttribute("id");
   73 |     String databaseId = context.getStringAttribute("databaseId");
   74 | 
   75 |     if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
   76 |       return;
   77 |     }
   78 | 
   79 |     String nodeName = context.getNode().getNodeName();
   80 |     SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
   81 |     boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
   82 |     boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
   83 |     boolean useCache = context.getBooleanAttribute("useCache", isSelect);
   84 |     boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);
   85 | 
   86 |     // Include Fragments before parsing
   87 |     XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
   88 |     includeParser.applyIncludes(context.getNode());
   89 | 
   90 |     String parameterType = context.getStringAttribute("parameterType");
   91 |     Class<?> parameterTypeClass = resolveClass(parameterType);
   92 |     ParamNameResolver paramNameResolver = null;
   93 |     if (parameterTypeClass == null && mapperClass != null) {
   94 |       List<Method> mapperMethods = Arrays.stream(mapperClass.getMethods())
   95 |           .filter(m -> m.getName().equals(id) && !m.isDefault() && !m.isBridge()).collect(Collectors.toList());
   96 |       if (mapperMethods.size() == 1) {
   97 |         paramNameResolver = new ParamNameResolver(configuration, mapperMethods.get(0), mapperClass);
   98 |         if (paramNameResolver.isUseParamMap()) {
   99 |           parameterTypeClass = ParamMap.class;
  100 |         } else {
  101 |           String[] paramNames = paramNameResolver.getNames();
  102 |           if (paramNames.length == 1) {
  103 |             Type paramType = paramNameResolver.getType(paramNames[0]);
  104 |             if (paramType instanceof Class) {
  105 |               parameterTypeClass = (Class<?>) paramType;
  106 |             }
  107 |           }
  108 |         }
  109 |       }
  110 |     }
  111 | 
  112 |     String lang = context.getStringAttribute("lang");
  113 |     LanguageDriver langDriver = getLanguageDriver(lang);
  114 | 
  115 |     // Parse selectKey after includes and remove them.
  116 |     processSelectKeyNodes(id, parameterTypeClass, langDriver);
  117 | 
  118 |     // Parse the SQL (pre: <selectKey> and <include> were parsed and removed)
  119 |     KeyGenerator keyGenerator;
  120 |     String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
  121 |     keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
  122 |     if (configuration.hasKeyGenerator(keyStatementId)) {
  123 |       keyGenerator = configuration.getKeyGenerator(keyStatementId);
  124 |     } else {
  125 |       keyGenerator = context.getBooleanAttribute("useGeneratedKeys",
  126 |           configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType))
  127 |               ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
  128 |     }
  129 | 
  130 |     SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass, paramNameResolver);
  131 |     StatementType statementType = StatementType
  132 |         .valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
  133 |     Integer fetchSize = context.getIntAttribute("fetchSize");
  134 |     Integer timeout = context.getIntAttribute("timeout");
  135 |     String parameterMap = context.getStringAttribute("parameterMap");
  136 |     String resultType = context.getStringAttribute("resultType");
  137 |     Class<?> resultTypeClass = resolveClass(resultType);
  138 |     String resultMap = context.getStringAttribute("resultMap");
  139 |     if (resultTypeClass == null && resultMap == null) {
  140 |       resultTypeClass = MapperAnnotationBuilder.getMethodReturnType(builderAssistant.getCurrentNamespace(), id);
  141 |     }
  142 |     String resultSetType = context.getStringAttribute("resultSetType");
  143 |     ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);
  144 |     if (resultSetTypeEnum == null) {
  145 |       resultSetTypeEnum = configuration.getDefaultResultSetType();
  146 |     }
  147 |     String keyProperty = context.getStringAttribute("keyProperty");
  148 |     String keyColumn = context.getStringAttribute("keyColumn");
  149 |     String resultSets = context.getStringAttribute("resultSets");
  150 |     boolean dirtySelect = context.getBooleanAttribute("affectData", Boolean.FALSE);
  151 | 
  152 |     builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
  153 |         parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum, flushCache, useCache, resultOrdered,
  154 |         keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets, dirtySelect, paramNameResolver);
  155 |   }
  156 | 
  157 |   private void processSelectKeyNodes(String id, Class<?> parameterTypeClass, LanguageDriver langDriver) {
  158 |     List<XNode> selectKeyNodes = context.evalNodes("selectKey");
  159 |     if (configuration.getDatabaseId() != null) {
  160 |       parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, configuration.getDatabaseId());
  161 |     }
  162 |     parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, null);
  163 |     removeSelectKeyNodes(selectKeyNodes);
  164 |   }
  165 | 
  166 |   private void parseSelectKeyNodes(String parentId, List<XNode> list, Class<?> parameterTypeClass,
  167 |       LanguageDriver langDriver, String skRequiredDatabaseId) {
  168 |     for (XNode nodeToHandle : list) {
  169 |       String id = parentId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
  170 |       String databaseId = nodeToHandle.getStringAttribute("databaseId");
  171 |       if (databaseIdMatchesCurrent(id, databaseId, skRequiredDatabaseId)) {
  172 |         parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, langDriver, databaseId);
  173 |       }
  174 |     }
  175 |   }
  176 | 
  177 |   private void parseSelectKeyNode(String id, XNode nodeToHandle, Class<?> parameterTypeClass, LanguageDriver langDriver,
  178 |       String databaseId) {
  179 |     String resultType = nodeToHandle.getStringAttribute("resultType");
  180 |     Class<?> resultTypeClass = resolveClass(resultType);
  181 |     StatementType statementType = StatementType
  182 |         .valueOf(nodeToHandle.getStringAttribute("statementType", StatementType.PREPARED.toString()));
  183 |     String keyProperty = nodeToHandle.getStringAttribute("keyProperty");
  184 |     String keyColumn = nodeToHandle.getStringAttribute("keyColumn");
  185 |     boolean executeBefore = "BEFORE".equals(nodeToHandle.getStringAttribute("order", "AFTER"));
  186 | 
  187 |     // defaults
  188 |     boolean useCache = false;
  189 |     boolean resultOrdered = false;
  190 |     KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
  191 |     Integer fetchSize = null;
  192 |     Integer timeout = null;
  193 |     boolean flushCache = false;
  194 |     String parameterMap = null;
  195 |     String resultMap = null;
  196 |     ResultSetType resultSetTypeEnum = null;
  197 | 
  198 |     SqlSource sqlSource = langDriver.createSqlSource(configuration, nodeToHandle, parameterTypeClass);
  199 |     SqlCommandType sqlCommandType = SqlCommandType.SELECT;
  200 | 
  201 |     builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
  202 |         parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum, flushCache, useCache, resultOrdered,
  203 |         keyGenerator, keyProperty, keyColumn, databaseId, langDriver, null, false, null);
  204 | 
  205 |     id = builderAssistant.applyCurrentNamespace(id, false);
  206 | 
  207 |     MappedStatement keyStatement = configuration.getMappedStatement(id, false);
  208 |     configuration.addKeyGenerator(id, new SelectKeyGenerator(keyStatement, executeBefore));
  209 |   }
  210 | 
  211 |   private void removeSelectKeyNodes(List<XNode> selectKeyNodes) {
  212 |     for (XNode nodeToHandle : selectKeyNodes) {
  213 |       nodeToHandle.getParent().getNode().removeChild(nodeToHandle.getNode());
  214 |     }
  215 |   }
  216 | 
  217 |   private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
  218 |     if (requiredDatabaseId != null) {
  219 |       return requiredDatabaseId.equals(databaseId);
  220 |     }
  221 |     if (databaseId != null) {
  222 |       return false;
  223 |     }
  224 |     id = builderAssistant.applyCurrentNamespace(id, false);
  225 |     if (!this.configuration.hasStatement(id, false)) {
  226 |       return true;
  227 |     }
  228 |     // skip this statement if there is a previous one with a not null databaseId
  229 |     MappedStatement previous = this.configuration.getMappedStatement(id, false); // issue #2
  230 |     return previous.getDatabaseId() == null;
  231 |   }
  232 | 
  233 |   private LanguageDriver getLanguageDriver(String lang) {
  234 |     Class<? extends LanguageDriver> langClass = null;
  235 |     if (lang != null) {
  236 |       langClass = resolveClass(lang);
  237 |     }
  238 |     return configuration.getLanguageDriver(langClass);
  239 |   }
  240 | 
  241 | }
```
