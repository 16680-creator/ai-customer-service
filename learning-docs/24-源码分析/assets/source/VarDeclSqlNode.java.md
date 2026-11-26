<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/xmltags/VarDeclSqlNode.java -->
<!-- 正文引用：[[源码:VarDeclSqlNode.java#L起-L止]] ；代码块头部：(src: VarDeclSqlNode.java#L起-L止) -->

# VarDeclSqlNode.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/xmltags/VarDeclSqlNode.java` |
| 行数 | 38 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:38:12.671Z |

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
   16 | package org.apache.ibatis.scripting.xmltags;
   17 | 
   18 | /**
   19 |  * @author Frank D. Martinez [mnesarco]
   20 |  */
   21 | public class VarDeclSqlNode implements SqlNode {
   22 | 
   23 |   private final String name;
   24 |   private final String expression;
   25 | 
   26 |   public VarDeclSqlNode(String name, String exp) {
   27 |     this.name = name;
   28 |     this.expression = exp;
   29 |   }
   30 | 
   31 |   @Override
   32 |   public boolean apply(DynamicContext context) {
   33 |     final Object value = OgnlCache.getValue(expression, context.getBindings());
   34 |     context.bind(name, value);
   35 |     return true;
   36 |   }
   37 | 
   38 | }
```
