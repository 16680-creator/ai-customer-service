<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/client/naming/utils/CacheDirUtil.java -->
<!-- 正文引用：[[源码:CacheDirUtil.java#L起-L止]] ；代码块头部：(src: CacheDirUtil.java#L起-L止) -->

# CacheDirUtil.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/client/naming/utils/CacheDirUtil.java` |
| 行数 | 78 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:11.486Z |

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
   19 | import com.alibaba.nacos.api.PropertyKeyConst;
   20 | import com.alibaba.nacos.common.utils.StringUtils;
   21 | 
   22 | import java.io.File;
   23 | 
   24 | import com.alibaba.nacos.client.env.NacosClientProperties;
   25 | 
   26 | /**
   27 |  * Cache Dir Utils.
   28 |  *
   29 |  * @author zongkang.guo
   30 |  */
   31 | public class CacheDirUtil {
   32 |     
   33 |     private static String cacheDir;
   34 |     
   35 |     private static final String JM_SNAPSHOT_PATH_PROPERTY = "JM.SNAPSHOT.PATH";
   36 |     
   37 |     private static final String FILE_PATH_NACOS = "nacos";
   38 |     
   39 |     private static final String FILE_PATH_NAMING = "naming";
   40 |     
   41 |     private static final String USER_HOME_PROPERTY = "user.home";
   42 |     
   43 |     /**
   44 |      * Init cache dir.
   45 |      *
   46 |      * @param namespace  namespace.
   47 |      * @param properties nacosClientProperties.
   48 |      * @return
   49 |      */
   50 |     public static String initCacheDir(String namespace, NacosClientProperties properties) {
   51 |         
   52 |         String jmSnapshotPath = properties.getProperty(JM_SNAPSHOT_PATH_PROPERTY);
   53 |         
   54 |         String namingCacheRegistryDir = "";
   55 |         if (properties.getProperty(PropertyKeyConst.NAMING_CACHE_REGISTRY_DIR) != null) {
   56 |             namingCacheRegistryDir =
   57 |                 File.separator
   58 |                     + properties.getProperty(PropertyKeyConst.NAMING_CACHE_REGISTRY_DIR);
   59 |         }
   60 |         
   61 |         if (!StringUtils.isBlank(jmSnapshotPath)) {
   62 |             cacheDir = jmSnapshotPath + File.separator + FILE_PATH_NACOS + namingCacheRegistryDir
   63 |                 + File.separator
   64 |                 + FILE_PATH_NAMING + File.separator + namespace;
   65 |         } else {
   66 |             cacheDir =
   67 |                 properties.getProperty(USER_HOME_PROPERTY) + File.separator + FILE_PATH_NACOS
   68 |                     + namingCacheRegistryDir
   69 |                     + File.separator + FILE_PATH_NAMING + File.separator + namespace;
   70 |         }
   71 |         
   72 |         return cacheDir;
   73 |     }
   74 |     
   75 |     public static String getCacheDir() {
   76 |         return cacheDir;
   77 |     }
   78 | }
```
