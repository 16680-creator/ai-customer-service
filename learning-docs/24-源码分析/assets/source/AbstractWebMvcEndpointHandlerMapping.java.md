<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc:4.2.0 | path: org/springframework/boot/webmvc/actuate/endpoint/web/AbstractWebMvcEndpointHandlerMapping.java -->
<!-- 正文引用：[[源码:AbstractWebMvcEndpointHandlerMapping.java#L起-L止]] ；代码块头部：(src: AbstractWebMvcEndpointHandlerMapping.java#L起-L止) -->

# AbstractWebMvcEndpointHandlerMapping.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/actuate/endpoint/web/AbstractWebMvcEndpointHandlerMapping.java` |
| 行数 | 566 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc/4.2.0/spring-boot-webmvc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:02.006Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.actuate.endpoint.web;
    6 | 
    7 | import java.lang.reflect.Method;
    8 | import java.nio.charset.StandardCharsets;
    9 | import java.security.Principal;
   10 | import java.util.ArrayList;
   11 | import java.util.Collection;
   12 | import java.util.Collections;
   13 | import java.util.List;
   14 | import java.util.Map;
   15 | import java.util.function.Function;
   16 | 
   17 | import jakarta.servlet.http.HttpServletRequest;
   18 | import jakarta.servlet.http.HttpServletResponse;
   19 | import org.jspecify.annotations.Nullable;
   20 | import reactor.core.publisher.Flux;
   21 | 
   22 | import org.springframework.aot.hint.RuntimeHints;
   23 | import org.springframework.aot.hint.RuntimeHintsRegistrar;
   24 | import org.springframework.aot.hint.annotation.Reflective;
   25 | import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
   26 | import org.springframework.beans.factory.InitializingBean;
   27 | import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
   28 | import org.springframework.boot.actuate.endpoint.InvocationContext;
   29 | import org.springframework.boot.actuate.endpoint.OperationArgumentResolver;
   30 | import org.springframework.boot.actuate.endpoint.ProducibleOperationArgumentResolver;
   31 | import org.springframework.boot.actuate.endpoint.SecurityContext;
   32 | import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
   33 | import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
   34 | import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
   35 | import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
   36 | import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
   37 | import org.springframework.boot.actuate.endpoint.web.WebOperation;
   38 | import org.springframework.boot.actuate.endpoint.web.WebOperationInvocationArguments;
   39 | import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
   40 | import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
   41 | import org.springframework.boot.webmvc.actuate.endpoint.web.AbstractWebMvcEndpointHandlerMapping.AbstractWebMvcEndpointHandlerMappingRuntimeHints;
   42 | import org.springframework.context.annotation.ImportRuntimeHints;
   43 | import org.springframework.http.HttpHeaders;
   44 | import org.springframework.http.HttpMethod;
   45 | import org.springframework.http.HttpStatus;
   46 | import org.springframework.http.MediaType;
   47 | import org.springframework.http.ResponseEntity;
   48 | import org.springframework.http.ResponseEntity.BodyBuilder;
   49 | import org.springframework.http.server.PathContainer;
   50 | import org.springframework.http.server.PathContainer.Element;
   51 | import org.springframework.http.server.PathContainer.PathSegment;
   52 | import org.springframework.http.server.ServletServerHttpRequest;
   53 | import org.springframework.util.AntPathMatcher;
   54 | import org.springframework.util.Assert;
   55 | import org.springframework.util.ClassUtils;
   56 | import org.springframework.util.ReflectionUtils;
   57 | import org.springframework.util.StringUtils;
   58 | import org.springframework.web.bind.annotation.RequestBody;
   59 | import org.springframework.web.bind.annotation.RequestMethod;
   60 | import org.springframework.web.bind.annotation.ResponseBody;
   61 | import org.springframework.web.context.WebApplicationContext;
   62 | import org.springframework.web.context.support.WebApplicationContextUtils;
   63 | import org.springframework.web.cors.CorsConfiguration;
   64 | import org.springframework.web.method.HandlerMethod;
   65 | import org.springframework.web.server.ResponseStatusException;
   66 | import org.springframework.web.servlet.HandlerMapping;
   67 | import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
   68 | import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
   69 | import org.springframework.web.util.ServletRequestPathUtils;
   70 | import org.springframework.web.util.pattern.PathPattern;
   71 | 
   72 | /**
   73 |  * A custom {@link HandlerMapping} that makes {@link ExposableWebEndpoint web endpoints}
   74 |  * available over HTTP using Spring MVC.
   75 |  *
   76 |  * @author Andy Wilkinson
   77 |  * @author Madhura Bhave
   78 |  * @author Phillip Webb
   79 |  * @author Brian Clozel
   80 |  * @author Wan bin yu
   81 |  * @since 4.0.0
   82 |  */
   83 | @ImportRuntimeHints(AbstractWebMvcEndpointHandlerMappingRuntimeHints.class)
   84 | public abstract class AbstractWebMvcEndpointHandlerMapping extends RequestMappingInfoHandlerMapping
   85 | 		implements InitializingBean {
   86 | 
   87 | 	private final EndpointMapping endpointMapping;
   88 | 
   89 | 	private final Collection<ExposableWebEndpoint> endpoints;
   90 | 
   91 | 	private final EndpointMediaTypes endpointMediaTypes;
   92 | 
   93 | 	private final @Nullable CorsConfiguration corsConfiguration;
   94 | 
   95 | 	private final boolean shouldRegisterLinksMapping;
   96 | 
   97 | 	private final Method handleMethod = getHandleMethod();
   98 | 
   99 | 	private final Method catchAllMethod = getCatchAllMethod();
  100 | 
  101 | 	private RequestMappingInfo.BuilderConfiguration builderConfig = new RequestMappingInfo.BuilderConfiguration();
  102 | 
  103 | 	/**
  104 | 	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
  105 | 	 * operations of the given {@code webEndpoints}.
  106 | 	 * @param endpointMapping the base mapping for all endpoints
  107 | 	 * @param endpoints the web endpoints
  108 | 	 * @param endpointMediaTypes media types consumed and produced by the endpoints
  109 | 	 * @param shouldRegisterLinksMapping whether the links endpoint should be registered
  110 | 	 */
  111 | 	public AbstractWebMvcEndpointHandlerMapping(EndpointMapping endpointMapping,
  112 | 			Collection<ExposableWebEndpoint> endpoints, EndpointMediaTypes endpointMediaTypes,
  113 | 			boolean shouldRegisterLinksMapping) {
  114 | 		this(endpointMapping, endpoints, endpointMediaTypes, null, shouldRegisterLinksMapping);
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * Creates a new {@code AbstractWebMvcEndpointHandlerMapping} that provides mappings
  119 | 	 * for the operations of the given endpoints.
  120 | 	 * @param endpointMapping the base mapping for all endpoints
  121 | 	 * @param endpoints the web endpoints
  122 | 	 * @param endpointMediaTypes media types consumed and produced by the endpoints
  123 | 	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
  124 | 	 * @param shouldRegisterLinksMapping whether the links endpoint should be registered
  125 | 	 */
  126 | 	public AbstractWebMvcEndpointHandlerMapping(EndpointMapping endpointMapping,
  127 | 			Collection<ExposableWebEndpoint> endpoints, EndpointMediaTypes endpointMediaTypes,
  128 | 			@Nullable CorsConfiguration corsConfiguration, boolean shouldRegisterLinksMapping) {
  129 | 		this.endpointMapping = endpointMapping;
  130 | 		this.endpoints = endpoints;
  131 | 		this.endpointMediaTypes = endpointMediaTypes;
  132 | 		this.corsConfiguration = corsConfiguration;
  133 | 		this.shouldRegisterLinksMapping = shouldRegisterLinksMapping;
  134 | 		setOrder(-100);
  135 | 	}
  136 | 
  137 | 	private static Method getHandleMethod() {
  138 | 		Method method = ReflectionUtils.findMethod(OperationHandler.class, "handle", HttpServletRequest.class,
  139 | 				Map.class);
  140 | 		Assert.state(method != null, "'method' must not be null");
  141 | 		return method;
  142 | 	}
  143 | 
  144 | 	private static Method getCatchAllMethod() {
  145 | 		Method method = ReflectionUtils.findMethod(CatchAllHandler.class, "handle", HttpServletResponse.class);
  146 | 		Assert.state(method != null, "'method' must not be null");
  147 | 		return method;
  148 | 	}
  149 | 
  150 | 	@Override
  151 | 	public void afterPropertiesSet() {
  152 | 		this.builderConfig = new RequestMappingInfo.BuilderConfiguration();
  153 | 		this.builderConfig.setPatternParser(getPatternParser());
  154 | 		super.afterPropertiesSet();
  155 | 	}
  156 | 
  157 | 	@Override
  158 | 	protected void initHandlerMethods() {
  159 | 		for (ExposableWebEndpoint endpoint : this.endpoints) {
  160 | 			for (WebOperation operation : endpoint.getOperations()) {
  161 | 				registerMappingForOperation(endpoint, operation);
  162 | 			}
  163 | 		}
  164 | 		if (this.shouldRegisterLinksMapping) {
  165 | 			registerLinksMapping();
  166 | 		}
  167 | 	}
  168 | 
  169 | 	@Override
  170 | 	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
  171 | 		HandlerMethod handlerMethod = super.createHandlerMethod(handler, method);
  172 | 		return new WebMvcEndpointHandlerMethod(handlerMethod.getBean(), handlerMethod.getMethod());
  173 | 	}
  174 | 
  175 | 	private void registerMappingForOperation(ExposableWebEndpoint endpoint, WebOperation operation) {
  176 | 		WebOperationRequestPredicate predicate = operation.getRequestPredicate();
  177 | 		String path = predicate.getPath();
  178 | 		String matchAllRemainingPathSegmentsVariable = predicate.getMatchAllRemainingPathSegmentsVariable();
  179 | 		if (matchAllRemainingPathSegmentsVariable != null) {
  180 | 			path = path.replace("{*" + matchAllRemainingPathSegmentsVariable + "}", "**");
  181 | 		}
  182 | 		registerMapping(endpoint, predicate, operation, path);
  183 | 	}
  184 | 
  185 | 	protected void registerMapping(ExposableWebEndpoint endpoint, WebOperationRequestPredicate predicate,
  186 | 			WebOperation operation, String path) {
  187 | 		ServletWebOperation servletWebOperation = wrapServletWebOperation(endpoint, operation,
  188 | 				new ServletWebOperationAdapter(operation));
  189 | 		registerMapping(createRequestMappingInfo(predicate, path), new OperationHandler(servletWebOperation),
  190 | 				this.handleMethod);
  191 | 	}
  192 | 
  193 | 	/**
  194 | 	 * Hook point that allows subclasses to wrap the {@link ServletWebOperation} before
  195 | 	 * it's called. Allows additional features, such as security, to be added.
  196 | 	 * @param endpoint the source endpoint
  197 | 	 * @param operation the source operation
  198 | 	 * @param servletWebOperation the servlet web operation to wrap
  199 | 	 * @return a wrapped servlet web operation
  200 | 	 */
  201 | 	protected ServletWebOperation wrapServletWebOperation(ExposableWebEndpoint endpoint, WebOperation operation,
  202 | 			ServletWebOperation servletWebOperation) {
  203 | 		return servletWebOperation;
  204 | 	}
  205 | 
  206 | 	/**
  207 | 	 * Register a "catch all" handler for the rest of actuator namespace, ensuring that
  208 | 	 * all requests are handled by this handler mapping.
  209 | 	 * @param responseStatus the response status to use for handled requests
  210 | 	 */
  211 | 	protected void registerCatchAllMapping(HttpStatus responseStatus) {
  212 | 		String subPath = this.endpointMapping.createSubPath("/**");
  213 | 		registerMapping(RequestMappingInfo.paths(subPath).options(this.builderConfig).build(),
  214 | 				new CatchAllHandler(responseStatus), this.catchAllMethod);
  215 | 	}
  216 | 
  217 | 	private RequestMappingInfo createRequestMappingInfo(WebOperationRequestPredicate predicate, String path) {
  218 | 		String subPath = this.endpointMapping.createSubPath(path);
  219 | 		List<String> paths = new ArrayList<>();
  220 | 		paths.add(subPath);
  221 | 		if (!StringUtils.hasLength(subPath)) {
  222 | 			paths.add("/");
  223 | 		}
  224 | 		return RequestMappingInfo.paths(paths.toArray(new String[0]))
  225 | 			.options(this.builderConfig)
  226 | 			.methods(RequestMethod.valueOf(predicate.getHttpMethod().name()))
  227 | 			.consumes(predicate.getConsumes().toArray(new String[0]))
  228 | 			.produces(predicate.getProduces().toArray(new String[0]))
  229 | 			.build();
  230 | 	}
  231 | 
  232 | 	private void registerLinksMapping() {
  233 | 		String path = this.endpointMapping.getPath();
  234 | 		String linksPath = (StringUtils.hasLength(path)) ? this.endpointMapping.createSubPath("/") : "/";
  235 | 		RequestMappingInfo mapping = RequestMappingInfo.paths(linksPath)
  236 | 			.methods(RequestMethod.GET)
  237 | 			.produces(this.endpointMediaTypes.getProduced().toArray(new String[0]))
  238 | 			.options(this.builderConfig)
  239 | 			.build();
  240 | 		LinksHandler linksHandler = getLinksHandler();
  241 | 		Method links = ReflectionUtils.findMethod(linksHandler.getClass(), "links", HttpServletRequest.class,
  242 | 				HttpServletResponse.class);
  243 | 		Assert.state(links != null, "'links' must not be null");
  244 | 		registerMapping(mapping, linksHandler, links);
  245 | 	}
  246 | 
  247 | 	@Override
  248 | 	protected boolean hasCorsConfigurationSource(Object handler) {
  249 | 		return this.corsConfiguration != null;
  250 | 	}
  251 | 
  252 | 	@Override
  253 | 	protected @Nullable CorsConfiguration initCorsConfiguration(Object handler, Method method,
  254 | 			RequestMappingInfo mapping) {
  255 | 		return this.corsConfiguration;
  256 | 	}
  257 | 
  258 | 	@Override
  259 | 	protected @Nullable CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
  260 | 		CorsConfiguration corsConfiguration = super.getCorsConfiguration(handler, request);
  261 | 		return (corsConfiguration != null) ? corsConfiguration : this.corsConfiguration;
  262 | 	}
  263 | 
  264 | 	@Override
  265 | 	protected boolean isHandler(Class<?> beanType) {
  266 | 		return false;
  267 | 	}
  268 | 
  269 | 	@Override
  270 | 	protected @Nullable RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
  271 | 		return null;
  272 | 	}
  273 | 
  274 | 	/**
  275 | 	 * Return the Handler providing actuator links at the root endpoint.
  276 | 	 * @return the links handler
  277 | 	 */
  278 | 	protected abstract LinksHandler getLinksHandler();
  279 | 
  280 | 	/**
  281 | 	 * Return the web endpoints being mapped.
  282 | 	 * @return the endpoints
  283 | 	 */
  284 | 	public Collection<ExposableWebEndpoint> getEndpoints() {
  285 | 		return this.endpoints;
  286 | 	}
  287 | 
  288 | 	/**
  289 | 	 * Handler providing actuator links at the root endpoint.
  290 | 	 */
  291 | 	@FunctionalInterface
  292 | 	protected interface LinksHandler {
  293 | 
  294 | 		@Nullable Object links(HttpServletRequest request, HttpServletResponse response);
  295 | 
  296 | 	}
  297 | 
  298 | 	/**
  299 | 	 * A servlet web operation that can be handled by Spring MVC.
  300 | 	 */
  301 | 	@FunctionalInterface
  302 | 	protected interface ServletWebOperation {
  303 | 
  304 | 		@Nullable Object handle(HttpServletRequest request, @Nullable Map<String, String> body);
  305 | 
  306 | 	}
  307 | 
  308 | 	/**
  309 | 	 * Adapter class to convert an {@link OperationInvoker} into a
  310 | 	 * {@link ServletWebOperation}.
  311 | 	 */
  312 | 	private static class ServletWebOperationAdapter implements ServletWebOperation {
  313 | 
  314 | 		private static final String PATH_SEPARATOR = AntPathMatcher.DEFAULT_PATH_SEPARATOR;
  315 | 
  316 | 		private static final List<Function<Object, Object>> BODY_CONVERTERS;
  317 | 
  318 | 		static {
  319 | 			List<Function<Object, Object>> converters = new ArrayList<>();
  320 | 			if (ClassUtils.isPresent("reactor.core.publisher.Flux",
  321 | 					ServletWebOperationAdapter.class.getClassLoader())) {
  322 | 				converters.add(new FluxBodyConverter());
  323 | 			}
  324 | 			BODY_CONVERTERS = Collections.unmodifiableList(converters);
  325 | 		}
  326 | 
  327 | 		private final WebOperation operation;
  328 | 
  329 | 		ServletWebOperationAdapter(WebOperation operation) {
  330 | 			this.operation = operation;
  331 | 		}
  332 | 
  333 | 		@Override
  334 | 		public @Nullable Object handle(HttpServletRequest request,
  335 | 				@RequestBody(required = false) @Nullable Map<String, String> body) {
  336 | 			HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
  337 | 			try {
  338 | 				ServletSecurityContext securityContext = new ServletSecurityContext(request);
  339 | 				InvocationContext invocationContext = getInvocationContext(request, body, headers, securityContext);
  340 | 				return handleResult(this.operation.invoke(invocationContext), HttpMethod.valueOf(request.getMethod()));
  341 | 			}
  342 | 			catch (InvalidEndpointRequestException ex) {
  343 | 				throw new InvalidEndpointBadRequestException(ex);
  344 | 			}
  345 | 		}
  346 | 
  347 | 		private InvocationContext getInvocationContext(HttpServletRequest request, @Nullable Map<String, String> body,
  348 | 				HttpHeaders headers, ServletSecurityContext securityContext) {
  349 | 			Map<String, Object> arguments = getArguments(request, body);
  350 | 			OperationArgumentResolver serverNamespaceResolver = OperationArgumentResolver.of(WebServerNamespace.class,
  351 | 					() -> getServerNamespace(request));
  352 | 			ProducibleOperationArgumentResolver producibleOperationResolver = new ProducibleOperationArgumentResolver(
  353 | 					() -> headers.get("Accept"));
  354 | 			return new InvocationContext(securityContext, arguments, serverNamespaceResolver,
  355 | 					producibleOperationResolver);
  356 | 		}
  357 | 
  358 | 		private @Nullable WebServerNamespace getServerNamespace(HttpServletRequest request) {
  359 | 			WebApplicationContext context = WebApplicationContextUtils
  360 | 				.getRequiredWebApplicationContext(request.getServletContext());
  361 | 			return WebServerNamespace.from(context);
  362 | 		}
  363 | 
  364 | 		@Override
  365 | 		public String toString() {
  366 | 			return "Actuator web endpoint '" + this.operation.getId() + "'";
  367 | 		}
  368 | 
  369 | 		private Map<String, Object> getArguments(HttpServletRequest request, @Nullable Map<String, String> body) {
  370 | 			return WebOperationInvocationArguments.get(this.operation, (invocationArguments) -> {
  371 | 				invocationArguments.queryParameters(request.getParameterMap());
  372 | 				invocationArguments.pathParameters(getTemplateVariables(request));
  373 | 				if (HttpMethod.POST.name().equals(request.getMethod())) {
  374 | 					invocationArguments.body(body);
  375 | 				}
  376 | 				invocationArguments.remainPathSegments((variable) -> getRemainingPathSegments(request));
  377 | 			});
  378 | 		}
  379 | 
  380 | 		private String[] getRemainingPathSegments(HttpServletRequest request) {
  381 | 			PathPattern pathPattern = getPathPattern(request);
  382 | 			if (pathPattern.hasPatternSyntax()) {
  383 | 				List<String> segments = new ArrayList<>();
  384 | 				PathContainer container = pathPattern.extractPathWithinPattern(
  385 | 						ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication());
  386 | 				for (Element element : container.elements()) {
  387 | 					if (element instanceof PathSegment pathSegment
  388 | 							&& StringUtils.hasLength(pathSegment.valueToMatch())) {
  389 | 						segments.add(pathSegment.valueToMatch());
  390 | 					}
  391 | 				}
  392 | 				return segments.toArray(new String[0]);
  393 | 			}
  394 | 			return tokenizePathSegments(pathPattern.toString());
  395 | 		}
  396 | 
  397 | 		private PathPattern getPathPattern(HttpServletRequest request) {
  398 | 			PathPattern pathPattern = (PathPattern) request
  399 | 				.getAttribute(HandlerMapping.BEST_MATCHING_PATH_PATTERN_ATTRIBUTE);
  400 | 			Assert.state(pathPattern != null, "'pathPattern' must not be null");
  401 | 			return pathPattern;
  402 | 		}
  403 | 
  404 | 		private String[] tokenizePathSegments(String value) {
  405 | 			String[] segments = StringUtils.tokenizeToStringArray(value, PATH_SEPARATOR, false, true);
  406 | 			for (int i = 0; i < segments.length; i++) {
  407 | 				if (segments[i].contains("%")) {
  408 | 					segments[i] = StringUtils.uriDecode(segments[i], StandardCharsets.UTF_8);
  409 | 				}
  410 | 			}
  411 | 			return segments;
  412 | 		}
  413 | 
  414 | 		@SuppressWarnings("unchecked")
  415 | 		private Map<String, String> getTemplateVariables(HttpServletRequest request) {
  416 | 			return (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
  417 | 		}
  418 | 
  419 | 		private @Nullable Object handleResult(@Nullable Object result, HttpMethod httpMethod) {
  420 | 			if (result == null) {
  421 | 				return new ResponseEntity<>(
  422 | 						(httpMethod != HttpMethod.GET) ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND);
  423 | 			}
  424 | 			if (!(result instanceof WebEndpointResponse<?> response)) {
  425 | 				return convertIfNecessary(result);
  426 | 			}
  427 | 			MediaType contentType = (response.getContentType() != null) ? new MediaType(response.getContentType())
  428 | 					: null;
  429 | 			BodyBuilder builder = ResponseEntity.status(response.getStatus());
  430 | 			if (contentType != null) {
  431 | 				builder = builder.contentType(contentType);
  432 | 			}
  433 | 			return builder.body(convertIfNecessary(response.getBody()));
  434 | 		}
  435 | 
  436 | 		private @Nullable Object convertIfNecessary(@Nullable Object body) {
  437 | 			if (body == null) {
  438 | 				return null;
  439 | 			}
  440 | 			for (Function<Object, Object> converter : BODY_CONVERTERS) {
  441 | 				body = converter.apply(body);
  442 | 			}
  443 | 			return body;
  444 | 		}
  445 | 
  446 | 		private static final class FluxBodyConverter implements Function<Object, Object> {
  447 | 
  448 | 			@Override
  449 | 			public Object apply(Object body) {
  450 | 				if (!(body instanceof Flux)) {
  451 | 					return body;
  452 | 				}
  453 | 				return ((Flux<?>) body).collectList();
  454 | 			}
  455 | 
  456 | 		}
  457 | 
  458 | 	}
  459 | 
  460 | 	/**
  461 | 	 * Handler for a {@link ServletWebOperation}.
  462 | 	 */
  463 | 	private static final class OperationHandler {
  464 | 
  465 | 		private final ServletWebOperation operation;
  466 | 
  467 | 		OperationHandler(ServletWebOperation operation) {
  468 | 			this.operation = operation;
  469 | 		}
  470 | 
  471 | 		@ResponseBody
  472 | 		@Reflective
  473 | 		@Nullable Object handle(HttpServletRequest request, @RequestBody(required = false) Map<String, String> body) {
  474 | 			return this.operation.handle(request, body);
  475 | 		}
  476 | 
  477 | 		@Override
  478 | 		public String toString() {
  479 | 			return this.operation.toString();
  480 | 		}
  481 | 
  482 | 	}
  483 | 
  484 | 	/**
  485 | 	 * Catch-all handler that always replies with a fixed HTTP status.
  486 | 	 */
  487 | 	private static final class CatchAllHandler {
  488 | 
  489 | 		private final HttpStatus responseStatus;
  490 | 
  491 | 		CatchAllHandler(HttpStatus responseStatus) {
  492 | 			this.responseStatus = responseStatus;
  493 | 		}
  494 | 
  495 | 		@Reflective
  496 | 		void handle(HttpServletResponse response) {
  497 | 			response.setStatus(this.responseStatus.value());
  498 | 		}
  499 | 
  500 | 	}
  501 | 
  502 | 	/**
  503 | 	 * {@link HandlerMethod} subclass for endpoint information logging.
  504 | 	 */
  505 | 	private static class WebMvcEndpointHandlerMethod extends HandlerMethod {
  506 | 
  507 | 		WebMvcEndpointHandlerMethod(Object bean, Method method) {
  508 | 			super(bean, method);
  509 | 		}
  510 | 
  511 | 		@Override
  512 | 		public String toString() {
  513 | 			return getBean().toString();
  514 | 		}
  515 | 
  516 | 		@Override
  517 | 		public HandlerMethod createWithResolvedBean() {
  518 | 			return this;
  519 | 		}
  520 | 
  521 | 	}
  522 | 
  523 | 	/**
  524 | 	 * Nested exception used to wrap an {@link InvalidEndpointRequestException} and
  525 | 	 * provide a {@link HttpStatus#BAD_REQUEST} status.
  526 | 	 */
  527 | 	private static class InvalidEndpointBadRequestException extends ResponseStatusException {
  528 | 
  529 | 		InvalidEndpointBadRequestException(InvalidEndpointRequestException cause) {
  530 | 			super(HttpStatus.BAD_REQUEST, cause.getReason(), cause);
  531 | 		}
  532 | 
  533 | 	}
  534 | 
  535 | 	private static final class ServletSecurityContext implements SecurityContext {
  536 | 
  537 | 		private final HttpServletRequest request;
  538 | 
  539 | 		private ServletSecurityContext(HttpServletRequest request) {
  540 | 			this.request = request;
  541 | 		}
  542 | 
  543 | 		@Override
  544 | 		public Principal getPrincipal() {
  545 | 			return this.request.getUserPrincipal();
  546 | 		}
  547 | 
  548 | 		@Override
  549 | 		public boolean isUserInRole(String role) {
  550 | 			return this.request.isUserInRole(role);
  551 | 		}
  552 | 
  553 | 	}
  554 | 
  555 | 	static class AbstractWebMvcEndpointHandlerMappingRuntimeHints implements RuntimeHintsRegistrar {
  556 | 
  557 | 		private final ReflectiveRuntimeHintsRegistrar reflectiveRegistrar = new ReflectiveRuntimeHintsRegistrar();
  558 | 
  559 | 		@Override
  560 | 		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
  561 | 			this.reflectiveRegistrar.registerRuntimeHints(hints, OperationHandler.class, CatchAllHandler.class);
  562 | 		}
  563 | 
  564 | 	}
  565 | 
  566 | }
```
