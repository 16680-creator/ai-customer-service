<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/mapping/SqlSource.java -->
<!-- 正文引用：[[源码:SqlSource.java#L起-L止]] ；代码块头部：(src: SqlSource.java#L起-L止) -->

# SqlSource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/mapping/SqlSource.java` |
| 行数 | 28 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:19.293Z |

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
   18 | /**
   19 |  * Represents the content of a mapped statement read from an XML file or an annotation. It creates the SQL that will be
   20 |  * passed to the database out of the input parameter received from the user.
   21 |  *
   22 |  * @author Clinton Begin
   23 |  */
   24 | public interface SqlSource {
   25 | 
   26 |   BoundSql getBoundSql(Object parameterObject);
   27 | 
   28 | }
```
