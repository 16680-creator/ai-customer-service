<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/FilterChainProxy.java -->
<!-- 正文引用：[[源码:FilterChainProxy.java#L起-L止]] ；代码块头部：(src: FilterChainProxy.java#L起-L止) -->

# FilterChainProxy.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/FilterChainProxy.java` |
| 行数 | 482 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:18.026Z |

```java
    1 | /*
    2 |  * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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
   17 | package org.springframework.security.web;
   18 | 
   19 | import java.io.IOException;
   20 | import java.util.Arrays;
   21 | import java.util.Collections;
   22 | import java.util.List;
   23 | 
   24 | import jakarta.servlet.Filter;
   25 | import jakarta.servlet.FilterChain;
   26 | import jakarta.servlet.ServletException;
   27 | import jakarta.servlet.ServletRequest;
   28 | import jakarta.servlet.ServletResponse;
   29 | import jakarta.servlet.http.HttpServletRequest;
   30 | import jakarta.servlet.http.HttpServletResponse;
   31 | import org.apache.commons.logging.Log;
   32 | import org.apache.commons.logging.LogFactory;
   33 | import org.jspecify.annotations.Nullable;
   34 | 
   35 | import org.springframework.core.log.LogMessage;
   36 | import org.springframework.security.core.context.SecurityContextHolder;
   37 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   38 | import org.springframework.security.web.access.PathPatternRequestTransformer;
   39 | import org.springframework.security.web.firewall.FirewalledRequest;
   40 | import org.springframework.security.web.firewall.HttpFirewall;
   41 | import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
   42 | import org.springframework.security.web.firewall.RequestRejectedException;
   43 | import org.springframework.security.web.firewall.RequestRejectedHandler;
   44 | import org.springframework.security.web.firewall.StrictHttpFirewall;
   45 | import org.springframework.security.web.util.ThrowableAnalyzer;
   46 | import org.springframework.security.web.util.UrlUtils;
   47 | import org.springframework.security.web.util.matcher.RequestMatcher;
   48 | import org.springframework.util.Assert;
   49 | import org.springframework.web.filter.DelegatingFilterProxy;
   50 | import org.springframework.web.filter.GenericFilterBean;
   51 | 
   52 | /**
   53 |  * Delegates {@code Filter} requests to a list of Spring-managed filter beans. As of
   54 |  * version 2.0, you shouldn't need to explicitly configure a {@code FilterChainProxy} bean
   55 |  * in your application context unless you need very fine control over the filter chain
   56 |  * contents. Most cases should be adequately covered by the default
   57 |  * {@code <security:http />} namespace configuration options.
   58 |  * <p>
   59 |  * The {@code FilterChainProxy} is linked into the servlet container filter chain by
   60 |  * adding a standard Spring {@link DelegatingFilterProxy} declaration in the application
   61 |  * {@code web.xml} file.
   62 |  *
   63 |  * <h2>Configuration</h2>
   64 |  * <p>
   65 |  * As of version 3.1, {@code FilterChainProxy} is configured using a list of
   66 |  * {@link SecurityFilterChain} instances, each of which contains a {@link RequestMatcher}
   67 |  * and a list of filters which should be applied to matching requests. Most applications
   68 |  * will only contain a single filter chain, and if you are using the namespace, you don't
   69 |  * have to set the chains explicitly. If you require finer-grained control, you can make
   70 |  * use of the {@code <filter-chain>} namespace element. This defines a URI pattern and the
   71 |  * list of filters (as comma-separated bean names) which should be applied to requests
   72 |  * which match the pattern. An example configuration might look like this:
   73 |  *
   74 |  * <pre>
   75 |  *  &lt;bean id="myfilterChainProxy" class="org.springframework.security.web.FilterChainProxy"&gt;
   76 |  *      &lt;constructor-arg&gt;
   77 |  *          &lt;util:list&gt;
   78 |  *              &lt;security:filter-chain pattern="/do/not/filter*" filters="none"/&gt;
   79 |  *              &lt;security:filter-chain pattern="/**" filters="filter1,filter2,filter3"/&gt;
   80 |  *          &lt;/util:list&gt;
   81 |  *      &lt;/constructor-arg&gt;
   82 |  *  &lt;/bean&gt;
   83 |  * </pre>
   84 |  *
   85 |  * The names "filter1", "filter2", "filter3" should be the bean names of {@code Filter}
   86 |  * instances defined in the application context. The order of the names defines the order
   87 |  * in which the filters will be applied. As shown above, use of the value "none" for the
   88 |  * "filters" can be used to exclude a request pattern from the security filter chain
   89 |  * entirely. Please consult the security namespace schema file for a full list of
   90 |  * available configuration options.
   91 |  *
   92 |  * <h2>Request Handling</h2>
   93 |  * <p>
   94 |  * Each possible pattern that the {@code FilterChainProxy} should service must be entered.
   95 |  * The first match for a given request will be used to define all of the {@code Filter}s
   96 |  * that apply to that request. This means you must put most specific matches at the top of
   97 |  * the list, and ensure all {@code Filter}s that should apply for a given matcher are
   98 |  * entered against the respective entry. The {@code FilterChainProxy} will not iterate
   99 |  * through the remainder of the map entries to locate additional {@code Filter}s.
  100 |  * <p>
  101 |  * {@code FilterChainProxy} respects normal handling of {@code Filter}s that elect not to
  102 |  * call
  103 |  * {@link jakarta.servlet.Filter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)}
  104 |  * , in that the remainder of the original or {@code FilterChainProxy}-declared filter
  105 |  * chain will not be called.
  106 |  *
  107 |  * <h3>Request Firewalling</h3>
  108 |  *
  109 |  * An {@link HttpFirewall} instance is used to validate incoming requests and create a
  110 |  * wrapped request which provides consistent path values for matching against. See
  111 |  * {@link StrictHttpFirewall}, for more information on the type of attacks which the
  112 |  * default implementation protects against. A custom implementation can be injected to
  113 |  * provide stricter control over the request contents or if an application needs to
  114 |  * support certain types of request which are rejected by default.
  115 |  * <p>
  116 |  * Note that this means that you must use the Spring Security filters in combination with
  117 |  * a {@code FilterChainProxy} if you want this protection. Don't define them explicitly in
  118 |  * your {@code web.xml} file.
  119 |  * <p>
  120 |  * {@code FilterChainProxy} will use the firewall instance to obtain both request and
  121 |  * response objects which will be fed down the filter chain, so it is also possible to use
  122 |  * this functionality to control the functionality of the response. When the request has
  123 |  * passed through the security filter chain, the {@code reset} method will be called. With
  124 |  * the default implementation this means that the original values of {@code servletPath}
  125 |  * and {@code pathInfo} will be returned thereafter, instead of the modified ones used for
  126 |  * security pattern matching.
  127 |  * <p>
  128 |  * Since this additional wrapping functionality is performed by the
  129 |  * {@code FilterChainProxy}, we don't recommend that you use multiple instances in the
  130 |  * same filter chain. It shouldn't be considered purely as a utility for wrapping filter
  131 |  * beans in a single {@code Filter} instance.
  132 |  *
  133 |  * <h2>Filter Lifecycle</h2>
  134 |  * <p>
  135 |  * Note the {@code Filter} lifecycle mismatch between the servlet container and IoC
  136 |  * container. As described in the {@link DelegatingFilterProxy} Javadocs, we recommend you
  137 |  * allow the IoC container to manage the lifecycle instead of the servlet container.
  138 |  * {@code FilterChainProxy} does not invoke the standard filter lifecycle methods on any
  139 |  * filter beans that you add to the application context.
  140 |  *
  141 |  * @author Carlos Sanchez
  142 |  * @author Ben Alex
  143 |  * @author Luke Taylor
  144 |  * @author Rob Winch
  145 |  */
  146 | public class FilterChainProxy extends GenericFilterBean {
  147 | 
  148 | 	private static final Log logger = LogFactory.getLog(FilterChainProxy.class);
  149 | 
  150 | 	private static final String FILTER_APPLIED = FilterChainProxy.class.getName().concat(".APPLIED");
  151 | 
  152 | 	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
  153 | 		.getContextHolderStrategy();
  154 | 
  155 | 	private List<SecurityFilterChain> filterChains;
  156 | 
  157 | 	private FilterChainValidator filterChainValidator = new NullFilterChainValidator();
  158 | 
  159 | 	private HttpFirewall firewall = new StrictHttpFirewall();
  160 | 
  161 | 	private RequestRejectedHandler requestRejectedHandler = new HttpStatusRequestRejectedHandler();
  162 | 
  163 | 	private ThrowableAnalyzer throwableAnalyzer = new ThrowableAnalyzer();
  164 | 
  165 | 	private FilterChainDecorator filterChainDecorator = new VirtualFilterChainDecorator();
  166 | 
  167 | 	public FilterChainProxy() {
  168 | 		this(Collections.emptyList());
  169 | 	}
  170 | 
  171 | 	public FilterChainProxy(SecurityFilterChain chain) {
  172 | 		this(Arrays.asList(chain));
  173 | 	}
  174 | 
  175 | 	public FilterChainProxy(List<SecurityFilterChain> filterChains) {
  176 | 		Assert.notNull(filterChains, "filterChains cannot be null");
  177 | 		this.filterChains = filterChains;
  178 | 	}
  179 | 
  180 | 	@Override
  181 | 	public void afterPropertiesSet() {
  182 | 		this.filterChainValidator.validate(this);
  183 | 	}
  184 | 
  185 | 	@Override
  186 | 	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
  187 | 			throws IOException, ServletException {
  188 | 		boolean clearContext = request.getAttribute(FILTER_APPLIED) == null;
  189 | 		if (!clearContext) {
  190 | 			doFilterInternal(request, response, chain);
  191 | 			return;
  192 | 		}
  193 | 		try {
  194 | 			request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
  195 | 			doFilterInternal(request, response, chain);
  196 | 		}
  197 | 		catch (Exception ex) {
  198 | 			Throwable[] causeChain = this.throwableAnalyzer.determineCauseChain(ex);
  199 | 			Throwable requestRejectedException = this.throwableAnalyzer
  200 | 				.getFirstThrowableOfType(RequestRejectedException.class, causeChain);
  201 | 			if (!(requestRejectedException instanceof RequestRejectedException)) {
  202 | 				throw ex;
  203 | 			}
  204 | 			this.requestRejectedHandler.handle((HttpServletRequest) request, (HttpServletResponse) response,
  205 | 					(RequestRejectedException) requestRejectedException);
  206 | 		}
  207 | 		finally {
  208 | 			this.securityContextHolderStrategy.clearContext();
  209 | 			request.removeAttribute(FILTER_APPLIED);
  210 | 		}
  211 | 	}
  212 | 
  213 | 	private void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
  214 | 			throws IOException, ServletException {
  215 | 		FirewalledRequest firewallRequest = this.firewall.getFirewalledRequest((HttpServletRequest) request);
  216 | 		HttpServletResponse firewallResponse = this.firewall.getFirewalledResponse((HttpServletResponse) response);
  217 | 		List<Filter> filters = getFilters(firewallRequest);
  218 | 		if (filters == null || filters.isEmpty()) {
  219 | 			if (logger.isTraceEnabled()) {
  220 | 				logger.trace(LogMessage.of(() -> "No security for " + requestLine(firewallRequest)));
  221 | 			}
  222 | 			firewallRequest.reset();
  223 | 			this.filterChainDecorator.decorate(chain).doFilter(firewallRequest, firewallResponse);
  224 | 			return;
  225 | 		}
  226 | 		if (logger.isDebugEnabled()) {
  227 | 			logger.debug(LogMessage.of(() -> "Securing " + requestLine(firewallRequest)));
  228 | 		}
  229 | 		FilterChain reset = (req, res) -> {
  230 | 			if (logger.isDebugEnabled()) {
  231 | 				logger.debug(LogMessage.of(() -> "Secured " + requestLine(firewallRequest)));
  232 | 			}
  233 | 			// Deactivate path stripping as we exit the security filter chain
  234 | 			firewallRequest.reset();
  235 | 			chain.doFilter(req, res);
  236 | 		};
  237 | 		this.filterChainDecorator.decorate(reset, filters).doFilter(firewallRequest, firewallResponse);
  238 | 	}
  239 | 
  240 | 	/**
  241 | 	 * Returns the first filter chain matching the supplied URL.
  242 | 	 * @param request the request to match
  243 | 	 * @return an ordered array of Filters defining the filter chain
  244 | 	 */
  245 | 	private @Nullable List<Filter> getFilters(HttpServletRequest request) {
  246 | 		int count = 0;
  247 | 		for (SecurityFilterChain chain : this.filterChains) {
  248 | 			if (logger.isTraceEnabled()) {
  249 | 				logger.trace(LogMessage.format("Trying to match request against %s (%d/%d)", chain, ++count,
  250 | 						this.filterChains.size()));
  251 | 			}
  252 | 			if (chain.matches(request)) {
  253 | 				return chain.getFilters();
  254 | 			}
  255 | 		}
  256 | 		return null;
  257 | 	}
  258 | 
  259 | 	/**
  260 | 	 * Convenience method, mainly for testing.
  261 | 	 * <p>
  262 | 	 * Attempt to find the matching filter chain based on the given {@code url}. Note that
  263 | 	 * the URI is often not enough information and this method should be used with
  264 | 	 * caution. Instead, consider using Spring Security's testing support that mocks a
  265 | 	 * full HTTP request.
  266 | 	 * @param url the URL
  267 | 	 * @return matching filter list
  268 | 	 */
  269 | 	public @Nullable List<Filter> getFilters(String url) {
  270 | 		PathPatternRequestTransformer requestTransformer = new PathPatternRequestTransformer();
  271 | 		HttpServletRequest transformed = requestTransformer.transform(new FilterInvocation(url, "GET").getRequest());
  272 | 		return getFilters(this.firewall.getFirewalledRequest(transformed));
  273 | 	}
  274 | 
  275 | 	/**
  276 | 	 * Returns the list of {@code SecurityFilterChain}s which will be matched against and
  277 | 	 * applied to incoming requests.
  278 | 	 * @return the list of {@code SecurityFilterChain}s which will be matched against and
  279 | 	 * applied to incoming requests.
  280 | 	 */
  281 | 	public List<SecurityFilterChain> getFilterChains() {
  282 | 		return Collections.unmodifiableList(this.filterChains);
  283 | 	}
  284 | 
  285 | 	/**
  286 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  287 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  288 | 	 *
  289 | 	 * @since 5.8
  290 | 	 */
  291 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
  292 | 		Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
  293 | 		this.securityContextHolderStrategy = securityContextHolderStrategy;
  294 | 	}
  295 | 
  296 | 	/**
  297 | 	 * Used (internally) to specify a validation strategy for the filters in each
  298 | 	 * configured chain.
  299 | 	 * @param filterChainValidator the validator instance which will be invoked on during
  300 | 	 * initialization to check the {@code FilterChainProxy} instance.
  301 | 	 */
  302 | 	public void setFilterChainValidator(FilterChainValidator filterChainValidator) {
  303 | 		this.filterChainValidator = filterChainValidator;
  304 | 	}
  305 | 
  306 | 	/**
  307 | 	 * Used to decorate the original {@link FilterChain} for each request.
  308 | 	 *
  309 | 	 * <p>
  310 | 	 * By default, this decorates the filter chain with a {@link VirtualFilterChain} that
  311 | 	 * iterates through security filters and then delegates to the original chain
  312 | 	 * @param filterChainDecorator the strategy for constructing the filter chain
  313 | 	 * @since 6.0
  314 | 	 */
  315 | 	public void setFilterChainDecorator(FilterChainDecorator filterChainDecorator) {
  316 | 		Assert.notNull(filterChainDecorator, "filterChainDecorator cannot be null");
  317 | 		this.filterChainDecorator = filterChainDecorator;
  318 | 	}
  319 | 
  320 | 	/**
  321 | 	 * Sets the "firewall" implementation which will be used to validate and wrap (or
  322 | 	 * potentially reject) the incoming requests. The default implementation should be
  323 | 	 * satisfactory for most requirements.
  324 | 	 * @param firewall
  325 | 	 */
  326 | 	public void setFirewall(HttpFirewall firewall) {
  327 | 		this.firewall = firewall;
  328 | 	}
  329 | 
  330 | 	/**
  331 | 	 * Sets the {@link RequestRejectedHandler} to be used for requests rejected by the
  332 | 	 * firewall.
  333 | 	 * @param requestRejectedHandler the {@link RequestRejectedHandler}
  334 | 	 * @since 5.2
  335 | 	 */
  336 | 	public void setRequestRejectedHandler(RequestRejectedHandler requestRejectedHandler) {
  337 | 		Assert.notNull(requestRejectedHandler, "requestRejectedHandler may not be null");
  338 | 		this.requestRejectedHandler = requestRejectedHandler;
  339 | 	}
  340 | 
  341 | 	@Override
  342 | 	public String toString() {
  343 | 		StringBuilder sb = new StringBuilder();
  344 | 		sb.append("FilterChainProxy[");
  345 | 		sb.append("Filter Chains: ");
  346 | 		sb.append(this.filterChains);
  347 | 		sb.append("]");
  348 | 		return sb.toString();
  349 | 	}
  350 | 
  351 | 	private static String requestLine(HttpServletRequest request) {
  352 | 		return request.getMethod() + " " + UrlUtils.buildRequestUrl(request);
  353 | 	}
  354 | 
  355 | 	/**
  356 | 	 * Internal {@code FilterChain} implementation that is used to pass a request through
  357 | 	 * the additional internal list of filters which match the request.
  358 | 	 */
  359 | 	private static final class VirtualFilterChain implements FilterChain {
  360 | 
  361 | 		private final FilterChain originalChain;
  362 | 
  363 | 		private final List<Filter> additionalFilters;
  364 | 
  365 | 		private final int size;
  366 | 
  367 | 		private int currentPosition = 0;
  368 | 
  369 | 		private VirtualFilterChain(FilterChain chain, List<Filter> additionalFilters) {
  370 | 			this.originalChain = chain;
  371 | 			this.additionalFilters = additionalFilters;
  372 | 			this.size = additionalFilters.size();
  373 | 		}
  374 | 
  375 | 		@Override
  376 | 		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
  377 | 			if (this.currentPosition == this.size) {
  378 | 				this.originalChain.doFilter(request, response);
  379 | 				return;
  380 | 			}
  381 | 			this.currentPosition++;
  382 | 			Filter nextFilter = this.additionalFilters.get(this.currentPosition - 1);
  383 | 			if (logger.isTraceEnabled()) {
  384 | 				String name = nextFilter.getClass().getSimpleName();
  385 | 				logger.trace(LogMessage.format("Invoking %s (%d/%d)", name, this.currentPosition, this.size));
  386 | 			}
  387 | 			nextFilter.doFilter(request, response, this);
  388 | 		}
  389 | 
  390 | 	}
  391 | 
  392 | 	public interface FilterChainValidator {
  393 | 
  394 | 		void validate(FilterChainProxy filterChainProxy);
  395 | 
  396 | 	}
  397 | 
  398 | 	private static class NullFilterChainValidator implements FilterChainValidator {
  399 | 
  400 | 		@Override
  401 | 		public void validate(FilterChainProxy filterChainProxy) {
  402 | 		}
  403 | 
  404 | 	}
  405 | 
  406 | 	/**
  407 | 	 * A strategy for decorating the provided filter chain with one that accounts for the
  408 | 	 * {@link SecurityFilterChain} for a given request.
  409 | 	 *
  410 | 	 * @author Josh Cummings
  411 | 	 * @since 6.0
  412 | 	 */
  413 | 	public interface FilterChainDecorator {
  414 | 
  415 | 		/**
  416 | 		 * Provide a new {@link FilterChain} that accounts for needed security
  417 | 		 * considerations when there are no security filters.
  418 | 		 * @param original the original {@link FilterChain}
  419 | 		 * @return a security-enabled {@link FilterChain}
  420 | 		 */
  421 | 		default FilterChain decorate(FilterChain original) {
  422 | 			return decorate(original, Collections.emptyList());
  423 | 		}
  424 | 
  425 | 		/**
  426 | 		 * Provide a new {@link FilterChain} that accounts for the provided filters as
  427 | 		 * well as the original filter chain.
  428 | 		 * @param original the original {@link FilterChain}
  429 | 		 * @param filters the security filters
  430 | 		 * @return a security-enabled {@link FilterChain} that includes the provided
  431 | 		 * filters
  432 | 		 */
  433 | 		FilterChain decorate(FilterChain original, List<Filter> filters);
  434 | 
  435 | 	}
  436 | 
  437 | 	/**
  438 | 	 * A {@link FilterChainDecorator} that uses the {@link VirtualFilterChain}.
  439 | 	 *
  440 | 	 * @author Josh Cummings
  441 | 	 * @since 6.0
  442 | 	 */
  443 | 	public static final class VirtualFilterChainDecorator implements FilterChainDecorator {
  444 | 
  445 | 		/**
  446 | 		 * {@inheritDoc}
  447 | 		 */
  448 | 		@Override
  449 | 		public FilterChain decorate(FilterChain original) {
  450 | 			return original;
  451 | 		}
  452 | 
  453 | 		/**
  454 | 		 * {@inheritDoc}
  455 | 		 */
  456 | 		@Override
  457 | 		public FilterChain decorate(FilterChain original, List<Filter> filters) {
  458 | 			return new VirtualFilterChain(original, filters);
  459 | 		}
  460 | 
  461 | 	}
  462 | 
  463 | 	private static final class FirewallFilter implements Filter {
  464 | 
  465 | 		private final HttpFirewall firewall;
  466 | 
  467 | 		private FirewallFilter(HttpFirewall firewall) {
  468 | 			this.firewall = firewall;
  469 | 		}
  470 | 
  471 | 		@Override
  472 | 		public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
  473 | 				throws IOException, ServletException {
  474 | 			HttpServletRequest request = (HttpServletRequest) servletRequest;
  475 | 			HttpServletResponse response = (HttpServletResponse) servletResponse;
  476 | 			filterChain.doFilter(this.firewall.getFirewalledRequest(request),
  477 | 					this.firewall.getFirewalledResponse(response));
  478 | 		}
  479 | 
  480 | 	}
  481 | 
  482 | }
```
