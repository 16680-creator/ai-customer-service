<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/registry/NacosAutoServiceRegistration.java -->
<!-- 正文引用：[[源码:NacosAutoServiceRegistration.java#L起-L止]] ；代码块头部：(src: NacosAutoServiceRegistration.java#L起-L止) -->

# NacosAutoServiceRegistration.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/registry/NacosAutoServiceRegistration.java` |
| 行数 | 118 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:21.411Z |

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
   17 | package com.alibaba.cloud.nacos.registry;
   18 | 
   19 | import com.alibaba.cloud.commons.lang.StringUtils;
   20 | import com.alibaba.cloud.nacos.event.NacosDiscoveryInfoChangedEvent;
   21 | import org.slf4j.Logger;
   22 | import org.slf4j.LoggerFactory;
   23 | 
   24 | import org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration;
   25 | import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
   26 | import org.springframework.cloud.client.serviceregistry.Registration;
   27 | import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
   28 | import org.springframework.context.ApplicationContext;
   29 | import org.springframework.context.event.EventListener;
   30 | import org.springframework.util.Assert;
   31 | 
   32 | /**
   33 |  * @author xiaojing
   34 |  * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
   35 |  */
   36 | public class NacosAutoServiceRegistration
   37 | 		extends AbstractAutoServiceRegistration<Registration> {
   38 | 
   39 | 	private static final Logger log = LoggerFactory
   40 | 			.getLogger(NacosAutoServiceRegistration.class);
   41 | 
   42 | 	private NacosRegistration registration;
   43 | 
   44 | 	public NacosAutoServiceRegistration(ApplicationContext context,
   45 | 			ServiceRegistry<Registration> serviceRegistry,
   46 | 			AutoServiceRegistrationProperties autoServiceRegistrationProperties,
   47 | 			NacosRegistration registration) {
   48 | 		super(context, serviceRegistry, autoServiceRegistrationProperties);
   49 | 		this.registration = registration;
   50 | 	}
   51 | 
   52 | 	@Deprecated
   53 | 	public void setPort(int port) {
   54 | 		getPort().set(port);
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	protected NacosRegistration getRegistration() {
   59 | 		if (this.registration.getPort() < 0 && this.getPort().get() > 0) {
   60 | 			this.registration.setPort(this.getPort().get());
   61 | 		}
   62 | 		Assert.isTrue(this.registration.getPort() > 0, "service.port has not been set");
   63 | 		return this.registration;
   64 | 	}
   65 | 
   66 | 	@Override
   67 | 	protected NacosRegistration getManagementRegistration() {
   68 | 		return null;
   69 | 	}
   70 | 
   71 | 	@Override
   72 | 	protected void register() {
   73 | 		if (!this.registration.getNacosDiscoveryProperties().isRegisterEnabled()) {
   74 | 			log.debug("Registration disabled.");
   75 | 			return;
   76 | 		}
   77 | 		if (this.registration.getPort() < 0) {
   78 | 			this.registration.setPort(getPort().get());
   79 | 		}
   80 | 		super.register();
   81 | 	}
   82 | 
   83 | 	@Override
   84 | 	protected void registerManagement() {
   85 | 		if (!this.registration.getNacosDiscoveryProperties().isRegisterEnabled()) {
   86 | 			return;
   87 | 		}
   88 | 		super.registerManagement();
   89 | 
   90 | 	}
   91 | 
   92 | 	@Override
   93 | 	protected Object getConfiguration() {
   94 | 		return this.registration.getNacosDiscoveryProperties();
   95 | 	}
   96 | 
   97 | 	@Override
   98 | 	protected boolean isEnabled() {
   99 | 		return this.registration.getNacosDiscoveryProperties().isRegisterEnabled();
  100 | 	}
  101 | 
  102 | 	@Override
  103 | 	@SuppressWarnings("deprecation")
  104 | 	protected String getAppName() {
  105 | 		String appName = registration.getNacosDiscoveryProperties().getService();
  106 | 		return StringUtils.isEmpty(appName) ? super.getAppName() : appName;
  107 | 	}
  108 | 
  109 | 	@EventListener
  110 | 	public void onNacosDiscoveryInfoChangedEvent(NacosDiscoveryInfoChangedEvent event) {
  111 | 		restart();
  112 | 	}
  113 | 
  114 | 	private void restart() {
  115 | 		this.stop();
  116 | 		this.start();
  117 | 	}
  118 | }
```
