<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/RequestMappingHandlerMapping.java -->
<!-- 正文引用：[[源码:RequestMappingHandlerMapping.java#L起-L止]] ；代码块头部：(src: RequestMappingHandlerMapping.java#L起-L止) -->

# RequestMappingHandlerMapping.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/RequestMappingHandlerMapping.java` |
| 行数 | 584 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:18:37.677Z |

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
   17 | package org.springframework.web.servlet.mvc.method.annotation;
   18 | 
   19 | import java.lang.annotation.Annotation;
   20 | import java.lang.reflect.AnnotatedElement;
   21 | import java.lang.reflect.Method;
   22 | import java.lang.reflect.Modifier;
   23 | import java.util.Collections;
   24 | import java.util.LinkedHashMap;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.Objects;
   28 | import java.util.function.Predicate;
   29 | import java.util.stream.Stream;
   30 | 
   31 | import org.jspecify.annotations.Nullable;
   32 | 
   33 | import org.springframework.context.EmbeddedValueResolverAware;
   34 | import org.springframework.core.MethodParameter;
   35 | import org.springframework.core.annotation.AnnotatedElementUtils;
   36 | import org.springframework.core.annotation.AnnotatedMethod;
   37 | import org.springframework.core.annotation.MergedAnnotation;
   38 | import org.springframework.core.annotation.MergedAnnotationPredicates;
   39 | import org.springframework.core.annotation.MergedAnnotations;
   40 | import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
   41 | import org.springframework.core.annotation.RepeatableContainers;
   42 | import org.springframework.stereotype.Controller;
   43 | import org.springframework.util.Assert;
   44 | import org.springframework.util.ClassUtils;
   45 | import org.springframework.util.CollectionUtils;
   46 | import org.springframework.util.StringUtils;
   47 | import org.springframework.util.StringValueResolver;
   48 | import org.springframework.web.accept.ContentNegotiationManager;
   49 | import org.springframework.web.accept.DefaultApiVersionStrategy;
   50 | import org.springframework.web.bind.annotation.CrossOrigin;
   51 | import org.springframework.web.bind.annotation.RequestBody;
   52 | import org.springframework.web.bind.annotation.RequestMapping;
   53 | import org.springframework.web.bind.annotation.RequestMethod;
   54 | import org.springframework.web.cors.CorsConfiguration;
   55 | import org.springframework.web.method.HandlerMethod;
   56 | import org.springframework.web.service.annotation.HttpExchange;
   57 | import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;
   58 | import org.springframework.web.servlet.mvc.condition.CompositeRequestCondition;
   59 | import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
   60 | import org.springframework.web.servlet.mvc.condition.RequestCondition;
   61 | import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
   62 | import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
   63 | 
   64 | /**
   65 |  * Creates {@link RequestMappingInfo} instances from type-level and method-level
   66 |  * {@link RequestMapping @RequestMapping} and {@link HttpExchange @HttpExchange}
   67 |  * annotations in {@link Controller @Controller} classes.
   68 |  *
   69 |  * @author Arjen Poutsma
   70 |  * @author Rossen Stoyanchev
   71 |  * @author Sam Brannen
   72 |  * @author Olga Maciaszek-Sharma
   73 |  * @author Yongjun Hong
   74 |  * @since 3.1
   75 |  */
   76 | @SuppressWarnings("removal")
   77 | public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
   78 | 		implements EmbeddedValueResolverAware {
   79 | 
   80 | 	private static final String[] EMPTY_STRING_ARRAY = new String[0];
   81 | 
   82 | 	private static final RequestMethod[] EMPTY_REQUEST_METHOD_ARRAY = new RequestMethod[0];
   83 | 
   84 | 
   85 | 	private Map<String, Predicate<Class<?>>> pathPrefixes = Collections.emptyMap();
   86 | 
   87 | 	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();
   88 | 
   89 | 	private @Nullable StringValueResolver embeddedValueResolver;
   90 | 
   91 | 	private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();
   92 | 
   93 | 
   94 | 	/**
   95 | 	 * Configure path prefixes to apply to controller methods.
   96 | 	 * <p>Prefixes are used to enrich the mappings of every {@code @RequestMapping}
   97 | 	 * method and {@code @HttpExchange} method whose controller type is matched
   98 | 	 * by a corresponding {@code Predicate} in the map. The prefix for the first
   99 | 	 * matching predicate is used, assuming the input map has predictable order.
  100 | 	 * <p>Consider using {@link org.springframework.web.method.HandlerTypePredicate
  101 | 	 * HandlerTypePredicate} to group controllers.
  102 | 	 * @param prefixes a map with path prefixes as key
  103 | 	 * @since 5.1
  104 | 	 */
  105 | 	public void setPathPrefixes(Map<String, Predicate<Class<?>>> prefixes) {
  106 | 		this.pathPrefixes = (!prefixes.isEmpty() ?
  107 | 				Collections.unmodifiableMap(new LinkedHashMap<>(prefixes)) :
  108 | 				Collections.emptyMap());
  109 | 	}
  110 | 
  111 | 	/**
  112 | 	 * The configured path prefixes as a read-only, possibly empty map.
  113 | 	 * @since 5.1
  114 | 	 */
  115 | 	public Map<String, Predicate<Class<?>>> getPathPrefixes() {
  116 | 		return this.pathPrefixes;
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * Set the {@link ContentNegotiationManager} to use to determine requested media types.
  121 | 	 * If not set, the default constructor is used.
  122 | 	 */
  123 | 	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
  124 | 		Assert.notNull(contentNegotiationManager, "ContentNegotiationManager must not be null");
  125 | 		this.contentNegotiationManager = contentNegotiationManager;
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Return the configured {@link ContentNegotiationManager}.
  130 | 	 */
  131 | 	public ContentNegotiationManager getContentNegotiationManager() {
  132 | 		return this.contentNegotiationManager;
  133 | 	}
  134 | 
  135 | 	@Override
  136 | 	public void setEmbeddedValueResolver(StringValueResolver resolver) {
  137 | 		this.embeddedValueResolver = resolver;
  138 | 	}
  139 | 
  140 | 	@Override
  141 | 	@SuppressWarnings("removal")
  142 | 	public void afterPropertiesSet() {
  143 | 		this.config = new RequestMappingInfo.BuilderConfiguration();
  144 | 		this.config.setContentNegotiationManager(getContentNegotiationManager());
  145 | 		this.config.setApiVersionStrategy(getApiVersionStrategy());
  146 | 
  147 | 		if (getPatternParser() != null) {
  148 | 			this.config.setPatternParser(getPatternParser());
  149 | 		}
  150 | 		else {
  151 | 			this.config.setPathMatcher(getPathMatcher());
  152 | 		}
  153 | 
  154 | 		super.afterPropertiesSet();
  155 | 	}
  156 | 
  157 | 
  158 | 	/**
  159 | 	 * Obtain a {@link RequestMappingInfo.BuilderConfiguration} that reflects
  160 | 	 * the internal configuration of this {@code HandlerMapping} and can be used
  161 | 	 * to set {@link RequestMappingInfo.Builder#options(RequestMappingInfo.BuilderConfiguration)}.
  162 | 	 * <p>This is useful for programmatic registration of request mappings via
  163 | 	 * {@link #registerHandlerMethod(Object, Method, RequestMappingInfo)}.
  164 | 	 * @return the builder configuration that reflects the internal state
  165 | 	 * @since 5.3.14
  166 | 	 */
  167 | 	public RequestMappingInfo.BuilderConfiguration getBuilderConfiguration() {
  168 | 		return this.config;
  169 | 	}
  170 | 
  171 | 
  172 | 	/**
  173 | 	 * {@inheritDoc}
  174 | 	 * <p>Expects a handler to have a type-level @{@link Controller} annotation.
  175 | 	 */
  176 | 	@Override
  177 | 	protected boolean isHandler(Class<?> beanType) {
  178 | 		return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
  179 | 	}
  180 | 
  181 | 
  182 | 	/**
  183 | 	 * Uses type-level and method-level {@link RequestMapping @RequestMapping}
  184 | 	 * and {@link HttpExchange @HttpExchange} annotations to create the
  185 | 	 * {@link RequestMappingInfo}.
  186 | 	 * <p>For CGLIB proxy classes, additional validation is performed based on
  187 | 	 * method visibility:
  188 | 	 * <ul>
  189 | 	 * <li>Private methods cannot be overridden and therefore cannot be used as
  190 | 	 * handler methods.</li>
  191 | 	 * <li>Package-private methods from different packages are inaccessible and
  192 | 	 * must be changed to public or protected.</li>
  193 | 	 * </ul>
  194 | 	 * @return the created {@code RequestMappingInfo}, or {@code null} if the method
  195 | 	 * does not have a {@code @RequestMapping} or {@code @HttpExchange} annotation
  196 | 	 * @see #getCustomMethodCondition(Method)
  197 | 	 * @see #getCustomTypeCondition(Class)
  198 | 	 */
  199 | 	@Override
  200 | 	protected @Nullable RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
  201 | 		validateCglibProxyMethodVisibility(method, handlerType);
  202 | 
  203 | 		RequestMappingInfo info = createRequestMappingInfo(method);
  204 | 		if (info != null) {
  205 | 			RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
  206 | 			if (typeInfo != null) {
  207 | 				info = typeInfo.combine(info);
  208 | 			}
  209 | 			if (info.isEmptyMapping()) {
  210 | 				info = info.mutate().paths("", "/").options(this.config).build();
  211 | 			}
  212 | 			String prefix = getPathPrefix(handlerType);
  213 | 			if (prefix != null) {
  214 | 				info = RequestMappingInfo.paths(prefix).options(this.config).build().combine(info);
  215 | 			}
  216 | 		}
  217 | 		return info;
  218 | 	}
  219 | 
  220 | 	/**
  221 | 	 * Validate the method visibility requirements specified in {@link #getMappingForMethod(Method, Class)}.
  222 | 	 * @since 7.0
  223 | 	 */
  224 | 	private static void validateCglibProxyMethodVisibility(Method method, Class<?> handlerType) {
  225 | 		if (handlerType.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
  226 | 			int modifiers = method.getModifiers();
  227 | 
  228 | 			if (Modifier.isPrivate(modifiers)) {
  229 | 				throw new IllegalStateException("""
  230 | 						Private method [%s] on CGLIB proxy class [%s] cannot be used as a request \
  231 | 						handler method, because private methods cannot be overridden. \
  232 | 						Change the method to non-private visibility, or use interface-based JDK \
  233 | 						proxying instead.""".formatted(method.getName(), handlerType.getName()));
  234 | 			}
  235 | 
  236 | 			if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)) {
  237 | 				Class<?> declaringClass = method.getDeclaringClass();
  238 | 				String declaringPackage = declaringClass.getPackage().getName();
  239 | 				String handlerPackage = handlerType.getPackage().getName();
  240 | 
  241 | 				if (!Objects.equals(declaringPackage, handlerPackage)) {
  242 | 					throw new IllegalStateException("""
  243 | 							Package-private method [%s] declared in class [%s] cannot be advised by \
  244 | 							CGLIB-proxied handler class [%s], because it is effectively private. Either \
  245 | 							make the method public or protected, or use interface-based JDK proxying instead."""
  246 | 								.formatted(method.getName(), declaringClass.getName(), handlerType.getName()));
  247 | 				}
  248 | 			}
  249 | 		}
  250 | 	}
  251 | 
  252 | 	@Nullable String getPathPrefix(Class<?> handlerType) {
  253 | 		for (Map.Entry<String, Predicate<Class<?>>> entry : this.pathPrefixes.entrySet()) {
  254 | 			if (entry.getValue().test(handlerType)) {
  255 | 				String prefix = entry.getKey();
  256 | 				if (this.embeddedValueResolver != null) {
  257 | 					prefix = this.embeddedValueResolver.resolveStringValue(prefix);
  258 | 				}
  259 | 				return prefix;
  260 | 			}
  261 | 		}
  262 | 		return null;
  263 | 	}
  264 | 
  265 | 	private @Nullable RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
  266 | 
  267 | 		List<AnnotationDescriptor> descriptors =
  268 | 				MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
  269 | 						.stream()
  270 | 						.filter(MergedAnnotationPredicates.typeIn(RequestMapping.class, HttpExchange.class))
  271 | 						.filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
  272 | 						.map(AnnotationDescriptor::new)
  273 | 						.distinct()
  274 | 						.toList();
  275 | 
  276 | 		RequestMappingInfo info = null;
  277 | 		RequestCondition<?> customCondition = (element instanceof Class<?> clazz ?
  278 | 				getCustomTypeCondition(clazz) : getCustomMethodCondition((Method) element));
  279 | 
  280 | 		List<AnnotationDescriptor> mappingDescriptors =
  281 | 				descriptors.stream().filter(desc -> desc.annotation instanceof RequestMapping).toList();
  282 | 
  283 | 		if (!mappingDescriptors.isEmpty()) {
  284 | 			checkMultipleAnnotations(element, mappingDescriptors);
  285 | 			info = createRequestMappingInfo((RequestMapping) mappingDescriptors.get(0).annotation, customCondition);
  286 | 		}
  287 | 
  288 | 		List<AnnotationDescriptor> exchangeDescriptors =
  289 | 				descriptors.stream().filter(desc -> desc.annotation instanceof HttpExchange).toList();
  290 | 
  291 | 		if (!exchangeDescriptors.isEmpty()) {
  292 | 			checkMultipleAnnotations(element, info, mappingDescriptors, exchangeDescriptors);
  293 | 			info = createRequestMappingInfo((HttpExchange) exchangeDescriptors.get(0).annotation, customCondition);
  294 | 		}
  295 | 
  296 | 		if (info != null && getApiVersionStrategy() instanceof DefaultApiVersionStrategy davs) {
  297 | 			String version = info.getVersionCondition().getVersion();
  298 | 			if (version != null) {
  299 | 				davs.addMappedVersion(version);
  300 | 			}
  301 | 		}
  302 | 
  303 | 		return info;
  304 | 	}
  305 | 
  306 | 	/**
  307 | 	 * Provide a custom type-level request condition.
  308 | 	 * The custom {@link RequestCondition} can be of any type so long as the
  309 | 	 * same condition type is returned from all calls to this method in order
  310 | 	 * to ensure custom request conditions can be combined and compared.
  311 | 	 * <p>Consider extending {@link AbstractRequestCondition} for custom
  312 | 	 * condition types and using {@link CompositeRequestCondition} to provide
  313 | 	 * multiple custom conditions.
  314 | 	 * @param handlerType the handler type for which to create the condition
  315 | 	 * @return the condition, or {@code null}
  316 | 	 */
  317 | 	protected @Nullable RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
  318 | 		return null;
  319 | 	}
  320 | 
  321 | 	/**
  322 | 	 * Provide a custom method-level request condition.
  323 | 	 * The custom {@link RequestCondition} can be of any type so long as the
  324 | 	 * same condition type is returned from all calls to this method in order
  325 | 	 * to ensure custom request conditions can be combined and compared.
  326 | 	 * <p>Consider extending {@link AbstractRequestCondition} for custom
  327 | 	 * condition types and using {@link CompositeRequestCondition} to provide
  328 | 	 * multiple custom conditions.
  329 | 	 * @param method the handler method for which to create the condition
  330 | 	 * @return the condition, or {@code null}
  331 | 	 */
  332 | 	protected @Nullable RequestCondition<?> getCustomMethodCondition(Method method) {
  333 | 		return null;
  334 | 	}
  335 | 
  336 | 	private void checkMultipleAnnotations(
  337 | 			AnnotatedElement element, List<AnnotationDescriptor> mappingDescriptors) {
  338 | 
  339 | 		if (logger.isWarnEnabled() && mappingDescriptors.size() > 1) {
  340 | 			logger.warn("Multiple @RequestMapping annotations found on %s, but only the first will be used: %s"
  341 | 					.formatted(element, mappingDescriptors));
  342 | 		}
  343 | 	}
  344 | 
  345 | 	private static void checkMultipleAnnotations(
  346 | 			AnnotatedElement element, @Nullable RequestMappingInfo info,
  347 | 			List<AnnotationDescriptor> mappingDescriptors, List<AnnotationDescriptor> exchangeDescriptors) {
  348 | 
  349 | 		Assert.state(info == null,
  350 | 				() -> "%s is annotated with @RequestMapping and @HttpExchange annotations, but only one is allowed: %s"
  351 | 						.formatted(element, Stream.of(mappingDescriptors, exchangeDescriptors).flatMap(List::stream).toList()));
  352 | 
  353 | 		Assert.state(exchangeDescriptors.size() == 1,
  354 | 				() -> "Multiple @HttpExchange annotations found on %s, but only one is allowed: %s"
  355 | 						.formatted(element, exchangeDescriptors));
  356 | 	}
  357 | 
  358 | 	/**
  359 | 	 * Create a {@link RequestMappingInfo} from the supplied
  360 | 	 * {@link RequestMapping @RequestMapping} annotation, meta-annotation,
  361 | 	 * or synthesized result of merging annotation attributes within an
  362 | 	 * annotation hierarchy.
  363 | 	 */
  364 | 	protected RequestMappingInfo createRequestMappingInfo(
  365 | 			RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {
  366 | 
  367 | 		RequestMappingInfo.Builder builder = RequestMappingInfo
  368 | 				.paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
  369 | 				.methods(requestMapping.method())
  370 | 				.params(requestMapping.params())
  371 | 				.headers(requestMapping.headers())
  372 | 				.consumes(requestMapping.consumes())
  373 | 				.produces(requestMapping.produces())
  374 | 				.version(requestMapping.version())
  375 | 				.mappingName(requestMapping.name());
  376 | 
  377 | 		if (customCondition != null) {
  378 | 			builder.customCondition(customCondition);
  379 | 		}
  380 | 
  381 | 		return builder.options(this.config).build();
  382 | 	}
  383 | 
  384 | 	/**
  385 | 	 * Create a {@link RequestMappingInfo} from the supplied
  386 | 	 * {@link HttpExchange @HttpExchange} annotation, meta-annotation,
  387 | 	 * or synthesized result of merging annotation attributes within an
  388 | 	 * annotation hierarchy.
  389 | 	 * @since 6.1
  390 | 	 */
  391 | 	protected RequestMappingInfo createRequestMappingInfo(
  392 | 			HttpExchange httpExchange, @Nullable RequestCondition<?> customCondition) {
  393 | 
  394 | 		RequestMappingInfo.Builder builder = RequestMappingInfo
  395 | 				.paths(resolveEmbeddedValuesInPatterns(toStringArray(httpExchange.value())))
  396 | 				.methods(toMethodArray(httpExchange.method()))
  397 | 				.consumes(toStringArray(httpExchange.contentType()))
  398 | 				.produces(httpExchange.accept())
  399 | 				.headers(httpExchange.headers())
  400 | 				.version(httpExchange.version());
  401 | 
  402 | 		if (customCondition != null) {
  403 | 			builder.customCondition(customCondition);
  404 | 		}
  405 | 
  406 | 		return builder.options(this.config).build();
  407 | 	}
  408 | 
  409 | 	/**
  410 | 	 * Resolve placeholder values in the given array of patterns.
  411 | 	 * @return a new array with updated patterns
  412 | 	 */
  413 | 	protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
  414 | 		if (this.embeddedValueResolver == null) {
  415 | 			return patterns;
  416 | 		}
  417 | 		else {
  418 | 			String[] resolvedPatterns = new String[patterns.length];
  419 | 			for (int i = 0; i < patterns.length; i++) {
  420 | 				resolvedPatterns[i] = Objects.requireNonNull(this.embeddedValueResolver.resolveStringValue(patterns[i]));
  421 | 			}
  422 | 			return resolvedPatterns;
  423 | 		}
  424 | 	}
  425 | 
  426 | 	private static String[] toStringArray(String value) {
  427 | 		return (StringUtils.hasText(value) ? new String[] {value} : EMPTY_STRING_ARRAY);
  428 | 	}
  429 | 
  430 | 	private static RequestMethod[] toMethodArray(String method) {
  431 | 		return (StringUtils.hasText(method) ?
  432 | 				new RequestMethod[] {RequestMethod.valueOf(method)} : EMPTY_REQUEST_METHOD_ARRAY);
  433 | 	}
  434 | 
  435 | 	@Override
  436 | 	public void registerMapping(RequestMappingInfo mapping, Object handler, Method method) {
  437 | 		super.registerMapping(mapping, handler, method);
  438 | 		updateConsumesCondition(mapping, method);
  439 | 	}
  440 | 
  441 | 	/**
  442 | 	 * {@inheritDoc}
  443 | 	 * <p><strong>Note:</strong> To create the {@link RequestMappingInfo},
  444 | 	 * please use {@link #getBuilderConfiguration()} and set the options on
  445 | 	 * {@link RequestMappingInfo.Builder#options(RequestMappingInfo.BuilderConfiguration)}
  446 | 	 * to match how this {@code HandlerMapping} is configured. This
  447 | 	 * is important for example to ensure use of
  448 | 	 * {@link org.springframework.web.util.pattern.PathPattern} or
  449 | 	 * {@link org.springframework.util.PathMatcher} based matching.
  450 | 	 * @param handler the bean name of the handler or the handler instance
  451 | 	 * @param method the method to register
  452 | 	 * @param mapping the mapping conditions associated with the handler method
  453 | 	 */
  454 | 	@Override
  455 | 	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
  456 | 		super.registerHandlerMethod(handler, method, mapping);
  457 | 		updateConsumesCondition(mapping, method);
  458 | 	}
  459 | 
  460 | 	private void updateConsumesCondition(RequestMappingInfo info, Method method) {
  461 | 		ConsumesRequestCondition condition = info.getConsumesCondition();
  462 | 		if (!condition.isEmpty()) {
  463 | 			AnnotatedMethod annotatedMethod = new AnnotatedMethod(method);
  464 | 			for (MethodParameter parameter : annotatedMethod.getMethodParameters()) {
  465 | 				RequestBody requestBody = parameter.getParameterAnnotation(RequestBody.class);
  466 | 				if (requestBody != null) {
  467 | 					condition.setBodyRequired(requestBody.required());
  468 | 					break;
  469 | 				}
  470 | 			}
  471 | 		}
  472 | 	}
  473 | 
  474 | 	@Override
  475 | 	protected @Nullable CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
  476 | 		HandlerMethod handlerMethod = createHandlerMethod(handler, method);
  477 | 		Class<?> beanType = handlerMethod.getBeanType();
  478 | 		CrossOrigin typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(beanType, CrossOrigin.class);
  479 | 		CrossOrigin methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, CrossOrigin.class);
  480 | 
  481 | 		if (typeAnnotation == null && methodAnnotation == null) {
  482 | 			return null;
  483 | 		}
  484 | 
  485 | 		CorsConfiguration config = new CorsConfiguration();
  486 | 		updateCorsConfig(config, typeAnnotation);
  487 | 		updateCorsConfig(config, methodAnnotation);
  488 | 
  489 | 		if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
  490 | 			for (RequestMethod allowedMethod : mappingInfo.getMethodsCondition().getMethods()) {
  491 | 				config.addAllowedMethod(allowedMethod.name());
  492 | 			}
  493 | 		}
  494 | 		return config.applyPermitDefaultValues();
  495 | 	}
  496 | 
  497 | 	private void updateCorsConfig(CorsConfiguration config, @Nullable CrossOrigin annotation) {
  498 | 		if (annotation == null) {
  499 | 			return;
  500 | 		}
  501 | 		for (String origin : annotation.origins()) {
  502 | 			config.addAllowedOrigin(resolveCorsAnnotationValue(origin));
  503 | 		}
  504 | 		for (String patterns : annotation.originPatterns()) {
  505 | 			config.addAllowedOriginPattern(resolveCorsAnnotationValue(patterns));
  506 | 		}
  507 | 		for (RequestMethod method : annotation.methods()) {
  508 | 			config.addAllowedMethod(method.name());
  509 | 		}
  510 | 		for (String header : annotation.allowedHeaders()) {
  511 | 			config.addAllowedHeader(resolveCorsAnnotationValue(header));
  512 | 		}
  513 | 		for (String header : annotation.exposedHeaders()) {
  514 | 			config.addExposedHeader(resolveCorsAnnotationValue(header));
  515 | 		}
  516 | 
  517 | 		String allowCredentials = resolveCorsAnnotationValue(annotation.allowCredentials());
  518 | 		if ("true".equalsIgnoreCase(allowCredentials)) {
  519 | 			config.setAllowCredentials(true);
  520 | 		}
  521 | 		else if ("false".equalsIgnoreCase(allowCredentials)) {
  522 | 			config.setAllowCredentials(false);
  523 | 		}
  524 | 		else if (!allowCredentials.isEmpty()) {
  525 | 			throw new IllegalStateException("@CrossOrigin's allowCredentials value must be \"true\", \"false\", " +
  526 | 					"or an empty string (\"\"): current value is [" + allowCredentials + "]");
  527 | 		}
  528 | 
  529 | 		String allowPrivateNetwork = resolveCorsAnnotationValue(annotation.allowPrivateNetwork());
  530 | 		if ("true".equalsIgnoreCase(allowPrivateNetwork)) {
  531 | 			config.setAllowPrivateNetwork(true);
  532 | 		}
  533 | 		else if ("false".equalsIgnoreCase(allowPrivateNetwork)) {
  534 | 			config.setAllowPrivateNetwork(false);
  535 | 		}
  536 | 		else if (!allowPrivateNetwork.isEmpty()) {
  537 | 			throw new IllegalStateException("@CrossOrigin's allowPrivateNetwork value must be \"true\", \"false\", " +
  538 | 					"or an empty string (\"\"): current value is [" + allowPrivateNetwork + "]");
  539 | 		}
  540 | 
  541 | 		if (annotation.maxAge() >= 0 ) {
  542 | 			config.setMaxAge(annotation.maxAge());
  543 | 		}
  544 | 	}
  545 | 
  546 | 	private String resolveCorsAnnotationValue(String value) {
  547 | 		if (this.embeddedValueResolver != null) {
  548 | 			String resolved = this.embeddedValueResolver.resolveStringValue(value);
  549 | 			return (resolved != null ? resolved : "");
  550 | 		}
  551 | 		else {
  552 | 			return value;
  553 | 		}
  554 | 	}
  555 | 
  556 | 
  557 | 	private static class AnnotationDescriptor {
  558 | 
  559 | 		private final Annotation annotation;
  560 | 		private final MergedAnnotation<?> root;
  561 | 
  562 | 		AnnotationDescriptor(MergedAnnotation<Annotation> mergedAnnotation) {
  563 | 			this.annotation = mergedAnnotation.synthesize();
  564 | 			this.root = mergedAnnotation.getRoot();
  565 | 		}
  566 | 
  567 | 		@Override
  568 | 		public boolean equals(@Nullable Object obj) {
  569 | 			return (obj instanceof AnnotationDescriptor that && this.annotation.equals(that.annotation));
  570 | 		}
  571 | 
  572 | 		@Override
  573 | 		public int hashCode() {
  574 | 			return this.annotation.hashCode();
  575 | 		}
  576 | 
  577 | 		@Override
  578 | 		public String toString() {
  579 | 			return this.root.synthesize().toString();
  580 | 		}
  581 | 
  582 | 	}
  583 | 
  584 | }
```
