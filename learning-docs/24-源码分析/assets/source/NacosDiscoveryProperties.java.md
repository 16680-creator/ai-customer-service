<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0 | path: com/alibaba/cloud/nacos/NacosDiscoveryProperties.java -->
<!-- 正文引用：[[源码:NacosDiscoveryProperties.java#L起-L止]] ；代码块头部：(src: NacosDiscoveryProperties.java#L起-L止) -->

# NacosDiscoveryProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery:2025.1.0.0` |
| 路径 | `com/alibaba/cloud/nacos/NacosDiscoveryProperties.java` |
| 行数 | 732 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-nacos-discovery/2025.1.0.0/spring-cloud-starter-alibaba-nacos-discovery-2025.1.0.0-sources.jar |
| 抓取时间 | 2026-11-25T15:35:21.961Z |

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
   19 | import java.net.Inet4Address;
   20 | import java.net.Inet6Address;
   21 | import java.net.InetAddress;
   22 | import java.net.NetworkInterface;
   23 | import java.util.Enumeration;
   24 | import java.util.HashMap;
   25 | import java.util.Locale;
   26 | import java.util.Map;
   27 | import java.util.Objects;
   28 | import java.util.Properties;
   29 | import java.util.regex.Matcher;
   30 | import java.util.regex.Pattern;
   31 | 
   32 | import com.alibaba.cloud.commons.context.support.PropertySourcesUtils;
   33 | import com.alibaba.cloud.commons.lang.StringUtils;
   34 | import com.alibaba.cloud.nacos.event.NacosDiscoveryInfoChangedEvent;
   35 | import com.alibaba.cloud.nacos.util.InetIPv6Utils;
   36 | import com.alibaba.nacos.api.naming.NamingService;
   37 | import com.alibaba.nacos.api.naming.PreservedMetadataKeys;
   38 | import com.alibaba.nacos.client.naming.utils.UtilAndComs;
   39 | import jakarta.annotation.PostConstruct;
   40 | import org.slf4j.Logger;
   41 | import org.slf4j.LoggerFactory;
   42 | 
   43 | import org.springframework.beans.factory.annotation.Autowired;
   44 | import org.springframework.beans.factory.annotation.Value;
   45 | import org.springframework.boot.context.properties.ConfigurationProperties;
   46 | import org.springframework.cloud.commons.util.InetUtils;
   47 | import org.springframework.context.ApplicationEventPublisher;
   48 | import org.springframework.core.env.ConfigurableEnvironment;
   49 | import org.springframework.core.env.Environment;
   50 | 
   51 | import static com.alibaba.nacos.api.PropertyKeyConst.ACCESS_KEY;
   52 | import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT;
   53 | import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT_PORT;
   54 | import static com.alibaba.nacos.api.PropertyKeyConst.NAMESPACE;
   55 | import static com.alibaba.nacos.api.PropertyKeyConst.NAMING_LOAD_CACHE_AT_START;
   56 | import static com.alibaba.nacos.api.PropertyKeyConst.PASSWORD;
   57 | import static com.alibaba.nacos.api.PropertyKeyConst.SECRET_KEY;
   58 | import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
   59 | import static com.alibaba.nacos.api.PropertyKeyConst.USERNAME;
   60 | 
   61 | /**
   62 |  * @author dungu.zpf
   63 |  * @author xiaojing
   64 |  * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
   65 |  * @author <a href="mailto:lyuzb@lyuzb.com">lyuzb</a>
   66 |  * @author <a href="mailto:78552423@qq.com">eshun</a>
   67 |  * @author freeman
   68 |  */
   69 | @ConfigurationProperties("spring.cloud.nacos.discovery")
   70 | public class NacosDiscoveryProperties {
   71 | 
   72 | 	/**
   73 | 	 * Prefix of {@link NacosDiscoveryProperties}.
   74 | 	 */
   75 | 	public static final String PREFIX = "spring.cloud.nacos.discovery";
   76 | 	private static final Logger log = LoggerFactory
   77 | 			.getLogger(NacosDiscoveryProperties.class);
   78 | 	private static final Pattern PATTERN = Pattern.compile("-(\\w)");
   79 | 
   80 | 	private static final String IPV4 = "IPv4";
   81 | 
   82 | 	private static final String IPV6 = "IPv6";
   83 | 
   84 | 	/**
   85 | 	 * nacos discovery server address.
   86 | 	 */
   87 | 	private String serverAddr;
   88 | 
   89 | 	/**
   90 | 	 * the nacos authentication username.
   91 | 	 */
   92 | 	private String username;
   93 | 
   94 | 	/**
   95 | 	 * the nacos authentication password.
   96 | 	 */
   97 | 	private String password;
   98 | 
   99 | 	/**
  100 | 	 * the domain name of a service, through which the server address can be dynamically
  101 | 	 * obtained.
  102 | 	 */
  103 | 	private String endpoint;
  104 | 
  105 | 	/**
  106 | 	 * namespace, separation registry of different environments.
  107 | 	 */
  108 | 	private String namespace;
  109 | 
  110 | 	/**
  111 | 	 * watch delay,duration to pull new service from nacos server.
  112 | 	 */
  113 | 	private long watchDelay = 30000;
  114 | 
  115 | 	/**
  116 | 	 * nacos naming log file name.
  117 | 	 */
  118 | 	private String logName;
  119 | 
  120 | 	/**
  121 | 	 * service name to registry.
  122 | 	 */
  123 | 	@Value("${spring.cloud.nacos.discovery.service:${spring.application.name:}}")
  124 | 	private String service;
  125 | 
  126 | 	/**
  127 | 	 * weight for service instance, the larger the value, the larger the weight.
  128 | 	 */
  129 | 	private float weight = 1;
  130 | 
  131 | 	/**
  132 | 	 * cluster name for nacos .
  133 | 	 */
  134 | 	private String clusterName;
  135 | 
  136 | 	/**
  137 | 	 * group name for nacos.
  138 | 	 */
  139 | 	private String group = "DEFAULT_GROUP";
  140 | 
  141 | 	/**
  142 | 	 * naming load from local cache at application start. true is load.
  143 | 	 */
  144 | 	private String namingLoadCacheAtStart = "false";
  145 | 
  146 | 	/**
  147 | 	 * extra metadata to register.
  148 | 	 */
  149 | 	private Map<String, String> metadata = new HashMap<>();
  150 | 
  151 | 	/**
  152 | 	 * if you just want to subscribe, but don't want to register your service, set it to
  153 | 	 * false.
  154 | 	 */
  155 | 	private boolean registerEnabled = true;
  156 | 
  157 | 	/**
  158 | 	 * The ip address your want to register for your service instance, needn't to set it
  159 | 	 * if the auto detect ip works well.
  160 | 	 */
  161 | 	private String ip;
  162 | 
  163 | 	/**
  164 | 	 * which network interface's ip you want to register.
  165 | 	 */
  166 | 	private String networkInterface = "";
  167 | 
  168 | 	/**
  169 | 	 * choose IPv4 or IPv6,if you don't set it will choose IPv4.
  170 | 	 * When IPv6 is chosen but no IPv6 can be found, system will automatically find IPv4 to ensure there is an
  171 | 	 * available service address.
  172 | 	 */
  173 | 	private String ipType;
  174 | 
  175 | 	/**
  176 | 	 * The port your want to register for your service instance, needn't to set it if the
  177 | 	 * auto detect port works well.
  178 | 	 */
  179 | 	private int port = -1;
  180 | 
  181 | 	/**
  182 | 	 * whether your service is a https service.
  183 | 	 */
  184 | 	private boolean secure = false;
  185 | 
  186 | 	/**
  187 | 	 * access key for namespace.
  188 | 	 */
  189 | 	private String accessKey;
  190 | 
  191 | 	/**
  192 | 	 * secret key for namespace.
  193 | 	 */
  194 | 	private String secretKey;
  195 | 
  196 | 	/**
  197 | 	 * Heart beat interval. Time unit: millisecond.
  198 | 	 */
  199 | 	private Integer heartBeatInterval;
  200 | 
  201 | 	/**
  202 | 	 * Heart beat timeout. Time unit: millisecond.
  203 | 	 */
  204 | 	private Integer heartBeatTimeout;
  205 | 
  206 | 	/**
  207 | 	 * Ip delete timeout. Time unit: millisecond.
  208 | 	 */
  209 | 	private Integer ipDeleteTimeout;
  210 | 
  211 | 	/**
  212 | 	 * If instance is enabled to accept request. The default value is true.
  213 | 	 */
  214 | 	private boolean instanceEnabled = true;
  215 | 
  216 | 	/**
  217 | 	 * If instance is ephemeral.The default value is true.
  218 | 	 */
  219 | 	private boolean ephemeral = true;
  220 | 
  221 | 	/**
  222 | 	 * Whether to enable nacos failure tolerance. If enabled, nacos will return cached
  223 | 	 * values when exceptions occur.
  224 | 	 */
  225 | 	private boolean failureToleranceEnabled;
  226 | 
  227 | 	/**
  228 | 	 * Throw exceptions during service registration if true, otherwise, log error
  229 | 	 * (defaults to true).
  230 | 	 */
  231 | 	private boolean failFast = true;
  232 | 
  233 | 	/**
  234 | 	 * graceful shutdown wait time. Time unit: millisecond.
  235 | 	 * default is 10s
  236 | 	 * When the Springboot shutdown hook is executed, remove the Nacos service, wait for 10 seconds, and then Tomcat rejects the request
  237 | 	 */
  238 | 	private Integer gracefulShutdownWaitTime = 10 * 1000;
  239 | 
  240 | 	@Autowired
  241 | 	private InetIPv6Utils inetIPv6Utils;
  242 | 
  243 | 	@Autowired
  244 | 	private InetUtils inetUtils;
  245 | 
  246 | 	@Autowired
  247 | 	private Environment environment;
  248 | 
  249 | 	@Autowired
  250 | 	private NacosServiceManager nacosServiceManager;
  251 | 
  252 | 	@Autowired
  253 | 	private ApplicationEventPublisher applicationEventPublisher;
  254 | 
  255 | 	@PostConstruct
  256 | 	public void init() throws Exception {
  257 | 
  258 | 		metadata.put(PreservedMetadataKeys.REGISTER_SOURCE, "SPRING_CLOUD");
  259 | 		if (secure) {
  260 | 			metadata.put("secure", "true");
  261 | 		}
  262 | 
  263 | 		serverAddr = Objects.toString(serverAddr, "");
  264 | 		if (serverAddr.endsWith("/")) {
  265 | 			serverAddr = serverAddr.substring(0, serverAddr.length() - 1);
  266 | 		}
  267 | 		endpoint = Objects.toString(endpoint, "");
  268 | 		namespace = Objects.toString(namespace, "");
  269 | 		logName = Objects.toString(logName, "");
  270 | 
  271 | 		if (StringUtils.isEmpty(ip)) {
  272 | 			// traversing network interfaces if didn't specify an interface
  273 | 			if (StringUtils.isEmpty(networkInterface)) {
  274 | 				if (ipType == null) {
  275 | 					ip = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
  276 | 					String ipv6Addr = inetIPv6Utils.findIPv6Address();
  277 | 					metadata.put(IPV6, ipv6Addr);
  278 | 					if (ipv6Addr != null) {
  279 | 						metadata.put(IPV6, ipv6Addr);
  280 | 					}
  281 | 				}
  282 | 				else if (IPV4.equalsIgnoreCase(ipType)) {
  283 | 					ip = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
  284 | 				}
  285 | 				else if (IPV6.equalsIgnoreCase(ipType)) {
  286 | 					ip = inetIPv6Utils.findIPv6Address();
  287 | 					if (StringUtils.isEmpty(ip)) {
  288 | 						log.warn("There is no available IPv6 found. Spring Cloud Alibaba will automatically find IPv4.");
  289 | 						ip = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
  290 | 					}
  291 | 				}
  292 | 				else {
  293 | 					throw new IllegalArgumentException(
  294 | 							"please checking the type of IP " + ipType);
  295 | 				}
  296 | 			}
  297 | 			else {
  298 | 				NetworkInterface netInterface = NetworkInterface
  299 | 						.getByName(networkInterface);
  300 | 				if (null == netInterface) {
  301 | 					throw new IllegalArgumentException(
  302 | 							"no such interface " + networkInterface);
  303 | 				}
  304 | 
  305 | 				Enumeration<InetAddress> inetAddress = netInterface.getInetAddresses();
  306 | 				while (inetAddress.hasMoreElements()) {
  307 | 					InetAddress currentAddress = inetAddress.nextElement();
  308 | 					if (currentAddress instanceof Inet4Address
  309 | 							|| currentAddress instanceof Inet6Address
  310 | 							&& !currentAddress.isLoopbackAddress()) {
  311 | 						ip = currentAddress.getHostAddress();
  312 | 						break;
  313 | 					}
  314 | 				}
  315 | 
  316 | 				if (StringUtils.isEmpty(ip)) {
  317 | 					throw new RuntimeException("cannot find available ip from"
  318 | 							+ " network interface " + networkInterface);
  319 | 				}
  320 | 
  321 | 			}
  322 | 		}
  323 | 
  324 | 		this.overrideFromEnv(environment);
  325 | 		if (nacosServiceManager.isNacosDiscoveryInfoChanged(this)) {
  326 | 			applicationEventPublisher
  327 | 					.publishEvent(new NacosDiscoveryInfoChangedEvent(this));
  328 | 		}
  329 | 		nacosServiceManager.setNacosDiscoveryProperties(this);
  330 | 	}
  331 | 
  332 | 	/**
  333 | 	 * recommend to use {@link NacosServiceManager#getNamingService()}.
  334 | 	 * @return NamingService
  335 | 	 */
  336 | 	@Deprecated
  337 | 	public NamingService namingServiceInstance() {
  338 | 		return nacosServiceManager.getNamingService();
  339 | 	}
  340 | 
  341 | 	public String getEndpoint() {
  342 | 		return endpoint;
  343 | 	}
  344 | 
  345 | 	public void setEndpoint(String endpoint) {
  346 | 		this.endpoint = endpoint;
  347 | 	}
  348 | 
  349 | 	public String getNamespace() {
  350 | 		return namespace;
  351 | 	}
  352 | 
  353 | 	public void setNamespace(String namespace) {
  354 | 		this.namespace = namespace;
  355 | 	}
  356 | 
  357 | 	public String getLogName() {
  358 | 		return logName;
  359 | 	}
  360 | 
  361 | 	public void setLogName(String logName) {
  362 | 		this.logName = logName;
  363 | 	}
  364 | 
  365 | 	public void setInetUtils(InetUtils inetUtils) {
  366 | 		this.inetUtils = inetUtils;
  367 | 	}
  368 | 
  369 | 	public float getWeight() {
  370 | 		return weight;
  371 | 	}
  372 | 
  373 | 	public void setWeight(float weight) {
  374 | 		this.weight = weight;
  375 | 	}
  376 | 
  377 | 	public String getClusterName() {
  378 | 		return clusterName;
  379 | 	}
  380 | 
  381 | 	public void setClusterName(String clusterName) {
  382 | 		this.clusterName = clusterName;
  383 | 	}
  384 | 
  385 | 	public String getService() {
  386 | 		return service;
  387 | 	}
  388 | 
  389 | 	public void setService(String service) {
  390 | 		this.service = service;
  391 | 	}
  392 | 
  393 | 	public boolean isRegisterEnabled() {
  394 | 		return registerEnabled;
  395 | 	}
  396 | 
  397 | 	public void setRegisterEnabled(boolean registerEnabled) {
  398 | 		this.registerEnabled = registerEnabled;
  399 | 	}
  400 | 
  401 | 	public String getIp() {
  402 | 		return ip;
  403 | 	}
  404 | 
  405 | 	public void setIp(String ip) {
  406 | 		this.ip = ip;
  407 | 	}
  408 | 
  409 | 	public String getIpType() {
  410 | 		return ipType;
  411 | 	}
  412 | 
  413 | 	public void setIpType(String ipType) {
  414 | 		this.ipType = ipType;
  415 | 	}
  416 | 
  417 | 	public String getNetworkInterface() {
  418 | 		return networkInterface;
  419 | 	}
  420 | 
  421 | 	public void setNetworkInterface(String networkInterface) {
  422 | 		this.networkInterface = networkInterface;
  423 | 	}
  424 | 
  425 | 	public int getPort() {
  426 | 		return port;
  427 | 	}
  428 | 
  429 | 	public void setPort(int port) {
  430 | 		this.port = port;
  431 | 	}
  432 | 
  433 | 	public boolean isSecure() {
  434 | 		return secure;
  435 | 	}
  436 | 
  437 | 	public void setSecure(boolean secure) {
  438 | 		this.secure = secure;
  439 | 	}
  440 | 
  441 | 	public Map<String, String> getMetadata() {
  442 | 		return metadata;
  443 | 	}
  444 | 
  445 | 	public void setMetadata(Map<String, String> metadata) {
  446 | 		this.metadata = metadata;
  447 | 	}
  448 | 
  449 | 	public String getServerAddr() {
  450 | 		return serverAddr;
  451 | 	}
  452 | 
  453 | 	public void setServerAddr(String serverAddr) {
  454 | 		this.serverAddr = serverAddr;
  455 | 	}
  456 | 
  457 | 	public String getAccessKey() {
  458 | 		return accessKey;
  459 | 	}
  460 | 
  461 | 	public void setAccessKey(String accessKey) {
  462 | 		this.accessKey = accessKey;
  463 | 	}
  464 | 
  465 | 	public String getSecretKey() {
  466 | 		return secretKey;
  467 | 	}
  468 | 
  469 | 	public void setSecretKey(String secretKey) {
  470 | 		this.secretKey = secretKey;
  471 | 	}
  472 | 
  473 | 	public Integer getHeartBeatInterval() {
  474 | 		return heartBeatInterval;
  475 | 	}
  476 | 
  477 | 	public void setHeartBeatInterval(Integer heartBeatInterval) {
  478 | 		this.heartBeatInterval = heartBeatInterval;
  479 | 	}
  480 | 
  481 | 	public Integer getHeartBeatTimeout() {
  482 | 		return heartBeatTimeout;
  483 | 	}
  484 | 
  485 | 	public void setHeartBeatTimeout(Integer heartBeatTimeout) {
  486 | 		this.heartBeatTimeout = heartBeatTimeout;
  487 | 	}
  488 | 
  489 | 	public Integer getIpDeleteTimeout() {
  490 | 		return ipDeleteTimeout;
  491 | 	}
  492 | 
  493 | 	public void setIpDeleteTimeout(Integer ipDeleteTimeout) {
  494 | 		this.ipDeleteTimeout = ipDeleteTimeout;
  495 | 	}
  496 | 
  497 | 	public String getNamingLoadCacheAtStart() {
  498 | 		return namingLoadCacheAtStart;
  499 | 	}
  500 | 
  501 | 	public void setNamingLoadCacheAtStart(String namingLoadCacheAtStart) {
  502 | 		this.namingLoadCacheAtStart = namingLoadCacheAtStart;
  503 | 	}
  504 | 
  505 | 	public long getWatchDelay() {
  506 | 		return watchDelay;
  507 | 	}
  508 | 
  509 | 	public void setWatchDelay(long watchDelay) {
  510 | 		this.watchDelay = watchDelay;
  511 | 	}
  512 | 
  513 | 	public String getGroup() {
  514 | 		return group;
  515 | 	}
  516 | 
  517 | 	public void setGroup(String group) {
  518 | 		this.group = group;
  519 | 	}
  520 | 
  521 | 	public String getUsername() {
  522 | 		return username;
  523 | 	}
  524 | 
  525 | 	public void setUsername(String username) {
  526 | 		this.username = username;
  527 | 	}
  528 | 
  529 | 	public String getPassword() {
  530 | 		return password;
  531 | 	}
  532 | 
  533 | 	public void setPassword(String password) {
  534 | 		this.password = password;
  535 | 	}
  536 | 
  537 | 	public boolean isInstanceEnabled() {
  538 | 		return instanceEnabled;
  539 | 	}
  540 | 
  541 | 	public void setInstanceEnabled(boolean instanceEnabled) {
  542 | 		this.instanceEnabled = instanceEnabled;
  543 | 	}
  544 | 
  545 | 	public boolean isEphemeral() {
  546 | 		return ephemeral;
  547 | 	}
  548 | 
  549 | 	public void setEphemeral(boolean ephemeral) {
  550 | 		this.ephemeral = ephemeral;
  551 | 	}
  552 | 
  553 | 	public boolean isFailureToleranceEnabled() {
  554 | 		return failureToleranceEnabled;
  555 | 	}
  556 | 
  557 | 	public void setFailureToleranceEnabled(boolean failureToleranceEnabled) {
  558 | 		this.failureToleranceEnabled = failureToleranceEnabled;
  559 | 	}
  560 | 
  561 | 	public boolean isFailFast() {
  562 | 		return failFast;
  563 | 	}
  564 | 
  565 | 	public void setFailFast(boolean failFast) {
  566 | 		this.failFast = failFast;
  567 | 	}
  568 | 
  569 | 	public Integer getGracefulShutdownWaitTime() {
  570 | 		return gracefulShutdownWaitTime;
  571 | 	}
  572 | 
  573 | 	public void setGracefulShutdownWaitTime(Integer gracefulShutdownWaitTime) {
  574 | 		this.gracefulShutdownWaitTime = gracefulShutdownWaitTime;
  575 | 	}
  576 | 
  577 | 	@Override
  578 | 	public boolean equals(Object o) {
  579 | 		if (this == o) {
  580 | 			return true;
  581 | 		}
  582 | 		if (o == null || getClass() != o.getClass()) {
  583 | 			return false;
  584 | 		}
  585 | 		NacosDiscoveryProperties that = (NacosDiscoveryProperties) o;
  586 | 		return watchDelay == that.watchDelay && Float.compare(that.weight, weight) == 0
  587 | 				&& registerEnabled == that.registerEnabled && port == that.port
  588 | 				&& secure == that.secure && instanceEnabled == that.instanceEnabled
  589 | 				&& ephemeral == that.ephemeral
  590 | 				&& failureToleranceEnabled == that.failureToleranceEnabled
  591 | 				&& Objects.equals(serverAddr, that.serverAddr)
  592 | 				&& Objects.equals(username, that.username)
  593 | 				&& Objects.equals(password, that.password)
  594 | 				&& Objects.equals(endpoint, that.endpoint)
  595 | 				&& Objects.equals(namespace, that.namespace)
  596 | 				&& Objects.equals(logName, that.logName)
  597 | 				&& Objects.equals(service, that.service)
  598 | 				&& Objects.equals(clusterName, that.clusterName)
  599 | 				&& Objects.equals(group, that.group)
  600 | 				&& Objects.equals(namingLoadCacheAtStart, that.namingLoadCacheAtStart)
  601 | 				&& Objects.equals(metadata, that.metadata) && Objects.equals(ip, that.ip)
  602 | 				&& Objects.equals(networkInterface, that.networkInterface)
  603 | 				&& Objects.equals(accessKey, that.accessKey)
  604 | 				&& Objects.equals(secretKey, that.secretKey)
  605 | 				&& Objects.equals(heartBeatInterval, that.heartBeatInterval)
  606 | 				&& Objects.equals(heartBeatTimeout, that.heartBeatTimeout)
  607 | 				&& Objects.equals(failFast, that.failFast)
  608 | 				&& Objects.equals(ipDeleteTimeout, that.ipDeleteTimeout);
  609 | 	}
  610 | 
  611 | 	@Override
  612 | 	public int hashCode() {
  613 | 		return Objects.hash(serverAddr, username, password, endpoint, namespace,
  614 | 				watchDelay, logName, service, weight, clusterName, group,
  615 | 				namingLoadCacheAtStart, metadata, registerEnabled, ip, networkInterface,
  616 | 				port, secure, accessKey, secretKey, heartBeatInterval, heartBeatTimeout,
  617 | 				ipDeleteTimeout, instanceEnabled, ephemeral, failureToleranceEnabled,
  618 | 				failFast);
  619 | 	}
  620 | 
  621 | 	@Override
  622 | 	public String toString() {
  623 | 		return "NacosDiscoveryProperties{" + "serverAddr='" + serverAddr + '\''
  624 | 				+ ", username='" + username + '\'' + ", password='" + password + '\''
  625 | 				+ ", endpoint='" + endpoint + '\'' + ", namespace='" + namespace + '\''
  626 | 				+ ", watchDelay=" + watchDelay + ", logName='" + logName + '\''
  627 | 				+ ", service='" + service + '\'' + ", weight=" + weight
  628 | 				+ ", clusterName='" + clusterName + '\'' + ", group='" + group + '\''
  629 | 				+ ", namingLoadCacheAtStart='" + namingLoadCacheAtStart + '\''
  630 | 				+ ", metadata=" + metadata + ", registerEnabled=" + registerEnabled
  631 | 				+ ", ip='" + ip + '\'' + ", networkInterface='" + networkInterface + '\''
  632 | 				+ ", port=" + port + ", secure=" + secure + ", accessKey='" + accessKey
  633 | 				+ '\'' + ", secretKey='" + secretKey + '\'' + ", heartBeatInterval="
  634 | 				+ heartBeatInterval + ", heartBeatTimeout=" + heartBeatTimeout
  635 | 				+ ", ipDeleteTimeout=" + ipDeleteTimeout + ", instanceEnabled="
  636 | 				+ instanceEnabled + ", ephemeral=" + ephemeral
  637 | 				+ ", failureToleranceEnabled=" + failureToleranceEnabled + '}'
  638 | 				+ ", ipDeleteTimeout=" + ipDeleteTimeout + ", failFast=" + failFast + '}';
  639 | 	}
  640 | 
  641 | 	public void overrideFromEnv(Environment env) {
  642 | 
  643 | 		if (StringUtils.isEmpty(this.getServerAddr())) {
  644 | 			String serverAddr = env
  645 | 					.resolvePlaceholders("${spring.cloud.nacos.discovery.server-addr:}");
  646 | 			if (StringUtils.isEmpty(serverAddr)) {
  647 | 				serverAddr = env.resolvePlaceholders(
  648 | 						"${spring.cloud.nacos.server-addr:127.0.0.1:8848}");
  649 | 			}
  650 | 			this.setServerAddr(serverAddr);
  651 | 		}
  652 | 		if (StringUtils.isEmpty(this.getNamespace())) {
  653 | 			this.setNamespace(env
  654 | 					.resolvePlaceholders("${spring.cloud.nacos.discovery.namespace:}"));
  655 | 		}
  656 | 		if (StringUtils.isEmpty(this.getAccessKey())) {
  657 | 			this.setAccessKey(env
  658 | 					.resolvePlaceholders("${spring.cloud.nacos.discovery.access-key:}"));
  659 | 		}
  660 | 		if (StringUtils.isEmpty(this.getSecretKey())) {
  661 | 			this.setSecretKey(env
  662 | 					.resolvePlaceholders("${spring.cloud.nacos.discovery.secret-key:}"));
  663 | 		}
  664 | 		if (StringUtils.isEmpty(this.getLogName())) {
  665 | 			this.setLogName(
  666 | 					env.resolvePlaceholders("${spring.cloud.nacos.discovery.log-name:}"));
  667 | 		}
  668 | 		if (StringUtils.isEmpty(this.getClusterName())) {
  669 | 			this.setClusterName(env.resolvePlaceholders(
  670 | 					"${spring.cloud.nacos.discovery.cluster-name:}"));
  671 | 		}
  672 | 		if (StringUtils.isEmpty(this.getEndpoint())) {
  673 | 			this.setEndpoint(
  674 | 					env.resolvePlaceholders("${spring.cloud.nacos.discovery.endpoint:}"));
  675 | 		}
  676 | 		if (StringUtils.isEmpty(this.getGroup())) {
  677 | 			this.setGroup(
  678 | 					env.resolvePlaceholders("${spring.cloud.nacos.discovery.group:}"));
  679 | 		}
  680 | 		if (StringUtils.isEmpty(this.getUsername())) {
  681 | 			this.setUsername(env.resolvePlaceholders("${spring.cloud.nacos.username:}"));
  682 | 		}
  683 | 		if (StringUtils.isEmpty(this.getPassword())) {
  684 | 			this.setPassword(env.resolvePlaceholders("${spring.cloud.nacos.password:}"));
  685 | 		}
  686 | 	}
  687 | 
  688 | 	public Properties getNacosProperties() {
  689 | 		Properties properties = new Properties();
  690 | 		properties.put(SERVER_ADDR, serverAddr);
  691 | 		properties.put(USERNAME, Objects.toString(username, ""));
  692 | 		properties.put(PASSWORD, Objects.toString(password, ""));
  693 | 		properties.put(NAMESPACE, namespace);
  694 | 		properties.put(UtilAndComs.NACOS_NAMING_LOG_NAME, logName);
  695 | 
  696 | 		if (endpoint.contains(":")) {
  697 | 			int index = endpoint.indexOf(":");
  698 | 			properties.put(ENDPOINT, endpoint.substring(0, index));
  699 | 			properties.put(ENDPOINT_PORT, endpoint.substring(index + 1));
  700 | 		}
  701 | 		else {
  702 | 			properties.put(ENDPOINT, endpoint);
  703 | 		}
  704 | 
  705 | 		properties.put(ACCESS_KEY, accessKey);
  706 | 		properties.put(SECRET_KEY, secretKey);
  707 | 		// only used for instance.setClusterName()
  708 | //		properties.put(CLUSTER_NAME, clusterName);
  709 | 		properties.put(NAMING_LOAD_CACHE_AT_START, namingLoadCacheAtStart);
  710 | 
  711 | 		enrichNacosDiscoveryProperties(properties);
  712 | 		return properties;
  713 | 	}
  714 | 
  715 | 	private void enrichNacosDiscoveryProperties(Properties nacosDiscoveryProperties) {
  716 | 		Map<String, Object> properties = PropertySourcesUtils
  717 | 				.getSubProperties((ConfigurableEnvironment) environment, PREFIX);
  718 | 		properties.forEach((k, v) -> nacosDiscoveryProperties.putIfAbsent(resolveKey(k),
  719 | 				String.valueOf(v)));
  720 | 	}
  721 | 
  722 | 	private String resolveKey(String key) {
  723 | 		Matcher matcher = PATTERN.matcher(key);
  724 | 		StringBuffer sb = new StringBuffer();
  725 | 		while (matcher.find()) {
  726 | 			matcher.appendReplacement(sb, matcher.group(1).toUpperCase(Locale.ROOT));
  727 | 		}
  728 | 		matcher.appendTail(sb);
  729 | 		return sb.toString();
  730 | 	}
  731 | 
  732 | }
```
