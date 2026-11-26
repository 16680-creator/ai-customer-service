<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/defaults/DefaultParameterHandler.java -->
<!-- 正文引用：[[源码:DefaultParameterHandler.java#L起-L止]] ；代码块头部：(src: DefaultParameterHandler.java#L起-L止) -->

# DefaultParameterHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/defaults/DefaultParameterHandler.java` |
| 行数 | 205 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:18.921Z |

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
   16 | package org.apache.ibatis.scripting.defaults;
   17 | 
   18 | import java.lang.reflect.Type;
   19 | import java.sql.ParameterMetaData;
   20 | import java.sql.PreparedStatement;
   21 | import java.sql.SQLException;
   22 | import java.util.HashMap;
   23 | import java.util.List;
   24 | import java.util.Map.Entry;
   25 | 
   26 | import org.apache.ibatis.binding.MapperMethod.ParamMap;
   27 | import org.apache.ibatis.executor.ErrorContext;
   28 | import org.apache.ibatis.executor.parameter.ParameterHandler;
   29 | import org.apache.ibatis.mapping.BoundSql;
   30 | import org.apache.ibatis.mapping.MappedStatement;
   31 | import org.apache.ibatis.mapping.ParameterMapping;
   32 | import org.apache.ibatis.mapping.ParameterMode;
   33 | import org.apache.ibatis.reflection.MetaClass;
   34 | import org.apache.ibatis.reflection.MetaObject;
   35 | import org.apache.ibatis.reflection.ParamNameResolver;
   36 | import org.apache.ibatis.reflection.property.PropertyTokenizer;
   37 | import org.apache.ibatis.session.Configuration;
   38 | import org.apache.ibatis.type.JdbcType;
   39 | import org.apache.ibatis.type.ObjectTypeHandler;
   40 | import org.apache.ibatis.type.TypeException;
   41 | import org.apache.ibatis.type.TypeHandler;
   42 | import org.apache.ibatis.type.TypeHandlerRegistry;
   43 | 
   44 | /**
   45 |  * @author Clinton Begin
   46 |  * @author Eduardo Macarron
   47 |  */
   48 | public class DefaultParameterHandler implements ParameterHandler {
   49 | 
   50 |   private final TypeHandlerRegistry typeHandlerRegistry;
   51 | 
   52 |   private final MappedStatement mappedStatement;
   53 |   private final Object parameterObject;
   54 |   private final BoundSql boundSql;
   55 |   private final Configuration configuration;
   56 | 
   57 |   private ParameterMetaData paramMetaData;
   58 |   private MetaObject paramMetaObject;
   59 |   private HashMap<Class<?>, MetaClass> metaClassCache = new HashMap<>();
   60 |   private static final ParameterMetaData NULL_PARAM_METADATA = new ParameterMetaData() {
   61 |     // @formatter:off
   62 |     public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
   63 |     public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
   64 |     public boolean isSigned(int param) throws SQLException { return false; }
   65 |     public int isNullable(int param) throws SQLException { return 0; }
   66 |     public int getScale(int param) throws SQLException { return 0; }
   67 |     public int getPrecision(int param) throws SQLException { return 0; }
   68 |     public String getParameterTypeName(int param) throws SQLException { return null; }
   69 |     public int getParameterType(int param) throws SQLException { return 0; }
   70 |     public int getParameterMode(int param) throws SQLException { return 0; }
   71 |     public int getParameterCount() throws SQLException { return 0; }
   72 |     public String getParameterClassName(int param) throws SQLException { return null; }
   73 |     // @formatter:on
   74 |   };
   75 | 
   76 |   public DefaultParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
   77 |     this.mappedStatement = mappedStatement;
   78 |     this.configuration = mappedStatement.getConfiguration();
   79 |     this.typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
   80 |     this.parameterObject = parameterObject;
   81 |     this.boundSql = boundSql;
   82 |   }
   83 | 
   84 |   @Override
   85 |   public Object getParameterObject() {
   86 |     return parameterObject;
   87 |   }
   88 | 
   89 |   @SuppressWarnings({ "rawtypes", "unchecked" })
   90 |   @Override
   91 |   public void setParameters(PreparedStatement ps) {
   92 |     ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
   93 |     List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
   94 |     if (parameterMappings != null) {
   95 |       ParamNameResolver paramNameResolver = mappedStatement.getParamNameResolver();
   96 |       for (int i = 0; i < parameterMappings.size(); i++) {
   97 |         ParameterMapping parameterMapping = parameterMappings.get(i);
   98 |         if (parameterMapping.getMode() != ParameterMode.OUT) {
   99 |           Object value;
  100 |           String propertyName = parameterMapping.getProperty();
  101 |           JdbcType jdbcType = parameterMapping.getJdbcType();
  102 |           JdbcType actualJdbcType = jdbcType == null ? getParamJdbcType(ps, i + 1) : jdbcType;
  103 |           Type propertyGenericType = null;
  104 |           TypeHandler typeHandler = parameterMapping.getTypeHandler();
  105 |           if (parameterMapping.hasValue()) {
  106 |             value = parameterMapping.getValue();
  107 |           } else if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
  108 |             value = boundSql.getAdditionalParameter(propertyName);
  109 |           } else if (parameterObject == null) {
  110 |             value = null;
  111 |           } else {
  112 |             Class<? extends Object> parameterClass = parameterObject.getClass();
  113 |             TypeHandler paramTypeHandler = typeHandlerRegistry.getTypeHandler(parameterClass, actualJdbcType);
  114 |             if (paramTypeHandler != null) {
  115 |               value = parameterObject;
  116 |               typeHandler = paramTypeHandler;
  117 |             } else {
  118 |               paramMetaObject = getParamMetaObject();
  119 |               value = paramMetaObject.getValue(propertyName);
  120 |               if (typeHandler == null && value != null) {
  121 |                 if (paramNameResolver != null && ParamMap.class.equals(parameterClass)) {
  122 |                   Type actualParamType = paramNameResolver.getType(propertyName);
  123 |                   if (actualParamType instanceof Class) {
  124 |                     Class<?> actualParamClass = (Class<?>) actualParamType;
  125 |                     MetaClass metaClass = metaClassCache.computeIfAbsent(actualParamClass,
  126 |                         k -> MetaClass.forClass(k, configuration.getReflectorFactory()));
  127 |                     PropertyTokenizer propertyTokenizer = new PropertyTokenizer(propertyName);
  128 |                     String multiParamsPropertyName;
  129 |                     if (propertyTokenizer.hasNext()) {
  130 |                       multiParamsPropertyName = propertyTokenizer.getChildren();
  131 |                       if (metaClass.hasGetter(multiParamsPropertyName)) {
  132 |                         Entry<Type, Class<?>> getterType = metaClass.getGenericGetterType(multiParamsPropertyName);
  133 |                         propertyGenericType = getterType.getKey();
  134 |                       }
  135 |                     } else {
  136 |                       propertyGenericType = actualParamClass;
  137 |                     }
  138 |                   } else {
  139 |                     propertyGenericType = actualParamType;
  140 |                   }
  141 |                 } else {
  142 |                   try {
  143 |                     propertyGenericType = paramMetaObject.getGenericGetterType(propertyName).getKey();
  144 |                     typeHandler = configuration.getTypeHandlerRegistry().getTypeHandler(propertyGenericType,
  145 |                         actualJdbcType, null);
  146 |                   } catch (Exception e) {
  147 |                     // Not always resolvable
  148 |                   }
  149 |                 }
  150 |               }
  151 |             }
  152 |           }
  153 |           if (value == null) {
  154 |             if (jdbcType == null) {
  155 |               jdbcType = configuration.getJdbcTypeForNull();
  156 |             }
  157 |             if (typeHandler == null) {
  158 |               typeHandler = ObjectTypeHandler.INSTANCE;
  159 |             }
  160 |           } else if (typeHandler == null) {
  161 |             if (propertyGenericType == null) {
  162 |               propertyGenericType = value.getClass();
  163 |             }
  164 |             typeHandler = typeHandlerRegistry.getTypeHandler(propertyGenericType, actualJdbcType, null);
  165 |           }
  166 |           if (typeHandler == null) {
  167 |             typeHandler = typeHandlerRegistry.getTypeHandler(actualJdbcType);
  168 |           }
  169 |           if (typeHandler == null) {
  170 |             throw new TypeException("Could not find type handler for Java type '" + propertyGenericType.getTypeName()
  171 |                 + "' nor JDBC type '" + actualJdbcType + "'");
  172 |           }
  173 |           try {
  174 |             typeHandler.setParameter(ps, i + 1, value, jdbcType);
  175 |           } catch (TypeException | SQLException e) {
  176 |             throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
  177 |           }
  178 |         }
  179 |       }
  180 |     }
  181 |   }
  182 | 
  183 |   private MetaObject getParamMetaObject() {
  184 |     if (paramMetaObject != null) {
  185 |       return paramMetaObject;
  186 |     }
  187 |     paramMetaObject = configuration.newMetaObject(parameterObject);
  188 |     return paramMetaObject;
  189 |   }
  190 | 
  191 |   private JdbcType getParamJdbcType(PreparedStatement ps, int paramIndex) {
  192 |     try {
  193 |       if (paramMetaData == null) {
  194 |         paramMetaData = ps.getParameterMetaData();
  195 |       }
  196 |       return paramMetaData == NULL_PARAM_METADATA ? null : JdbcType.forCode(paramMetaData.getParameterType(paramIndex));
  197 |     } catch (SQLException e) {
  198 |       if (paramMetaData == null) {
  199 |         paramMetaData = NULL_PARAM_METADATA;
  200 |       }
  201 |       return null;
  202 |     }
  203 |   }
  204 | 
  205 | }
```
