<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/defaults/RawLanguageDriver.java -->
<!-- 正文引用：[[源码:RawLanguageDriver.java#L起-L止]] ；代码块头部：(src: RawLanguageDriver.java#L起-L止) -->

# RawLanguageDriver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/defaults/RawLanguageDriver.java` |
| 行数 | 56 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:22.884Z |

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
   18 | import org.apache.ibatis.builder.BuilderException;
   19 | import org.apache.ibatis.mapping.SqlSource;
   20 | import org.apache.ibatis.parsing.XNode;
   21 | import org.apache.ibatis.reflection.ParamNameResolver;
   22 | import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
   23 | import org.apache.ibatis.session.Configuration;
   24 | 
   25 | /**
   26 |  * As of 3.2.4 the default XML language is able to identify static statements and create a {@link RawSqlSource}. So
   27 |  * there is no need to use RAW unless you want to make sure that there is not any dynamic tag for any reason.
   28 |  *
   29 |  * @since 3.2.0
   30 |  *
   31 |  * @author Eduardo Macarron
   32 |  */
   33 | public class RawLanguageDriver extends XMLLanguageDriver {
   34 | 
   35 |   @Override
   36 |   public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
   37 |     SqlSource source = super.createSqlSource(configuration, script, parameterType);
   38 |     checkIsNotDynamic(source);
   39 |     return source;
   40 |   }
   41 | 
   42 |   @Override
   43 |   public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType,
   44 |       ParamNameResolver paramNameResolver) {
   45 |     SqlSource source = super.createSqlSource(configuration, script, parameterType, paramNameResolver);
   46 |     checkIsNotDynamic(source);
   47 |     return source;
   48 |   }
   49 | 
   50 |   private void checkIsNotDynamic(SqlSource source) {
   51 |     if (!RawSqlSource.class.equals(source.getClass())) {
   52 |       throw new BuilderException("Dynamic content is not allowed when using RAW language");
   53 |     }
   54 |   }
   55 | 
   56 | }
```
