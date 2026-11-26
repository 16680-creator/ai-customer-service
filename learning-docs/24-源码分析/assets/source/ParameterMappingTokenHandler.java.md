<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/builder/ParameterMappingTokenHandler.java -->
<!-- 正文引用：[[源码:ParameterMappingTokenHandler.java#L起-L止]] ；代码块头部：(src: ParameterMappingTokenHandler.java#L起-L止) -->

# ParameterMappingTokenHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/builder/ParameterMappingTokenHandler.java` |
| 行数 | 196 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:03:14.820Z |

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
   16 | package org.apache.ibatis.builder;
   17 | 
   18 | import java.lang.reflect.Type;
   19 | import java.sql.ResultSet;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | import java.util.Map.Entry;
   23 | 
   24 | import org.apache.ibatis.binding.MapperMethod.ParamMap;
   25 | import org.apache.ibatis.mapping.ParameterMapping;
   26 | import org.apache.ibatis.mapping.ParameterMode;
   27 | import org.apache.ibatis.parsing.TokenHandler;
   28 | import org.apache.ibatis.reflection.MetaClass;
   29 | import org.apache.ibatis.reflection.MetaObject;
   30 | import org.apache.ibatis.reflection.ParamNameResolver;
   31 | import org.apache.ibatis.reflection.property.PropertyTokenizer;
   32 | import org.apache.ibatis.session.Configuration;
   33 | import org.apache.ibatis.type.JdbcType;
   34 | import org.apache.ibatis.type.TypeHandler;
   35 | 
   36 | public class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {
   37 | 
   38 |   private static final String PARAMETER_PROPERTIES = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";
   39 |   private final List<ParameterMapping> parameterMappings;
   40 |   private final Class<?> parameterType;
   41 |   private final MetaObject metaParameters;
   42 |   private final Object parameterObject;
   43 |   private final boolean paramExists;
   44 |   private final ParamNameResolver paramNameResolver;
   45 | 
   46 |   private Type genericType = null;
   47 |   private TypeHandler<?> typeHandler = null;
   48 | 
   49 |   public ParameterMappingTokenHandler(List<ParameterMapping> parameterMappings, Configuration configuration,
   50 |       Object parameterObject, Class<?> parameterType, Map<String, Object> additionalParameters,
   51 |       ParamNameResolver paramNameResolver, boolean paramExists) {
   52 |     super(configuration);
   53 |     this.parameterType = parameterObject == null ? (parameterType == null ? Object.class : parameterType)
   54 |         : parameterObject.getClass();
   55 |     this.metaParameters = configuration.newMetaObject(additionalParameters);
   56 |     this.parameterObject = parameterObject;
   57 |     this.paramExists = paramExists;
   58 |     this.parameterMappings = parameterMappings;
   59 |     this.paramNameResolver = paramNameResolver;
   60 |   }
   61 | 
   62 |   public ParameterMappingTokenHandler(List<ParameterMapping> parameterMappings, Configuration configuration,
   63 |       Class<?> parameterType, Map<String, Object> additionalParameters, ParamNameResolver paramNameResolver) {
   64 |     super(configuration);
   65 |     this.parameterType = parameterType;
   66 |     this.metaParameters = configuration.newMetaObject(additionalParameters);
   67 |     this.parameterObject = null;
   68 |     this.paramExists = false;
   69 |     this.parameterMappings = parameterMappings;
   70 |     this.paramNameResolver = paramNameResolver;
   71 |   }
   72 | 
   73 |   public List<ParameterMapping> getParameterMappings() {
   74 |     return parameterMappings;
   75 |   }
   76 | 
   77 |   @Override
   78 |   public String handleToken(String content) {
   79 |     genericType = null;
   80 |     typeHandler = null;
   81 |     parameterMappings.add(buildParameterMapping(content));
   82 |     return "?";
   83 |   }
   84 | 
   85 |   private ParameterMapping buildParameterMapping(String content) {
   86 |     Map<String, String> propertiesMap = parseParameterMapping(content);
   87 | 
   88 |     final String property = propertiesMap.remove("property");
   89 |     final JdbcType jdbcType = resolveJdbcType(propertiesMap.remove("jdbcType"));
   90 |     final String typeHandlerAlias = propertiesMap.remove("typeHandler");
   91 | 
   92 |     ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, (Class<?>) null);
   93 |     PropertyTokenizer propertyTokenizer = new PropertyTokenizer(property);
   94 |     builder.jdbcType(jdbcType);
   95 |     final Class<?> javaType = figureOutJavaType(propertiesMap, property, propertyTokenizer, jdbcType);
   96 |     builder.javaType(javaType);
   97 |     if (genericType == null) {
   98 |       genericType = javaType;
   99 |     }
  100 |     if (typeHandler == null || typeHandlerAlias != null) {
  101 |       typeHandler = resolveTypeHandler(genericType, jdbcType, typeHandlerAlias);
  102 |     }
  103 |     builder.typeHandler(typeHandler);
  104 | 
  105 |     ParameterMode mode = null;
  106 |     for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
  107 |       String name = entry.getKey();
  108 |       String value = entry.getValue();
  109 |       if ("mode".equals(name)) {
  110 |         mode = resolveParameterMode(value);
  111 |         builder.mode(mode);
  112 |       } else if ("numericScale".equals(name)) {
  113 |         builder.numericScale(Integer.valueOf(value));
  114 |       } else if ("resultMap".equals(name)) {
  115 |         builder.resultMapId(value);
  116 |       } else if ("jdbcTypeName".equals(name)) {
  117 |         builder.jdbcTypeName(value);
  118 |       } else if ("expression".equals(name)) {
  119 |         throw new BuilderException("Expression based parameters are not supported yet");
  120 |       } else {
  121 |         throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content
  122 |             + "}.  Valid properties are " + PARAMETER_PROPERTIES);
  123 |       }
  124 |     }
  125 |     if (!ParameterMode.OUT.equals(mode) && paramExists) {
  126 |       if (metaParameters.hasGetter(propertyTokenizer.getName())) {
  127 |         builder.value(metaParameters.getValue(property));
  128 |       } else if (parameterObject == null) {
  129 |         builder.value(null);
  130 |       } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
  131 |         builder.value(parameterObject);
  132 |       } else {
  133 |         MetaObject metaObject = configuration.newMetaObject(parameterObject);
  134 |         builder.value(metaObject.getValue(property));
  135 |       }
  136 |     }
  137 |     return builder.build();
  138 |   }
  139 | 
  140 |   private Class<?> figureOutJavaType(Map<String, String> propertiesMap, String property,
  141 |       PropertyTokenizer propertyTokenizer, JdbcType jdbcType) {
  142 |     Class<?> javaType = resolveClass(propertiesMap.remove("javaType"));
  143 |     if (javaType != null) {
  144 |       return javaType;
  145 |     }
  146 |     if (metaParameters.hasGetter(propertyTokenizer.getName())) { // issue #448 get type from additional params
  147 |       return metaParameters.getGetterType(property);
  148 |     }
  149 |     typeHandler = resolveTypeHandler(parameterType, jdbcType, (Class<? extends TypeHandler<?>>) null);
  150 |     if (typeHandler != null) {
  151 |       return parameterType;
  152 |     }
  153 |     if (JdbcType.CURSOR.equals(jdbcType)) {
  154 |       return ResultSet.class;
  155 |     }
  156 |     if (paramNameResolver != null && ParamMap.class.equals(parameterType)) {
  157 |       Type actualParamType = paramNameResolver.getType(property);
  158 |       if (actualParamType instanceof Type) {
  159 |         MetaClass metaClass = MetaClass.forClass(actualParamType, configuration.getReflectorFactory());
  160 |         String multiParamsPropertyName;
  161 |         if (propertyTokenizer.hasNext()) {
  162 |           multiParamsPropertyName = propertyTokenizer.getChildren();
  163 |           if (metaClass.hasGetter(multiParamsPropertyName)) {
  164 |             Entry<Type, Class<?>> getterType = metaClass.getGenericGetterType(multiParamsPropertyName);
  165 |             genericType = getterType.getKey();
  166 |             return getterType.getValue();
  167 |           }
  168 |         } else {
  169 |           genericType = actualParamType;
  170 |         }
  171 |       }
  172 |       return Object.class;
  173 |     }
  174 |     if (Map.class.isAssignableFrom(parameterType)) {
  175 |       return Object.class;
  176 |     }
  177 |     MetaClass metaClass = MetaClass.forClass(parameterType, configuration.getReflectorFactory());
  178 |     if (metaClass.hasGetter(property)) {
  179 |       Entry<Type, Class<?>> getterType = metaClass.getGenericGetterType(property);
  180 |       genericType = getterType.getKey();
  181 |       return getterType.getValue();
  182 |     }
  183 |     return Object.class;
  184 |   }
  185 | 
  186 |   private Map<String, String> parseParameterMapping(String content) {
  187 |     try {
  188 |       return new ParameterExpression(content);
  189 |     } catch (BuilderException ex) {
  190 |       throw ex;
  191 |     } catch (Exception ex) {
  192 |       throw new BuilderException("Parsing error was found in mapping #{" + content
  193 |           + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
  194 |     }
  195 |   }
  196 | }
```
