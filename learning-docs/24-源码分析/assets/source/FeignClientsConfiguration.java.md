<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/FeignClientsConfiguration.java -->
<!-- 正文引用：[[源码:FeignClientsConfiguration.java#L起-L止]] ；代码块头部：(src: FeignClientsConfiguration.java#L起-L止) -->

# FeignClientsConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/FeignClientsConfiguration.java` |
| 行数 | 312 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:25.310Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | 
   22 | import feign.Contract;
   23 | import feign.Feign;
   24 | import feign.Logger;
   25 | import feign.QueryMapEncoder;
   26 | import feign.Retryer;
   27 | import feign.codec.Decoder;
   28 | import feign.codec.Encoder;
   29 | import feign.form.MultipartFormContentProcessor;
   30 | import feign.form.spring.SpringFormEncoder;
   31 | import feign.micrometer.MicrometerCapability;
   32 | import feign.micrometer.MicrometerObservationCapability;
   33 | import feign.optionals.OptionalDecoder;
   34 | import io.micrometer.core.instrument.MeterRegistry;
   35 | import io.micrometer.observation.ObservationRegistry;
   36 | 
   37 | import org.springframework.beans.factory.ObjectProvider;
   38 | import org.springframework.beans.factory.annotation.Autowired;
   39 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   40 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   41 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   42 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
   43 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   44 | import org.springframework.boot.data.autoconfigure.web.DataWebProperties;
   45 | import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
   46 | import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
   47 | import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
   48 | import org.springframework.cloud.openfeign.FeignAutoConfiguration.CircuitBreakerPresentFeignTargeterConfiguration.AlphanumericCircuitBreakerNameResolver;
   49 | import org.springframework.cloud.openfeign.FeignAutoConfiguration.CircuitBreakerPresentFeignTargeterConfiguration.DefaultCircuitBreakerNameResolver;
   50 | import org.springframework.cloud.openfeign.clientconfig.FeignClientConfigurer;
   51 | import org.springframework.cloud.openfeign.support.AbstractFormWriter;
   52 | import org.springframework.cloud.openfeign.support.FeignEncoderProperties;
   53 | import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
   54 | import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
   55 | import org.springframework.cloud.openfeign.support.PageableSpringEncoder;
   56 | import org.springframework.cloud.openfeign.support.PageableSpringQueryMapEncoder;
   57 | import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
   58 | import org.springframework.cloud.openfeign.support.SpringDecoder;
   59 | import org.springframework.cloud.openfeign.support.SpringEncoder;
   60 | import org.springframework.cloud.openfeign.support.SpringFieldQueryMapEncoder;
   61 | import org.springframework.cloud.openfeign.support.SpringMvcContract;
   62 | import org.springframework.context.annotation.Bean;
   63 | import org.springframework.context.annotation.Conditional;
   64 | import org.springframework.context.annotation.Configuration;
   65 | import org.springframework.context.annotation.Scope;
   66 | import org.springframework.core.convert.ConversionService;
   67 | import org.springframework.data.domain.Pageable;
   68 | import org.springframework.format.support.DefaultFormattingConversionService;
   69 | import org.springframework.format.support.FormattingConversionService;
   70 | import org.springframework.http.converter.HttpMessageConverter;
   71 | 
   72 | import static feign.form.ContentType.MULTIPART;
   73 | 
   74 | /**
   75 |  * @author Dave Syer
   76 |  * @author Venil Noronha
   77 |  * @author Darren Foong
   78 |  * @author Jonatan Ivanov
   79 |  * @author Olga Maciaszek-Sharma
   80 |  * @author Hyeonmin Park
   81 |  * @author Yanming Zhou
   82 |  */
   83 | @Configuration(proxyBeanMethods = false)
   84 | public class FeignClientsConfiguration {
   85 | 
   86 | 	@Autowired
   87 | 	private ObjectProvider<HttpMessageConverter<?>> messageConverters;
   88 | 
   89 | 	@Autowired(required = false)
   90 | 	private List<AnnotatedParameterProcessor> parameterProcessors = new ArrayList<>();
   91 | 
   92 | 	@Autowired(required = false)
   93 | 	private List<FeignFormatterRegistrar> feignFormatterRegistrars = new ArrayList<>();
   94 | 
   95 | 	@Autowired(required = false)
   96 | 	private Logger logger;
   97 | 
   98 | 	@Autowired(required = false)
   99 | 	private FeignClientProperties feignClientProperties;
  100 | 
  101 | 	@Autowired(required = false)
  102 | 	private FeignEncoderProperties encoderProperties;
  103 | 
  104 | 	@Bean
  105 | 	@ConditionalOnMissingBean
  106 | 	public FeignHttpMessageConverters feignHttpMessageConverters(
  107 | 			ObjectProvider<ClientHttpMessageConvertersCustomizer> customizers,
  108 | 			ObjectProvider<HttpMessageConverterCustomizer> cloudCustomizers) {
  109 | 		return new FeignHttpMessageConverters(customizers, cloudCustomizers);
  110 | 	}
  111 | 
  112 | 	@Bean
  113 | 	@ConditionalOnMissingBean
  114 | 	public Decoder feignDecoder(ObjectProvider<FeignHttpMessageConverters> messageConverters) {
  115 | 		return new OptionalDecoder(new ResponseEntityDecoder(new SpringDecoder(messageConverters)));
  116 | 	}
  117 | 
  118 | 	@Bean
  119 | 	@ConditionalOnMissingBean
  120 | 	@ConditionalOnMissingClass("org.springframework.data.domain.Pageable")
  121 | 	public Encoder feignEncoder(ObjectProvider<AbstractFormWriter> formWriterProvider,
  122 | 			ObjectProvider<FeignHttpMessageConverters> feignHttpMessageConverters) {
  123 | 		return springEncoder(formWriterProvider, encoderProperties, feignHttpMessageConverters);
  124 | 	}
  125 | 
  126 | 	@Bean
  127 | 	@ConditionalOnMissingBean
  128 | 	@ConditionalOnMissingClass("org.springframework.data.domain.Pageable")
  129 | 	public QueryMapEncoder feignQueryMapEncoder() {
  130 | 		return new SpringFieldQueryMapEncoder();
  131 | 	}
  132 | 
  133 | 	@Bean
  134 | 	@ConditionalOnMissingBean
  135 | 	public Contract feignContract(ConversionService feignConversionService) {
  136 | 		return new SpringMvcContract(parameterProcessors, feignConversionService, feignClientProperties);
  137 | 	}
  138 | 
  139 | 	@Bean
  140 | 	public FormattingConversionService feignConversionService() {
  141 | 		FormattingConversionService conversionService = new DefaultFormattingConversionService();
  142 | 		for (FeignFormatterRegistrar feignFormatterRegistrar : feignFormatterRegistrars) {
  143 | 			feignFormatterRegistrar.registerFormatters(conversionService);
  144 | 		}
  145 | 		return conversionService;
  146 | 	}
  147 | 
  148 | 	@Bean
  149 | 	@ConditionalOnMissingBean
  150 | 	public Retryer feignRetryer() {
  151 | 		return Retryer.NEVER_RETRY;
  152 | 	}
  153 | 
  154 | 	@Bean
  155 | 	@ConditionalOnMissingBean(FeignLoggerFactory.class)
  156 | 	public FeignLoggerFactory feignLoggerFactory() {
  157 | 		return new DefaultFeignLoggerFactory(logger);
  158 | 	}
  159 | 
  160 | 	@Bean
  161 | 	@ConditionalOnMissingBean(FeignClientConfigurer.class)
  162 | 	public FeignClientConfigurer feignClientConfigurer() {
  163 | 		return new FeignClientConfigurer() {
  164 | 		};
  165 | 	}
  166 | 
  167 | 	private static Encoder springEncoder(ObjectProvider<AbstractFormWriter> formWriterProvider,
  168 | 			FeignEncoderProperties encoderProperties,
  169 | 			ObjectProvider<FeignHttpMessageConverters> feignHttpMessageConverters) {
  170 | 		AbstractFormWriter formWriter = formWriterProvider.getIfAvailable();
  171 | 
  172 | 		if (formWriter != null) {
  173 | 			return new SpringEncoder(new SpringPojoFormEncoder(formWriter), encoderProperties,
  174 | 					feignHttpMessageConverters);
  175 | 		}
  176 | 		else {
  177 | 			return new SpringEncoder(new SpringFormEncoder(), encoderProperties, feignHttpMessageConverters);
  178 | 		}
  179 | 	}
  180 | 
  181 | 	private static class SpringPojoFormEncoder extends SpringFormEncoder {
  182 | 
  183 | 		SpringPojoFormEncoder(AbstractFormWriter formWriter) {
  184 | 			super();
  185 | 
  186 | 			MultipartFormContentProcessor processor = (MultipartFormContentProcessor) getContentProcessor(MULTIPART);
  187 | 			processor.addFirstWriter(formWriter);
  188 | 		}
  189 | 
  190 | 	}
  191 | 
  192 | 	@Configuration(proxyBeanMethods = false)
  193 | 	@ConditionalOnClass({ Pageable.class, DataWebProperties.class })
  194 | 	static class SpringDataConfiguration {
  195 | 
  196 | 		private final DataWebProperties dataWebProperties;
  197 | 
  198 | 		SpringDataConfiguration(ObjectProvider<DataWebProperties> dataWebProperties) {
  199 | 			this.dataWebProperties = dataWebProperties.getIfAvailable();
  200 | 		}
  201 | 
  202 | 		@Bean
  203 | 		@ConditionalOnMissingBean
  204 | 		public Encoder feignEncoderPageable(ObjectProvider<AbstractFormWriter> formWriterProvider,
  205 | 				ObjectProvider<FeignEncoderProperties> encoderProperties,
  206 | 				ObjectProvider<FeignHttpMessageConverters> feignHttpMessageConverters) {
  207 | 			PageableSpringEncoder encoder = new PageableSpringEncoder(
  208 | 					springEncoder(formWriterProvider, encoderProperties.getIfAvailable(), feignHttpMessageConverters));
  209 | 
  210 | 			if (dataWebProperties != null) {
  211 | 				encoder.setPageParameter(dataWebProperties.getPageable().getPageParameter());
  212 | 				encoder.setSizeParameter(dataWebProperties.getPageable().getSizeParameter());
  213 | 				encoder.setSortParameter(dataWebProperties.getSort().getSortParameter());
  214 | 			}
  215 | 			return encoder;
  216 | 		}
  217 | 
  218 | 		@Bean
  219 | 		@ConditionalOnMissingBean
  220 | 		public QueryMapEncoder feignQueryMapEncoderPageable() {
  221 | 			PageableSpringQueryMapEncoder queryMapEncoder = new PageableSpringQueryMapEncoder();
  222 | 			if (dataWebProperties != null) {
  223 | 				queryMapEncoder.setPageParameter(dataWebProperties.getPageable().getPageParameter());
  224 | 				queryMapEncoder.setSizeParameter(dataWebProperties.getPageable().getSizeParameter());
  225 | 				queryMapEncoder.setSortParameter(dataWebProperties.getSort().getSortParameter());
  226 | 			}
  227 | 			return queryMapEncoder;
  228 | 		}
  229 | 
  230 | 	}
  231 | 
  232 | 	@Configuration(proxyBeanMethods = false)
  233 | 	@Conditional(FeignCircuitBreakerDisabledConditions.class)
  234 | 	protected static class DefaultFeignBuilderConfiguration {
  235 | 
  236 | 		@Bean
  237 | 		@Scope("prototype")
  238 | 		@ConditionalOnMissingBean
  239 | 		public Feign.Builder feignBuilder(Retryer retryer) {
  240 | 			return Feign.builder().retryer(retryer);
  241 | 		}
  242 | 
  243 | 	}
  244 | 
  245 | 	@Configuration(proxyBeanMethods = false)
  246 | 	@ConditionalOnClass(CircuitBreaker.class)
  247 | 	@ConditionalOnProperty("spring.cloud.openfeign.circuitbreaker.enabled")
  248 | 	protected static class CircuitBreakerPresentFeignBuilderConfiguration {
  249 | 
  250 | 		@Bean
  251 | 		@Scope("prototype")
  252 | 		@ConditionalOnMissingBean({ Feign.Builder.class, CircuitBreakerFactory.class })
  253 | 		public Feign.Builder defaultFeignBuilder(Retryer retryer) {
  254 | 			return Feign.builder().retryer(retryer);
  255 | 		}
  256 | 
  257 | 		/**
  258 | 		 * @deprecated in favor of
  259 | 		 * {@link #circuitBreakerFeignBuilder(CircuitBreakerFactory, FeignCircuitBreakerProperties, ObjectProvider)}.
  260 | 		 */
  261 | 		@Deprecated
  262 | 		public Feign.Builder circuitBreakerFeignBuilder() {
  263 | 			return FeignCircuitBreaker.builder();
  264 | 		}
  265 | 
  266 | 		@Bean
  267 | 		@Scope("prototype")
  268 | 		@ConditionalOnMissingBean
  269 | 		@ConditionalOnBean(CircuitBreakerFactory.class)
  270 | 		public Feign.Builder circuitBreakerFeignBuilder(CircuitBreakerFactory circuitBreakerFactory,
  271 | 				FeignCircuitBreakerProperties circuitBreakerProperties,
  272 | 				ObjectProvider<CircuitBreakerNameResolver> circuitBreakerNameResolver) {
  273 | 			return FeignCircuitBreaker.builder()
  274 | 				.circuitBreakerFactory(circuitBreakerFactory)
  275 | 				.circuitBreakerGroupEnabled(circuitBreakerProperties.getGroup().isEnabled())
  276 | 				.circuitBreakerNameResolver(circuitBreakerNameResolver
  277 | 					.getIfAvailable(() -> defaultCircuitBreakerNameResolver(circuitBreakerProperties)));
  278 | 		}
  279 | 
  280 | 		private CircuitBreakerNameResolver defaultCircuitBreakerNameResolver(
  281 | 				FeignCircuitBreakerProperties circuitBreakerProperties) {
  282 | 			if (circuitBreakerProperties.getAlphanumericIds().isEnabled()) {
  283 | 				return new AlphanumericCircuitBreakerNameResolver();
  284 | 			}
  285 | 			return new DefaultCircuitBreakerNameResolver();
  286 | 		}
  287 | 
  288 | 	}
  289 | 
  290 | 	@Configuration(proxyBeanMethods = false)
  291 | 	@ConditionalOnProperty(name = "spring.cloud.openfeign.micrometer.enabled", matchIfMissing = true)
  292 | 	@ConditionalOnClass({ MicrometerObservationCapability.class, MicrometerCapability.class, MeterRegistry.class })
  293 | 	@Conditional(FeignClientMicrometerEnabledCondition.class)
  294 | 	protected static class MicrometerConfiguration {
  295 | 
  296 | 		@Bean
  297 | 		@ConditionalOnMissingBean
  298 | 		@ConditionalOnBean(type = "io.micrometer.observation.ObservationRegistry")
  299 | 		public MicrometerObservationCapability micrometerObservationCapability(ObservationRegistry registry) {
  300 | 			return new MicrometerObservationCapability(registry);
  301 | 		}
  302 | 
  303 | 		@Bean
  304 | 		@ConditionalOnBean(type = "io.micrometer.core.instrument.MeterRegistry")
  305 | 		@ConditionalOnMissingBean({ MicrometerCapability.class, MicrometerObservationCapability.class })
  306 | 		public MicrometerCapability micrometerCapability(MeterRegistry registry) {
  307 | 			return new MicrometerCapability(registry);
  308 | 		}
  309 | 
  310 | 	}
  311 | 
  312 | }
```
