<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-config-client:5.1.0 | path: org/springframework/cloud/config/client/ConfigClientProperties.java -->
<!-- 正文引用：[[源码:ConfigClientProperties.java#L起-L止]] ；代码块头部：(src: ConfigClientProperties.java#L起-L止) -->

# ConfigClientProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-config-client:5.1.0` |
| 路径 | `org/springframework/cloud/config/client/ConfigClientProperties.java` |
| 行数 | 567 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-config-client/5.1.0/spring-cloud-config-client-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:48.025Z |

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
   17 | package org.springframework.cloud.config.client;
   18 | 
   19 | import java.net.MalformedURLException;
   20 | import java.net.URL;
   21 | import java.net.URLDecoder;
   22 | import java.nio.charset.Charset;
   23 | import java.nio.charset.StandardCharsets;
   24 | import java.util.Arrays;
   25 | import java.util.HashMap;
   26 | import java.util.Map;
   27 | 
   28 | import org.springframework.beans.BeanUtils;
   29 | import org.springframework.beans.factory.annotation.Value;
   30 | import org.springframework.boot.context.properties.ConfigurationProperties;
   31 | import org.springframework.cloud.config.environment.EnvironmentMediaType;
   32 | import org.springframework.cloud.configuration.TlsProperties;
   33 | import org.springframework.core.env.Environment;
   34 | import org.springframework.util.Assert;
   35 | import org.springframework.util.ObjectUtils;
   36 | import org.springframework.util.StringUtils;
   37 | import org.springframework.util.unit.DataSize;
   38 | import org.springframework.web.util.UriComponentsBuilder;
   39 | 
   40 | /**
   41 |  * @author Dave Syer
   42 |  *
   43 |  */
   44 | @ConfigurationProperties(ConfigClientProperties.PREFIX)
   45 | public class ConfigClientProperties {
   46 | 
   47 | 	/**
   48 | 	 * Prefix for Spring Cloud Config properties.
   49 | 	 */
   50 | 	public static final String PREFIX = "spring.cloud.config";
   51 | 
   52 | 	/**
   53 | 	 * Default application name.
   54 | 	 */
   55 | 	public static final String DEFAULT_APPLICATION = "application";
   56 | 
   57 | 	/**
   58 | 	 * Placeholder string that allows ${spring.cloud.config.name} to override
   59 | 	 * ${spring.application.name:application}.
   60 | 	 */
   61 | 	public static final String NAME_PLACEHOLDER = "${" + ConfigClientProperties.PREFIX
   62 | 			+ ".name:${spring.application.name:" + DEFAULT_APPLICATION + "}}";
   63 | 
   64 | 	/**
   65 | 	 * Name of config discovery enabled property.
   66 | 	 */
   67 | 	public static final String CONFIG_DISCOVERY_ENABLED = PREFIX + ".discovery.enabled";
   68 | 
   69 | 	/**
   70 | 	 * Token header name.
   71 | 	 */
   72 | 	public static final String TOKEN_HEADER = "X-Config-Token";
   73 | 
   74 | 	/**
   75 | 	 * State header name.
   76 | 	 */
   77 | 	public static final String STATE_HEADER = "X-Config-State";
   78 | 
   79 | 	/**
   80 | 	 * Authorization header name.
   81 | 	 */
   82 | 	public static final String AUTHORIZATION = "authorization";
   83 | 
   84 | 	/**
   85 | 	 * Default profile value.
   86 | 	 */
   87 | 	public static final String DEFAULT_PROFILE = "default";
   88 | 
   89 | 	/**
   90 | 	 * Flag to say that remote configuration is enabled. Default true;
   91 | 	 */
   92 | 	private boolean enabled = true;
   93 | 
   94 | 	/**
   95 | 	 * The default profile to use when fetching remote configuration (comma-separated).
   96 | 	 * Default is "default".
   97 | 	 */
   98 | 	private String profile = DEFAULT_PROFILE;
   99 | 
  100 | 	/**
  101 | 	 * Name of application used to fetch remote properties.
  102 | 	 */
  103 | 	@Value("${spring.application.name:" + DEFAULT_APPLICATION + "}")
  104 | 	private String name;
  105 | 
  106 | 	/**
  107 | 	 * The label name to use to pull remote configuration properties. The default is set
  108 | 	 * on the server (generally "main" for a git based server).
  109 | 	 */
  110 | 	private String label;
  111 | 
  112 | 	/**
  113 | 	 * The username to use (HTTP Basic) when contacting the remote server.
  114 | 	 */
  115 | 	private String username;
  116 | 
  117 | 	/**
  118 | 	 * The password to use (HTTP Basic) when contacting the remote server.
  119 | 	 */
  120 | 	private String password;
  121 | 
  122 | 	/**
  123 | 	 * The URI of the remote server (default http://localhost:8888).
  124 | 	 */
  125 | 	private String[] uri = { "http://localhost:8888" };
  126 | 
  127 | 	/**
  128 | 	 * The strategy to use when call to server fails and there are multiple URLs
  129 | 	 * configured on the uri property (default {@link MultipleUriStrategy#ALWAYS}).
  130 | 	 */
  131 | 	private MultipleUriStrategy multipleUriStrategy = MultipleUriStrategy.ALWAYS;
  132 | 
  133 | 	/**
  134 | 	 * The Accept header media type to send to config server.
  135 | 	 */
  136 | 	private String mediaType = EnvironmentMediaType.V2_JSON;
  137 | 
  138 | 	/**
  139 | 	 * The charset to read the resource from the config server.
  140 | 	 */
  141 | 	private Charset charset = StandardCharsets.UTF_8;
  142 | 
  143 | 	/**
  144 | 	 * Discovery properties.
  145 | 	 */
  146 | 	private Discovery discovery = new Discovery();
  147 | 
  148 | 	/**
  149 | 	 * Settings of the {@link ConfigResourceClient} that reads plain text and binary files
  150 | 	 * from the config server.
  151 | 	 */
  152 | 	private ResourceClient resourceClient = new ResourceClient();
  153 | 
  154 | 	/**
  155 | 	 * TLS properties.
  156 | 	 */
  157 | 	private TlsProperties tls = new TlsProperties();
  158 | 
  159 | 	/**
  160 | 	 * Flag to indicate that failure to connect to the server is fatal (default false).
  161 | 	 */
  162 | 	private boolean failFast = false;
  163 | 
  164 | 	/**
  165 | 	 * Security Token passed thru to underlying environment repository.
  166 | 	 */
  167 | 	private String token;
  168 | 
  169 | 	/**
  170 | 	 * timeout on waiting to read data from the Config Server.
  171 | 	 */
  172 | 	private int requestReadTimeout = (60 * 1000 * 3) + 5000;
  173 | 
  174 | 	/**
  175 | 	 * timeout on waiting to connect to the Config Server.
  176 | 	 */
  177 | 	private int requestConnectTimeout = 1000 * 10;
  178 | 
  179 | 	/**
  180 | 	 * Flag to indicate whether to send state. Default true.
  181 | 	 */
  182 | 	private boolean sendState = true;
  183 | 
  184 | 	/**
  185 | 	 * Additional headers used to create the client request.
  186 | 	 */
  187 | 	private Map<String, String> headers = new HashMap<>();
  188 | 
  189 | 	/**
  190 | 	 * If set to true the client will send all labels to the server instead of sending one
  191 | 	 * at a time. Support for this would require a config server version of 4.2.0 or
  192 | 	 * higher.
  193 | 	 */
  194 | 	private boolean sendAllLabels = false;
  195 | 
  196 | 	ConfigClientProperties() {
  197 | 	}
  198 | 
  199 | 	public ConfigClientProperties(Environment environment) {
  200 | 		String[] profiles = environment.getActiveProfiles();
  201 | 		if (profiles.length == 0) {
  202 | 			profiles = environment.getDefaultProfiles();
  203 | 		}
  204 | 		this.setProfile(StringUtils.arrayToCommaDelimitedString(profiles));
  205 | 	}
  206 | 
  207 | 	public boolean isEnabled() {
  208 | 		return this.enabled;
  209 | 	}
  210 | 
  211 | 	public void setEnabled(boolean enabled) {
  212 | 		this.enabled = enabled;
  213 | 	}
  214 | 
  215 | 	public String[] getUri() {
  216 | 		return this.uri;
  217 | 	}
  218 | 
  219 | 	public void setUri(String[] url) {
  220 | 		this.uri = url;
  221 | 	}
  222 | 
  223 | 	public MultipleUriStrategy getMultipleUriStrategy() {
  224 | 		return multipleUriStrategy;
  225 | 	}
  226 | 
  227 | 	public void setMultipleUriStrategy(MultipleUriStrategy multipleUriStrategy) {
  228 | 		this.multipleUriStrategy = multipleUriStrategy;
  229 | 	}
  230 | 
  231 | 	public String getName() {
  232 | 		return this.name;
  233 | 	}
  234 | 
  235 | 	public void setName(String name) {
  236 | 		this.name = name;
  237 | 	}
  238 | 
  239 | 	public String getProfile() {
  240 | 		return this.profile;
  241 | 	}
  242 | 
  243 | 	public void setProfile(String env) {
  244 | 		this.profile = env;
  245 | 	}
  246 | 
  247 | 	public String getLabel() {
  248 | 		return this.label;
  249 | 	}
  250 | 
  251 | 	public void setLabel(String label) {
  252 | 		this.label = label;
  253 | 	}
  254 | 
  255 | 	public String getUsername() {
  256 | 		return this.username;
  257 | 	}
  258 | 
  259 | 	public void setUsername(String username) {
  260 | 		this.username = username;
  261 | 	}
  262 | 
  263 | 	public String getPassword() {
  264 | 		return this.password;
  265 | 	}
  266 | 
  267 | 	public void setPassword(String password) {
  268 | 		this.password = password;
  269 | 	}
  270 | 
  271 | 	public Credentials getCredentials(int index) {
  272 | 		return extractCredentials(index);
  273 | 	}
  274 | 
  275 | 	public String getMediaType() {
  276 | 		return this.mediaType;
  277 | 	}
  278 | 
  279 | 	public void setMediaType(String mediaType) {
  280 | 		this.mediaType = mediaType;
  281 | 	}
  282 | 
  283 | 	public Charset getCharset() {
  284 | 		return charset;
  285 | 	}
  286 | 
  287 | 	public void setCharset(Charset charset) {
  288 | 		this.charset = charset;
  289 | 	}
  290 | 
  291 | 	public Discovery getDiscovery() {
  292 | 		return this.discovery;
  293 | 	}
  294 | 
  295 | 	public ResourceClient getResourceClient() {
  296 | 		return this.resourceClient;
  297 | 	}
  298 | 
  299 | 	public void setResourceClient(ResourceClient resourceClient) {
  300 | 		this.resourceClient = resourceClient;
  301 | 	}
  302 | 
  303 | 	public void setDiscovery(Discovery discovery) {
  304 | 		this.discovery = discovery;
  305 | 	}
  306 | 
  307 | 	public TlsProperties getTls() {
  308 | 		return tls;
  309 | 	}
  310 | 
  311 | 	public void setTls(TlsProperties tls) {
  312 | 		this.tls = tls;
  313 | 	}
  314 | 
  315 | 	public boolean isFailFast() {
  316 | 		return this.failFast;
  317 | 	}
  318 | 
  319 | 	public void setFailFast(boolean failFast) {
  320 | 		this.failFast = failFast;
  321 | 	}
  322 | 
  323 | 	public String getToken() {
  324 | 		return this.token;
  325 | 	}
  326 | 
  327 | 	public void setToken(String token) {
  328 | 		this.token = token;
  329 | 	}
  330 | 
  331 | 	public int getRequestReadTimeout() {
  332 | 		return this.requestReadTimeout;
  333 | 	}
  334 | 
  335 | 	public void setRequestReadTimeout(int requestReadTimeout) {
  336 | 		this.requestReadTimeout = requestReadTimeout;
  337 | 	}
  338 | 
  339 | 	public int getRequestConnectTimeout() {
  340 | 		return this.requestConnectTimeout;
  341 | 	}
  342 | 
  343 | 	public void setRequestConnectTimeout(int requestConnectTimeout) {
  344 | 		this.requestConnectTimeout = requestConnectTimeout;
  345 | 	}
  346 | 
  347 | 	public boolean isSendState() {
  348 | 		return this.sendState;
  349 | 	}
  350 | 
  351 | 	public void setSendState(boolean sendState) {
  352 | 		this.sendState = sendState;
  353 | 	}
  354 | 
  355 | 	public Map<String, String> getHeaders() {
  356 | 		return this.headers;
  357 | 	}
  358 | 
  359 | 	public void setHeaders(Map<String, String> headers) {
  360 | 		this.headers = headers;
  361 | 	}
  362 | 
  363 | 	public boolean isSendAllLabels() {
  364 | 		return sendAllLabels;
  365 | 	}
  366 | 
  367 | 	public void setSendAllLabels(boolean sendAllLabels) {
  368 | 		this.sendAllLabels = sendAllLabels;
  369 | 	}
  370 | 
  371 | 	private Credentials extractCredentials(int index) {
  372 | 		Credentials result = new Credentials();
  373 | 		int noOfUrl = this.uri.length;
  374 | 		if (index < 0 || index >= noOfUrl) {
  375 | 			throw new IllegalStateException("Trying to access an invalid array index");
  376 | 		}
  377 | 		String uri = this.uri[index];
  378 | 		result.uri = uri;
  379 | 		Credentials explicitCredentials = getUsernamePassword();
  380 | 		result.username = explicitCredentials.username;
  381 | 		result.password = explicitCredentials.password;
  382 | 		try {
  383 | 			URL url = new URL(uri);
  384 | 			String userInfo = url.getUserInfo();
  385 | 			// no credentials in url, return explicit credentials
  386 | 			if (ObjectUtils.isEmpty(userInfo) || ":".equals(userInfo)) {
  387 | 				return result;
  388 | 			}
  389 | 			result.uri = UriComponentsBuilder.fromUriString(uri, UriComponentsBuilder.ParserType.WHAT_WG)
  390 | 				.userInfo(null)
  391 | 				.build()
  392 | 				.toUriString();
  393 | 
  394 | 			// if userInfo does not contain a :, then append a : to it
  395 | 			if (!userInfo.contains(":")) {
  396 | 				userInfo = userInfo + ":";
  397 | 			}
  398 | 
  399 | 			int sepIndex = userInfo.indexOf(":");
  400 | 			// set username and password from uri
  401 | 			result.username = userInfo.substring(0, sepIndex);
  402 | 			result.password = userInfo.substring(sepIndex + 1);
  403 | 
  404 | 			// override password if explicitly set
  405 | 			if (explicitCredentials.password != null) {
  406 | 				// Explicit username / password takes precedence
  407 | 				result.password = explicitCredentials.password;
  408 | 			}
  409 | 			// override username if explicitly set
  410 | 			if (!"user".equals(explicitCredentials.username)) {
  411 | 				// But the username can be overridden
  412 | 				result.username = explicitCredentials.username;
  413 | 			}
  414 | 
  415 | 			result.password = URLDecoder.decode(result.password, StandardCharsets.UTF_8);
  416 | 			result.username = URLDecoder.decode(result.username, StandardCharsets.UTF_8);
  417 | 			return result;
  418 | 		}
  419 | 		catch (MalformedURLException e) {
  420 | 			throw new IllegalStateException("Invalid URL: " + uri, e);
  421 | 		}
  422 | 	}
  423 | 
  424 | 	private Credentials getUsernamePassword() {
  425 | 		Credentials credentials = new Credentials();
  426 | 
  427 | 		if (StringUtils.hasText(this.password)) {
  428 | 			credentials.password = this.password.trim();
  429 | 		}
  430 | 
  431 | 		if (StringUtils.hasText(this.username)) {
  432 | 			credentials.username = this.username.trim();
  433 | 		}
  434 | 		else {
  435 | 			credentials.username = "user";
  436 | 		}
  437 | 		return credentials;
  438 | 	}
  439 | 
  440 | 	public ConfigClientProperties override(org.springframework.core.env.Environment environment) {
  441 | 		ConfigClientProperties override = new ConfigClientProperties();
  442 | 		BeanUtils.copyProperties(this, override);
  443 | 		override.setName(environment.resolvePlaceholders(NAME_PLACEHOLDER));
  444 | 		if (environment.containsProperty(ConfigClientProperties.PREFIX + ".profile")) {
  445 | 			override.setProfile(environment.getProperty(ConfigClientProperties.PREFIX + ".profile"));
  446 | 		}
  447 | 		if (environment.containsProperty(ConfigClientProperties.PREFIX + ".label")) {
  448 | 			override.setLabel(environment.getProperty(ConfigClientProperties.PREFIX + ".label"));
  449 | 		}
  450 | 		return override;
  451 | 	}
  452 | 
  453 | 	@Override
  454 | 	public String toString() {
  455 | 		return "ConfigClientProperties [enabled=" + this.enabled + ", profile=" + this.profile + ", name=" + this.name
  456 | 				+ ", label=" + this.label + ", username=" + this.username + ", password=" + this.password + ", uri="
  457 | 				+ Arrays.toString(this.uri) + ", mediaType=" + this.mediaType + ", discovery=" + this.discovery
  458 | 				+ ", failFast=" + this.failFast + ", token=" + this.token + ", requestConnectTimeout="
  459 | 				+ this.requestConnectTimeout + ", requestReadTimeout=" + this.requestReadTimeout + ", sendState="
  460 | 				+ this.sendState + ", headers=" + this.headers + ", sendAllLabels=" + this.sendAllLabels + "]";
  461 | 	}
  462 | 
  463 | 	/**
  464 | 	 * Credentials properties.
  465 | 	 */
  466 | 	public static class Credentials {
  467 | 
  468 | 		private String username;
  469 | 
  470 | 		private String password;
  471 | 
  472 | 		private String uri;
  473 | 
  474 | 		public String getUsername() {
  475 | 			return this.username;
  476 | 		}
  477 | 
  478 | 		public String getPassword() {
  479 | 			return this.password;
  480 | 		}
  481 | 
  482 | 		public String getUri() {
  483 | 			return this.uri;
  484 | 		}
  485 | 
  486 | 	}
  487 | 
  488 | 	/**
  489 | 	 * Settings of the {@link ConfigResourceClient}.
  490 | 	 */
  491 | 	public static class ResourceClient {
  492 | 
  493 | 		/**
  494 | 		 * Maximum size of a file read from the config server; a larger file is rejected
  495 | 		 * instead of being loaded into memory. Zero disables the limit.
  496 | 		 */
  497 | 		private DataSize maxSize = DataSize.ofMegabytes(1);
  498 | 
  499 | 		public DataSize getMaxSize() {
  500 | 			return this.maxSize;
  501 | 		}
  502 | 
  503 | 		public void setMaxSize(DataSize maxSize) {
  504 | 			Assert.isTrue(!maxSize.isNegative(), "maxSize must not be negative");
  505 | 			this.maxSize = maxSize;
  506 | 		}
  507 | 
  508 | 	}
  509 | 
  510 | 	/**
  511 | 	 * Discovery properties.
  512 | 	 */
  513 | 	public static class Discovery {
  514 | 
  515 | 		/**
  516 | 		 * Default config server service id name.
  517 | 		 */
  518 | 		public static final String DEFAULT_CONFIG_SERVER = "configserver";
  519 | 
  520 | 		/**
  521 | 		 * Flag to indicate that config server discovery is enabled (config server URL
  522 | 		 * will be looked up via discovery).
  523 | 		 */
  524 | 		private boolean enabled;
  525 | 
  526 | 		/**
  527 | 		 * Service id to locate config server.
  528 | 		 */
  529 | 		private String serviceId = DEFAULT_CONFIG_SERVER;
  530 | 
  531 | 		public boolean isEnabled() {
  532 | 			return this.enabled;
  533 | 		}
  534 | 
  535 | 		public void setEnabled(boolean enabled) {
  536 | 			this.enabled = enabled;
  537 | 		}
  538 | 
  539 | 		public String getServiceId() {
  540 | 			return this.serviceId;
  541 | 		}
  542 | 
  543 | 		public void setServiceId(String serviceId) {
  544 | 			this.serviceId = serviceId;
  545 | 		}
  546 | 
  547 | 	}
  548 | 
  549 | 	/**
  550 | 	 * Enumerates possible strategies to use when multiple URLs are provided and an error
  551 | 	 * occurs.
  552 | 	 */
  553 | 	public enum MultipleUriStrategy {
  554 | 
  555 | 		/**
  556 | 		 * Try the next URL in the list on any error.
  557 | 		 */
  558 | 		ALWAYS,
  559 | 
  560 | 		/**
  561 | 		 * Try the next URL in the list only if no response was received.
  562 | 		 */
  563 | 		CONNECTION_TIMEOUT_ONLY
  564 | 
  565 | 	}
  566 | 
  567 | }
```
