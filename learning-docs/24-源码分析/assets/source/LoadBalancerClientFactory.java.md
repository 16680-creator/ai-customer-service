<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/support/LoadBalancerClientFactory.java -->
<!-- 正文引用：[[源码:LoadBalancerClientFactory.java#L起-L止]] ；代码块头部：(src: LoadBalancerClientFactory.java#L起-L止) -->

# LoadBalancerClientFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/support/LoadBalancerClientFactory.java` |
| 行数 | 135 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:02.710Z |

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
   17 | package org.springframework.cloud.loadbalancer.support;
   18 | 
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | 
   22 | import org.apache.commons.logging.Log;
   23 | import org.apache.commons.logging.LogFactory;
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   27 | import org.springframework.cloud.client.ServiceInstance;
   28 | import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
   29 | import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
   30 | import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
   31 | import org.springframework.cloud.context.named.NamedContextFactory;
   32 | import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration;
   33 | import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
   34 | import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
   35 | import org.springframework.context.ApplicationContextInitializer;
   36 | import org.springframework.context.support.GenericApplicationContext;
   37 | import org.springframework.core.env.Environment;
   38 | 
   39 | /**
   40 |  * A factory that creates client, load balancer and client configuration instances. It
   41 |  * creates a Spring ApplicationContext per client name, and extracts the beans that it
   42 |  * needs from there.
   43 |  *
   44 |  * @author Spencer Gibb
   45 |  * @author Dave Syer
   46 |  * @author Olga Maciaszek-Sharma
   47 |  */
   48 | public class LoadBalancerClientFactory extends NamedContextFactory<LoadBalancerClientSpecification>
   49 | 		implements ReactiveLoadBalancer.Factory<ServiceInstance> {
   50 | 
   51 | 	private static final Log log = LogFactory.getLog(LoadBalancerClientFactory.class);
   52 | 
   53 | 	/**
   54 | 	 * Property source name for load balancer.
   55 | 	 */
   56 | 	public static final String NAMESPACE = "loadbalancer";
   57 | 
   58 | 	/**
   59 | 	 * Property for client name within the load balancer namespace.
   60 | 	 */
   61 | 	public static final String PROPERTY_NAME = NAMESPACE + ".client.name";
   62 | 
   63 | 	private final LoadBalancerClientsProperties properties;
   64 | 
   65 | 	public LoadBalancerClientFactory(LoadBalancerClientsProperties properties) {
   66 | 		super(LoadBalancerClientConfiguration.class, NAMESPACE, PROPERTY_NAME, new HashMap<>());
   67 | 		this.properties = properties;
   68 | 	}
   69 | 
   70 | 	public LoadBalancerClientFactory(LoadBalancerClientsProperties properties,
   71 | 			Map<String, ApplicationContextInitializer<GenericApplicationContext>> applicationContextInitializers) {
   72 | 		super(LoadBalancerClientConfiguration.class, NAMESPACE, PROPERTY_NAME, applicationContextInitializers);
   73 | 		this.properties = properties;
   74 | 	}
   75 | 
   76 | 	public static String getName(Environment environment) {
   77 | 		return environment.getProperty(PROPERTY_NAME);
   78 | 	}
   79 | 
   80 | 	@Override
   81 | 	public ReactiveLoadBalancer<ServiceInstance> getInstance(String serviceId) {
   82 | 		return getInstance(serviceId, ReactorServiceInstanceLoadBalancer.class);
   83 | 	}
   84 | 
   85 | 	/**
   86 | 	 * Returns the bean of the given type declared for the given client, resolved like
   87 | 	 * {@link #getInstance(String, Class)} but without looking in the parent context.
   88 | 	 * @param name the client name
   89 | 	 * @param type the bean type
   90 | 	 * @param <T> the bean type
   91 | 	 * @return the bean, or {@code null} if the client context does not declare a bean of
   92 | 	 * the given type or declares several without a primary one
   93 | 	 */
   94 | 	public <T> @Nullable T getInstanceWithoutAncestors(String name, Class<T> type) {
   95 | 		GenericApplicationContext context = getContext(name);
   96 | 		if (context.getBeanNamesForType(type).length == 0) {
   97 | 			return null;
   98 | 		}
   99 | 		try {
  100 | 			return context.getBean(type);
  101 | 		}
  102 | 		catch (NoSuchBeanDefinitionException ex) {
  103 | 			return null;
  104 | 		}
  105 | 	}
  106 | 
  107 | 	@Override
  108 | 	public LoadBalancerProperties getProperties(String serviceId) {
  109 | 		if (properties == null) {
  110 | 			if (log.isWarnEnabled()) {
  111 | 				log.warn("LoadBalancerClientsProperties is null. Please use the new constructor.");
  112 | 			}
  113 | 			return null;
  114 | 		}
  115 | 		if (serviceId == null || !properties.getClients().containsKey(serviceId)) {
  116 | 			// no specific client properties, return default
  117 | 			return properties;
  118 | 		}
  119 | 		// because specifics are overlayed on top of defaults, everything in `properties`,
  120 | 		// unless overridden, is in `clientsProperties`
  121 | 		return properties.getClients().get(serviceId);
  122 | 	}
  123 | 
  124 | 	@SuppressWarnings("unchecked")
  125 | 	public LoadBalancerClientFactory withApplicationContextInitializers(
  126 | 			Map<String, Object> applicationContextInitializers) {
  127 | 		Map<String, ApplicationContextInitializer<GenericApplicationContext>> convertedInitializers = new HashMap<>();
  128 | 		applicationContextInitializers.keySet()
  129 | 			.forEach(contextId -> convertedInitializers.put(contextId,
  130 | 					(ApplicationContextInitializer<GenericApplicationContext>) applicationContextInitializers
  131 | 						.get(contextId)));
  132 | 		return new LoadBalancerClientFactory(properties, convertedInitializers);
  133 | 	}
  134 | 
  135 | }
```
