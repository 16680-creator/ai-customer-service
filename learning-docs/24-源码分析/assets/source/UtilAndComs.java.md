<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/client/naming/utils/UtilAndComs.java -->
<!-- 正文引用：[[源码:UtilAndComs.java#L起-L止]] ；代码块头部：(src: UtilAndComs.java#L起-L止) -->

# UtilAndComs.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/client/naming/utils/UtilAndComs.java` |
| 行数 | 56 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:11.253Z |

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
   17 | package com.alibaba.nacos.client.naming.utils;
   18 | 
   19 | /**
   20 |  * Util and constants.
   21 |  *
   22 |  * @author xuanyin.zy
   23 |  */
   24 | public class UtilAndComs {
   25 |     
   26 |     public static String webContext = "/nacos";
   27 |     
   28 |     public static String nacosUrlBase = webContext + "/v1/ns";
   29 |     
   30 |     public static String nacosUrlInstance = nacosUrlBase + "/instance";
   31 |     
   32 |     public static String nacosUrlService = nacosUrlBase + "/service";
   33 |     
   34 |     public static final String ENV_LIST_KEY = "envList";
   35 |     
   36 |     public static final String ALL_IPS = "000--00-ALL_IPS--00--000";
   37 |     
   38 |     public static final String FAILOVER_SWITCH = "00-00---000-VIPSRV_FAILOVER_SWITCH-000---00-00";
   39 |     
   40 |     public static final String DEFAULT_NAMESPACE_ID = "public";
   41 |     
   42 |     public static final int REQUEST_DOMAIN_RETRY_COUNT = 3;
   43 |     
   44 |     @Deprecated
   45 |     public static final String NACOS_NAMING_LOG_NAME = "com.alibaba.nacos.naming.log.filename";
   46 |     
   47 |     @Deprecated
   48 |     public static final String NACOS_NAMING_LOG_LEVEL = "com.alibaba.nacos.naming.log.level";
   49 |     
   50 |     public static final String ENV_CONFIGS = "00-00---000-ENV_CONFIGS-000---00-00";
   51 |     
   52 |     public static final String VIP_CLIENT_FILE = "vipclient.properties";
   53 |     
   54 |     public static final String ALL_HOSTS = "00-00---000-ALL_HOSTS-000---00-00";
   55 |     
   56 | }
```
