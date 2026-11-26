<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/RequestMappingInfoHandlerMapping.java -->
<!-- 正文引用：[[源码:RequestMappingInfoHandlerMapping.java#L起-L止]] ；代码块头部：(src: RequestMappingInfoHandlerMapping.java#L起-L止) -->

# RequestMappingInfoHandlerMapping.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/RequestMappingInfoHandlerMapping.java` |
| 行数 | 581 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:11.105Z |

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
   17 | package org.springframework.web.servlet.mvc.method;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.util.ArrayList;
   21 | import java.util.Collections;
   22 | import java.util.Comparator;
   23 | import java.util.LinkedHashMap;
   24 | import java.util.LinkedHashSet;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.Set;
   28 | 
   29 | import jakarta.servlet.ServletException;
   30 | import jakarta.servlet.http.HttpServletRequest;
   31 | import org.jspecify.annotations.Nullable;
   32 | 
   33 | import org.springframework.http.HttpHeaders;
   34 | import org.springframework.http.HttpMethod;
   35 | import org.springframework.http.InvalidMediaTypeException;
   36 | import org.springframework.http.MediaType;
   37 | import org.springframework.http.server.PathContainer;
   38 | import org.springframework.util.Assert;
   39 | import org.springframework.util.CollectionUtils;
   40 | import org.springframework.util.MultiValueMap;
   41 | import org.springframework.util.StringUtils;
   42 | import org.springframework.web.HttpMediaTypeNotAcceptableException;
   43 | import org.springframework.web.HttpMediaTypeNotSupportedException;
   44 | import org.springframework.web.HttpRequestMethodNotSupportedException;
   45 | import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
   46 | import org.springframework.web.bind.annotation.RequestMethod;
   47 | import org.springframework.web.filter.ServerHttpObservationFilter;
   48 | import org.springframework.web.method.HandlerMethod;
   49 | import org.springframework.web.servlet.HandlerMapping;
   50 | import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
   51 | import org.springframework.web.servlet.mvc.condition.NameValueExpression;
   52 | import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
   53 | import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
   54 | import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
   55 | import org.springframework.web.servlet.mvc.condition.RequestCondition;
   56 | import org.springframework.web.util.ServletRequestPathUtils;
   57 | import org.springframework.web.util.WebUtils;
   58 | import org.springframework.web.util.pattern.PathPattern;
   59 | 
   60 | /**
   61 |  * Abstract base class for classes for which {@link RequestMappingInfo} defines
   62 |  * the mapping between a request and a handler method.
   63 |  *
   64 |  * @author Arjen Poutsma
   65 |  * @author Rossen Stoyanchev
   66 |  * @since 3.1
   67 |  */
   68 | public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {
   69 | 
   70 | 	private static final Method HTTP_OPTIONS_HANDLE_METHOD;
   71 | 
   72 | 	private static final Method HTTP_HEAD_QUERY_HANDLE_METHOD;
   73 | 
   74 | 	static {
   75 | 		try {
   76 | 			HTTP_OPTIONS_HANDLE_METHOD = HttpOptionsHandler.class.getMethod("handle");
   77 | 			HTTP_HEAD_QUERY_HANDLE_METHOD = HttpHeadQueryHandler.class.getMethod("handle");
   78 | 		}
   79 | 		catch (NoSuchMethodException ex) {
   80 | 			// Should never happen
   81 | 			throw new IllegalStateException("Failed to retrieve internal handler method for HTTP OPTIONS", ex);
   82 | 		}
   83 | 	}
   84 | 
   85 | 
   86 | 	protected RequestMappingInfoHandlerMapping() {
   87 | 		setHandlerMethodMappingNamingStrategy(new RequestMappingInfoHandlerMethodMappingNamingStrategy());
   88 | 	}
   89 | 
   90 | 
   91 | 	@Override
   92 | 	protected Set<String> getDirectPaths(RequestMappingInfo info) {
   93 | 		return info.getDirectPaths();
   94 | 	}
   95 | 
   96 | 	/**
   97 | 	 * Check if the given RequestMappingInfo matches the current request and
   98 | 	 * return a (potentially new) instance with conditions that match the
   99 | 	 * current request -- for example with a subset of URL patterns.
  100 | 	 * @return an info in case of a match; or {@code null} otherwise.
  101 | 	 */
  102 | 	@Override
  103 | 	protected @Nullable RequestMappingInfo getMatchingMapping(RequestMappingInfo info, HttpServletRequest request) {
  104 | 		return info.getMatchingCondition(request);
  105 | 	}
  106 | 
  107 | 	/**
  108 | 	 * Provide a Comparator to sort RequestMappingInfos matched to a request.
  109 | 	 */
  110 | 	@Override
  111 | 	protected Comparator<RequestMappingInfo> getMappingComparator(final HttpServletRequest request) {
  112 | 		return (info1, info2) -> info1.compareTo(info2, request);
  113 | 	}
  114 | 
  115 | 	@Override
  116 | 	protected @Nullable HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
  117 | 		request.removeAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
  118 | 		try {
  119 | 			return super.getHandlerInternal(request);
  120 | 		}
  121 | 		finally {
  122 | 			ProducesRequestCondition.clearMediaTypesAttribute(request);
  123 | 		}
  124 | 	}
  125 | 
  126 | 	/**
  127 | 	 * Expose URI template variables, matrix variables, and producible media types in the request.
  128 | 	 * @see HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE
  129 | 	 * @see HandlerMapping#MATRIX_VARIABLES_ATTRIBUTE
  130 | 	 * @see HandlerMapping#PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE
  131 | 	 */
  132 | 	@SuppressWarnings("removal")
  133 | 	@Override
  134 | 	protected void handleMatch(RequestMappingInfo info, String lookupPath, HttpServletRequest request) {
  135 | 		super.handleMatch(info, lookupPath, request);
  136 | 
  137 | 		info.getVersionCondition().handleMatch(request);
  138 | 
  139 | 		RequestCondition<?> condition = info.getActivePatternsCondition();
  140 | 		if (condition instanceof PathPatternsRequestCondition pprc) {
  141 | 			extractMatchDetails(pprc, lookupPath, request);
  142 | 		}
  143 | 		else {
  144 | 			extractMatchDetails((PatternsRequestCondition) condition, lookupPath, request);
  145 | 		}
  146 | 
  147 | 		ProducesRequestCondition producesCondition = info.getProducesCondition();
  148 | 		if (!producesCondition.isEmpty()) {
  149 | 			Set<MediaType> mediaTypes = producesCondition.getProducibleMediaTypes();
  150 | 			if (!mediaTypes.isEmpty()) {
  151 | 				request.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
  152 | 			}
  153 | 		}
  154 | 	}
  155 | 
  156 | 	private void extractMatchDetails(
  157 | 			PathPatternsRequestCondition condition, String lookupPath, HttpServletRequest request) {
  158 | 
  159 | 		PathPattern bestPattern;
  160 | 		Map<String, String> uriVariables;
  161 | 		if (condition.isEmptyPathMapping()) {
  162 | 			bestPattern = condition.getFirstPattern();
  163 | 			uriVariables = Collections.emptyMap();
  164 | 		}
  165 | 		else {
  166 | 			PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
  167 | 			bestPattern = condition.getFirstPattern();
  168 | 			PathPattern.PathMatchInfo result = bestPattern.matchAndExtract(path);
  169 | 			Assert.notNull(result, () ->
  170 | 					"Expected bestPattern: " + bestPattern + " to match lookupPath " + path);
  171 | 			uriVariables = result.getUriVariables();
  172 | 			request.setAttribute(MATRIX_VARIABLES_ATTRIBUTE, result.getMatrixVariables());
  173 | 		}
  174 | 		request.setAttribute(BEST_MATCHING_PATH_PATTERN_ATTRIBUTE, bestPattern);
  175 | 		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern.getPatternString());
  176 | 		ServerHttpObservationFilter.findObservationContext(request)
  177 | 				.ifPresent(context -> context.setPathPattern(bestPattern.getPatternString()));
  178 | 		request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
  179 | 	}
  180 | 
  181 | 	@SuppressWarnings("removal")
  182 | 	private void extractMatchDetails(
  183 | 			PatternsRequestCondition condition, String lookupPath, HttpServletRequest request) {
  184 | 
  185 | 		String bestPattern;
  186 | 		Map<String, String> uriVariables;
  187 | 		if (condition.isEmptyPathMapping()) {
  188 | 			bestPattern = lookupPath;
  189 | 			uriVariables = Collections.emptyMap();
  190 | 		}
  191 | 		else {
  192 | 			bestPattern = condition.getPatterns().iterator().next();
  193 | 			uriVariables = getPathMatcher().extractUriTemplateVariables(bestPattern, lookupPath);
  194 | 			if (!getUrlPathHelper().shouldRemoveSemicolonContent()) {
  195 | 				request.setAttribute(MATRIX_VARIABLES_ATTRIBUTE, extractMatrixVariables(request, uriVariables));
  196 | 			}
  197 | 			uriVariables = getUrlPathHelper().decodePathVariables(request, uriVariables);
  198 | 		}
  199 | 		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
  200 | 		ServerHttpObservationFilter.findObservationContext(request)
  201 | 				.ifPresent(context -> context.setPathPattern(bestPattern));
  202 | 		request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
  203 | 	}
  204 | 
  205 | 	@SuppressWarnings("removal")
  206 | 	private Map<String, MultiValueMap<String, String>> extractMatrixVariables(
  207 | 			HttpServletRequest request, Map<String, String> uriVariables) {
  208 | 
  209 | 		Map<String, MultiValueMap<String, String>> result = new LinkedHashMap<>();
  210 | 		uriVariables.forEach((uriVarKey, uriVarValue) -> {
  211 | 
  212 | 			int equalsIndex = uriVarValue.indexOf('=');
  213 | 			if (equalsIndex == -1) {
  214 | 				return;
  215 | 			}
  216 | 
  217 | 			int semicolonIndex = uriVarValue.indexOf(';');
  218 | 			if (semicolonIndex != -1 && semicolonIndex != 0) {
  219 | 				uriVariables.put(uriVarKey, uriVarValue.substring(0, semicolonIndex));
  220 | 			}
  221 | 
  222 | 			String matrixVariables;
  223 | 			if (semicolonIndex == -1 || semicolonIndex == 0 || equalsIndex < semicolonIndex) {
  224 | 				matrixVariables = uriVarValue;
  225 | 			}
  226 | 			else {
  227 | 				matrixVariables = uriVarValue.substring(semicolonIndex + 1);
  228 | 			}
  229 | 
  230 | 			MultiValueMap<String, String> vars = WebUtils.parseMatrixVariables(matrixVariables);
  231 | 			result.put(uriVarKey, getUrlPathHelper().decodeMatrixVariables(request, vars));
  232 | 		});
  233 | 		return result;
  234 | 	}
  235 | 
  236 | 	/**
  237 | 	 * Iterate all RequestMappingInfo's once again, look if any match by URL at
  238 | 	 * least and raise exceptions according to what doesn't match.
  239 | 	 * @throws HttpRequestMethodNotSupportedException if there are matches by URL
  240 | 	 * but not by HTTP method
  241 | 	 * @throws HttpMediaTypeNotAcceptableException if there are matches by URL
  242 | 	 * but not by consumable/producible media types
  243 | 	 */
  244 | 	@Override
  245 | 	protected @Nullable HandlerMethod handleNoMatch(
  246 | 			Set<RequestMappingInfo> infos, String lookupPath, HttpServletRequest request) throws ServletException {
  247 | 
  248 | 		if (CollectionUtils.isEmpty(infos)) {
  249 | 			return null;
  250 | 		}
  251 | 
  252 | 		PartialMatchHelper helper = new PartialMatchHelper(infos, request);
  253 | 		if (helper.isEmpty()) {
  254 | 			return null;
  255 | 		}
  256 | 
  257 | 		if (helper.hasMethodsMismatch()) {
  258 | 			Set<String> methods = helper.getAllowedMethods();
  259 | 			if (HttpMethod.OPTIONS.matches(request.getMethod())) {
  260 | 				Set<MediaType> patchMediaTypes = helper.getConsumablePatchMediaTypes();
  261 | 				Set<MediaType> queryMediaTypes = helper.getConsumableQueryMediaTypes();
  262 | 				HttpOptionsHandler handler = new HttpOptionsHandler(methods, patchMediaTypes, queryMediaTypes);
  263 | 				return new HandlerMethod(handler, HTTP_OPTIONS_HANDLE_METHOD);
  264 | 			}
  265 | 			if (HttpMethod.HEAD.matches(request.getMethod()) && methods.contains(HttpMethod.QUERY.name())) {
  266 | 				Set<MediaType> queryMediaTypes = helper.getConsumableQueryMediaTypes();
  267 | 				HttpHeadQueryHandler handler = new HttpHeadQueryHandler(methods, queryMediaTypes);
  268 | 				return new HandlerMethod(handler, HTTP_HEAD_QUERY_HANDLE_METHOD);
  269 | 			}
  270 | 			throw new HttpRequestMethodNotSupportedException(request.getMethod(), methods);
  271 | 		}
  272 | 
  273 | 		if (helper.hasConsumesMismatch()) {
  274 | 			Set<MediaType> mediaTypes = helper.getConsumableMediaTypes();
  275 | 			MediaType contentType = null;
  276 | 			if (StringUtils.hasLength(request.getContentType())) {
  277 | 				try {
  278 | 					contentType = MediaType.parseMediaType(request.getContentType());
  279 | 				}
  280 | 				catch (InvalidMediaTypeException ex) {
  281 | 					throw new HttpMediaTypeNotSupportedException(ex.getMessage(), new ArrayList<>(mediaTypes));
  282 | 				}
  283 | 			}
  284 | 			throw new HttpMediaTypeNotSupportedException(
  285 | 					contentType, new ArrayList<>(mediaTypes), HttpMethod.valueOf(request.getMethod()));
  286 | 		}
  287 | 
  288 | 		if (helper.hasProducesMismatch()) {
  289 | 			Set<MediaType> mediaTypes = helper.getProducibleMediaTypes();
  290 | 			throw new HttpMediaTypeNotAcceptableException(new ArrayList<>(mediaTypes));
  291 | 		}
  292 | 
  293 | 		if (helper.hasParamsMismatch()) {
  294 | 			List<String[]> conditions = helper.getParamConditions();
  295 | 			throw new UnsatisfiedServletRequestParameterException(conditions, request.getParameterMap());
  296 | 		}
  297 | 
  298 | 		return null;
  299 | 	}
  300 | 
  301 | 
  302 | 	/**
  303 | 	 * Aggregate all partial matches and expose methods checking across them.
  304 | 	 */
  305 | 	private static final class PartialMatchHelper {
  306 | 
  307 | 		private final List<PartialMatch> partialMatches = new ArrayList<>();
  308 | 
  309 | 		PartialMatchHelper(Set<RequestMappingInfo> infos, HttpServletRequest request) {
  310 | 			for (RequestMappingInfo info : infos) {
  311 | 				if (info.getActivePatternsCondition().getMatchingCondition(request) != null) {
  312 | 					this.partialMatches.add(new PartialMatch(info, request));
  313 | 				}
  314 | 			}
  315 | 		}
  316 | 
  317 | 		/**
  318 | 		 * Whether there are any partial matches.
  319 | 		 */
  320 | 		public boolean isEmpty() {
  321 | 			return this.partialMatches.isEmpty();
  322 | 		}
  323 | 
  324 | 		/**
  325 | 		 * Any partial matches for "methods"?
  326 | 		 */
  327 | 		public boolean hasMethodsMismatch() {
  328 | 			for (PartialMatch match : this.partialMatches) {
  329 | 				if (match.hasMethodsMatch()) {
  330 | 					return false;
  331 | 				}
  332 | 			}
  333 | 			return true;
  334 | 		}
  335 | 
  336 | 		/**
  337 | 		 * Any partial matches for "methods" and "consumes"?
  338 | 		 */
  339 | 		public boolean hasConsumesMismatch() {
  340 | 			for (PartialMatch match : this.partialMatches) {
  341 | 				if (match.hasConsumesMatch()) {
  342 | 					return false;
  343 | 				}
  344 | 			}
  345 | 			return true;
  346 | 		}
  347 | 
  348 | 		/**
  349 | 		 * Any partial matches for "methods", "consumes", and "produces"?
  350 | 		 */
  351 | 		public boolean hasProducesMismatch() {
  352 | 			for (PartialMatch match : this.partialMatches) {
  353 | 				if (match.hasProducesMatch()) {
  354 | 					return false;
  355 | 				}
  356 | 			}
  357 | 			return true;
  358 | 		}
  359 | 
  360 | 		/**
  361 | 		 * Any partial matches for "methods", "consumes", "produces", and "params"?
  362 | 		 */
  363 | 		public boolean hasParamsMismatch() {
  364 | 			for (PartialMatch match : this.partialMatches) {
  365 | 				if (match.hasParamsMatch()) {
  366 | 					return false;
  367 | 				}
  368 | 			}
  369 | 			return true;
  370 | 		}
  371 | 
  372 | 		/**
  373 | 		 * Return declared HTTP methods.
  374 | 		 */
  375 | 		public Set<String> getAllowedMethods() {
  376 | 			Set<String> result = new LinkedHashSet<>();
  377 | 			for (PartialMatch match : this.partialMatches) {
  378 | 				for (RequestMethod method : match.getInfo().getMethodsCondition().getMethods()) {
  379 | 					result.add(method.name());
  380 | 				}
  381 | 			}
  382 | 			return result;
  383 | 		}
  384 | 
  385 | 		/**
  386 | 		 * Return declared "consumable" types but only among those that also
  387 | 		 * match the "methods" condition.
  388 | 		 */
  389 | 		public Set<MediaType> getConsumableMediaTypes() {
  390 | 			Set<MediaType> result = new LinkedHashSet<>();
  391 | 			for (PartialMatch match : this.partialMatches) {
  392 | 				if (match.hasMethodsMatch()) {
  393 | 					result.addAll(match.getInfo().getConsumesCondition().getConsumableMediaTypes());
  394 | 				}
  395 | 			}
  396 | 			return result;
  397 | 		}
  398 | 
  399 | 		/**
  400 | 		 * Return declared "producible" types but only among those that also
  401 | 		 * match the "methods" and "consumes" conditions.
  402 | 		 */
  403 | 		public Set<MediaType> getProducibleMediaTypes() {
  404 | 			Set<MediaType> result = new LinkedHashSet<>();
  405 | 			for (PartialMatch match : this.partialMatches) {
  406 | 				if (match.hasConsumesMatch()) {
  407 | 					result.addAll(match.getInfo().getProducesCondition().getProducibleMediaTypes());
  408 | 				}
  409 | 			}
  410 | 			return result;
  411 | 		}
  412 | 
  413 | 		/**
  414 | 		 * Return declared "params" conditions but only among those that also
  415 | 		 * match the "methods", "consumes", and "params" conditions.
  416 | 		 */
  417 | 		public List<String[]> getParamConditions() {
  418 | 			List<String[]> result = new ArrayList<>();
  419 | 			for (PartialMatch match : this.partialMatches) {
  420 | 				if (match.hasProducesMatch()) {
  421 | 					Set<NameValueExpression<String>> set = match.getInfo().getParamsCondition().getExpressions();
  422 | 					if (!CollectionUtils.isEmpty(set)) {
  423 | 						int i = 0;
  424 | 						String[] array = new String[set.size()];
  425 | 						for (NameValueExpression<String> expression : set) {
  426 | 							array[i++] = expression.toString();
  427 | 						}
  428 | 						result.add(array);
  429 | 					}
  430 | 				}
  431 | 			}
  432 | 			return result;
  433 | 		}
  434 | 
  435 | 		/**
  436 | 		 * Return declared "consumable" types but only among those that have
  437 | 		 * PATCH specified, or that have no methods at all.
  438 | 		 */
  439 | 		public Set<MediaType> getConsumablePatchMediaTypes() {
  440 | 			Set<MediaType> result = new LinkedHashSet<>();
  441 | 			for (PartialMatch match : this.partialMatches) {
  442 | 				Set<RequestMethod> methods = match.getInfo().getMethodsCondition().getMethods();
  443 | 				if (methods.isEmpty() || methods.contains(RequestMethod.PATCH)) {
  444 | 					result.addAll(match.getInfo().getConsumesCondition().getConsumableMediaTypes());
  445 | 				}
  446 | 			}
  447 | 			return result;
  448 | 		}
  449 | 
  450 | 		/**
  451 | 		 * Return declared "consumable" types but only among those that have
  452 | 		 * QUERY specified, or that have no methods at all.
  453 | 		 */
  454 | 		public Set<MediaType> getConsumableQueryMediaTypes() {
  455 | 			Set<MediaType> result = new LinkedHashSet<>();
  456 | 			for (PartialMatch match : this.partialMatches) {
  457 | 				Set<RequestMethod> methods = match.getInfo().getMethodsCondition().getMethods();
  458 | 				if (methods.isEmpty() || methods.contains(RequestMethod.QUERY)) {
  459 | 					result.addAll(match.getInfo().getConsumesCondition().getConsumableMediaTypes());
  460 | 				}
  461 | 			}
  462 | 			return result;
  463 | 		}
  464 | 
  465 | 
  466 | 		/**
  467 | 		 * Container for a RequestMappingInfo that matches the URL path at least.
  468 | 		 */
  469 | 		private static class PartialMatch {
  470 | 
  471 | 			private final RequestMappingInfo info;
  472 | 
  473 | 			private final boolean methodsMatch;
  474 | 
  475 | 			private final boolean consumesMatch;
  476 | 
  477 | 			private final boolean producesMatch;
  478 | 
  479 | 			private final boolean paramsMatch;
  480 | 
  481 | 			/**
  482 | 			 * Create a new {@link PartialMatch} instance.
  483 | 			 * @param info the RequestMappingInfo that matches the URL path.
  484 | 			 * @param request the current request
  485 | 			 */
  486 | 			public PartialMatch(RequestMappingInfo info, HttpServletRequest request) {
  487 | 				this.info = info;
  488 | 				this.methodsMatch = (info.getMethodsCondition().getMatchingCondition(request) != null);
  489 | 				this.consumesMatch = (info.getConsumesCondition().getMatchingCondition(request) != null);
  490 | 				this.producesMatch = (info.getProducesCondition().getMatchingCondition(request) != null);
  491 | 				this.paramsMatch = (info.getParamsCondition().getMatchingCondition(request) != null);
  492 | 			}
  493 | 
  494 | 			public RequestMappingInfo getInfo() {
  495 | 				return this.info;
  496 | 			}
  497 | 
  498 | 			public boolean hasMethodsMatch() {
  499 | 				return this.methodsMatch;
  500 | 			}
  501 | 
  502 | 			public boolean hasConsumesMatch() {
  503 | 				return (hasMethodsMatch() && this.consumesMatch);
  504 | 			}
  505 | 
  506 | 			public boolean hasProducesMatch() {
  507 | 				return (hasConsumesMatch() && this.producesMatch);
  508 | 			}
  509 | 
  510 | 			public boolean hasParamsMatch() {
  511 | 				return (hasProducesMatch() && this.paramsMatch);
  512 | 			}
  513 | 
  514 | 			@Override
  515 | 			public String toString() {
  516 | 				return this.info.toString();
  517 | 			}
  518 | 		}
  519 | 	}
  520 | 
  521 | 
  522 | 	/**
  523 | 	 * Default handler for HTTP OPTIONS.
  524 | 	 */
  525 | 	private static class HttpOptionsHandler {
  526 | 
  527 | 		private final HttpHeaders headers = new HttpHeaders();
  528 | 
  529 | 		public HttpOptionsHandler(Set<String> declaredMethods, Set<MediaType> acceptPatch, Set<MediaType> acceptQuery) {
  530 | 			this.headers.setAllow(initAllowedHttpMethods(declaredMethods));
  531 | 			this.headers.setAcceptPatch(new ArrayList<>(acceptPatch));
  532 | 			this.headers.setAcceptQuery(new ArrayList<>(acceptQuery));
  533 | 		}
  534 | 
  535 | 		private static Set<HttpMethod> initAllowedHttpMethods(Set<String> declaredMethods) {
  536 | 			Set<HttpMethod> result = CollectionUtils.newLinkedHashSet(declaredMethods.size());
  537 | 			if (declaredMethods.isEmpty()) {
  538 | 				for (HttpMethod method : HttpMethod.values()) {
  539 | 					if (method != HttpMethod.TRACE) {
  540 | 						result.add(method);
  541 | 					}
  542 | 				}
  543 | 			}
  544 | 			else {
  545 | 				for (String method : declaredMethods) {
  546 | 					HttpMethod httpMethod = HttpMethod.valueOf(method);
  547 | 					result.add(httpMethod);
  548 | 					if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.QUERY) {
  549 | 						result.add(HttpMethod.HEAD);
  550 | 					}
  551 | 				}
  552 | 				result.add(HttpMethod.OPTIONS);
  553 | 			}
  554 | 			return result;
  555 | 		}
  556 | 
  557 | 		@SuppressWarnings("unused")
  558 | 		public HttpHeaders handle() {
  559 | 			return this.headers;
  560 | 		}
  561 | 	}
  562 | 
  563 | 	/**
  564 | 	 * Default handler for HTTP HEAD targeting a QUERY endpoint.
  565 | 	 */
  566 | 	private static class HttpHeadQueryHandler {
  567 | 
  568 | 		private final HttpHeaders headers = new HttpHeaders();
  569 | 
  570 | 		public HttpHeadQueryHandler(Set<String> declaredMethods, Set<MediaType> acceptQuery) {
  571 | 			this.headers.setAllow(HttpOptionsHandler.initAllowedHttpMethods(declaredMethods));
  572 | 			this.headers.setAcceptQuery(new ArrayList<>(acceptQuery));
  573 | 		}
  574 | 
  575 | 		@SuppressWarnings("unused")
  576 | 		public HttpHeaders handle() {
  577 | 			return this.headers;
  578 | 		}
  579 | 	}
  580 | 
  581 | }
```
