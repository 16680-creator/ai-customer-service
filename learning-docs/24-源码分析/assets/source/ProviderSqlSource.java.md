<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/builder/annotation/ProviderSqlSource.java -->
<!-- 正文引用：[[源码:ProviderSqlSource.java#L起-L止]] ；代码块头部：(src: ProviderSqlSource.java#L起-L止) -->

# ProviderSqlSource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/builder/annotation/ProviderSqlSource.java` |
| 行数 | 271 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:08:51.664Z |

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
   16 | package org.apache.ibatis.builder.annotation;
   17 | 
   18 | import java.lang.annotation.Annotation;
   19 | import java.lang.reflect.InvocationTargetException;
   20 | import java.lang.reflect.Method;
   21 | import java.lang.reflect.Modifier;
   22 | import java.util.Map;
   23 | 
   24 | import org.apache.ibatis.annotations.Lang;
   25 | import org.apache.ibatis.builder.BuilderException;
   26 | import org.apache.ibatis.mapping.BoundSql;
   27 | import org.apache.ibatis.mapping.SqlSource;
   28 | import org.apache.ibatis.reflection.ParamNameResolver;
   29 | import org.apache.ibatis.scripting.LanguageDriver;
   30 | import org.apache.ibatis.session.Configuration;
   31 | 
   32 | /**
   33 |  * @author Clinton Begin
   34 |  * @author Kazuki Shimizu
   35 |  */
   36 | public class ProviderSqlSource implements SqlSource {
   37 | 
   38 |   private final Configuration configuration;
   39 |   private final Class<?> providerType;
   40 |   private final LanguageDriver languageDriver;
   41 |   private final Method mapperMethod;
   42 |   private final Method providerMethod;
   43 |   private final ParamNameResolver paramNameResolver;
   44 |   private final Class<?>[] providerMethodParameterTypes;
   45 |   private final ProviderContext providerContext;
   46 |   private final Integer providerContextIndex;
   47 | 
   48 |   /**
   49 |    * This constructor will remove at a future version.
   50 |    *
   51 |    * @param configuration
   52 |    *          the configuration
   53 |    * @param provider
   54 |    *          the provider
   55 |    *
   56 |    * @deprecated Since 3.5.3, Please use the {@link #ProviderSqlSource(Configuration, Annotation, Class, Method)}
   57 |    *             instead of this.
   58 |    */
   59 |   @Deprecated
   60 |   public ProviderSqlSource(Configuration configuration, Object provider) {
   61 |     this(configuration, provider, null, null);
   62 |   }
   63 | 
   64 |   /**
   65 |    * This constructor will remove at a future version.
   66 |    *
   67 |    * @param configuration
   68 |    *          the configuration
   69 |    * @param provider
   70 |    *          the provider
   71 |    * @param mapperType
   72 |    *          the mapper type
   73 |    * @param mapperMethod
   74 |    *          the mapper method
   75 |    *
   76 |    * @since 3.4.5
   77 |    *
   78 |    * @deprecated Since 3.5.3, Please use the {@link #ProviderSqlSource(Configuration, Annotation, Class, Method)}
   79 |    *             instead of this.
   80 |    */
   81 |   @Deprecated
   82 |   public ProviderSqlSource(Configuration configuration, Object provider, Class<?> mapperType, Method mapperMethod) {
   83 |     this(configuration, (Annotation) provider, mapperType, mapperMethod);
   84 |   }
   85 | 
   86 |   /**
   87 |    * Instantiates a new provider sql source.
   88 |    *
   89 |    * @param configuration
   90 |    *          the configuration
   91 |    * @param provider
   92 |    *          the provider
   93 |    * @param mapperType
   94 |    *          the mapper type
   95 |    * @param mapperMethod
   96 |    *          the mapper method
   97 |    *
   98 |    * @since 3.5.3
   99 |    */
  100 |   public ProviderSqlSource(Configuration configuration, Annotation provider, Class<?> mapperType, Method mapperMethod) {
  101 |     String candidateProviderMethodName;
  102 |     Method candidateProviderMethod = null;
  103 |     try {
  104 |       this.configuration = configuration;
  105 |       this.mapperMethod = mapperMethod;
  106 |       Lang lang = mapperMethod == null ? null : mapperMethod.getAnnotation(Lang.class);
  107 |       this.languageDriver = configuration.getLanguageDriver(lang == null ? null : lang.value());
  108 |       this.providerType = getProviderType(configuration, provider, mapperMethod);
  109 |       candidateProviderMethodName = (String) provider.annotationType().getMethod("method").invoke(provider);
  110 | 
  111 |       if (candidateProviderMethodName.length() == 0
  112 |           && ProviderMethodResolver.class.isAssignableFrom(this.providerType)) {
  113 |         candidateProviderMethod = ((ProviderMethodResolver) this.providerType.getDeclaredConstructor().newInstance())
  114 |             .resolveMethod(new ProviderContext(mapperType, mapperMethod, configuration.getDatabaseId()));
  115 |       }
  116 |       if (candidateProviderMethod == null) {
  117 |         candidateProviderMethodName = candidateProviderMethodName.length() == 0 ? "provideSql"
  118 |             : candidateProviderMethodName;
  119 |         for (Method m : this.providerType.getMethods()) {
  120 |           if (candidateProviderMethodName.equals(m.getName())
  121 |               && CharSequence.class.isAssignableFrom(m.getReturnType())) {
  122 |             if (candidateProviderMethod != null) {
  123 |               throw new BuilderException("Error creating SqlSource for SqlProvider. Method '"
  124 |                   + candidateProviderMethodName + "' is found multiple in SqlProvider '" + this.providerType.getName()
  125 |                   + "'. Sql provider method can not overload.");
  126 |             }
  127 |             candidateProviderMethod = m;
  128 |           }
  129 |         }
  130 |       }
  131 |     } catch (BuilderException e) {
  132 |       throw e;
  133 |     } catch (Exception e) {
  134 |       throw new BuilderException("Error creating SqlSource for SqlProvider.  Cause: " + e, e);
  135 |     }
  136 |     if (candidateProviderMethod == null) {
  137 |       throw new BuilderException("Error creating SqlSource for SqlProvider. Method '" + candidateProviderMethodName
  138 |           + "' not found in SqlProvider '" + this.providerType.getName() + "'.");
  139 |     }
  140 |     this.providerMethod = candidateProviderMethod;
  141 |     this.paramNameResolver = new ParamNameResolver(configuration, this.providerMethod, mapperType);
  142 |     this.providerMethodParameterTypes = this.providerMethod.getParameterTypes();
  143 | 
  144 |     ProviderContext candidateProviderContext = null;
  145 |     Integer candidateProviderContextIndex = null;
  146 |     for (int i = 0; i < this.providerMethodParameterTypes.length; i++) {
  147 |       Class<?> parameterType = this.providerMethodParameterTypes[i];
  148 |       if (parameterType == ProviderContext.class) {
  149 |         if (candidateProviderContext != null) {
  150 |           throw new BuilderException(
  151 |               "Error creating SqlSource for SqlProvider. ProviderContext found multiple in SqlProvider method ("
  152 |                   + this.providerType.getName() + "." + providerMethod.getName()
  153 |                   + "). ProviderContext can not define multiple in SqlProvider method argument.");
  154 |         }
  155 |         candidateProviderContext = new ProviderContext(mapperType, mapperMethod, configuration.getDatabaseId());
  156 |         candidateProviderContextIndex = i;
  157 |       }
  158 |     }
  159 |     this.providerContext = candidateProviderContext;
  160 |     this.providerContextIndex = candidateProviderContextIndex;
  161 |   }
  162 | 
  163 |   @Override
  164 |   public BoundSql getBoundSql(Object parameterObject) {
  165 |     SqlSource sqlSource = createSqlSource(parameterObject);
  166 |     return sqlSource.getBoundSql(parameterObject);
  167 |   }
  168 | 
  169 |   private SqlSource createSqlSource(Object parameterObject) {
  170 |     try {
  171 |       String sql;
  172 |       if (parameterObject instanceof Map) {
  173 |         int bindParameterCount = providerMethodParameterTypes.length - (providerContext == null ? 0 : 1);
  174 |         if (bindParameterCount == 1
  175 |             && providerMethodParameterTypes[Integer.valueOf(0).equals(providerContextIndex) ? 1 : 0]
  176 |                 .isAssignableFrom(parameterObject.getClass())) {
  177 |           sql = invokeProviderMethod(extractProviderMethodArguments(parameterObject));
  178 |         } else {
  179 |           @SuppressWarnings("unchecked")
  180 |           Map<String, Object> params = (Map<String, Object>) parameterObject;
  181 |           sql = invokeProviderMethod(extractProviderMethodArguments(params, paramNameResolver.getNames()));
  182 |         }
  183 |       } else {
  184 |         switch (providerMethodParameterTypes.length) {
  185 |           case 0:
  186 |             sql = invokeProviderMethod();
  187 |             break;
  188 |           case 1:
  189 |             if (providerContext == null) {
  190 |               sql = invokeProviderMethod(parameterObject);
  191 |             } else {
  192 |               sql = invokeProviderMethod(providerContext);
  193 |             }
  194 |             break;
  195 |           case 2:
  196 |             sql = invokeProviderMethod(extractProviderMethodArguments(parameterObject));
  197 |             break;
  198 |           default:
  199 |             throw new BuilderException("Cannot invoke SqlProvider method '" + providerMethod
  200 |                 + "' with specify parameter '" + (parameterObject == null ? null : parameterObject.getClass())
  201 |                 + "' because SqlProvider method arguments for '" + mapperMethod + "' is an invalid combination.");
  202 |         }
  203 |       }
  204 |       Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
  205 |       return languageDriver.createSqlSource(configuration, sql, parameterType, paramNameResolver);
  206 |     } catch (BuilderException e) {
  207 |       throw e;
  208 |     } catch (Exception e) {
  209 |       throw new BuilderException("Error invoking SqlProvider method '" + providerMethod + "' with specify parameter '"
  210 |           + (parameterObject == null ? null : parameterObject.getClass()) + "'.  Cause: " + extractRootCause(e), e);
  211 |     }
  212 |   }
  213 | 
  214 |   private Throwable extractRootCause(Exception e) {
  215 |     Throwable cause = e;
  216 |     while (cause.getCause() != null) {
  217 |       cause = cause.getCause();
  218 |     }
  219 |     return cause;
  220 |   }
  221 | 
  222 |   private Object[] extractProviderMethodArguments(Object parameterObject) {
  223 |     if (providerContext != null) {
  224 |       Object[] args = new Object[2];
  225 |       args[providerContextIndex == 0 ? 1 : 0] = parameterObject;
  226 |       args[providerContextIndex] = providerContext;
  227 |       return args;
  228 |     }
  229 |     return new Object[] { parameterObject };
  230 |   }
  231 | 
  232 |   private Object[] extractProviderMethodArguments(Map<String, Object> params, String[] argumentNames) {
  233 |     Object[] args = new Object[argumentNames.length];
  234 |     for (int i = 0; i < args.length; i++) {
  235 |       if (providerContextIndex != null && providerContextIndex == i) {
  236 |         args[i] = providerContext;
  237 |       } else {
  238 |         args[i] = params.get(argumentNames[i]);
  239 |       }
  240 |     }
  241 |     return args;
  242 |   }
  243 | 
  244 |   private String invokeProviderMethod(Object... args) throws Exception {
  245 |     Object targetObject = null;
  246 |     if (!Modifier.isStatic(providerMethod.getModifiers())) {
  247 |       targetObject = providerType.getDeclaredConstructor().newInstance();
  248 |     }
  249 |     CharSequence sql = (CharSequence) providerMethod.invoke(targetObject, args);
  250 |     return sql != null ? sql.toString() : null;
  251 |   }
  252 | 
  253 |   private Class<?> getProviderType(Configuration configuration, Annotation providerAnnotation, Method mapperMethod)
  254 |       throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
  255 |     Class<?> type = (Class<?>) providerAnnotation.annotationType().getMethod("type").invoke(providerAnnotation);
  256 |     Class<?> value = (Class<?>) providerAnnotation.annotationType().getMethod("value").invoke(providerAnnotation);
  257 |     if (value == void.class && type == void.class) {
  258 |       if (configuration.getDefaultSqlProviderType() != null) {
  259 |         return configuration.getDefaultSqlProviderType();
  260 |       }
  261 |       throw new BuilderException("Please specify either 'value' or 'type' attribute of @"
  262 |           + providerAnnotation.annotationType().getSimpleName() + " at the '" + mapperMethod.toString() + "'.");
  263 |     }
  264 |     if (value != void.class && type != void.class && value != type) {
  265 |       throw new BuilderException("Cannot specify different class on 'value' and 'type' attribute of @"
  266 |           + providerAnnotation.annotationType().getSimpleName() + " at the '" + mapperMethod.toString() + "'.");
  267 |     }
  268 |     return value == void.class ? type : value;
  269 |   }
  270 | 
  271 | }
```
