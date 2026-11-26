<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/api/naming/listener/AbstractEventListener.java -->
<!-- 正文引用：[[源码:AbstractEventListener.java#L起-L止]] ；代码块头部：(src: AbstractEventListener.java#L起-L止) -->

# AbstractEventListener.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/api/naming/listener/AbstractEventListener.java` |
| 行数 | 38 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:11.378Z |

```java
    1 | /*
    2 |  * Copyright 1999-2018 Alibaba Group Holding Ltd.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      http://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package com.alibaba.nacos.api.naming.listener;
   18 | 
   19 | import java.util.concurrent.Executor;
   20 | 
   21 | /**
   22 |  * Abstract event listener, to support handle event by user custom executor.
   23 |  *
   24 |  * @author horizonzy
   25 |  * @since 1.4.1
   26 |  */
   27 | public abstract class AbstractEventListener implements EventListener {
   28 |     
   29 |     /**
   30 |      * Get executor for execute this receive.
   31 |      *
   32 |      * @return Executor
   33 |      */
   34 |     public Executor getExecutor() {
   35 |         return null;
   36 |     }
   37 |     
   38 | }
```
