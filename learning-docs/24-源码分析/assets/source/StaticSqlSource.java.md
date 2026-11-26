<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/builder/StaticSqlSource.java -->
<!-- 正文引用：[[源码:StaticSqlSource.java#L起-L止]] ；代码块头部：(src: StaticSqlSource.java#L起-L止) -->

# StaticSqlSource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/builder/StaticSqlSource.java` |
| 行数 | 49 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:03:15.045Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2022 the original author or authors.
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
   18 | import java.util.List;
   19 | 
   20 | import org.apache.ibatis.mapping.BoundSql;
   21 | import org.apache.ibatis.mapping.ParameterMapping;
   22 | import org.apache.ibatis.mapping.SqlSource;
   23 | import org.apache.ibatis.session.Configuration;
   24 | 
   25 | /**
   26 |  * @author Clinton Begin
   27 |  */
   28 | public class StaticSqlSource implements SqlSource {
   29 | 
   30 |   private final String sql;
   31 |   private final List<ParameterMapping> parameterMappings;
   32 |   private final Configuration configuration;
   33 | 
   34 |   public StaticSqlSource(Configuration configuration, String sql) {
   35 |     this(configuration, sql, null);
   36 |   }
   37 | 
   38 |   public StaticSqlSource(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
   39 |     this.sql = sql;
   40 |     this.parameterMappings = parameterMappings;
   41 |     this.configuration = configuration;
   42 |   }
   43 | 
   44 |   @Override
   45 |   public BoundSql getBoundSql(Object parameterObject) {
   46 |     return new BoundSql(configuration, sql, parameterMappings, parameterObject);
   47 |   }
   48 | 
   49 | }
```
