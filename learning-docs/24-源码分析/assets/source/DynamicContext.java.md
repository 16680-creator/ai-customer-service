<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/xmltags/DynamicContext.java -->
<!-- 正文引用：[[源码:DynamicContext.java#L起-L止]] ；代码块头部：(src: DynamicContext.java#L起-L止) -->

# DynamicContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/xmltags/DynamicContext.java` |
| 行数 | 195 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:16.982Z |

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
   16 | package org.apache.ibatis.scripting.xmltags;
   17 | 
   18 | import java.util.ArrayList;
   19 | import java.util.HashMap;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | import java.util.StringJoiner;
   23 | 
   24 | import org.apache.ibatis.ognl.OgnlContext;
   25 | import org.apache.ibatis.ognl.OgnlRuntime;
   26 | import org.apache.ibatis.ognl.PropertyAccessor;
   27 | 
   28 | import org.apache.ibatis.builder.ParameterMappingTokenHandler;
   29 | import org.apache.ibatis.mapping.ParameterMapping;
   30 | import org.apache.ibatis.parsing.GenericTokenParser;
   31 | import org.apache.ibatis.reflection.MetaObject;
   32 | import org.apache.ibatis.reflection.ParamNameResolver;
   33 | import org.apache.ibatis.session.Configuration;
   34 | 
   35 | /**
   36 |  * @author Clinton Begin
   37 |  */
   38 | public class DynamicContext {
   39 | 
   40 |   public static final String PARAMETER_OBJECT_KEY = "_parameter";
   41 |   public static final String DATABASE_ID_KEY = "_databaseId";
   42 | 
   43 |   static {
   44 |     OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
   45 |   }
   46 | 
   47 |   protected final ContextMap bindings;
   48 |   private final StringJoiner sqlBuilder = new StringJoiner(" ");
   49 | 
   50 |   private final Configuration configuration;
   51 |   private final Object parameterObject;
   52 |   private final Class<?> parameterType;
   53 |   private final ParamNameResolver paramNameResolver;
   54 |   private final boolean paramExists;
   55 | 
   56 |   private GenericTokenParser tokenParser;
   57 |   private ParameterMappingTokenHandler tokenHandler;
   58 | 
   59 |   public DynamicContext(Configuration configuration, Class<?> parameterType, ParamNameResolver paramNameResolver) {
   60 |     this(configuration, null, parameterType, paramNameResolver, false);
   61 |   }
   62 | 
   63 |   public DynamicContext(Configuration configuration, Object parameterObject, Class<?> parameterType,
   64 |       ParamNameResolver paramNameResolver, boolean paramExists) {
   65 |     if (parameterObject == null || parameterObject instanceof Map) {
   66 |       bindings = new ContextMap(null, false);
   67 |     } else {
   68 |       MetaObject metaObject = configuration.newMetaObject(parameterObject);
   69 |       boolean existsTypeHandler = configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass());
   70 |       bindings = new ContextMap(metaObject, existsTypeHandler);
   71 |     }
   72 |     bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
   73 |     bindings.put(DATABASE_ID_KEY, configuration.getDatabaseId());
   74 |     this.configuration = configuration;
   75 |     this.parameterObject = parameterObject;
   76 |     this.paramExists = paramExists;
   77 |     this.parameterType = parameterType;
   78 |     this.paramNameResolver = paramNameResolver;
   79 |   }
   80 | 
   81 |   public Map<String, Object> getBindings() {
   82 |     return bindings;
   83 |   }
   84 | 
   85 |   public void bind(String name, Object value) {
   86 |     bindings.put(name, value);
   87 |   }
   88 | 
   89 |   public void appendSql(String sql) {
   90 |     sqlBuilder.add(sql);
   91 |   }
   92 | 
   93 |   public String getSql() {
   94 |     return sqlBuilder.toString().trim();
   95 |   }
   96 | 
   97 |   private void initTokenParser(List<ParameterMapping> parameterMappings) {
   98 |     if (tokenParser == null) {
   99 |       tokenHandler = new ParameterMappingTokenHandler(parameterMappings != null ? parameterMappings : new ArrayList<>(),
  100 |           configuration, parameterObject, parameterType, bindings, paramNameResolver, paramExists);
  101 |       tokenParser = new GenericTokenParser("#{", "}", tokenHandler);
  102 |     }
  103 |   }
  104 | 
  105 |   public List<ParameterMapping> getParameterMappings() {
  106 |     initTokenParser(null);
  107 |     return tokenHandler.getParameterMappings();
  108 |   }
  109 | 
  110 |   protected String parseParam(String sql) {
  111 |     initTokenParser(getParameterMappings());
  112 |     return tokenParser.parse(sql);
  113 |   }
  114 | 
  115 |   protected Object getParameterObject() {
  116 |     return parameterObject;
  117 |   }
  118 | 
  119 |   protected Class<?> getParameterType() {
  120 |     return parameterType;
  121 |   }
  122 | 
  123 |   protected ParamNameResolver getParamNameResolver() {
  124 |     return paramNameResolver;
  125 |   }
  126 | 
  127 |   protected boolean isParamExists() {
  128 |     return paramExists;
  129 |   }
  130 | 
  131 |   static class ContextMap extends HashMap<String, Object> {
  132 |     private static final long serialVersionUID = 2977601501966151582L;
  133 |     private final MetaObject parameterMetaObject;
  134 |     private final boolean fallbackParameterObject;
  135 | 
  136 |     public ContextMap(MetaObject parameterMetaObject, boolean fallbackParameterObject) {
  137 |       this.parameterMetaObject = parameterMetaObject;
  138 |       this.fallbackParameterObject = fallbackParameterObject;
  139 |     }
  140 | 
  141 |     @Override
  142 |     public Object get(Object key) {
  143 |       String strKey = (String) key;
  144 |       if (super.containsKey(strKey)) {
  145 |         return super.get(strKey);
  146 |       }
  147 | 
  148 |       if (parameterMetaObject == null) {
  149 |         return null;
  150 |       }
  151 | 
  152 |       if (fallbackParameterObject && !parameterMetaObject.hasGetter(strKey)) {
  153 |         return parameterMetaObject.getOriginalObject();
  154 |       }
  155 |       // issue #61 do not modify the context when reading
  156 |       return parameterMetaObject.getValue(strKey);
  157 |     }
  158 |   }
  159 | 
  160 |   static class ContextAccessor implements PropertyAccessor {
  161 | 
  162 |     @Override
  163 |     public Object getProperty(OgnlContext context, Object target, Object name) {
  164 |       Map map = (Map) target;
  165 | 
  166 |       Object result = map.get(name);
  167 |       if (map.containsKey(name) || result != null) {
  168 |         return result;
  169 |       }
  170 | 
  171 |       Object parameterObject = map.get(PARAMETER_OBJECT_KEY);
  172 |       if (parameterObject instanceof Map) {
  173 |         return ((Map) parameterObject).get(name);
  174 |       }
  175 | 
  176 |       return null;
  177 |     }
  178 | 
  179 |     @Override
  180 |     public void setProperty(OgnlContext context, Object target, Object name, Object value) {
  181 |       Map<Object, Object> map = (Map<Object, Object>) target;
  182 |       map.put(name, value);
  183 |     }
  184 | 
  185 |     @Override
  186 |     public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
  187 |       return null;
  188 |     }
  189 | 
  190 |     @Override
  191 |     public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
  192 |       return null;
  193 |     }
  194 |   }
  195 | }
```
