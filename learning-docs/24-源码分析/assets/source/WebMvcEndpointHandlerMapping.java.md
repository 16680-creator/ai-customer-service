<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc:4.2.0 | path: org/springframework/boot/webmvc/actuate/endpoint/web/WebMvcEndpointHandlerMapping.java -->
<!-- 正文引用：[[源码:WebMvcEndpointHandlerMapping.java#L起-L止]] ；代码块头部：(src: WebMvcEndpointHandlerMapping.java#L起-L止) -->

# WebMvcEndpointHandlerMapping.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/actuate/endpoint/web/WebMvcEndpointHandlerMapping.java` |
| 行数 | 103 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc/4.2.0/spring-boot-webmvc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:01.897Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.actuate.endpoint.web;
    6 | 
    7 | import java.util.Collection;
    8 | import java.util.Collections;
    9 | import java.util.Map;
   10 | 
   11 | import jakarta.servlet.http.HttpServletRequest;
   12 | import jakarta.servlet.http.HttpServletResponse;
   13 | import org.jspecify.annotations.Nullable;
   14 | 
   15 | import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
   16 | import org.springframework.aot.hint.RuntimeHints;
   17 | import org.springframework.aot.hint.RuntimeHintsRegistrar;
   18 | import org.springframework.aot.hint.annotation.Reflective;
   19 | import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
   20 | import org.springframework.boot.actuate.endpoint.OperationResponseBody;
   21 | import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
   22 | import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
   23 | import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
   24 | import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
   25 | import org.springframework.boot.actuate.endpoint.web.Link;
   26 | import org.springframework.boot.webmvc.actuate.endpoint.web.WebMvcEndpointHandlerMapping.WebMvcEndpointHandlerMappingRuntimeHints;
   27 | import org.springframework.context.annotation.ImportRuntimeHints;
   28 | import org.springframework.web.bind.annotation.ResponseBody;
   29 | import org.springframework.web.cors.CorsConfiguration;
   30 | import org.springframework.web.servlet.HandlerMapping;
   31 | 
   32 | /**
   33 |  * A custom {@link HandlerMapping} that makes web endpoints available over HTTP using
   34 |  * Spring MVC.
   35 |  *
   36 |  * @author Andy Wilkinson
   37 |  * @author Phillip Webb
   38 |  * @since 4.0.0
   39 |  */
   40 | @ImportRuntimeHints(WebMvcEndpointHandlerMappingRuntimeHints.class)
   41 | public class WebMvcEndpointHandlerMapping extends AbstractWebMvcEndpointHandlerMapping {
   42 | 
   43 | 	private final EndpointLinksResolver linksResolver;
   44 | 
   45 | 	/**
   46 | 	 * Creates a new {@code WebMvcEndpointHandlerMapping} instance that provides mappings
   47 | 	 * for the given endpoints.
   48 | 	 * @param endpointMapping the base mapping for all endpoints
   49 | 	 * @param endpoints the web endpoints
   50 | 	 * @param endpointMediaTypes media types consumed and produced by the endpoints
   51 | 	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
   52 | 	 * @param linksResolver resolver for determining links to available endpoints
   53 | 	 * @param shouldRegisterLinksMapping whether the links endpoint should be registered
   54 | 	 */
   55 | 	public WebMvcEndpointHandlerMapping(EndpointMapping endpointMapping, Collection<ExposableWebEndpoint> endpoints,
   56 | 			EndpointMediaTypes endpointMediaTypes, @Nullable CorsConfiguration corsConfiguration,
   57 | 			EndpointLinksResolver linksResolver, boolean shouldRegisterLinksMapping) {
   58 | 		super(endpointMapping, endpoints, endpointMediaTypes, corsConfiguration, shouldRegisterLinksMapping);
   59 | 		this.linksResolver = linksResolver;
   60 | 		setOrder(-100);
   61 | 	}
   62 | 
   63 | 	@Override
   64 | 	protected LinksHandler getLinksHandler() {
   65 | 		return new WebMvcLinksHandler();
   66 | 	}
   67 | 
   68 | 	/**
   69 | 	 * Handler for root endpoint providing links.
   70 | 	 */
   71 | 	class WebMvcLinksHandler implements LinksHandler {
   72 | 
   73 | 		@Override
   74 | 		@ResponseBody
   75 | 		@Reflective
   76 | 		public Map<String, Map<String, Link>> links(HttpServletRequest request, HttpServletResponse response) {
   77 | 			Map<String, Link> links = WebMvcEndpointHandlerMapping.this.linksResolver
   78 | 				.resolveLinks(request.getRequestURL().toString());
   79 | 			return OperationResponseBody.of(Collections.singletonMap("_links", links));
   80 | 		}
   81 | 
   82 | 		@Override
   83 | 		public String toString() {
   84 | 			return "Actuator root web endpoint";
   85 | 		}
   86 | 
   87 | 	}
   88 | 
   89 | 	static class WebMvcEndpointHandlerMappingRuntimeHints implements RuntimeHintsRegistrar {
   90 | 
   91 | 		private final ReflectiveRuntimeHintsRegistrar reflectiveRegistrar = new ReflectiveRuntimeHintsRegistrar();
   92 | 
   93 | 		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();
   94 | 
   95 | 		@Override
   96 | 		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
   97 | 			this.reflectiveRegistrar.registerRuntimeHints(hints, WebMvcLinksHandler.class);
   98 | 			this.bindingRegistrar.registerReflectionHints(hints.reflection(), Link.class);
   99 | 		}
  100 | 
  101 | 	}
  102 | 
  103 | }
```
