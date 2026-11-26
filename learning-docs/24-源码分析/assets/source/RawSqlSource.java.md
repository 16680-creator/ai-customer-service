<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/defaults/RawSqlSource.java -->
<!-- 正文引用：[[源码:RawSqlSource.java#L起-L止]] ；代码块头部：(src: RawSqlSource.java#L起-L止) -->

# RawSqlSource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/defaults/RawSqlSource.java` |
| 行数 | 76 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:18.480Z |

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
   18 | import java.util.ArrayList;
   19 | import java.util.HashMap;
   20 | import java.util.List;
   21 | 
   22 | import org.apache.ibatis.builder.ParameterMappingTokenHandler;
   23 | import org.apache.ibatis.builder.SqlSourceBuilder;
   24 | import org.apache.ibatis.mapping.BoundSql;
   25 | import org.apache.ibatis.mapping.ParameterMapping;
   26 | import org.apache.ibatis.mapping.SqlSource;
   27 | import org.apache.ibatis.parsing.GenericTokenParser;
   28 | import org.apache.ibatis.reflection.ParamNameResolver;
   29 | import org.apache.ibatis.scripting.xmltags.DynamicContext;
   30 | import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
   31 | import org.apache.ibatis.scripting.xmltags.SqlNode;
   32 | import org.apache.ibatis.session.Configuration;
   33 | 
   34 | /**
   35 |  * Static SqlSource. It is faster than {@link DynamicSqlSource} because mappings are calculated during startup.
   36 |  *
   37 |  * @since 3.2.0
   38 |  *
   39 |  * @author Eduardo Macarron
   40 |  */
   41 | public class RawSqlSource implements SqlSource {
   42 | 
   43 |   private final SqlSource sqlSource;
   44 | 
   45 |   public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
   46 |     this(configuration, rootSqlNode, parameterType, null);
   47 |   }
   48 | 
   49 |   public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType,
   50 |       ParamNameResolver paramNameResolver) {
   51 |     DynamicContext context = new DynamicContext(configuration, parameterType, paramNameResolver);
   52 |     rootSqlNode.apply(context);
   53 |     String sql = context.getSql();
   54 |     sqlSource = SqlSourceBuilder.buildSqlSource(configuration, sql, context.getParameterMappings());
   55 |   }
   56 | 
   57 |   public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
   58 |     this(configuration, sql, parameterType, null);
   59 |   }
   60 | 
   61 |   public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType,
   62 |       ParamNameResolver paramNameResolver) {
   63 |     Class<?> clazz = parameterType == null ? Object.class : parameterType;
   64 |     List<ParameterMapping> parameterMappings = new ArrayList<>();
   65 |     ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler(parameterMappings, configuration,
   66 |         clazz, new HashMap<>(), paramNameResolver);
   67 |     GenericTokenParser parser = new GenericTokenParser("#{", "}", tokenHandler);
   68 |     sqlSource = SqlSourceBuilder.buildSqlSource(configuration, parser.parse(sql), parameterMappings);
   69 |   }
   70 | 
   71 |   @Override
   72 |   public BoundSql getBoundSql(Object parameterObject) {
   73 |     return sqlSource.getBoundSql(parameterObject);
   74 |   }
   75 | 
   76 | }
```
