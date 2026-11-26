<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/client/naming/core/ProtectMode.java -->
<!-- 正文引用：[[源码:ProtectMode.java#L起-L止]] ；代码块头部：(src: ProtectMode.java#L起-L止) -->

# ProtectMode.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/client/naming/core/ProtectMode.java` |
| 行数 | 39 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:10.982Z |

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
   17 | package com.alibaba.nacos.client.naming.core;
   18 | 
   19 | /**
   20 |  * Protect mode.
   21 |  *
   22 |  * @author nkorange
   23 |  */
   24 | public class ProtectMode {
   25 |     
   26 |     private float protectThreshold;
   27 |     
   28 |     public ProtectMode() {
   29 |         this.protectThreshold = 0.8F;
   30 |     }
   31 |     
   32 |     public float getProtectThreshold() {
   33 |         return protectThreshold;
   34 |     }
   35 |     
   36 |     public void setProtectThreshold(float protectThreshold) {
   37 |         this.protectThreshold = protectThreshold;
   38 |     }
   39 | }
```
