<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/NacosServiceInstance.java -->
<!-- 正文引用：[[源码:NacosServiceInstance.java#L起-L止]] ；代码块头部：(src: NacosServiceInstance.java#L起-L止) -->

# NacosServiceInstance.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/NacosServiceInstance.java` |
| 行数 | 130 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:21.875Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
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
   17 | package com.alibaba.cloud.nacos;
   18 | 
   19 | import java.net.URI;
   20 | import java.util.Map;
   21 | import java.util.Objects;
   22 | 
   23 | import org.springframework.cloud.client.ServiceInstance;
   24 | 
   25 | /**
   26 |  * @author xiaojing
   27 |  * @author changjin wei(魏昌进)
   28 |  */
   29 | public class NacosServiceInstance implements ServiceInstance {
   30 | 
   31 | 	private String serviceId;
   32 | 
   33 | 	private String instanceId;
   34 | 
   35 | 	private String host;
   36 | 
   37 | 	private int port;
   38 | 
   39 | 	private boolean secure;
   40 | 
   41 | 	private Map<String, String> metadata;
   42 | 
   43 | 	@Override
   44 | 	public String getServiceId() {
   45 | 		return serviceId;
   46 | 	}
   47 | 
   48 | 	@Override
   49 | 	public String getInstanceId() {
   50 | 		return instanceId;
   51 | 	}
   52 | 
   53 | 	@Override
   54 | 	public String getHost() {
   55 | 		return host;
   56 | 	}
   57 | 
   58 | 	@Override
   59 | 	public int getPort() {
   60 | 		return port;
   61 | 	}
   62 | 
   63 | 	@Override
   64 | 	public boolean isSecure() {
   65 | 		return secure;
   66 | 	}
   67 | 
   68 | 	@Override
   69 | 	public URI getUri() {
   70 | 		return ServiceInstance.createUri(this);
   71 | 	}
   72 | 
   73 | 	@Override
   74 | 	public Map<String, String> getMetadata() {
   75 | 		return metadata;
   76 | 	}
   77 | 
   78 | 	@Override
   79 | 	public String getScheme() {
   80 | 		return getUri().getScheme();
   81 | 	}
   82 | 
   83 | 	public void setServiceId(String serviceId) {
   84 | 		this.serviceId = serviceId;
   85 | 	}
   86 | 
   87 | 	public void setInstanceId(String instanceId) {
   88 | 		this.instanceId = instanceId;
   89 | 	}
   90 | 
   91 | 	public void setHost(String host) {
   92 | 		this.host = host;
   93 | 	}
   94 | 
   95 | 	public void setPort(int port) {
   96 | 		this.port = port;
   97 | 	}
   98 | 
   99 | 	public void setSecure(boolean secure) {
  100 | 		this.secure = secure;
  101 | 	}
  102 | 
  103 | 	public void setMetadata(Map<String, String> metadata) {
  104 | 		this.metadata = metadata;
  105 | 	}
  106 | 
  107 | 	@Override
  108 | 	public boolean equals(Object o) {
  109 | 		if (this == o) {
  110 | 			return true;
  111 | 		}
  112 | 		if (o == null || getClass() != o.getClass()) {
  113 | 			return false;
  114 | 		}
  115 | 		NacosServiceInstance that = (NacosServiceInstance) o;
  116 | 
  117 | 		return Objects.equals(this.serviceId, that.serviceId)
  118 | 				&& Objects.equals(this.instanceId, that.instanceId)
  119 | 				&& Objects.equals(this.host, that.host)
  120 | 				&& this.port == that.port
  121 | 				&& this.secure == that.secure
  122 | 				&& Objects.equals(this.metadata, that.metadata);
  123 | 	}
  124 | 
  125 | 	@Override
  126 | 	public int hashCode() {
  127 | 		return (instanceId == null) ? 31 : (instanceId.hashCode() + 31);
  128 | 	}
  129 | 
  130 | }
```
