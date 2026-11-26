<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/serviceregistry/AbstractAutoServiceRegistration.java -->
<!-- 正文引用：[[源码:AbstractAutoServiceRegistration.java#L起-L止]] ；代码块头部：(src: AbstractAutoServiceRegistration.java#L起-L止) -->

# AbstractAutoServiceRegistration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/serviceregistry/AbstractAutoServiceRegistration.java` |
| 行数 | 314 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:32:39.973Z |

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
   17 | package org.springframework.cloud.client.serviceregistry;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.concurrent.atomic.AtomicBoolean;
   22 | import java.util.concurrent.atomic.AtomicInteger;
   23 | 
   24 | import jakarta.annotation.Nullable;
   25 | import jakarta.annotation.PreDestroy;
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | 
   29 | import org.springframework.boot.web.server.context.ConfigurableWebServerApplicationContext;
   30 | import org.springframework.boot.web.server.context.WebServerInitializedEvent;
   31 | import org.springframework.cloud.client.discovery.ManagementServerPortUtils;
   32 | import org.springframework.cloud.client.discovery.event.InstancePreRegisteredEvent;
   33 | import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
   34 | import org.springframework.context.ApplicationContext;
   35 | import org.springframework.context.ApplicationListener;
   36 | import org.springframework.core.env.Environment;
   37 | 
   38 | /**
   39 |  * Lifecycle methods that may be useful and common to {@link ServiceRegistry}
   40 |  * implementations.
   41 |  *
   42 |  * TODO: Document the lifecycle.
   43 |  *
   44 |  * @param <R> Registration type passed to the {@link ServiceRegistry}.
   45 |  * @author Spencer Gibb
   46 |  * @author Zen Huifer
   47 |  */
   48 | public abstract class AbstractAutoServiceRegistration<R extends Registration>
   49 | 		implements AutoServiceRegistration, ApplicationListener<WebServerInitializedEvent> {
   50 | 
   51 | 	private static final Log logger = LogFactory.getLog(AbstractAutoServiceRegistration.class);
   52 | 
   53 | 	private final ServiceRegistry<R> serviceRegistry;
   54 | 
   55 | 	private final boolean autoStartup = true;
   56 | 
   57 | 	private final AtomicBoolean running = new AtomicBoolean(false);
   58 | 
   59 | 	private final int order = 0;
   60 | 
   61 | 	private final AtomicInteger port = new AtomicInteger(0);
   62 | 
   63 | 	private ApplicationContext context;
   64 | 
   65 | 	private Environment environment;
   66 | 
   67 | 	private AutoServiceRegistrationProperties properties;
   68 | 
   69 | 	private List<RegistrationManagementLifecycle<R>> registrationManagementLifecycles = new ArrayList<>();
   70 | 
   71 | 	private List<RegistrationLifecycle<R>> registrationLifecycles = new ArrayList<>();
   72 | 
   73 | 	protected AbstractAutoServiceRegistration(ApplicationContext context, ServiceRegistry<R> serviceRegistry,
   74 | 			AutoServiceRegistrationProperties properties) {
   75 | 		this.context = context;
   76 | 		this.environment = context.getEnvironment();
   77 | 		this.serviceRegistry = serviceRegistry;
   78 | 		this.properties = properties;
   79 | 	}
   80 | 
   81 | 	protected AbstractAutoServiceRegistration(ApplicationContext context, ServiceRegistry<R> serviceRegistry,
   82 | 			AutoServiceRegistrationProperties properties,
   83 | 			List<RegistrationManagementLifecycle<R>> registrationManagementLifecycles,
   84 | 			List<RegistrationLifecycle<R>> registrationLifecycles) {
   85 | 		this.context = context;
   86 | 		this.environment = context.getEnvironment();
   87 | 		this.serviceRegistry = serviceRegistry;
   88 | 		this.properties = properties;
   89 | 		this.registrationManagementLifecycles = registrationManagementLifecycles;
   90 | 		this.registrationLifecycles = registrationLifecycles;
   91 | 	}
   92 | 
   93 | 	protected AbstractAutoServiceRegistration(ApplicationContext context, ServiceRegistry<R> serviceRegistry,
   94 | 			AutoServiceRegistrationProperties properties, List<RegistrationLifecycle<R>> registrationLifecycles) {
   95 | 		this.context = context;
   96 | 		this.environment = context.getEnvironment();
   97 | 		this.serviceRegistry = serviceRegistry;
   98 | 		this.properties = properties;
   99 | 		this.registrationLifecycles = registrationLifecycles;
  100 | 	}
  101 | 
  102 | 	public void addRegistrationManagementLifecycle(RegistrationManagementLifecycle<R> registrationManagementLifecycle) {
  103 | 		this.registrationManagementLifecycles.add(registrationManagementLifecycle);
  104 | 	}
  105 | 
  106 | 	public void addRegistrationLifecycle(RegistrationLifecycle<R> registrationLifecycle) {
  107 | 		this.registrationLifecycles.add(registrationLifecycle);
  108 | 	}
  109 | 
  110 | 	protected ApplicationContext getContext() {
  111 | 		return this.context;
  112 | 	}
  113 | 
  114 | 	@Override
  115 | 	@SuppressWarnings("deprecation")
  116 | 	public void onApplicationEvent(WebServerInitializedEvent event) {
  117 | 		ApplicationContext context = event.getApplicationContext();
  118 | 		if (context instanceof ConfigurableWebServerApplicationContext configurableWebServerApplicationContext) {
  119 | 			if ("management".equals(configurableWebServerApplicationContext.getServerNamespace())) {
  120 | 				return;
  121 | 			}
  122 | 		}
  123 | 		this.port.compareAndSet(0, event.getWebServer().getPort());
  124 | 		this.start();
  125 | 	}
  126 | 
  127 | 	@Deprecated
  128 | 	protected Environment getEnvironment() {
  129 | 		return this.environment;
  130 | 	}
  131 | 
  132 | 	@Deprecated
  133 | 	protected AtomicInteger getPort() {
  134 | 		return this.port;
  135 | 	}
  136 | 
  137 | 	public boolean isAutoStartup() {
  138 | 		return this.autoStartup;
  139 | 	}
  140 | 
  141 | 	public void start() {
  142 | 		if (!isEnabled()) {
  143 | 			if (logger.isDebugEnabled()) {
  144 | 				logger.debug("Discovery Lifecycle disabled. Not starting");
  145 | 			}
  146 | 			return;
  147 | 		}
  148 | 
  149 | 		// only initialize if nonSecurePort is greater than 0 and it isn't already running
  150 | 		// because of containerPortInitializer below
  151 | 		if (!this.running.get()) {
  152 | 			this.context.publishEvent(new InstancePreRegisteredEvent(this, getRegistration()));
  153 | 			registrationLifecycles.forEach(
  154 | 					registrationLifecycle -> registrationLifecycle.postProcessBeforeStartRegister(getRegistration()));
  155 | 			register();
  156 | 			this.registrationLifecycles.forEach(
  157 | 					registrationLifecycle -> registrationLifecycle.postProcessAfterStartRegister(getRegistration()));
  158 | 			if (shouldRegisterManagement()) {
  159 | 				this.registrationManagementLifecycles
  160 | 					.forEach(registrationManagementLifecycle -> registrationManagementLifecycle
  161 | 						.postProcessBeforeStartRegisterManagement(getManagementRegistration()));
  162 | 				this.registerManagement();
  163 | 				registrationManagementLifecycles
  164 | 					.forEach(registrationManagementLifecycle -> registrationManagementLifecycle
  165 | 						.postProcessAfterStartRegisterManagement(getManagementRegistration()));
  166 | 
  167 | 			}
  168 | 			this.context.publishEvent(new InstanceRegisteredEvent<>(this, getConfiguration()));
  169 | 			this.running.compareAndSet(false, true);
  170 | 		}
  171 | 
  172 | 	}
  173 | 
  174 | 	/**
  175 | 	 * @return Whether the management service should be registered with the
  176 | 	 * {@link ServiceRegistry}.
  177 | 	 */
  178 | 	protected boolean shouldRegisterManagement() {
  179 | 		if (this.properties == null || this.properties.isRegisterManagement()) {
  180 | 			return getManagementPort() != null && ManagementServerPortUtils.isDifferent(this.context);
  181 | 		}
  182 | 		return false;
  183 | 	}
  184 | 
  185 | 	/**
  186 | 	 * @return The object used to configure the registration.
  187 | 	 */
  188 | 	@Deprecated
  189 | 	protected abstract Object getConfiguration();
  190 | 
  191 | 	/**
  192 | 	 * @return True, if this is enabled.
  193 | 	 */
  194 | 	protected abstract boolean isEnabled();
  195 | 
  196 | 	/**
  197 | 	 * @return The serviceId of the Management Service.
  198 | 	 */
  199 | 	@Deprecated
  200 | 	protected String getManagementServiceId() {
  201 | 		// TODO: configurable management suffix
  202 | 		return this.context.getId() + ":management";
  203 | 	}
  204 | 
  205 | 	/**
  206 | 	 * @return The service name of the Management Service.
  207 | 	 */
  208 | 	@Deprecated
  209 | 	protected String getManagementServiceName() {
  210 | 		// TODO: configurable management suffix
  211 | 		return getAppName() + ":management";
  212 | 	}
  213 | 
  214 | 	/**
  215 | 	 * @return The management server port.
  216 | 	 */
  217 | 	@Deprecated
  218 | 	protected @Nullable Integer getManagementPort() {
  219 | 		return ManagementServerPortUtils.getPort(this.context);
  220 | 	}
  221 | 
  222 | 	/**
  223 | 	 * @return The app name (currently the spring.application.name property).
  224 | 	 */
  225 | 	@Deprecated
  226 | 	protected String getAppName() {
  227 | 		return this.environment.getProperty("spring.application.name", "application");
  228 | 	}
  229 | 
  230 | 	@PreDestroy
  231 | 	public void destroy() {
  232 | 		stop();
  233 | 	}
  234 | 
  235 | 	public boolean isRunning() {
  236 | 		return this.running.get();
  237 | 	}
  238 | 
  239 | 	protected AtomicBoolean getRunning() {
  240 | 		return this.running;
  241 | 	}
  242 | 
  243 | 	public int getOrder() {
  244 | 		return this.order;
  245 | 	}
  246 | 
  247 | 	public int getPhase() {
  248 | 		return 0;
  249 | 	}
  250 | 
  251 | 	protected ServiceRegistry<R> getServiceRegistry() {
  252 | 		return this.serviceRegistry;
  253 | 	}
  254 | 
  255 | 	protected abstract R getRegistration();
  256 | 
  257 | 	protected abstract R getManagementRegistration();
  258 | 
  259 | 	/**
  260 | 	 * Register the local service with the {@link ServiceRegistry}.
  261 | 	 */
  262 | 	protected void register() {
  263 | 		this.serviceRegistry.register(getRegistration());
  264 | 	}
  265 | 
  266 | 	/**
  267 | 	 * Register the local management service with the {@link ServiceRegistry}.
  268 | 	 */
  269 | 	protected void registerManagement() {
  270 | 		R registration = getManagementRegistration();
  271 | 		if (registration != null) {
  272 | 			this.serviceRegistry.register(registration);
  273 | 		}
  274 | 	}
  275 | 
  276 | 	/**
  277 | 	 * De-register the local service with the {@link ServiceRegistry}.
  278 | 	 */
  279 | 	protected void deregister() {
  280 | 		this.serviceRegistry.deregister(getRegistration());
  281 | 	}
  282 | 
  283 | 	/**
  284 | 	 * De-register the local management service with the {@link ServiceRegistry}.
  285 | 	 */
  286 | 	protected void deregisterManagement() {
  287 | 		R registration = getManagementRegistration();
  288 | 		if (registration != null) {
  289 | 			this.serviceRegistry.deregister(registration);
  290 | 		}
  291 | 	}
  292 | 
  293 | 	public void stop() {
  294 | 		if (this.getRunning().compareAndSet(true, false) && isEnabled()) {
  295 | 
  296 | 			this.registrationLifecycles.forEach(
  297 | 					registrationLifecycle -> registrationLifecycle.postProcessBeforeStopRegister(getRegistration()));
  298 | 			deregister();
  299 | 			this.registrationLifecycles.forEach(
  300 | 					registrationLifecycle -> registrationLifecycle.postProcessAfterStopRegister(getRegistration()));
  301 | 			if (shouldRegisterManagement()) {
  302 | 				this.registrationManagementLifecycles
  303 | 					.forEach(registrationManagementLifecycle -> registrationManagementLifecycle
  304 | 						.postProcessBeforeStopRegisterManagement(getManagementRegistration()));
  305 | 				deregisterManagement();
  306 | 				this.registrationManagementLifecycles
  307 | 					.forEach(registrationManagementLifecycle -> registrationManagementLifecycle
  308 | 						.postProcessAfterStopRegisterManagement(getManagementRegistration()));
  309 | 			}
  310 | 			this.serviceRegistry.close();
  311 | 		}
  312 | 	}
  313 | 
  314 | }
```
