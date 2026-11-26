<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/ServiceInstance.java -->
<!-- 正文引用：[[源码:ServiceInstance.java#L起-L止]] ；代码块头部：(src: ServiceInstance.java#L起-L止) -->

# ServiceInstance.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/ServiceInstance.java` |
| 行数 | 92 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:32:50.855Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package org.springframework.cloud.client;
   18 | 
   19 | import java.net.URI;
   20 | import java.util.Map;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | /**
   25 |  * Represents an instance of a service in a discovery system.
   26 |  *
   27 |  * @author Spencer Gibb
   28 |  * @author Tim Ysewyn
   29 |  */
   30 | public interface ServiceInstance {
   31 | 
   32 | 	/**
   33 | 	 * @return The unique instance ID as registered.
   34 | 	 */
   35 | 	default @Nullable String getInstanceId() {
   36 | 		return null;
   37 | 	}
   38 | 
   39 | 	/**
   40 | 	 * @return The service ID as registered.
   41 | 	 */
   42 | 	String getServiceId();
   43 | 
   44 | 	/**
   45 | 	 * @return The hostname of the registered service instance.
   46 | 	 */
   47 | 	String getHost();
   48 | 
   49 | 	/**
   50 | 	 * @return The port of the registered service instance.
   51 | 	 */
   52 | 	int getPort();
   53 | 
   54 | 	/**
   55 | 	 * @return Whether the port of the registered service instance uses HTTPS.
   56 | 	 */
   57 | 	boolean isSecure();
   58 | 
   59 | 	/**
   60 | 	 * @return The service URI address.
   61 | 	 */
   62 | 	URI getUri();
   63 | 
   64 | 	/**
   65 | 	 * @return The key / value pair metadata associated with the service instance.
   66 | 	 */
   67 | 	@Nullable Map<String, String> getMetadata();
   68 | 
   69 | 	/**
   70 | 	 * @return The scheme of the service instance.
   71 | 	 */
   72 | 	default @Nullable String getScheme() {
   73 | 		return null;
   74 | 	}
   75 | 
   76 | 	/**
   77 | 	 * Creates a URI from the given ServiceInstance's host:port.
   78 | 	 * @param instance the ServiceInstance.
   79 | 	 * @return URI of the form (secure)?https:http + "host:port". Scheme port default used
   80 | 	 * if port not set.
   81 | 	 */
   82 | 	static URI createUri(ServiceInstance instance) {
   83 | 		String scheme = (instance.isSecure()) ? "https" : "http";
   84 | 		int port = instance.getPort();
   85 | 		if (port <= 0) {
   86 | 			port = (instance.isSecure()) ? 443 : 80;
   87 | 		}
   88 | 		String uri = String.format("%s://%s:%s", scheme, instance.getHost(), port);
   89 | 		return URI.create(uri);
   90 | 	}
   91 | 
   92 | }
```
