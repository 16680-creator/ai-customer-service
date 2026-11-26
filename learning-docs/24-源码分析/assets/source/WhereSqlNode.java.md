<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/xmltags/WhereSqlNode.java -->
<!-- 正文引用：[[源码:WhereSqlNode.java#L起-L止]] ；代码块头部：(src: WhereSqlNode.java#L起-L止) -->

# WhereSqlNode.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/xmltags/WhereSqlNode.java` |
| 行数 | 35 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:17.700Z |

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
   16 | package org.apache.ibatis.scripting.xmltags;
   17 | 
   18 | import java.util.Arrays;
   19 | import java.util.List;
   20 | 
   21 | import org.apache.ibatis.session.Configuration;
   22 | 
   23 | /**
   24 |  * @author Clinton Begin
   25 |  */
   26 | public class WhereSqlNode extends TrimSqlNode {
   27 | 
   28 |   private static final List<String> prefixList = Arrays.asList("AND ", "OR ", "AND\n", "OR\n", "AND\r", "OR\r", "AND\t",
   29 |       "OR\t");
   30 | 
   31 |   public WhereSqlNode(Configuration configuration, SqlNode contents) {
   32 |     super(configuration, contents, "WHERE", prefixList, null, null);
   33 |   }
   34 | 
   35 | }
```
