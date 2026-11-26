<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/xmltags/XMLLanguageDriver.java -->
<!-- 正文引用：[[源码:XMLLanguageDriver.java#L起-L止]] ；代码块头部：(src: XMLLanguageDriver.java#L起-L止) -->

# XMLLanguageDriver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/xmltags/XMLLanguageDriver.java` |
| 行数 | 78 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:23.069Z |

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
   18 | import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
   19 | import org.apache.ibatis.executor.parameter.ParameterHandler;
   20 | import org.apache.ibatis.mapping.BoundSql;
   21 | import org.apache.ibatis.mapping.MappedStatement;
   22 | import org.apache.ibatis.mapping.SqlSource;
   23 | import org.apache.ibatis.parsing.PropertyParser;
   24 | import org.apache.ibatis.parsing.XNode;
   25 | import org.apache.ibatis.parsing.XPathParser;
   26 | import org.apache.ibatis.reflection.ParamNameResolver;
   27 | import org.apache.ibatis.scripting.LanguageDriver;
   28 | import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
   29 | import org.apache.ibatis.scripting.defaults.RawSqlSource;
   30 | import org.apache.ibatis.session.Configuration;
   31 | 
   32 | /**
   33 |  * @author Eduardo Macarron
   34 |  */
   35 | public class XMLLanguageDriver implements LanguageDriver {
   36 | 
   37 |   @Override
   38 |   public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject,
   39 |       BoundSql boundSql) {
   40 |     return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
   41 |   }
   42 | 
   43 |   @Override
   44 |   public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
   45 |     return createSqlSource(configuration, script, parameterType, null);
   46 |   }
   47 | 
   48 |   @Override
   49 |   public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType,
   50 |       ParamNameResolver paramNameResolver) {
   51 |     XMLScriptBuilder builder = new XMLScriptBuilder(configuration, script, parameterType, paramNameResolver);
   52 |     return builder.parseScriptNode();
   53 |   }
   54 | 
   55 |   @Override
   56 |   public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
   57 |     return createSqlSource(configuration, script, parameterType, null);
   58 |   }
   59 | 
   60 |   @Override
   61 |   public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType,
   62 |       ParamNameResolver paramNameResolver) {
   63 |     // issue #3
   64 |     if (script.startsWith("<script>")) {
   65 |       XPathParser parser = new XPathParser(script, false, configuration.getVariables(), new XMLMapperEntityResolver());
   66 |       return createSqlSource(configuration, parser.evalNode("/script"), parameterType);
   67 |     }
   68 |     // issue #127
   69 |     script = PropertyParser.parse(script, configuration.getVariables());
   70 |     TextSqlNode textSqlNode = new TextSqlNode(script);
   71 |     if (textSqlNode.isDynamic()) {
   72 |       return new DynamicSqlSource(configuration, textSqlNode);
   73 |     } else {
   74 |       return new RawSqlSource(configuration, script, parameterType, paramNameResolver);
   75 |     }
   76 |   }
   77 | 
   78 | }
```
