<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-config-client:5.1.0 | path: org/springframework/cloud/config/client/ConfigServerConfigDataLoader.java -->
<!-- 正文引用：[[源码:ConfigServerConfigDataLoader.java#L起-L止]] ；代码块头部：(src: ConfigServerConfigDataLoader.java#L起-L止) -->

# ConfigServerConfigDataLoader.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-config-client:5.1.0` |
| 路径 | `org/springframework/cloud/config/client/ConfigServerConfigDataLoader.java` |
| 行数 | 408 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-config-client/5.1.0/spring-cloud-config-client-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:49.016Z |

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
   22 | import java.util.Collections;
   23 | import java.util.EnumSet;
   24 | import java.util.HashMap;
   25 | import java.util.LinkedHashMap;
   26 | import java.util.List;
   27 | import java.util.Map;
   28 | 
   29 | import org.apache.commons.logging.Log;
   30 | 
   31 | import org.springframework.boot.context.config.ConfigData;
   32 | import org.springframework.boot.context.config.ConfigData.Option;
   33 | import org.springframework.boot.context.config.ConfigDataLoader;
   34 | import org.springframework.boot.context.config.ConfigDataLoaderContext;
   35 | import org.springframework.boot.context.properties.bind.Binder;
   36 | import org.springframework.boot.env.OriginTrackedMapPropertySource;
   37 | import org.springframework.boot.logging.DeferredLogFactory;
   38 | import org.springframework.boot.origin.Origin;
   39 | import org.springframework.boot.origin.OriginTrackedValue;
   40 | import org.springframework.cloud.config.client.ConfigClientProperties.MultipleUriStrategy;
   41 | import org.springframework.cloud.config.client.ConfigServerBootstrapper.LoadContext;
   42 | import org.springframework.cloud.config.client.ConfigServerBootstrapper.LoaderInterceptor;
   43 | import org.springframework.cloud.config.environment.Environment;
   44 | import org.springframework.core.Ordered;
   45 | import org.springframework.core.env.MapPropertySource;
   46 | import org.springframework.core.env.PropertySource;
   47 | import org.springframework.http.HttpEntity;
   48 | import org.springframework.http.HttpHeaders;
   49 | import org.springframework.http.HttpMethod;
   50 | import org.springframework.http.HttpStatus;
   51 | import org.springframework.http.MediaType;
   52 | import org.springframework.http.ResponseEntity;
   53 | import org.springframework.util.StringUtils;
   54 | import org.springframework.web.client.HttpClientErrorException;
   55 | import org.springframework.web.client.HttpServerErrorException;
   56 | import org.springframework.web.client.ResourceAccessException;
   57 | import org.springframework.web.client.RestTemplate;
   58 | import org.springframework.web.client.UnknownContentTypeException;
   59 | 
   60 | import static org.springframework.cloud.config.client.ConfigClientProperties.DEFAULT_PROFILE;
   61 | import static org.springframework.cloud.config.client.ConfigClientProperties.STATE_HEADER;
   62 | import static org.springframework.cloud.config.client.ConfigClientProperties.TOKEN_HEADER;
   63 | 
   64 | public class ConfigServerConfigDataLoader implements ConfigDataLoader<ConfigServerConfigDataResource>, Ordered {
   65 | 
   66 | 	/**
   67 | 	 * PropertySource name for the config client.
   68 | 	 */
   69 | 	public static final String CONFIG_CLIENT_PROPERTYSOURCE_NAME = "configClient";
   70 | 
   71 | 	private static final EnumSet<Option> ALL_OPTIONS = EnumSet.allOf(Option.class);
   72 | 
   73 | 	private static final String OVERRIDES_NAME = "configserver:overrides";
   74 | 
   75 | 	protected final Log logger;
   76 | 
   77 | 	public ConfigServerConfigDataLoader(DeferredLogFactory logFactory) {
   78 | 		this.logger = logFactory.getLog(getClass());
   79 | 	}
   80 | 
   81 | 	@Override
   82 | 	public int getOrder() {
   83 | 		return -1;
   84 | 	}
   85 | 
   86 | 	@Override
   87 | 	public ConfigData load(ConfigDataLoaderContext context, ConfigServerConfigDataResource resource) {
   88 | 		if (context.getBootstrapContext().isRegistered(ConfigServerInstanceMonitor.class)) {
   89 | 			// force initialization if needed
   90 | 			context.getBootstrapContext().get(ConfigServerInstanceMonitor.class);
   91 | 		}
   92 | 		if (context.getBootstrapContext().isRegistered(LoaderInterceptor.class)) {
   93 | 			LoaderInterceptor interceptor = context.getBootstrapContext().get(LoaderInterceptor.class);
   94 | 			if (interceptor != null) {
   95 | 				Binder binder = context.getBootstrapContext().get(Binder.class);
   96 | 				try {
   97 | 					return interceptor.apply(new LoadContext(context, resource, binder, this::doLoad));
   98 | 				}
   99 | 				catch (ConfigClientFailFastException e) {
  100 | 					context.getBootstrapContext().addCloseListener(event -> {
  101 | 						throw e;
  102 | 					});
  103 | 					return new ConfigData(Collections.emptyList());
  104 | 				}
  105 | 			}
  106 | 		}
  107 | 		return doLoad(context, resource);
  108 | 	}
  109 | 
  110 | 	public ConfigData doLoad(ConfigDataLoaderContext context, ConfigServerConfigDataResource resource) {
  111 | 
  112 | 		ConfigClientProperties properties = resource.getProperties();
  113 | 		List<PropertySource<?>> propertySources = new ArrayList<>();
  114 | 		Exception error = null;
  115 | 		String errorBody = null;
  116 | 		try {
  117 | 			String labelProperty = properties.getLabel();
  118 | 			String[] labels;
  119 | 			if (!properties.isSendAllLabels() && StringUtils.hasText(labelProperty)) {
  120 | 				labels = StringUtils.commaDelimitedListToStringArray(labelProperty);
  121 | 			}
  122 | 			else {
  123 | 				// This could contain a comma separated list of labels sent directly to
  124 | 				// the config server
  125 | 				// For this to work you would need to be using a config server version of
  126 | 				// 4.2.0 or later
  127 | 				labels = new String[] { StringUtils.hasText(labelProperty) ? labelProperty : "" };
  128 | 			}
  129 | 			String state = ConfigClientStateHolder.getState();
  130 | 			for (String label : labels) {
  131 | 				Environment result = getRemoteEnvironment(context, resource, label.trim(), state);
  132 | 				if (result != null) {
  133 | 					log(result);
  134 | 
  135 | 					// result.getPropertySources() can be null if using xml
  136 | 					if (result.getPropertySources() != null) {
  137 | 						for (org.springframework.cloud.config.environment.PropertySource source : result
  138 | 							.getPropertySources()) {
  139 | 							@SuppressWarnings("unchecked")
  140 | 							Map<String, Object> map = translateOrigins(source.getName(),
  141 | 									(Map<String, Object>) source.getSource());
  142 | 							propertySources.add(0,
  143 | 									new OriginTrackedMapPropertySource("configserver:" + source.getName(), map, true));
  144 | 						}
  145 | 					}
  146 | 
  147 | 					HashMap<String, Object> map = new HashMap<>();
  148 | 					if (StringUtils.hasText(result.getState())) {
  149 | 						putValue(map, "config.client.state", result.getState());
  150 | 					}
  151 | 					if (StringUtils.hasText(result.getVersion())) {
  152 | 						putValue(map, "config.client.version", result.getVersion());
  153 | 					}
  154 | 					// the existence of this property source confirms a successful
  155 | 					// response from config server
  156 | 					propertySources.add(0, new MapPropertySource(CONFIG_CLIENT_PROPERTYSOURCE_NAME, map));
  157 | 					if (ALL_OPTIONS.size() == 1) {
  158 | 						// boot 2.4.2 and prior
  159 | 						return new ConfigData(propertySources);
  160 | 					}
  161 | 					else if (ALL_OPTIONS.size() == 2) {
  162 | 						// boot 2.4.3 and 2.4.4
  163 | 						return new ConfigData(propertySources, Option.IGNORE_IMPORTS, Option.IGNORE_PROFILES);
  164 | 					}
  165 | 					else if (ALL_OPTIONS.size() > 2) {
  166 | 						// boot 2.4.5+
  167 | 						return new ConfigData(propertySources, propertySource -> {
  168 | 							String propertySourceName = propertySource.getName();
  169 | 							List<Option> options = new ArrayList<>();
  170 | 							options.add(Option.IGNORE_IMPORTS);
  171 | 							// TODO: the profile is now available on the backend
  172 | 							// in a future minor, add the profile associated with a
  173 | 							// PropertySource see
  174 | 							// https://github.com/spring-cloud/spring-cloud-config/issues/1874
  175 | 							for (String profile : resource.getAcceptedProfiles()) {
  176 | 								// TODO: switch to match
  177 | 								// , is used as a profile-separator for property sources
  178 | 								// from vault
  179 | 								// - is the default profile-separator for property sources
  180 | 								// TODO This is error prone logic see
  181 | 								// https://github.com/spring-cloud/spring-cloud-config/issues/2291
  182 | 								// When we see the overrides property source name we
  183 | 								// should always prioritize those
  184 | 								// properties over everything else, even profile specific
  185 | 								// property sources so also
  186 | 								// label this property source profile specific.
  187 | 								if (OVERRIDES_NAME.equals(propertySourceName) || (!DEFAULT_PROFILE.equals(profile)
  188 | 										&& propertySourceName.matches(".*[-,]" + profile + "\\b.*"))) {
  189 | 									// // TODO: switch to Options.with() when implemented
  190 | 									options.add(Option.PROFILE_SPECIFIC);
  191 | 									options.add(Option.IGNORE_PROFILES);
  192 | 								}
  193 | 							}
  194 | 							return ConfigData.Options.of(options.toArray(new Option[0]));
  195 | 						});
  196 | 					}
  197 | 				}
  198 | 			}
  199 | 			errorBody = String.format("None of labels %s found", Arrays.toString(labels));
  200 | 		}
  201 | 		catch (HttpServerErrorException e) {
  202 | 			error = e;
  203 | 			if (MediaType.APPLICATION_JSON.includes(e.getResponseHeaders().getContentType())) {
  204 | 				errorBody = e.getResponseBodyAsString();
  205 | 			}
  206 | 		}
  207 | 		catch (Exception e) {
  208 | 			error = e;
  209 | 		}
  210 | 		if (properties.isFailFast() || !resource.isOptional()) {
  211 | 			String reason;
  212 | 			if (properties.isFailFast()) {
  213 | 				reason = "the fail fast property is set";
  214 | 			}
  215 | 			else {
  216 | 				reason = "the resource is not optional";
  217 | 			}
  218 | 			throw new ConfigClientFailFastException("Could not locate PropertySource and " + reason + ", failing"
  219 | 					+ (errorBody == null ? "" : ": " + errorBody), error);
  220 | 		}
  221 | 		logger.warn("Could not locate PropertySource (" + resource + "): "
  222 | 				+ (error != null ? error.getMessage() : errorBody));
  223 | 		return null;
  224 | 	}
  225 | 
  226 | 	protected void log(Environment result) {
  227 | 		if (logger.isInfoEnabled()) {
  228 | 			logger.info(String.format("Located environment: name=%s, profiles=%s, label=%s, version=%s, state=%s",
  229 | 					result.getName(), result.getProfiles() == null ? "" : Arrays.asList(result.getProfiles()),
  230 | 					result.getLabel(), result.getVersion(), result.getState()));
  231 | 		}
  232 | 		if (logger.isDebugEnabled()) {
  233 | 			List<org.springframework.cloud.config.environment.PropertySource> propertySourceList = result
  234 | 				.getPropertySources();
  235 | 			if (propertySourceList != null) {
  236 | 				int propertyCount = 0;
  237 | 				for (org.springframework.cloud.config.environment.PropertySource propertySource : propertySourceList) {
  238 | 					propertyCount += propertySource.getSource().size();
  239 | 				}
  240 | 				logger.debug(String.format("Environment %s has %d property sources with %d properties.",
  241 | 						result.getName(), result.getPropertySources().size(), propertyCount));
  242 | 			}
  243 | 
  244 | 		}
  245 | 	}
  246 | 
  247 | 	protected Map<String, Object> translateOrigins(String name, Map<String, Object> source) {
  248 | 		Map<String, Object> withOrigins = new LinkedHashMap<>();
  249 | 		for (Map.Entry<String, Object> entry : source.entrySet()) {
  250 | 			boolean hasOrigin = false;
  251 | 
  252 | 			if (entry.getValue() instanceof Map) {
  253 | 				@SuppressWarnings("unchecked")
  254 | 				Map<String, Object> value = (Map<String, Object>) entry.getValue();
  255 | 				if (value.size() == 2 && value.containsKey("origin") && value.containsKey("value")) {
  256 | 					Origin origin = new ConfigServicePropertySourceLocator.ConfigServiceOrigin(name,
  257 | 							value.get("origin"));
  258 | 					OriginTrackedValue trackedValue = OriginTrackedValue.of(value.get("value"), origin);
  259 | 					withOrigins.put(entry.getKey(), trackedValue);
  260 | 					hasOrigin = true;
  261 | 				}
  262 | 			}
  263 | 
  264 | 			if (!hasOrigin) {
  265 | 				withOrigins.put(entry.getKey(), entry.getValue());
  266 | 			}
  267 | 		}
  268 | 		return withOrigins;
  269 | 	}
  270 | 
  271 | 	protected void putValue(HashMap<String, Object> map, String key, String value) {
  272 | 		if (StringUtils.hasText(value)) {
  273 | 			map.put(key, value);
  274 | 		}
  275 | 	}
  276 | 
  277 | 	protected Environment getRemoteEnvironment(ConfigDataLoaderContext context, ConfigServerConfigDataResource resource,
  278 | 			String label, String state) {
  279 | 		ConfigClientProperties properties = resource.getProperties();
  280 | 		RestTemplate restTemplate = context.getBootstrapContext().get(RestTemplate.class);
  281 | 
  282 | 		String path = "/{name}/{profile}";
  283 | 		String name = properties.getName();
  284 | 		String profile = resource.getProfiles();
  285 | 		String token = properties.getToken();
  286 | 		String[] uris;
  287 | 		boolean discoveryEnabled = properties.getDiscovery().isEnabled();
  288 | 		ConfigClientProperties bootstrapConfigClientProperties = context.getBootstrapContext()
  289 | 			.get(ConfigClientProperties.class);
  290 | 		// In the case where discovery is enabled we need to extract the config server
  291 | 		// uris, username, and password
  292 | 		// from the properties from the context. These are set in
  293 | 		// ConfigServerInstanceMonitor.refresh which will only
  294 | 		// be called the first time we fetch configuration.
  295 | 		if (discoveryEnabled) {
  296 | 			uris = bootstrapConfigClientProperties.getUri();
  297 | 		}
  298 | 		else {
  299 | 			uris = properties.getUri();
  300 | 		}
  301 | 		int noOfUrls = uris.length;
  302 | 		if (uris.length > 1) {
  303 | 			logger.info("Multiple Config Server Urls found listed.");
  304 | 		}
  305 | 
  306 | 		Object[] args = new String[] { name, profile };
  307 | 		if (StringUtils.hasText(label)) {
  308 | 			// workaround for Spring MVC matching / in paths
  309 | 			label = Environment.denormalize(label);
  310 | 			args = new String[] { name, profile, label };
  311 | 			path = path + "/{label}";
  312 | 		}
  313 | 		ResponseEntity<Environment> response = null;
  314 | 		List<MediaType> acceptHeader = Collections.singletonList(MediaType.parseMediaType(properties.getMediaType()));
  315 | 		List<Charset> acceptCharsetHeader = Collections.singletonList(properties.getCharset());
  316 | 
  317 | 		ConfigClientRequestTemplateFactory requestTemplateFactory = context.getBootstrapContext()
  318 | 			.get(ConfigClientRequestTemplateFactory.class);
  319 | 
  320 | 		for (int i = 0; i < noOfUrls; i++) {
  321 | 			String username;
  322 | 			String password;
  323 | 			String uri = uris[i];
  324 | 			if (discoveryEnabled) {
  325 | 				password = bootstrapConfigClientProperties.getPassword();
  326 | 				username = bootstrapConfigClientProperties.getUsername();
  327 | 			}
  328 | 			else {
  329 | 				ConfigClientProperties.Credentials credentials = properties.getCredentials(i);
  330 | 				uri = credentials.getUri();
  331 | 				username = credentials.getUsername();
  332 | 				password = credentials.getPassword();
  333 | 			}
  334 | 
  335 | 			logger.info("Fetching config from server at : " + uri);
  336 | 
  337 | 			try {
  338 | 				HttpHeaders headers = new HttpHeaders();
  339 | 				headers.setAccept(acceptHeader);
  340 | 				headers.setAcceptCharset(acceptCharsetHeader);
  341 | 				requestTemplateFactory.addAuthorizationToken(headers, username, password);
  342 | 				if (StringUtils.hasText(token)) {
  343 | 					headers.add(TOKEN_HEADER, token);
  344 | 				}
  345 | 				if (StringUtils.hasText(state) && properties.isSendState()) {
  346 | 					headers.add(STATE_HEADER, state);
  347 | 				}
  348 | 
  349 | 				final HttpEntity<Void> entity = new HttpEntity<>(null, headers);
  350 | 				response = restTemplate.exchange(uri + path, HttpMethod.GET, entity, Environment.class, args);
  351 | 			}
  352 | 			catch (HttpClientErrorException | HttpServerErrorException e) {
  353 | 				if (i < noOfUrls - 1 && properties.getMultipleUriStrategy() == MultipleUriStrategy.ALWAYS) {
  354 | 					logger.info("Failed to fetch configs from server at  : " + uri
  355 | 							+ ". Will try the next url if available. Error : " + e.getMessage());
  356 | 					continue;
  357 | 				}
  358 | 
  359 | 				if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
  360 | 					throw e;
  361 | 				}
  362 | 			}
  363 | 			catch (ResourceAccessException e) {
  364 | 				logger.info("Exception on Url - " + uri + ":" + e + ". Will be trying the next url if available");
  365 | 				if (i == noOfUrls - 1) {
  366 | 					throw e;
  367 | 				}
  368 | 				else {
  369 | 					continue;
  370 | 				}
  371 | 			}
  372 | 			catch (UnknownContentTypeException e) {
  373 | 				if (i < noOfUrls - 1 && properties.getMultipleUriStrategy() == MultipleUriStrategy.ALWAYS) {
  374 | 					logger.info(
  375 | 							"Failed to read the response of a config server. Will try the next url if available. Error : "
  376 | 									+ e.getMessage());
  377 | 					continue;
  378 | 				}
  379 | 				throw e;
  380 | 			}
  381 | 
  382 | 			if (response == null) {
  383 | 				if (i < noOfUrls - 1 && properties.getMultipleUriStrategy() == MultipleUriStrategy.ALWAYS) {
  384 | 					logger.info("Failed to fetch configs from server at  : " + uri
  385 | 							+ ". The response was null. Will try the next url if available.");
  386 | 					continue;
  387 | 				}
  388 | 
  389 | 				return null;
  390 | 			}
  391 | 			else if (response.getStatusCode() != HttpStatus.OK) {
  392 | 				if (i < noOfUrls - 1 && properties.getMultipleUriStrategy() == MultipleUriStrategy.ALWAYS) {
  393 | 					logger.info("Failed to fetch configs from server at  : " + uri
  394 | 							+ ". Will try the next url if available. StatusCode : " + response.getStatusCode());
  395 | 					continue;
  396 | 				}
  397 | 
  398 | 				return null;
  399 | 			}
  400 | 
  401 | 			Environment result = response.getBody();
  402 | 			return result;
  403 | 		}
  404 | 
  405 | 		return null;
  406 | 	}
  407 | 
  408 | }
```
