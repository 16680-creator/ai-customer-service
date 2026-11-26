<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/config/annotation/DelegatingWebMvcConfiguration.java -->
<!-- 正文引用：[[源码:DelegatingWebMvcConfiguration.java#L起-L止]] ；代码块头部：(src: DelegatingWebMvcConfiguration.java#L起-L止) -->

# DelegatingWebMvcConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/config/annotation/DelegatingWebMvcConfiguration.java` |
| 行数 | 173 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:14.598Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.web.servlet.config.annotation;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.beans.factory.annotation.Autowired;
   24 | import org.springframework.context.annotation.Configuration;
   25 | import org.springframework.format.FormatterRegistry;
   26 | import org.springframework.http.converter.HttpMessageConverter;
   27 | import org.springframework.http.converter.HttpMessageConverters;
   28 | import org.springframework.util.CollectionUtils;
   29 | import org.springframework.validation.MessageCodesResolver;
   30 | import org.springframework.validation.Validator;
   31 | import org.springframework.web.ErrorResponse;
   32 | import org.springframework.web.method.support.HandlerMethodArgumentResolver;
   33 | import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
   34 | import org.springframework.web.servlet.HandlerExceptionResolver;
   35 | 
   36 | /**
   37 |  * A subclass of {@code WebMvcConfigurationSupport} that detects and delegates
   38 |  * to all beans of type {@link WebMvcConfigurer} allowing them to customize the
   39 |  * configuration provided by {@code WebMvcConfigurationSupport}. This is the
   40 |  * class actually imported by {@link EnableWebMvc @EnableWebMvc}.
   41 |  *
   42 |  * @author Rossen Stoyanchev
   43 |  * @since 3.1
   44 |  */
   45 | @Configuration(proxyBeanMethods = false)
   46 | public class DelegatingWebMvcConfiguration extends WebMvcConfigurationSupport {
   47 | 
   48 | 	private final WebMvcConfigurerComposite configurers = new WebMvcConfigurerComposite();
   49 | 
   50 | 
   51 | 	@Autowired(required = false)
   52 | 	public void setConfigurers(List<WebMvcConfigurer> configurers) {
   53 | 		if (!CollectionUtils.isEmpty(configurers)) {
   54 | 			this.configurers.addWebMvcConfigurers(configurers);
   55 | 		}
   56 | 	}
   57 | 
   58 | 
   59 | 	@Override
   60 | 	protected void configurePathMatch(PathMatchConfigurer configurer) {
   61 | 		this.configurers.configurePathMatch(configurer);
   62 | 	}
   63 | 
   64 | 	@Override
   65 | 	protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
   66 | 		this.configurers.configureContentNegotiation(configurer);
   67 | 	}
   68 | 
   69 | 	@Override
   70 | 	protected void configureApiVersioning(ApiVersionConfigurer configurer) {
   71 | 		this.configurers.configureApiVersioning(configurer);
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
   76 | 		this.configurers.configureAsyncSupport(configurer);
   77 | 	}
   78 | 
   79 | 	@Override
   80 | 	protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
   81 | 		this.configurers.configureDefaultServletHandling(configurer);
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	protected void addFormatters(FormatterRegistry registry) {
   86 | 		this.configurers.addFormatters(registry);
   87 | 	}
   88 | 
   89 | 	@Override
   90 | 	protected void addInterceptors(InterceptorRegistry registry) {
   91 | 		this.configurers.addInterceptors(registry);
   92 | 	}
   93 | 
   94 | 	@Override
   95 | 	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
   96 | 		this.configurers.addResourceHandlers(registry);
   97 | 	}
   98 | 
   99 | 	@Override
  100 | 	protected void addCorsMappings(CorsRegistry registry) {
  101 | 		this.configurers.addCorsMappings(registry);
  102 | 	}
  103 | 
  104 | 	@Override
  105 | 	protected void addViewControllers(ViewControllerRegistry registry) {
  106 | 		this.configurers.addViewControllers(registry);
  107 | 	}
  108 | 
  109 | 	@Override
  110 | 	protected void configureViewResolvers(ViewResolverRegistry registry) {
  111 | 		this.configurers.configureViewResolvers(registry);
  112 | 	}
  113 | 
  114 | 	@Override
  115 | 	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
  116 | 		this.configurers.addArgumentResolvers(argumentResolvers);
  117 | 	}
  118 | 
  119 | 	@Override
  120 | 	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
  121 | 		this.configurers.addReturnValueHandlers(returnValueHandlers);
  122 | 	}
  123 | 
  124 | 	@Override
  125 | 	protected void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
  126 | 		this.configurers.configureMessageConverters(builder);
  127 | 	}
  128 | 
  129 | 	@Override
  130 | 	@Deprecated(since = "7.0", forRemoval = true)
  131 | 	@SuppressWarnings("removal")
  132 | 	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
  133 | 		this.configurers.configureMessageConverters(converters);
  134 | 	}
  135 | 
  136 | 	@Override
  137 | 	@Deprecated(since = "7.0", forRemoval = true)
  138 | 	@SuppressWarnings("removal")
  139 | 	protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
  140 | 		this.configurers.extendMessageConverters(converters);
  141 | 	}
  142 | 
  143 | 	@Override
  144 | 	protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
  145 | 		this.configurers.configureHandlerExceptionResolvers(exceptionResolvers);
  146 | 	}
  147 | 
  148 | 	@Override
  149 | 	protected void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
  150 | 		this.configurers.extendHandlerExceptionResolvers(exceptionResolvers);
  151 | 	}
  152 | 
  153 | 	@Override
  154 | 	protected void customizeProblemDetails(ProblemDetailsConfigurer configurer) {
  155 | 		this.configurers.configureProblemDetails(configurer);
  156 | 	}
  157 | 
  158 | 	@Override
  159 | 	protected void configureErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
  160 | 		this.configurers.addErrorResponseInterceptors(interceptors);
  161 | 	}
  162 | 
  163 | 	@Override
  164 | 	protected @Nullable Validator getValidator() {
  165 | 		return this.configurers.getValidator();
  166 | 	}
  167 | 
  168 | 	@Override
  169 | 	protected @Nullable MessageCodesResolver getMessageCodesResolver() {
  170 | 		return this.configurers.getMessageCodesResolver();
  171 | 	}
  172 | 
  173 | }
```
