<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/api/naming/NamingFactory.java -->
<!-- 正文引用：[[源码:NamingFactory.java#L起-L止]] ；代码块头部：(src: NamingFactory.java#L起-L止) -->

# NamingFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/api/naming/NamingFactory.java` |
| 行数 | 66 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:09.954Z |

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
   17 | package com.alibaba.nacos.api.naming;
   18 | 
   19 | import com.alibaba.nacos.api.exception.NacosException;
   20 | 
   21 | import java.lang.reflect.Constructor;
   22 | import java.util.Properties;
   23 | 
   24 | /**
   25 |  * Naming Factory.
   26 |  *
   27 |  * @author nkorange
   28 |  */
   29 | public class NamingFactory {
   30 |     
   31 |     /**
   32 |      * Create a new naming service.
   33 |      *
   34 |      * @param serverList server list
   35 |      * @return new naming service
   36 |      * @throws NacosException nacos exception
   37 |      */
   38 |     public static NamingService createNamingService(String serverList) throws NacosException {
   39 |         try {
   40 |             Class<?> driverImplClass =
   41 |                 Class.forName("com.alibaba.nacos.client.naming.NacosNamingService");
   42 |             Constructor constructor = driverImplClass.getConstructor(String.class);
   43 |             return (NamingService) constructor.newInstance(serverList);
   44 |         } catch (Throwable e) {
   45 |             throw new NacosException(NacosException.CLIENT_INVALID_PARAM, e);
   46 |         }
   47 |     }
   48 |     
   49 |     /**
   50 |      * Create a new naming service.
   51 |      *
   52 |      * @param properties naming service properties
   53 |      * @return new naming service
   54 |      * @throws NacosException nacos exception
   55 |      */
   56 |     public static NamingService createNamingService(Properties properties) throws NacosException {
   57 |         try {
   58 |             Class<?> driverImplClass =
   59 |                 Class.forName("com.alibaba.nacos.client.naming.NacosNamingService");
   60 |             Constructor constructor = driverImplClass.getConstructor(Properties.class);
   61 |             return (NamingService) constructor.newInstance(properties);
   62 |         } catch (Throwable e) {
   63 |             throw new NacosException(NacosException.CLIENT_INVALID_PARAM, e);
   64 |         }
   65 |     }
   66 | }
```
