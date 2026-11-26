<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/DefaultServiceInstance.java -->
<!-- 正文引用：[[源码:DefaultServiceInstance.java#L起-L止]] ；代码块头部：(src: DefaultServiceInstance.java#L起-L止) -->

# DefaultServiceInstance.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/DefaultServiceInstance.java` |
| 行数 | 172 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:32:50.917Z |

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
   20 | import java.util.LinkedHashMap;
   21 | import java.util.Map;
   22 | import java.util.Objects;
   23 | 
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | /**
   27 |  * Default implementation of {@link ServiceInstance}.
   28 |  *
   29 |  * @author Spencer Gibb
   30 |  * @author Tim Ysewyn
   31 |  * @author Charu Covindane
   32 |  * @author Neil Powell
   33 |  */
   34 | public class DefaultServiceInstance implements ServiceInstance {
   35 | 
   36 | 	private @Nullable String instanceId;
   37 | 
   38 | 	private String serviceId;
   39 | 
   40 | 	private String host;
   41 | 
   42 | 	private int port;
   43 | 
   44 | 	private boolean secure;
   45 | 
   46 | 	private Map<String, String> metadata = new LinkedHashMap<>();
   47 | 
   48 | 	private @Nullable URI uri;
   49 | 
   50 | 	/**
   51 | 	 * @param instanceId the id of the instance.
   52 | 	 * @param serviceId the id of the service.
   53 | 	 * @param host the host where the service instance can be found.
   54 | 	 * @param port the port on which the service is running.
   55 | 	 * @param secure indicates whether or not the connection needs to be secure.
   56 | 	 * @param metadata a map containing metadata.
   57 | 	 */
   58 | 	public DefaultServiceInstance(@Nullable String instanceId, String serviceId, String host, int port, boolean secure,
   59 | 			@Nullable Map<String, String> metadata) {
   60 | 		this.instanceId = instanceId;
   61 | 		this.serviceId = serviceId;
   62 | 		this.host = host;
   63 | 		this.port = port;
   64 | 		this.secure = secure;
   65 | 		if (metadata != null) {
   66 | 			this.metadata = metadata;
   67 | 		}
   68 | 	}
   69 | 
   70 | 	/**
   71 | 	 * @param instanceId the id of the instance.
   72 | 	 * @param serviceId the id of the service.
   73 | 	 * @param host the host where the service instance can be found.
   74 | 	 * @param port the port on which the service is running.
   75 | 	 * @param secure indicates whether or not the connection needs to be secure.
   76 | 	 */
   77 | 	public DefaultServiceInstance(@Nullable String instanceId, String serviceId, String host, int port,
   78 | 			boolean secure) {
   79 | 		this(instanceId, serviceId, host, port, secure, new LinkedHashMap<>());
   80 | 	}
   81 | 
   82 | 	@Override
   83 | 	public URI getUri() {
   84 | 		return ServiceInstance.createUri(this);
   85 | 	}
   86 | 
   87 | 	@Override
   88 | 	public Map<String, String> getMetadata() {
   89 | 		return metadata;
   90 | 	}
   91 | 
   92 | 	@Override
   93 | 	public @Nullable String getInstanceId() {
   94 | 		return instanceId;
   95 | 	}
   96 | 
   97 | 	@Override
   98 | 	public String getServiceId() {
   99 | 		return serviceId;
  100 | 	}
  101 | 
  102 | 	@Override
  103 | 	public String getHost() {
  104 | 		return host;
  105 | 	}
  106 | 
  107 | 	@Override
  108 | 	public int getPort() {
  109 | 		return port;
  110 | 	}
  111 | 
  112 | 	@Override
  113 | 	public boolean isSecure() {
  114 | 		return secure;
  115 | 	}
  116 | 
  117 | 	public void setInstanceId(String instanceId) {
  118 | 		this.instanceId = instanceId;
  119 | 	}
  120 | 
  121 | 	public void setServiceId(String serviceId) {
  122 | 		this.serviceId = serviceId;
  123 | 	}
  124 | 
  125 | 	public void setHost(String host) {
  126 | 		this.host = host;
  127 | 	}
  128 | 
  129 | 	public void setPort(int port) {
  130 | 		this.port = port;
  131 | 	}
  132 | 
  133 | 	public void setSecure(boolean secure) {
  134 | 		this.secure = secure;
  135 | 	}
  136 | 
  137 | 	public void setUri(URI uri) {
  138 | 		this.uri = uri;
  139 | 		this.host = this.uri.getHost();
  140 | 		this.port = this.uri.getPort();
  141 | 		String scheme = this.uri.getScheme();
  142 | 		if ("https".equals(scheme)) {
  143 | 			this.secure = true;
  144 | 		}
  145 | 	}
  146 | 
  147 | 	@Override
  148 | 	public String toString() {
  149 | 		return "DefaultServiceInstance{" + "instanceId='" + instanceId + '\'' + ", serviceId='" + serviceId + '\''
  150 | 				+ ", host='" + host + '\'' + ", port=" + port + ", secure=" + secure + ", metadata=" + metadata + '}';
  151 | 	}
  152 | 
  153 | 	@Override
  154 | 	public boolean equals(Object o) {
  155 | 		if (this == o) {
  156 | 			return true;
  157 | 		}
  158 | 		if (o == null || getClass() != o.getClass()) {
  159 | 			return false;
  160 | 		}
  161 | 		DefaultServiceInstance that = (DefaultServiceInstance) o;
  162 | 		return port == that.port && secure == that.secure && Objects.equals(instanceId, that.instanceId)
  163 | 				&& Objects.equals(serviceId, that.serviceId) && Objects.equals(host, that.host)
  164 | 				&& Objects.equals(metadata, that.metadata);
  165 | 	}
  166 | 
  167 | 	@Override
  168 | 	public int hashCode() {
  169 | 		return Objects.hash(instanceId, serviceId, host, port, secure, metadata);
  170 | 	}
  171 | 
  172 | }
```
