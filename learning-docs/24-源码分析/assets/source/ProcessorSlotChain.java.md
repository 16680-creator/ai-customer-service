<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.csp:sentinel-core:1.8.10 | path: com/alibaba/csp/sentinel/slotchain/ProcessorSlotChain.java -->
<!-- 正文引用：[[源码:ProcessorSlotChain.java#L起-L止]] ；代码块头部：(src: ProcessorSlotChain.java#L起-L止) -->

# ProcessorSlotChain.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.csp:sentinel-core:1.8.10` |
| 路径 | `com/alibaba/csp/sentinel/slotchain/ProcessorSlotChain.java` |
| 行数 | 38 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/csp/sentinel-core/1.8.10/sentinel-core-1.8.10-sources.jar |
| 抓取时间 | 2026-11-25T15:33:29.602Z |

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
   16 | package com.alibaba.csp.sentinel.slotchain;
   17 | 
   18 | /**
   19 |  * Link all processor slots as a chain.
   20 |  *
   21 |  * @author qinan.qn
   22 |  */
   23 | public abstract class ProcessorSlotChain extends AbstractLinkedProcessorSlot<Object> {
   24 | 
   25 |     /**
   26 |      * Add a processor to the head of this slot chain.
   27 |      *
   28 |      * @param protocolProcessor processor to be added.
   29 |      */
   30 |     public abstract void addFirst(AbstractLinkedProcessorSlot<?> protocolProcessor);
   31 | 
   32 |     /**
   33 |      * Add a processor to the tail of this slot chain.
   34 |      *
   35 |      * @param protocolProcessor processor to be added.
   36 |      */
   37 |     public abstract void addLast(AbstractLinkedProcessorSlot<?> protocolProcessor);
   38 | }
```
