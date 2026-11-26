<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/support/SpringMvcContract.java -->
<!-- 正文引用：[[源码:SpringMvcContract.java#L起-L止]] ；代码块头部：(src: SpringMvcContract.java#L起-L止) -->

# SpringMvcContract.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/support/SpringMvcContract.java` |
| 行数 | 583 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:25.775Z |

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
   17 | package org.springframework.cloud.openfeign.support;
   18 | 
   19 | import java.lang.annotation.Annotation;
   20 | import java.lang.reflect.Method;
   21 | import java.lang.reflect.Parameter;
   22 | import java.lang.reflect.Type;
   23 | import java.net.URI;
   24 | import java.util.ArrayList;
   25 | import java.util.Arrays;
   26 | import java.util.Collection;
   27 | import java.util.Collections;
   28 | import java.util.HashMap;
   29 | import java.util.LinkedHashMap;
   30 | import java.util.List;
   31 | import java.util.Map;
   32 | import java.util.Objects;
   33 | 
   34 | import feign.Contract;
   35 | import feign.Feign;
   36 | import feign.MethodMetadata;
   37 | import feign.Param;
   38 | import feign.QueryMap;
   39 | import feign.Request;
   40 | import org.apache.commons.logging.Log;
   41 | import org.apache.commons.logging.LogFactory;
   42 | 
   43 | import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
   44 | import org.springframework.cloud.openfeign.CollectionFormat;
   45 | import org.springframework.cloud.openfeign.FeignClientProperties;
   46 | import org.springframework.cloud.openfeign.SpringQueryMap;
   47 | import org.springframework.cloud.openfeign.annotation.CookieValueParameterProcessor;
   48 | import org.springframework.cloud.openfeign.annotation.MatrixVariableParameterProcessor;
   49 | import org.springframework.cloud.openfeign.annotation.PathVariableParameterProcessor;
   50 | import org.springframework.cloud.openfeign.annotation.QueryMapParameterProcessor;
   51 | import org.springframework.cloud.openfeign.annotation.RequestHeaderParameterProcessor;
   52 | import org.springframework.cloud.openfeign.annotation.RequestParamParameterProcessor;
   53 | import org.springframework.cloud.openfeign.annotation.RequestPartParameterProcessor;
   54 | import org.springframework.cloud.openfeign.encoding.HttpEncoding;
   55 | import org.springframework.context.ConfigurableApplicationContext;
   56 | import org.springframework.context.ResourceLoaderAware;
   57 | import org.springframework.core.DefaultParameterNameDiscoverer;
   58 | import org.springframework.core.MethodParameter;
   59 | import org.springframework.core.ParameterNameDiscoverer;
   60 | import org.springframework.core.ResolvableType;
   61 | import org.springframework.core.annotation.AnnotationUtils;
   62 | import org.springframework.core.convert.ConversionService;
   63 | import org.springframework.core.convert.TypeDescriptor;
   64 | import org.springframework.core.convert.support.DefaultConversionService;
   65 | import org.springframework.core.io.DefaultResourceLoader;
   66 | import org.springframework.core.io.ResourceLoader;
   67 | import org.springframework.data.domain.Pageable;
   68 | import org.springframework.http.InvalidMediaTypeException;
   69 | import org.springframework.http.MediaType;
   70 | import org.springframework.util.Assert;
   71 | import org.springframework.util.StringUtils;
   72 | import org.springframework.web.bind.annotation.RequestMapping;
   73 | import org.springframework.web.bind.annotation.RequestMethod;
   74 | import org.springframework.web.bind.annotation.RequestParam;
   75 | 
   76 | import static feign.Util.checkState;
   77 | import static feign.Util.emptyToNull;
   78 | import static org.springframework.cloud.openfeign.support.FeignUtils.addTemplateParameter;
   79 | import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
   80 | 
   81 | /**
   82 |  * @author Spencer Gibb
   83 |  * @author Abhijit Sarkar
   84 |  * @author Halvdan Hoem Grelland
   85 |  * @author Aram Peres
   86 |  * @author Olga Maciaszek-Sharma
   87 |  * @author Aaron Whiteside
   88 |  * @author Artyom Romanenko
   89 |  * @author Darren Foong
   90 |  * @author Ram Anaswara
   91 |  * @author Sam Kruglov
   92 |  * @author Tang Xiong
   93 |  * @author Juhyeong An
   94 |  * @author Olof Segergren
   95 |  * @author Hyundoo Park
   96 |  * @author Rene Choi
   97 |  */
   98 | public class SpringMvcContract extends Contract.BaseContract implements ResourceLoaderAware {
   99 | 
  100 | 	private static final Log LOG = LogFactory.getLog(SpringMvcContract.class);
  101 | 
  102 | 	private static final String ACCEPT = "Accept";
  103 | 
  104 | 	private static final String CONTENT_TYPE = "Content-Type";
  105 | 
  106 | 	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);
  107 | 
  108 | 	private static final TypeDescriptor ITERABLE_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Iterable.class);
  109 | 
  110 | 	private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
  111 | 
  112 | 	private final Map<Class<? extends Annotation>, AnnotatedParameterProcessor> annotatedArgumentProcessors;
  113 | 
  114 | 	private final Map<String, Method> processedMethods = new HashMap<>();
  115 | 
  116 | 	private final ConversionService conversionService;
  117 | 
  118 | 	private final ConvertingExpanderFactory convertingExpanderFactory;
  119 | 
  120 | 	private ResourceLoader resourceLoader = new DefaultResourceLoader();
  121 | 
  122 | 	private final boolean decodeSlash;
  123 | 
  124 | 	private final boolean removeTrailingSlash;
  125 | 
  126 | 	public SpringMvcContract() {
  127 | 		this(Collections.emptyList());
  128 | 	}
  129 | 
  130 | 	public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors) {
  131 | 		this(annotatedParameterProcessors, new DefaultConversionService());
  132 | 	}
  133 | 
  134 | 	public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors,
  135 | 			ConversionService conversionService) {
  136 | 		this(annotatedParameterProcessors, conversionService, null);
  137 | 	}
  138 | 
  139 | 	public SpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors,
  140 | 			ConversionService conversionService, FeignClientProperties feignClientProperties) {
  141 | 		Assert.notNull(annotatedParameterProcessors, "Parameter processors can not be null.");
  142 | 		Assert.notNull(conversionService, "ConversionService can not be null.");
  143 | 
  144 | 		List<AnnotatedParameterProcessor> processors = getDefaultAnnotatedArgumentsProcessors();
  145 | 		processors.addAll(annotatedParameterProcessors);
  146 | 
  147 | 		annotatedArgumentProcessors = toAnnotatedArgumentProcessorMap(processors);
  148 | 		this.conversionService = conversionService;
  149 | 		convertingExpanderFactory = new ConvertingExpanderFactory(conversionService);
  150 | 		this.decodeSlash = feignClientProperties == null || feignClientProperties.isDecodeSlash();
  151 | 		this.removeTrailingSlash = feignClientProperties != null && feignClientProperties.isRemoveTrailingSlash();
  152 | 	}
  153 | 
  154 | 	private static TypeDescriptor createTypeDescriptor(Method method, int paramIndex) {
  155 | 		Parameter parameter = method.getParameters()[paramIndex];
  156 | 		MethodParameter methodParameter = MethodParameter.forParameter(parameter);
  157 | 		TypeDescriptor typeDescriptor = new TypeDescriptor(methodParameter);
  158 | 
  159 | 		// Feign applies the Param.Expander to each element of an Iterable, so in those
  160 | 		// cases we need to provide a TypeDescriptor of the element.
  161 | 		if (typeDescriptor.isAssignableTo(ITERABLE_TYPE_DESCRIPTOR)) {
  162 | 			TypeDescriptor elementTypeDescriptor = getElementTypeDescriptor(typeDescriptor);
  163 | 
  164 | 			checkState(elementTypeDescriptor != null,
  165 | 					"Could not resolve element type of Iterable type %s. Not declared?", typeDescriptor);
  166 | 
  167 | 			typeDescriptor = elementTypeDescriptor;
  168 | 		}
  169 | 		return typeDescriptor;
  170 | 	}
  171 | 
  172 | 	private static TypeDescriptor getElementTypeDescriptor(TypeDescriptor typeDescriptor) {
  173 | 		TypeDescriptor elementTypeDescriptor = typeDescriptor.getElementTypeDescriptor();
  174 | 		// that means it's not a collection, but it is iterable, gh-135
  175 | 		if (elementTypeDescriptor == null && Iterable.class.isAssignableFrom(typeDescriptor.getType())) {
  176 | 			ResolvableType type = typeDescriptor.getResolvableType().as(Iterable.class).getGeneric(0);
  177 | 			if (type.resolve() == null) {
  178 | 				return null;
  179 | 			}
  180 | 			return new TypeDescriptor(type, null, typeDescriptor.getAnnotations());
  181 | 		}
  182 | 		return elementTypeDescriptor;
  183 | 	}
  184 | 
  185 | 	@Override
  186 | 	public void setResourceLoader(ResourceLoader resourceLoader) {
  187 | 		this.resourceLoader = resourceLoader;
  188 | 	}
  189 | 
  190 | 	@Override
  191 | 	protected void processAnnotationOnClass(MethodMetadata data, Class<?> clz) {
  192 | 		RequestMapping classAnnotation = findMergedAnnotation(clz, RequestMapping.class);
  193 | 		if (classAnnotation != null) {
  194 | 			LOG.error("Cannot process class: " + clz.getName()
  195 | 					+ ". @RequestMapping annotation is not allowed on @FeignClient interfaces.");
  196 | 			throw new IllegalArgumentException("@RequestMapping annotation not allowed on @FeignClient interfaces");
  197 | 		}
  198 | 		CollectionFormat collectionFormat = findMergedAnnotation(clz, CollectionFormat.class);
  199 | 		if (collectionFormat != null) {
  200 | 			data.template().collectionFormat(collectionFormat.value());
  201 | 		}
  202 | 	}
  203 | 
  204 | 	@Override
  205 | 	public MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
  206 | 		processedMethods.put(Feign.configKey(targetType, method), method);
  207 | 		MethodMetadata metadata = super.parseAndValidateMetadata(targetType, method);
  208 | 
  209 | 		if (isGetMethod(metadata) && method.getParameterCount() > 0
  210 | 				&& shouldWarnAboutUnannotatedGetParameters(method)) {
  211 | 			if (LOG.isWarnEnabled()) {
  212 | 				LOG.warn(String
  213 | 					.format("[OpenFeign Warning] Feign method '%s' is declared as GET with parameters, but none of the parameters are annotated "
  214 | 							+ "(e.g. @RequestParam, @RequestHeader, @PathVariable, etc). This may result in fallback to POST at runtime. "
  215 | 							+ "Consider explicitly annotating parameters.", method.toGenericString()));
  216 | 			}
  217 | 		}
  218 | 
  219 | 		return metadata;
  220 | 	}
  221 | 
  222 | 	private boolean isGetMethod(MethodMetadata metadata) {
  223 | 		return "GET".equalsIgnoreCase(metadata.template().method());
  224 | 	}
  225 | 
  226 | 	private boolean shouldWarnAboutUnannotatedGetParameters(Method method) {
  227 | 		Parameter[] params = method.getParameters();
  228 | 		if (params.length == 1 && params[0].getType() == URI.class) {
  229 | 			return false;
  230 | 		}
  231 | 		int fromIndex = (params.length > 0 && params[0].getType() == URI.class) ? 1 : 0;
  232 | 		return !hasBoundParameters(method, fromIndex);
  233 | 	}
  234 | 
  235 | 	private boolean hasBoundParameters(Method method, int fromIndex) {
  236 | 		Parameter[] parameters = method.getParameters();
  237 | 		for (int i = fromIndex; i < parameters.length; i++) {
  238 | 			Parameter parameter = parameters[i];
  239 | 			if (isPageable(parameter.getType())) {
  240 | 				return true;
  241 | 			}
  242 | 			for (Annotation annotation : parameter.getAnnotations()) {
  243 | 				Class<? extends Annotation> annotationType = annotation.annotationType();
  244 | 				if (annotationType == QueryMap.class || annotatedArgumentProcessors.containsKey(annotationType)) {
  245 | 					return true;
  246 | 				}
  247 | 			}
  248 | 		}
  249 | 		return false;
  250 | 	}
  251 | 
  252 | 	private boolean isPageable(Class<?> parameterType) {
  253 | 		try {
  254 | 			return Pageable.class.isAssignableFrom(parameterType);
  255 | 		}
  256 | 		catch (NoClassDefFoundError ignored) {
  257 | 			// Do nothing; added to avoid exceptions if optional dependency not present
  258 | 			return false;
  259 | 		}
  260 | 	}
  261 | 
  262 | 	@Override
  263 | 	protected void processAnnotationOnMethod(MethodMetadata data, Annotation methodAnnotation, Method method) {
  264 | 		if (methodAnnotation instanceof CollectionFormat) {
  265 | 			CollectionFormat collectionFormat = findMergedAnnotation(method, CollectionFormat.class);
  266 | 			data.template().collectionFormat(collectionFormat.value());
  267 | 		}
  268 | 
  269 | 		if (!(methodAnnotation instanceof RequestMapping)
  270 | 				&& !methodAnnotation.annotationType().isAnnotationPresent(RequestMapping.class)) {
  271 | 			return;
  272 | 		}
  273 | 
  274 | 		RequestMapping methodMapping = findMergedAnnotation(method, RequestMapping.class);
  275 | 		// HTTP Method
  276 | 		RequestMethod[] methods = methodMapping.method();
  277 | 		if (methods.length == 0) {
  278 | 			methods = new RequestMethod[] { RequestMethod.GET };
  279 | 		}
  280 | 		checkOne(method, methods, "method");
  281 | 		data.template().method(Request.HttpMethod.valueOf(methods[0].name()));
  282 | 
  283 | 		// path
  284 | 		checkAtMostOne(method, methodMapping.value(), "value");
  285 | 		if (methodMapping.value().length > 0) {
  286 | 			String pathValue = emptyToNull(methodMapping.value()[0]);
  287 | 			if (pathValue != null) {
  288 | 				pathValue = resolve(pathValue);
  289 | 				// Append path from @RequestMapping if value is present on method
  290 | 				if (!pathValue.startsWith("/") && !data.template().path().endsWith("/")) {
  291 | 					pathValue = "/" + pathValue;
  292 | 				}
  293 | 				if (removeTrailingSlash && pathValue.endsWith("/")) {
  294 | 					pathValue = pathValue.substring(0, pathValue.length() - 1);
  295 | 				}
  296 | 				data.template().uri(pathValue, true);
  297 | 				if (data.template().decodeSlash() != decodeSlash) {
  298 | 					data.template().decodeSlash(decodeSlash);
  299 | 				}
  300 | 			}
  301 | 		}
  302 | 
  303 | 		// produces
  304 | 		parseProduces(data, methodMapping);
  305 | 
  306 | 		// consumes
  307 | 		parseConsumes(data, methodMapping);
  308 | 
  309 | 		// headers
  310 | 		parseHeaders(data, methodMapping);
  311 | 
  312 | 		// params
  313 | 		parseParams(data, methodMapping);
  314 | 
  315 | 		data.indexToExpander(new LinkedHashMap<>());
  316 | 	}
  317 | 
  318 | 	private String resolve(String value) {
  319 | 		if (StringUtils.hasText(value) && resourceLoader instanceof ConfigurableApplicationContext) {
  320 | 			return ((ConfigurableApplicationContext) resourceLoader).getEnvironment().resolvePlaceholders(value);
  321 | 		}
  322 | 		return value;
  323 | 	}
  324 | 
  325 | 	private void checkAtMostOne(Method method, Object[] values, String fieldName) {
  326 | 		checkState(values != null && (values.length == 0 || values.length == 1),
  327 | 				"Method %s can only contain at most 1 %s field. Found: %s", method.getName(), fieldName,
  328 | 				values == null ? null : Arrays.asList(values));
  329 | 	}
  330 | 
  331 | 	private void checkOne(Method method, Object[] values, String fieldName) {
  332 | 		checkState(values != null && values.length == 1, "Method %s can only contain 1 %s field. Found: %s",
  333 | 				method.getName(), fieldName, values == null ? null : Arrays.asList(values));
  334 | 	}
  335 | 
  336 | 	@Override
  337 | 	protected boolean processAnnotationsOnParameter(MethodMetadata data, Annotation[] annotations, int paramIndex) {
  338 | 		boolean isHttpAnnotation = false;
  339 | 
  340 | 		if (isPageable(data.method().getParameterTypes()[paramIndex])) {
  341 | 			// do not set a Pageable as QueryMap if there's an actual QueryMap param
  342 | 			// present
  343 | 			if (!queryMapParamPresent(data)) {
  344 | 				data.queryMapIndex(paramIndex);
  345 | 				return false;
  346 | 			}
  347 | 		}
  348 | 
  349 | 		AnnotatedParameterProcessor.AnnotatedParameterContext context = new SimpleAnnotatedParameterContext(data,
  350 | 				paramIndex);
  351 | 		Method method = processedMethods.get(data.configKey());
  352 | 		for (Annotation parameterAnnotation : annotations) {
  353 | 			AnnotatedParameterProcessor processor = annotatedArgumentProcessors
  354 | 				.get(parameterAnnotation.annotationType());
  355 | 			if (processor != null) {
  356 | 				Annotation processParameterAnnotation;
  357 | 				// synthesize, handling @AliasFor, while falling back to parameter name on
  358 | 				// missing String #value():
  359 | 				processParameterAnnotation = synthesizeWithMethodParameterNameAsFallbackValue(parameterAnnotation,
  360 | 						method, paramIndex);
  361 | 				isHttpAnnotation |= processor.processArgument(context, processParameterAnnotation, method);
  362 | 			}
  363 | 		}
  364 | 
  365 | 		if (!isMultipartFormData(data) && isHttpAnnotation && data.indexToExpander().get(paramIndex) == null) {
  366 | 			TypeDescriptor typeDescriptor = createTypeDescriptor(method, paramIndex);
  367 | 			if (conversionService.canConvert(typeDescriptor, STRING_TYPE_DESCRIPTOR)) {
  368 | 				Param.Expander expander = convertingExpanderFactory.getExpander(typeDescriptor);
  369 | 				if (expander != null) {
  370 | 					data.indexToExpander().put(paramIndex, expander);
  371 | 				}
  372 | 			}
  373 | 		}
  374 | 		return isHttpAnnotation;
  375 | 	}
  376 | 
  377 | 	private boolean queryMapParamPresent(MethodMetadata data) {
  378 | 		Annotation[][] paramsAnnotations = data.method().getParameterAnnotations();
  379 | 		for (int i = 0; i < paramsAnnotations.length; i++) {
  380 | 			Annotation[] paramAnnotations = paramsAnnotations[i];
  381 | 			Class<?> parameterType = data.method().getParameterTypes()[i];
  382 | 			if (Arrays.stream(paramAnnotations)
  383 | 				.anyMatch(annotation -> Map.class.isAssignableFrom(parameterType) && annotation instanceof RequestParam
  384 | 						|| annotation instanceof SpringQueryMap || annotation instanceof QueryMap)) {
  385 | 				return true;
  386 | 			}
  387 | 		}
  388 | 		return false;
  389 | 	}
  390 | 
  391 | 	private void parseProduces(MethodMetadata md, RequestMapping annotation) {
  392 | 		String[] clientAccepts = Arrays.stream(annotation.produces())
  393 | 			.map(this::resolve)
  394 | 			.map(s -> emptyToNull(s))
  395 | 			.filter(Objects::nonNull)
  396 | 			.toArray(String[]::new);
  397 | 		if (clientAccepts.length > 0) {
  398 | 			md.template().header(ACCEPT, clientAccepts);
  399 | 		}
  400 | 	}
  401 | 
  402 | 	private void parseConsumes(MethodMetadata md, RequestMapping annotation) {
  403 | 		String[] serverConsumes = annotation.consumes();
  404 | 		String clientProduces = serverConsumes.length == 0 ? null : emptyToNull(resolve(serverConsumes[0]));
  405 | 		if (clientProduces != null) {
  406 | 			md.template().header(CONTENT_TYPE, clientProduces);
  407 | 		}
  408 | 	}
  409 | 
  410 | 	private void parseHeaders(MethodMetadata md, RequestMapping annotation) {
  411 | 		// TODO: only supports one header value per key
  412 | 		if (annotation.headers() != null) {
  413 | 			for (String header : annotation.headers()) {
  414 | 				header = resolve(header);
  415 | 				int index = header.indexOf('=');
  416 | 				if (!header.contains("!=") && index >= 0) {
  417 | 					md.template().header(header.substring(0, index), header.substring(index + 1).trim());
  418 | 				}
  419 | 			}
  420 | 		}
  421 | 	}
  422 | 
  423 | 	private void parseParams(MethodMetadata data, RequestMapping methodMapping) {
  424 | 		String[] params = methodMapping.params();
  425 | 		if (params == null) {
  426 | 			return;
  427 | 		}
  428 | 		for (String param : params) {
  429 | 			NameValueResolver nameValueResolver = new NameValueResolver(param);
  430 | 			if (!nameValueResolver.isNegated()) {
  431 | 				data.template().query(resolve(nameValueResolver.getName()), resolve(nameValueResolver.getValue()));
  432 | 			}
  433 | 			else {
  434 | 				if (LOG.isDebugEnabled()) {
  435 | 					LOG.debug("Negated params are not supported by Feign and ignored during parameter processing: "
  436 | 							+ param);
  437 | 				}
  438 | 			}
  439 | 		}
  440 | 	}
  441 | 
  442 | 	private Map<Class<? extends Annotation>, AnnotatedParameterProcessor> toAnnotatedArgumentProcessorMap(
  443 | 			List<AnnotatedParameterProcessor> processors) {
  444 | 		Map<Class<? extends Annotation>, AnnotatedParameterProcessor> result = new HashMap<>();
  445 | 		for (AnnotatedParameterProcessor processor : processors) {
  446 | 			result.put(processor.getAnnotationType(), processor);
  447 | 		}
  448 | 		return result;
  449 | 	}
  450 | 
  451 | 	private List<AnnotatedParameterProcessor> getDefaultAnnotatedArgumentsProcessors() {
  452 | 
  453 | 		List<AnnotatedParameterProcessor> annotatedArgumentResolvers = new ArrayList<>();
  454 | 
  455 | 		annotatedArgumentResolvers.add(new MatrixVariableParameterProcessor());
  456 | 		annotatedArgumentResolvers.add(new PathVariableParameterProcessor());
  457 | 		annotatedArgumentResolvers.add(new RequestParamParameterProcessor());
  458 | 		annotatedArgumentResolvers.add(new RequestHeaderParameterProcessor());
  459 | 		annotatedArgumentResolvers.add(new QueryMapParameterProcessor());
  460 | 		annotatedArgumentResolvers.add(new RequestPartParameterProcessor());
  461 | 		annotatedArgumentResolvers.add(new CookieValueParameterProcessor());
  462 | 
  463 | 		return annotatedArgumentResolvers;
  464 | 	}
  465 | 
  466 | 	private Annotation synthesizeWithMethodParameterNameAsFallbackValue(Annotation parameterAnnotation, Method method,
  467 | 			int parameterIndex) {
  468 | 		Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(parameterAnnotation);
  469 | 		Object defaultValue = AnnotationUtils.getDefaultValue(parameterAnnotation);
  470 | 		if (defaultValue instanceof String && defaultValue.equals(annotationAttributes.get(AnnotationUtils.VALUE))) {
  471 | 			Type[] parameterTypes = method.getGenericParameterTypes();
  472 | 			String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
  473 | 			if (shouldAddParameterName(parameterIndex, parameterTypes, parameterNames)) {
  474 | 				annotationAttributes.put(AnnotationUtils.VALUE, parameterNames[parameterIndex]);
  475 | 			}
  476 | 		}
  477 | 		return AnnotationUtils.synthesizeAnnotation(annotationAttributes, parameterAnnotation.annotationType(), null);
  478 | 	}
  479 | 
  480 | 	private boolean shouldAddParameterName(int parameterIndex, Type[] parameterTypes, String[] parameterNames) {
  481 | 		// has a parameter name
  482 | 		return parameterNames != null && parameterNames.length > parameterIndex
  483 | 		// has a type
  484 | 				&& parameterTypes != null && parameterTypes.length > parameterIndex;
  485 | 	}
  486 | 
  487 | 	private boolean isMultipartFormData(MethodMetadata data) {
  488 | 		Collection<String> contentTypes = data.template().headers().get(HttpEncoding.CONTENT_TYPE);
  489 | 
  490 | 		if (contentTypes != null && !contentTypes.isEmpty()) {
  491 | 			String type = contentTypes.iterator().next();
  492 | 			try {
  493 | 				return Objects.equals(MediaType.valueOf(type), MediaType.MULTIPART_FORM_DATA);
  494 | 			}
  495 | 			catch (InvalidMediaTypeException ignored) {
  496 | 				return false;
  497 | 			}
  498 | 		}
  499 | 
  500 | 		return false;
  501 | 	}
  502 | 
  503 | 	private record ConvertingExpanderFactory(ConversionService conversionService) {
  504 | 
  505 | 		Param.Expander getExpander(TypeDescriptor typeDescriptor) {
  506 | 			return value -> {
  507 | 				Object converted = conversionService.convert(value, typeDescriptor, STRING_TYPE_DESCRIPTOR);
  508 | 				return (String) converted;
  509 | 			};
  510 | 		}
  511 | 
  512 | 	}
  513 | 
  514 | 	private class SimpleAnnotatedParameterContext implements AnnotatedParameterProcessor.AnnotatedParameterContext {
  515 | 
  516 | 		private final MethodMetadata methodMetadata;
  517 | 
  518 | 		private final int parameterIndex;
  519 | 
  520 | 		SimpleAnnotatedParameterContext(MethodMetadata methodMetadata, int parameterIndex) {
  521 | 			this.methodMetadata = methodMetadata;
  522 | 			this.parameterIndex = parameterIndex;
  523 | 		}
  524 | 
  525 | 		@Override
  526 | 		public MethodMetadata getMethodMetadata() {
  527 | 			return methodMetadata;
  528 | 		}
  529 | 
  530 | 		@Override
  531 | 		public int getParameterIndex() {
  532 | 			return parameterIndex;
  533 | 		}
  534 | 
  535 | 		@Override
  536 | 		public void setParameterName(String name) {
  537 | 			nameParam(methodMetadata, name, parameterIndex);
  538 | 		}
  539 | 
  540 | 		@Override
  541 | 		public Collection<String> setTemplateParameter(String name, Collection<String> rest) {
  542 | 			return addTemplateParameter(rest, name);
  543 | 		}
  544 | 
  545 | 	}
  546 | 
  547 | 	private static class NameValueResolver {
  548 | 
  549 | 		private final String name;
  550 | 
  551 | 		private final String value;
  552 | 
  553 | 		private final boolean isNegated;
  554 | 
  555 | 		NameValueResolver(String expression) {
  556 | 			int separator = expression.indexOf('=');
  557 | 			if (separator == -1) {
  558 | 				isNegated = expression.startsWith("!");
  559 | 				name = (isNegated ? expression.substring(1) : expression);
  560 | 				value = null;
  561 | 			}
  562 | 			else {
  563 | 				isNegated = (separator > 0) && (expression.charAt(separator - 1) == '!');
  564 | 				name = (isNegated ? expression.substring(0, separator - 1) : expression.substring(0, separator));
  565 | 				value = expression.substring(separator + 1);
  566 | 			}
  567 | 		}
  568 | 
  569 | 		public String getName() {
  570 | 			return name;
  571 | 		}
  572 | 
  573 | 		public String getValue() {
  574 | 			return value;
  575 | 		}
  576 | 
  577 | 		public boolean isNegated() {
  578 | 			return isNegated;
  579 | 		}
  580 | 
  581 | 	}
  582 | 
  583 | }
```
