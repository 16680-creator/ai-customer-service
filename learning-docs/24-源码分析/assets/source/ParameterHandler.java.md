<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/parameter/ParameterHandler.java -->
<!-- 正文引用：[[源码:ParameterHandler.java#L起-L止]] ；代码块头部：(src: ParameterHandler.java#L起-L止) -->

# ParameterHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/parameter/ParameterHandler.java` |
| 行数 | 32 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:31.122Z |

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
   16 | package org.apache.ibatis.executor.parameter;
   17 | 
   18 | import java.sql.PreparedStatement;
   19 | import java.sql.SQLException;
   20 | 
   21 | /**
   22 |  * A parameter handler sets the parameters of the {@code PreparedStatement}.
   23 |  *
   24 |  * @author Clinton Begin
   25 |  */
   26 | public interface ParameterHandler {
   27 | 
   28 |   Object getParameterObject();
   29 | 
   30 |   void setParameters(PreparedStatement ps) throws SQLException;
   31 | 
   32 | }
```
