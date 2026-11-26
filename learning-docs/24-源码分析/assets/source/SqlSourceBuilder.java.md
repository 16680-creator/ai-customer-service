<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/builder/SqlSourceBuilder.java -->
<!-- 正文引用：[[源码:SqlSourceBuilder.java#L起-L止]] ；代码块头部：(src: SqlSourceBuilder.java#L起-L止) -->

# SqlSourceBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/builder/SqlSourceBuilder.java` |
| 行数 | 44 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:07:59.548Z |

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
   16 | package org.apache.ibatis.builder;
   17 | 
   18 | import java.util.List;
   19 | 
   20 | import org.apache.ibatis.mapping.ParameterMapping;
   21 | import org.apache.ibatis.mapping.SqlSource;
   22 | import org.apache.ibatis.session.Configuration;
   23 | 
   24 | /**
   25 |  * @author Clinton Begin
   26 |  */
   27 | public class SqlSourceBuilder {
   28 | 
   29 |   private SqlSourceBuilder() {
   30 |     super();
   31 |   }
   32 | 
   33 |   public static SqlSource buildSqlSource(Configuration configuration, String sql,
   34 |       List<ParameterMapping> parameterMappings) {
   35 |     return new StaticSqlSource(configuration,
   36 |         configuration.isShrinkWhitespacesInSql() ? SqlSourceBuilder.removeExtraWhitespaces(sql) : sql,
   37 |         parameterMappings);
   38 |   }
   39 | 
   40 |   public static String removeExtraWhitespaces(final String original) {
   41 |     return original.replaceAll("\\s+", " ").trim();
   42 |   }
   43 | 
   44 | }
```
