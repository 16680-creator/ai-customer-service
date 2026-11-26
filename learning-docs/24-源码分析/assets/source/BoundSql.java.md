<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/mapping/BoundSql.java -->
<!-- 正文引用：[[源码:BoundSql.java#L起-L止]] ；代码块头部：(src: BoundSql.java#L起-L止) -->

# BoundSql.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/mapping/BoundSql.java` |
| 行数 | 80 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:00:26.440Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2023 the original author or authors.
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
   16 | package org.apache.ibatis.mapping;
   17 | 
   18 | import java.util.HashMap;
   19 | import java.util.List;
   20 | import java.util.Map;
   21 | 
   22 | import org.apache.ibatis.reflection.MetaObject;
   23 | import org.apache.ibatis.reflection.property.PropertyTokenizer;
   24 | import org.apache.ibatis.session.Configuration;
   25 | 
   26 | /**
   27 |  * An actual SQL String got from an {@link SqlSource} after having processed any dynamic content. The SQL may have SQL
   28 |  * placeholders "?" and a list (ordered) of a parameter mappings with the additional information for each parameter (at
   29 |  * least the property name of the input object to read the value from).
   30 |  * <p>
   31 |  * Can also have additional parameters that are created by the dynamic language (for loops, bind...).
   32 |  *
   33 |  * @author Clinton Begin
   34 |  */
   35 | public class BoundSql {
   36 | 
   37 |   private final String sql;
   38 |   private final List<ParameterMapping> parameterMappings;
   39 |   private final Object parameterObject;
   40 |   private final Map<String, Object> additionalParameters;
   41 |   private final MetaObject metaParameters;
   42 | 
   43 |   public BoundSql(Configuration configuration, String sql, List<ParameterMapping> parameterMappings,
   44 |       Object parameterObject) {
   45 |     this.sql = sql;
   46 |     this.parameterMappings = parameterMappings;
   47 |     this.parameterObject = parameterObject;
   48 |     this.additionalParameters = new HashMap<>();
   49 |     this.metaParameters = configuration.newMetaObject(additionalParameters);
   50 |   }
   51 | 
   52 |   public String getSql() {
   53 |     return sql;
   54 |   }
   55 | 
   56 |   public List<ParameterMapping> getParameterMappings() {
   57 |     return parameterMappings;
   58 |   }
   59 | 
   60 |   public Object getParameterObject() {
   61 |     return parameterObject;
   62 |   }
   63 | 
   64 |   public boolean hasAdditionalParameter(String name) {
   65 |     String paramName = new PropertyTokenizer(name).getName();
   66 |     return additionalParameters.containsKey(paramName);
   67 |   }
   68 | 
   69 |   public void setAdditionalParameter(String name, Object value) {
   70 |     metaParameters.setValue(name, value);
   71 |   }
   72 | 
   73 |   public Object getAdditionalParameter(String name) {
   74 |     return metaParameters.getValue(name);
   75 |   }
   76 | 
   77 |   public Map<String, Object> getAdditionalParameters() {
   78 |     return additionalParameters;
   79 |   }
   80 | }
```
