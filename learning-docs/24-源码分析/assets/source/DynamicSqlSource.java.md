<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/xmltags/DynamicSqlSource.java -->
<!-- 正文引用：[[源码:DynamicSqlSource.java#L起-L止]] ；代码块头部：(src: DynamicSqlSource.java#L起-L止) -->

# DynamicSqlSource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/xmltags/DynamicSqlSource.java` |
| 行数 | 54 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:22.553Z |

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
   18 | import org.apache.ibatis.builder.SqlSourceBuilder;
   19 | import org.apache.ibatis.mapping.BoundSql;
   20 | import org.apache.ibatis.mapping.SqlSource;
   21 | import org.apache.ibatis.reflection.ParamNameResolver;
   22 | import org.apache.ibatis.session.Configuration;
   23 | 
   24 | /**
   25 |  * @author Clinton Begin
   26 |  */
   27 | public class DynamicSqlSource implements SqlSource {
   28 | 
   29 |   private final Configuration configuration;
   30 |   private final SqlNode rootSqlNode;
   31 |   private final ParamNameResolver paramNameResolver;
   32 | 
   33 |   public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
   34 |     this(configuration, rootSqlNode, null);
   35 |   }
   36 | 
   37 |   public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode, ParamNameResolver paramNameResolver) {
   38 |     this.configuration = configuration;
   39 |     this.rootSqlNode = rootSqlNode;
   40 |     this.paramNameResolver = paramNameResolver;
   41 |   }
   42 | 
   43 |   @Override
   44 |   public BoundSql getBoundSql(Object parameterObject) {
   45 |     DynamicContext context = new DynamicContext(configuration, parameterObject, null, paramNameResolver, true);
   46 |     rootSqlNode.apply(context);
   47 |     String sql = context.getSql();
   48 |     SqlSource sqlSource = SqlSourceBuilder.buildSqlSource(configuration, sql, context.getParameterMappings());
   49 |     BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
   50 |     context.getBindings().forEach(boundSql::setAdditionalParameter);
   51 |     return boundSql;
   52 |   }
   53 | 
   54 | }
```
