<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-config-client:5.1.0 | path: org/springframework/cloud/config/client/ConfigServicePropertySourceLocator.java -->
<!-- 正文引用：[[源码:ConfigServicePropertySourceLocator.java#L起-L止]] ；代码块头部：(src: ConfigServicePropertySourceLocator.java#L起-L止) -->

# ConfigServicePropertySourceLocator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-config-client:5.1.0` |
| 路径 | `org/springframework/cloud/config/client/ConfigServicePropertySourceLocator.java` |
| 行数 | 374 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-config-client/5.1.0/spring-cloud-config-client-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:48.449Z |

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
   19 | import java.nio.charset.Charset;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.Collection;
   23 | import java.util.Collections;
   24 | import java.util.HashMap;
   25 | import java.util.LinkedHashMap;
   26 | import java.util.List;
   27 | import java.util.Map;
   28 | import java.util.stream.Collectors;
   29 | import java.util.stream.Stream;
   30 | 
   31 | import org.apache.commons.logging.Log;
   32 | import org.apache.commons.logging.LogFactory;
   33 | 
   34 | import org.springframework.boot.env.OriginTrackedMapPropertySource;
   35 | import org.springframework.boot.origin.Origin;
   36 | import org.springframework.boot.origin.OriginTrackedValue;
   37 | import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
   38 | import org.springframework.cloud.bootstrap.support.OriginTrackedCompositePropertySource;
   39 | import org.springframework.cloud.config.client.ConfigClientProperties.Credentials;
   40 | import org.springframework.cloud.config.client.ConfigClientProperties.MultipleUriStrategy;
   41 | import org.springframework.cloud.config.client.validation.ApplicationNameValidator;
   42 | import org.springframework.cloud.config.environment.Environment;
   43 | import org.springframework.cloud.config.environment.PropertySource;
   44 | import org.springframework.core.annotation.Order;
   45 | import org.springframework.core.env.CompositePropertySource;
   46 | import org.springframework.core.env.MapPropertySource;
   47 | import org.springframework.http.HttpEntity;
   48 | import org.springframework.http.HttpHeaders;
   49 | import org.springframework.http.HttpMethod;
   50 | import org.springframework.http.HttpStatus;
   51 | import org.springframework.http.MediaType;
   52 | import org.springframework.http.ResponseEntity;
   53 | import org.springframework.http.client.ClientHttpRequestInterceptor;
   54 | import org.springframework.util.Assert;
   55 | import org.springframework.util.StringUtils;
   56 | import org.springframework.web.client.HttpClientErrorException;
   57 | import org.springframework.web.client.HttpServerErrorException;
   58 | import org.springframework.web.client.ResourceAccessException;
   59 | import org.springframework.web.client.RestTemplate;
   60 | import org.springframework.web.client.UnknownContentTypeException;
   61 | 
   62 | import static org.springframework.cloud.config.client.ConfigClientProperties.STATE_HEADER;
   63 | import static org.springframework.cloud.config.client.ConfigClientProperties.TOKEN_HEADER;
   64 | 
   65 | /**
   66 |  * @author Dave Syer
   67 |  * @author Mathieu Ouellet
   68 |  * @author Marnee DeRider
   69 |  */
   70 | @Order(0)
   71 | public class ConfigServicePropertySourceLocator implements PropertySourceLocator {
   72 | 
   73 | 	private static Log logger = LogFactory.getLog(ConfigServicePropertySourceLocator.class);
   74 | 
   75 | 	private RestTemplate restTemplate;
   76 | 
   77 | 	private ConfigClientProperties defaultProperties;
   78 | 
   79 | 	private List<ClientHttpRequestInterceptor> additionalInterceptors = Collections.emptyList();
   80 | 
   81 | 	public ConfigServicePropertySourceLocator(ConfigClientProperties defaultProperties) {
   82 | 		this.defaultProperties = defaultProperties;
   83 | 	}
   84 | 
   85 | 	/**
   86 | 	 * Additional {@link ClientHttpRequestInterceptor}s applied to the
   87 | 	 * {@link RestTemplate} built per
   88 | 	 * {@link #locate(org.springframework.core.env.Environment)} call. Used by
   89 | 	 * {@link ConfigServiceBootstrapConfiguration} to attach the OAuth2 bearer-token
   90 | 	 * interceptor when {@code spring-security-oauth2-client} is on the classpath.
   91 | 	 * @param additionalInterceptors the interceptors to apply
   92 | 	 */
   93 | 	public void setAdditionalInterceptors(List<ClientHttpRequestInterceptor> additionalInterceptors) {
   94 | 		this.additionalInterceptors = (additionalInterceptors != null) ? additionalInterceptors
   95 | 				: Collections.emptyList();
   96 | 	}
   97 | 
   98 | 	/**
   99 | 	 * Combine the active and default profiles from the environment.
  100 | 	 * @param properties config client properties,
  101 | 	 * @param environment application environment.
  102 | 	 * @return A list of combined profiles.
  103 | 	 */
  104 | 	private List<String> combineProfiles(ConfigClientProperties properties,
  105 | 			org.springframework.core.env.Environment environment) {
  106 | 		List<String> combinedProfiles = new ArrayList<>();
  107 | 		if (environment.getActiveProfiles().length > 0) {
  108 | 			List<String> finalCombinedProfiles = combinedProfiles;
  109 | 			List<String> filteredActiveProfiles = Stream.of(environment.getActiveProfiles())
  110 | 				.filter(s -> !finalCombinedProfiles.contains(s))
  111 | 				.collect(Collectors.toList());
  112 | 			combinedProfiles.addAll(filteredActiveProfiles);
  113 | 		}
  114 | 		else if (environment.getDefaultProfiles().length > 0 && combinedProfiles.isEmpty()) {
  115 | 			combinedProfiles = Arrays.asList(environment.getDefaultProfiles());
  116 | 		}
  117 | 		return combinedProfiles;
  118 | 	}
  119 | 
  120 | 	@Override
  121 | 	public org.springframework.core.env.PropertySource<?> locate(org.springframework.core.env.Environment environment) {
  122 | 		ConfigClientProperties properties = this.defaultProperties.override(environment);
  123 | 		if (!StringUtils.hasText(properties.getProfile())) {
  124 | 			properties.setProfile(String.join(",", combineProfiles(properties, environment)));
  125 | 		}
  126 | 
  127 | 		if (!ApplicationNameValidator.validate(properties, logger)) {
  128 | 			return null;
  129 | 		}
  130 | 
  131 | 		CompositePropertySource composite = new OriginTrackedCompositePropertySource("configService");
  132 | 		ConfigClientRequestTemplateFactory requestTemplateFactory = new ConfigClientRequestTemplateFactory(logger,
  133 | 				properties, this.additionalInterceptors);
  134 | 
  135 | 		Exception error = null;
  136 | 		String errorBody = null;
  137 | 		try {
  138 | 			String[] labels = new String[] { "" };
  139 | 			if (StringUtils.hasText(properties.getLabel())) {
  140 | 				labels = StringUtils.commaDelimitedListToStringArray(properties.getLabel());
  141 | 			}
  142 | 			String state = ConfigClientStateHolder.getState();
  143 | 			// Try all the labels until one works
  144 | 			for (String label : labels) {
  145 | 				Environment result = getRemoteEnvironment(requestTemplateFactory, label.trim(), state);
  146 | 				if (result != null) {
  147 | 					log(result);
  148 | 
  149 | 					// result.getPropertySources() can be null if using xml
  150 | 					if (result.getPropertySources() != null) {
  151 | 						for (PropertySource source : result.getPropertySources()) {
  152 | 							@SuppressWarnings("unchecked")
  153 | 							Map<String, Object> map = translateOrigins(source.getName(),
  154 | 									(Map<String, Object>) source.getSource());
  155 | 							composite.addPropertySource(new OriginTrackedMapPropertySource(source.getName(), map));
  156 | 						}
  157 | 					}
  158 | 
  159 | 					HashMap<String, Object> map = new HashMap<>();
  160 | 					if (StringUtils.hasText(result.getState())) {
  161 | 						putValue(map, "config.client.state", result.getState());
  162 | 					}
  163 | 					if (StringUtils.hasText(result.getVersion())) {
  164 | 						putValue(map, "config.client.version", result.getVersion());
  165 | 					}
  166 | 					// the existence of this property source confirms a successful
  167 | 					// response from config server
  168 | 					composite.addFirstPropertySource(new MapPropertySource("configClient", map));
  169 | 					return composite;
  170 | 				}
  171 | 			}
  172 | 			errorBody = String.format("None of labels %s found", Arrays.toString(labels));
  173 | 		}
  174 | 		catch (HttpServerErrorException e) {
  175 | 			error = e;
  176 | 			if (MediaType.APPLICATION_JSON.includes(e.getResponseHeaders().getContentType())) {
  177 | 				errorBody = e.getResponseBodyAsString();
  178 | 			}
  179 | 		}
  180 | 		catch (Exception e) {
  181 | 			error = e;
  182 | 		}
  183 | 		if (properties.isFailFast()) {
  184 | 			throw new IllegalStateException("Could not locate PropertySource and the fail fast property is set, failing"
  185 | 					+ (errorBody == null ? "" : ": " + errorBody), error);
  186 | 		}
  187 | 		logger.warn("Could not locate PropertySource: " + (error != null ? error.getMessage() : errorBody));
  188 | 		return null;
  189 | 
  190 | 	}
  191 | 
  192 | 	@Override
  193 | 	public Collection<org.springframework.core.env.PropertySource<?>> locateCollection(
  194 | 			org.springframework.core.env.Environment environment) {
  195 | 		return PropertySourceLocator.locateCollection(this, environment);
  196 | 	}
  197 | 
  198 | 	private void log(Environment result) {
  199 | 		if (logger.isInfoEnabled()) {
  200 | 			logger.info(String.format("Located environment: name=%s, profiles=%s, label=%s, version=%s, state=%s",
  201 | 					result.getName(), result.getProfiles() == null ? "" : Arrays.asList(result.getProfiles()),
  202 | 					result.getLabel(), result.getVersion(), result.getState()));
  203 | 		}
  204 | 		if (logger.isDebugEnabled()) {
  205 | 			List<PropertySource> propertySourceList = result.getPropertySources();
  206 | 			if (propertySourceList != null) {
  207 | 				int propertyCount = 0;
  208 | 				for (PropertySource propertySource : propertySourceList) {
  209 | 					propertyCount += propertySource.getSource().size();
  210 | 				}
  211 | 				logger.debug(String.format("Environment %s has %d property sources with %d properties.",
  212 | 						result.getName(), result.getPropertySources().size(), propertyCount));
  213 | 			}
  214 | 
  215 | 		}
  216 | 	}
  217 | 
  218 | 	private Map<String, Object> translateOrigins(String name, Map<String, Object> source) {
  219 | 		Map<String, Object> withOrigins = new LinkedHashMap<>();
  220 | 		for (Map.Entry<String, Object> entry : source.entrySet()) {
  221 | 			boolean hasOrigin = false;
  222 | 
  223 | 			if (entry.getValue() instanceof Map) {
  224 | 				@SuppressWarnings("unchecked")
  225 | 				Map<String, Object> value = (Map<String, Object>) entry.getValue();
  226 | 				if (value.size() == 2 && value.containsKey("origin") && value.containsKey("value")) {
  227 | 					Origin origin = new ConfigServiceOrigin(name, value.get("origin"));
  228 | 					OriginTrackedValue trackedValue = OriginTrackedValue.of(value.get("value"), origin);
  229 | 					withOrigins.put(entry.getKey(), trackedValue);
  230 | 					hasOrigin = true;
  231 | 				}
  232 | 			}
  233 | 
  234 | 			if (!hasOrigin) {
  235 | 				withOrigins.put(entry.getKey(), entry.getValue());
  236 | 			}
  237 | 		}
  238 | 		return withOrigins;
  239 | 	}
  240 | 
  241 | 	private void putValue(HashMap<String, Object> map, String key, String value) {
  242 | 		if (StringUtils.hasText(value)) {
  243 | 			map.put(key, value);
  244 | 		}
  245 | 	}
  246 | 
  247 | 	private Environment getRemoteEnvironment(ConfigClientRequestTemplateFactory requestTemplateFactory, String label,
  248 | 			String state) {
  249 | 		RestTemplate restTemplate = this.restTemplate == null ? requestTemplateFactory.create() : this.restTemplate;
  250 | 		ConfigClientProperties properties = requestTemplateFactory.getProperties();
  251 | 		String path = "/{name}/{profile}";
  252 | 		String name = properties.getName();
  253 | 		String profile = properties.getProfile();
  254 | 		String token = properties.getToken();
  255 | 		int noOfUrls = properties.getUri().length;
  256 | 		if (noOfUrls > 1) {
  257 | 			logger.info("Multiple Config Server Urls found listed.");
  258 | 		}
  259 | 
  260 | 		Object[] args = new String[] { name, profile };
  261 | 		if (StringUtils.hasText(label)) {
  262 | 			// workaround for Spring MVC matching / in paths
  263 | 			label = Environment.denormalize(label);
  264 | 			args = new String[] { name, profile, label };
  265 | 			path = path + "/{label}";
  266 | 		}
  267 | 		ResponseEntity<Environment> response = null;
  268 | 		List<MediaType> acceptHeader = Collections.singletonList(MediaType.parseMediaType(properties.getMediaType()));
  269 | 		List<Charset> acceptCharsetHeader = Collections.singletonList(properties.getCharset());
  270 | 
  271 | 		for (int i = 0; i < noOfUrls; i++) {
  272 | 			Credentials credentials = properties.getCredentials(i);
  273 | 			String uri = credentials.getUri();
  274 | 			String username = credentials.getUsername();
  275 | 			String password = credentials.getPassword();
  276 | 
  277 | 			logger.info("Fetching config from server at : " + uri);
  278 | 
  279 | 			try {
  280 | 				HttpHeaders headers = new HttpHeaders();
  281 | 				headers.setAccept(acceptHeader);
  282 | 				headers.setAcceptCharset(acceptCharsetHeader);
  283 | 				requestTemplateFactory.addAuthorizationToken(headers, username, password);
  284 | 				if (StringUtils.hasText(token)) {
  285 | 					headers.add(TOKEN_HEADER, token);
  286 | 				}
  287 | 				if (StringUtils.hasText(state) && properties.isSendState()) {
  288 | 					headers.add(STATE_HEADER, state);
  289 | 				}
  290 | 
  291 | 				final HttpEntity<Void> entity = new HttpEntity<>(null, headers);
  292 | 				response = restTemplate.exchange(uri + path, HttpMethod.GET, entity, Environment.class, args);
  293 | 			}
  294 | 			catch (HttpClientErrorException | HttpServerErrorException e) {
  295 | 				if (i < noOfUrls - 1 && defaultProperties.getMultipleUriStrategy() == MultipleUriStrategy.ALWAYS) {
  296 | 					logger.info("Failed to fetch configs from server at  : " + uri
  297 | 							+ ". Will try the next url if available. Error : " + e.getMessage());
  298 | 					continue;
  299 | 				}
  300 | 
  301 | 				if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
  302 | 					throw e;
  303 | 				}
  304 | 			}
  305 | 			catch (ResourceAccessException e) {
  306 | 				logger.info("Exception on Url - " + uri + ":" + e + ". Will be trying the next url if available");
  307 | 				if (i == noOfUrls - 1) {
  308 | 					throw e;
  309 | 				}
  310 | 				else {
  311 | 					continue;
  312 | 				}
  313 | 			}
  314 | 			catch (UnknownContentTypeException e) {
  315 | 				if (i < noOfUrls - 1 && defaultProperties.getMultipleUriStrategy() == MultipleUriStrategy.ALWAYS) {
  316 | 					logger.info(
  317 | 							"Failed to read the response of a config server. Will try the next url if available. Error : "
  318 | 									+ e.getMessage());
  319 | 					continue;
  320 | 				}
  321 | 				throw e;
  322 | 			}
  323 | 
  324 | 			if (response == null) {
  325 | 				if (i < noOfUrls - 1 && defaultProperties.getMultipleUriStrategy() == MultipleUriStrategy.ALWAYS) {
  326 | 					logger.info("Failed to fetch configs from server at  : " + uri
  327 | 							+ ". The response was null. Will try the next url if available.");
  328 | 					continue;
  329 | 				}
  330 | 
  331 | 				return null;
  332 | 			}
  333 | 			else if (response.getStatusCode() != HttpStatus.OK) {
  334 | 				if (i < noOfUrls - 1 && defaultProperties.getMultipleUriStrategy() == MultipleUriStrategy.ALWAYS) {
  335 | 					logger.info("Failed to fetch configs from server at  : " + uri
  336 | 							+ ". Will try the next url if available. StatusCode : " + response.getStatusCode());
  337 | 					continue;
  338 | 				}
  339 | 
  340 | 				return null;
  341 | 			}
  342 | 
  343 | 			Environment result = response.getBody();
  344 | 			return result;
  345 | 		}
  346 | 
  347 | 		return null;
  348 | 	}
  349 | 
  350 | 	public void setRestTemplate(RestTemplate restTemplate) {
  351 | 		this.restTemplate = restTemplate;
  352 | 	}
  353 | 
  354 | 	static class ConfigServiceOrigin implements Origin {
  355 | 
  356 | 		private final String remotePropertySource;
  357 | 
  358 | 		private final Object origin;
  359 | 
  360 | 		ConfigServiceOrigin(String remotePropertySource, Object origin) {
  361 | 			this.remotePropertySource = remotePropertySource;
  362 | 			Assert.notNull(origin, "origin may not be null");
  363 | 			this.origin = origin;
  364 | 
  365 | 		}
  366 | 
  367 | 		@Override
  368 | 		public String toString() {
  369 | 			return "Config Server " + this.remotePropertySource + ":" + this.origin.toString();
  370 | 		}
  371 | 
  372 | 	}
  373 | 
  374 | }
```
