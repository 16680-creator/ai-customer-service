<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/FeignClientProperties.java -->
<!-- 正文引用：[[源码:FeignClientProperties.java#L起-L止]] ；代码块头部：(src: FeignClientProperties.java#L起-L止) -->

# FeignClientProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/FeignClientProperties.java` |
| 行数 | 416 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:26.044Z |

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
   17 | package org.springframework.cloud.openfeign;
   18 | 
   19 | import java.util.Collection;
   20 | import java.util.HashMap;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.Objects;
   24 | 
   25 | import feign.Capability;
   26 | import feign.Contract;
   27 | import feign.ExceptionPropagationPolicy;
   28 | import feign.Logger;
   29 | import feign.QueryMapEncoder;
   30 | import feign.RequestInterceptor;
   31 | import feign.ResponseInterceptor;
   32 | import feign.Retryer;
   33 | import feign.codec.Decoder;
   34 | import feign.codec.Encoder;
   35 | import feign.codec.ErrorDecoder;
   36 | 
   37 | import org.springframework.boot.context.properties.ConfigurationProperties;
   38 | 
   39 | /**
   40 |  * @author Eko Kurniawan Khannedy
   41 |  * @author Ilia Ilinykh
   42 |  * @author Ram Anaswara
   43 |  * @author Jonatan Ivanov
   44 |  * @author Olga Maciaszek-Sharma
   45 |  * @author Hyeonmin Park
   46 |  * @author Jasbir Singh
   47 |  * @author Dominique Villard
   48 |  * @author Sharang Gupta
   49 |  */
   50 | @ConfigurationProperties("spring.cloud.openfeign.client")
   51 | public class FeignClientProperties {
   52 | 
   53 | 	private boolean defaultToProperties = true;
   54 | 
   55 | 	private String defaultConfig = "default";
   56 | 
   57 | 	private Map<String, FeignClientConfiguration> config = new HashMap<>();
   58 | 
   59 | 	/**
   60 | 	 * Feign clients do not encode slash `/` characters by default. To change this
   61 | 	 * behavior, set the `decodeSlash` to `false`.
   62 | 	 */
   63 | 	private boolean decodeSlash = true;
   64 | 
   65 | 	/**
   66 | 	 * If {@code true}, trailing slashes at the end of request urls will be removed.
   67 | 	 */
   68 | 	private boolean removeTrailingSlash;
   69 | 
   70 | 	public boolean isDefaultToProperties() {
   71 | 		return defaultToProperties;
   72 | 	}
   73 | 
   74 | 	public void setDefaultToProperties(boolean defaultToProperties) {
   75 | 		this.defaultToProperties = defaultToProperties;
   76 | 	}
   77 | 
   78 | 	public String getDefaultConfig() {
   79 | 		return defaultConfig;
   80 | 	}
   81 | 
   82 | 	public void setDefaultConfig(String defaultConfig) {
   83 | 		this.defaultConfig = defaultConfig;
   84 | 	}
   85 | 
   86 | 	public Map<String, FeignClientConfiguration> getConfig() {
   87 | 		return config;
   88 | 	}
   89 | 
   90 | 	public void setConfig(Map<String, FeignClientConfiguration> config) {
   91 | 		this.config = config;
   92 | 	}
   93 | 
   94 | 	public boolean isDecodeSlash() {
   95 | 		return decodeSlash;
   96 | 	}
   97 | 
   98 | 	public void setDecodeSlash(boolean decodeSlash) {
   99 | 		this.decodeSlash = decodeSlash;
  100 | 	}
  101 | 
  102 | 	public boolean isRemoveTrailingSlash() {
  103 | 		return removeTrailingSlash;
  104 | 	}
  105 | 
  106 | 	public void setRemoveTrailingSlash(boolean removeTrailingSlash) {
  107 | 		this.removeTrailingSlash = removeTrailingSlash;
  108 | 	}
  109 | 
  110 | 	@Override
  111 | 	public boolean equals(Object o) {
  112 | 		if (this == o) {
  113 | 			return true;
  114 | 		}
  115 | 		if (o == null || getClass() != o.getClass()) {
  116 | 			return false;
  117 | 		}
  118 | 		FeignClientProperties that = (FeignClientProperties) o;
  119 | 		return defaultToProperties == that.defaultToProperties && Objects.equals(defaultConfig, that.defaultConfig)
  120 | 				&& Objects.equals(config, that.config) && Objects.equals(decodeSlash, that.decodeSlash)
  121 | 				&& Objects.equals(removeTrailingSlash, that.removeTrailingSlash);
  122 | 	}
  123 | 
  124 | 	@Override
  125 | 	public int hashCode() {
  126 | 		return Objects.hash(defaultToProperties, defaultConfig, config, decodeSlash, removeTrailingSlash);
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * Feign client configuration.
  131 | 	 */
  132 | 	public static class FeignClientConfiguration {
  133 | 
  134 | 		private Logger.Level loggerLevel;
  135 | 
  136 | 		private Integer connectTimeout;
  137 | 
  138 | 		private Integer readTimeout;
  139 | 
  140 | 		private Class<Retryer> retryer;
  141 | 
  142 | 		private Class<ErrorDecoder> errorDecoder;
  143 | 
  144 | 		private List<Class<RequestInterceptor>> requestInterceptors;
  145 | 
  146 | 		private Class<ResponseInterceptor> responseInterceptor;
  147 | 
  148 | 		private Map<String, Collection<String>> defaultRequestHeaders = new HashMap<>();
  149 | 
  150 | 		private Map<String, Collection<String>> defaultQueryParameters = new HashMap<>();
  151 | 
  152 | 		private Boolean dismiss404;
  153 | 
  154 | 		private Class<Decoder> decoder;
  155 | 
  156 | 		private Class<Encoder> encoder;
  157 | 
  158 | 		private Class<Contract> contract;
  159 | 
  160 | 		private ExceptionPropagationPolicy exceptionPropagationPolicy;
  161 | 
  162 | 		private List<Class<Capability>> capabilities;
  163 | 
  164 | 		private Class<QueryMapEncoder> queryMapEncoder;
  165 | 
  166 | 		/**
  167 | 		 * If {@code true}, the nested objects of a {@code @SpringQueryMap} parameter are
  168 | 		 * sent as nested query parameters (for example, {@code range.from=1&range.to=2})
  169 | 		 * instead of their {@code toString()} value, and the objects of a collection or
  170 | 		 * an array as indexed query parameters (for example,
  171 | 		 * {@code filters[0].name=status}).
  172 | 		 */
  173 | 		private Boolean nestedQueryMapEnabled;
  174 | 
  175 | 		private MicrometerProperties micrometer;
  176 | 
  177 | 		private Boolean followRedirects;
  178 | 
  179 | 		/**
  180 | 		 * Allows setting Feign client host URL. This value will only be taken into
  181 | 		 * account if the url is not set in the @FeignClient annotation.
  182 | 		 */
  183 | 		private String url;
  184 | 
  185 | 		public Logger.Level getLoggerLevel() {
  186 | 			return loggerLevel;
  187 | 		}
  188 | 
  189 | 		public void setLoggerLevel(Logger.Level loggerLevel) {
  190 | 			this.loggerLevel = loggerLevel;
  191 | 		}
  192 | 
  193 | 		public Integer getConnectTimeout() {
  194 | 			return connectTimeout;
  195 | 		}
  196 | 
  197 | 		public void setConnectTimeout(Integer connectTimeout) {
  198 | 			this.connectTimeout = connectTimeout;
  199 | 		}
  200 | 
  201 | 		public Integer getReadTimeout() {
  202 | 			return readTimeout;
  203 | 		}
  204 | 
  205 | 		public void setReadTimeout(Integer readTimeout) {
  206 | 			this.readTimeout = readTimeout;
  207 | 		}
  208 | 
  209 | 		public Class<Retryer> getRetryer() {
  210 | 			return retryer;
  211 | 		}
  212 | 
  213 | 		public void setRetryer(Class<Retryer> retryer) {
  214 | 			this.retryer = retryer;
  215 | 		}
  216 | 
  217 | 		public Class<ErrorDecoder> getErrorDecoder() {
  218 | 			return errorDecoder;
  219 | 		}
  220 | 
  221 | 		public void setErrorDecoder(Class<ErrorDecoder> errorDecoder) {
  222 | 			this.errorDecoder = errorDecoder;
  223 | 		}
  224 | 
  225 | 		public List<Class<RequestInterceptor>> getRequestInterceptors() {
  226 | 			return requestInterceptors;
  227 | 		}
  228 | 
  229 | 		public void setRequestInterceptors(List<Class<RequestInterceptor>> requestInterceptors) {
  230 | 			this.requestInterceptors = requestInterceptors;
  231 | 		}
  232 | 
  233 | 		public Class<ResponseInterceptor> getResponseInterceptor() {
  234 | 			return responseInterceptor;
  235 | 		}
  236 | 
  237 | 		public void setResponseInterceptor(Class<ResponseInterceptor> responseInterceptor) {
  238 | 			this.responseInterceptor = responseInterceptor;
  239 | 		}
  240 | 
  241 | 		public Map<String, Collection<String>> getDefaultRequestHeaders() {
  242 | 			return defaultRequestHeaders;
  243 | 		}
  244 | 
  245 | 		public void setDefaultRequestHeaders(Map<String, Collection<String>> defaultRequestHeaders) {
  246 | 			this.defaultRequestHeaders = defaultRequestHeaders;
  247 | 		}
  248 | 
  249 | 		public Map<String, Collection<String>> getDefaultQueryParameters() {
  250 | 			return defaultQueryParameters;
  251 | 		}
  252 | 
  253 | 		public void setDefaultQueryParameters(Map<String, Collection<String>> defaultQueryParameters) {
  254 | 			this.defaultQueryParameters = defaultQueryParameters;
  255 | 		}
  256 | 
  257 | 		public Boolean getDismiss404() {
  258 | 			return dismiss404;
  259 | 		}
  260 | 
  261 | 		public void setDismiss404(Boolean dismiss404) {
  262 | 			this.dismiss404 = dismiss404;
  263 | 		}
  264 | 
  265 | 		public Class<Decoder> getDecoder() {
  266 | 			return decoder;
  267 | 		}
  268 | 
  269 | 		public void setDecoder(Class<Decoder> decoder) {
  270 | 			this.decoder = decoder;
  271 | 		}
  272 | 
  273 | 		public Class<Encoder> getEncoder() {
  274 | 			return encoder;
  275 | 		}
  276 | 
  277 | 		public void setEncoder(Class<Encoder> encoder) {
  278 | 			this.encoder = encoder;
  279 | 		}
  280 | 
  281 | 		public Class<Contract> getContract() {
  282 | 			return contract;
  283 | 		}
  284 | 
  285 | 		public void setContract(Class<Contract> contract) {
  286 | 			this.contract = contract;
  287 | 		}
  288 | 
  289 | 		public ExceptionPropagationPolicy getExceptionPropagationPolicy() {
  290 | 			return exceptionPropagationPolicy;
  291 | 		}
  292 | 
  293 | 		public void setExceptionPropagationPolicy(ExceptionPropagationPolicy exceptionPropagationPolicy) {
  294 | 			this.exceptionPropagationPolicy = exceptionPropagationPolicy;
  295 | 		}
  296 | 
  297 | 		public List<Class<Capability>> getCapabilities() {
  298 | 			return capabilities;
  299 | 		}
  300 | 
  301 | 		public void setCapabilities(List<Class<Capability>> capabilities) {
  302 | 			this.capabilities = capabilities;
  303 | 		}
  304 | 
  305 | 		public Class<QueryMapEncoder> getQueryMapEncoder() {
  306 | 			return queryMapEncoder;
  307 | 		}
  308 | 
  309 | 		public void setQueryMapEncoder(Class<QueryMapEncoder> queryMapEncoder) {
  310 | 			this.queryMapEncoder = queryMapEncoder;
  311 | 		}
  312 | 
  313 | 		public Boolean getNestedQueryMapEnabled() {
  314 | 			return nestedQueryMapEnabled;
  315 | 		}
  316 | 
  317 | 		public void setNestedQueryMapEnabled(Boolean nestedQueryMapEnabled) {
  318 | 			this.nestedQueryMapEnabled = nestedQueryMapEnabled;
  319 | 		}
  320 | 
  321 | 		public MicrometerProperties getMicrometer() {
  322 | 			return micrometer;
  323 | 		}
  324 | 
  325 | 		public void setMicrometer(MicrometerProperties micrometer) {
  326 | 			this.micrometer = micrometer;
  327 | 		}
  328 | 
  329 | 		public Boolean isFollowRedirects() {
  330 | 			return followRedirects;
  331 | 		}
  332 | 
  333 | 		public void setFollowRedirects(Boolean followRedirects) {
  334 | 			this.followRedirects = followRedirects;
  335 | 		}
  336 | 
  337 | 		public String getUrl() {
  338 | 			return url;
  339 | 		}
  340 | 
  341 | 		public void setUrl(String url) {
  342 | 			this.url = url;
  343 | 		}
  344 | 
  345 | 		@Override
  346 | 		public boolean equals(Object o) {
  347 | 			if (this == o) {
  348 | 				return true;
  349 | 			}
  350 | 			if (o == null || getClass() != o.getClass()) {
  351 | 				return false;
  352 | 			}
  353 | 			FeignClientConfiguration that = (FeignClientConfiguration) o;
  354 | 			return loggerLevel == that.loggerLevel && Objects.equals(connectTimeout, that.connectTimeout)
  355 | 					&& Objects.equals(readTimeout, that.readTimeout) && Objects.equals(retryer, that.retryer)
  356 | 					&& Objects.equals(errorDecoder, that.errorDecoder)
  357 | 					&& Objects.equals(requestInterceptors, that.requestInterceptors)
  358 | 					&& Objects.equals(responseInterceptor, that.responseInterceptor)
  359 | 					&& Objects.equals(dismiss404, that.dismiss404) && Objects.equals(encoder, that.encoder)
  360 | 					&& Objects.equals(decoder, that.decoder) && Objects.equals(contract, that.contract)
  361 | 					&& Objects.equals(exceptionPropagationPolicy, that.exceptionPropagationPolicy)
  362 | 					&& Objects.equals(defaultRequestHeaders, that.defaultRequestHeaders)
  363 | 					&& Objects.equals(defaultQueryParameters, that.defaultQueryParameters)
  364 | 					&& Objects.equals(capabilities, that.capabilities)
  365 | 					&& Objects.equals(queryMapEncoder, that.queryMapEncoder)
  366 | 					&& Objects.equals(nestedQueryMapEnabled, that.nestedQueryMapEnabled)
  367 | 					&& Objects.equals(micrometer, that.micrometer)
  368 | 					&& Objects.equals(followRedirects, that.followRedirects) && Objects.equals(url, that.url);
  369 | 		}
  370 | 
  371 | 		@Override
  372 | 		public int hashCode() {
  373 | 			return Objects.hash(loggerLevel, connectTimeout, readTimeout, retryer, errorDecoder, requestInterceptors,
  374 | 					responseInterceptor, dismiss404, encoder, decoder, contract, exceptionPropagationPolicy,
  375 | 					defaultQueryParameters, defaultRequestHeaders, capabilities, queryMapEncoder, nestedQueryMapEnabled,
  376 | 					micrometer, followRedirects, url);
  377 | 		}
  378 | 
  379 | 	}
  380 | 
  381 | 	/**
  382 | 	 * Micrometer configuration for Feign Client.
  383 | 	 */
  384 | 	public static class MicrometerProperties {
  385 | 
  386 | 		private Boolean enabled = true;
  387 | 
  388 | 		public Boolean getEnabled() {
  389 | 			return enabled;
  390 | 		}
  391 | 
  392 | 		public void setEnabled(Boolean enabled) {
  393 | 			this.enabled = enabled;
  394 | 		}
  395 | 
  396 | 		@Override
  397 | 		public boolean equals(Object o) {
  398 | 			if (this == o) {
  399 | 				return true;
  400 | 			}
  401 | 			if (o == null || getClass() != o.getClass()) {
  402 | 				return false;
  403 | 			}
  404 | 
  405 | 			MicrometerProperties that = (MicrometerProperties) o;
  406 | 			return Objects.equals(enabled, that.enabled);
  407 | 		}
  408 | 
  409 | 		@Override
  410 | 		public int hashCode() {
  411 | 			return Objects.hash(enabled);
  412 | 		}
  413 | 
  414 | 	}
  415 | 
  416 | }
```
