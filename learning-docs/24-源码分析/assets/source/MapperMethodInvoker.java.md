<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/binding/MapperMethodInvoker.java -->
<!-- 正文引用：[[源码:MapperMethodInvoker.java#L起-L止]] ；代码块头部：(src: MapperMethodInvoker.java#L起-L止) -->

# MapperMethodInvoker.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/binding/MapperMethodInvoker.java` |
| 行数 | 26 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:28:27.524Z |

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
   16 | package org.apache.ibatis.binding;
   17 | 
   18 | import java.lang.reflect.Method;
   19 | 
   20 | import org.apache.ibatis.session.SqlSession;
   21 | 
   22 | public interface MapperMethodInvoker {
   23 | 
   24 |   Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable;
   25 | 
   26 | }
```
