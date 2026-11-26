<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/xmltags/IfSqlNode.java -->
<!-- 正文引用：[[源码:IfSqlNode.java#L起-L止]] ；代码块头部：(src: IfSqlNode.java#L起-L止) -->

# IfSqlNode.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/xmltags/IfSqlNode.java` |
| 行数 | 40 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:17.312Z |

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
   16 | package org.apache.ibatis.scripting.xmltags;
   17 | 
   18 | /**
   19 |  * @author Clinton Begin
   20 |  */
   21 | public class IfSqlNode implements SqlNode {
   22 |   private final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;
   23 |   private final String test;
   24 |   private final SqlNode contents;
   25 | 
   26 |   public IfSqlNode(SqlNode contents, String test) {
   27 |     this.test = test;
   28 |     this.contents = contents;
   29 |   }
   30 | 
   31 |   @Override
   32 |   public boolean apply(DynamicContext context) {
   33 |     if (evaluator.evaluateBoolean(test, context.getBindings())) {
   34 |       contents.apply(context);
   35 |       return true;
   36 |     }
   37 |     return false;
   38 |   }
   39 | 
   40 | }
```
