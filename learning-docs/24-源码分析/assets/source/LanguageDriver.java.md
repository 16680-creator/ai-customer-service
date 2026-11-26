<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/LanguageDriver.java -->
<!-- 正文引用：[[源码:LanguageDriver.java#L起-L止]] ；代码块头部：(src: LanguageDriver.java#L起-L止) -->

# LanguageDriver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/LanguageDriver.java` |
| 行数 | 89 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:31.329Z |

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
   16 | package org.apache.ibatis.scripting;
   17 | 
   18 | import org.apache.ibatis.executor.parameter.ParameterHandler;
   19 | import org.apache.ibatis.mapping.BoundSql;
   20 | import org.apache.ibatis.mapping.MappedStatement;
   21 | import org.apache.ibatis.mapping.SqlSource;
   22 | import org.apache.ibatis.parsing.XNode;
   23 | import org.apache.ibatis.reflection.ParamNameResolver;
   24 | import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
   25 | import org.apache.ibatis.session.Configuration;
   26 | 
   27 | public interface LanguageDriver {
   28 | 
   29 |   /**
   30 |    * Creates a {@link ParameterHandler} that passes the actual parameters to the the JDBC statement.
   31 |    *
   32 |    * @author Frank D. Martinez [mnesarco]
   33 |    *
   34 |    * @param mappedStatement
   35 |    *          The mapped statement that is being executed
   36 |    * @param parameterObject
   37 |    *          The input parameter object (can be null)
   38 |    * @param boundSql
   39 |    *          The resulting SQL once the dynamic language has been executed.
   40 |    *
   41 |    * @return the parameter handler
   42 |    *
   43 |    * @see DefaultParameterHandler
   44 |    */
   45 |   ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);
   46 | 
   47 |   /**
   48 |    * Creates an {@link SqlSource} that will hold the statement read from a mapper xml file. It is called during startup,
   49 |    * when the mapped statement is read from a class or an xml file.
   50 |    *
   51 |    * @param configuration
   52 |    *          The MyBatis configuration
   53 |    * @param script
   54 |    *          XNode parsed from a XML file
   55 |    * @param parameterType
   56 |    *          input parameter type got from a mapper method or specified in the parameterType xml attribute. Can be
   57 |    *          null.
   58 |    *
   59 |    * @return the sql source
   60 |    */
   61 |   SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);
   62 | 
   63 |   default SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType,
   64 |       ParamNameResolver paramNameResolver) {
   65 |     return createSqlSource(configuration, script, parameterType);
   66 |   }
   67 | 
   68 |   /**
   69 |    * Creates an {@link SqlSource} that will hold the statement read from an annotation. It is called during startup,
   70 |    * when the mapped statement is read from a class or an xml file.
   71 |    *
   72 |    * @param configuration
   73 |    *          The MyBatis configuration
   74 |    * @param script
   75 |    *          The content of the annotation
   76 |    * @param parameterType
   77 |    *          input parameter type got from a mapper method or specified in the parameterType xml attribute. Can be
   78 |    *          null.
   79 |    *
   80 |    * @return the sql source
   81 |    */
   82 |   SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);
   83 | 
   84 |   default SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType,
   85 |       ParamNameResolver paramNameResolver) {
   86 |     return createSqlSource(configuration, script, parameterType);
   87 |   }
   88 | 
   89 | }
```
