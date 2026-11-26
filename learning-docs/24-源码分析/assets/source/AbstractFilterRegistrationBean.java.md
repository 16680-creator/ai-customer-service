<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/web/servlet/AbstractFilterRegistrationBean.java -->
<!-- 正文引用：[[源码:AbstractFilterRegistrationBean.java#L起-L止]] ；代码块头部：(src: AbstractFilterRegistrationBean.java#L起-L止) -->

# AbstractFilterRegistrationBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/web/servlet/AbstractFilterRegistrationBean.java` |
| 行数 | 286 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:30:51.069Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.servlet;
    6 | 
    7 | import java.util.Arrays;
    8 | import java.util.Collection;
    9 | import java.util.Collections;
   10 | import java.util.EnumSet;
   11 | import java.util.LinkedHashSet;
   12 | import java.util.Set;
   13 | 
   14 | import jakarta.servlet.DispatcherType;
   15 | import jakarta.servlet.Filter;
   16 | import jakarta.servlet.FilterRegistration;
   17 | import jakarta.servlet.FilterRegistration.Dynamic;
   18 | import jakarta.servlet.ServletContext;
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.util.Assert;
   22 | import org.springframework.util.ClassUtils;
   23 | import org.springframework.util.CollectionUtils;
   24 | import org.springframework.util.StringUtils;
   25 | import org.springframework.web.filter.OncePerRequestFilter;
   26 | 
   27 | /**
   28 |  * Abstract base {@link ServletContextInitializer} to register {@link Filter}s in a
   29 |  * Servlet 3.0+ container.
   30 |  *
   31 |  * @param <T> the type of {@link Filter} to register
   32 |  * @author Phillip Webb
   33 |  * @author Brian Clozel
   34 |  * @since 1.5.22
   35 |  */
   36 | public abstract class AbstractFilterRegistrationBean<T extends Filter> extends DynamicRegistrationBean<Dynamic> {
   37 | 
   38 | 	private static final String[] DEFAULT_URL_MAPPINGS = { "/*" };
   39 | 
   40 | 	private Set<ServletRegistrationBean<?>> servletRegistrationBeans = new LinkedHashSet<>();
   41 | 
   42 | 	private Set<String> servletNames = new LinkedHashSet<>();
   43 | 
   44 | 	private Set<String> urlPatterns = new LinkedHashSet<>();
   45 | 
   46 | 	private @Nullable EnumSet<DispatcherType> dispatcherTypes;
   47 | 
   48 | 	private boolean matchAfter;
   49 | 
   50 | 	/**
   51 | 	 * Create a new instance to be registered with the specified
   52 | 	 * {@link ServletRegistrationBean}s.
   53 | 	 * @param servletRegistrationBeans associate {@link ServletRegistrationBean}s
   54 | 	 */
   55 | 	AbstractFilterRegistrationBean(ServletRegistrationBean<?>... servletRegistrationBeans) {
   56 | 		Assert.notNull(servletRegistrationBeans, "'servletRegistrationBeans' must not be null");
   57 | 		Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
   58 | 	}
   59 | 
   60 | 	/**
   61 | 	 * Set {@link ServletRegistrationBean}s that the filter will be registered against.
   62 | 	 * @param servletRegistrationBeans the Servlet registration beans
   63 | 	 */
   64 | 	public void setServletRegistrationBeans(Collection<? extends ServletRegistrationBean<?>> servletRegistrationBeans) {
   65 | 		Assert.notNull(servletRegistrationBeans, "'servletRegistrationBeans' must not be null");
   66 | 		this.servletRegistrationBeans = new LinkedHashSet<>(servletRegistrationBeans);
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Return a mutable collection of the {@link ServletRegistrationBean} that the filter
   71 | 	 * will be registered against. {@link ServletRegistrationBean}s.
   72 | 	 * @return the Servlet registration beans
   73 | 	 * @see #setServletNames
   74 | 	 * @see #setUrlPatterns
   75 | 	 */
   76 | 	public Collection<ServletRegistrationBean<?>> getServletRegistrationBeans() {
   77 | 		return this.servletRegistrationBeans;
   78 | 	}
   79 | 
   80 | 	/**
   81 | 	 * Add {@link ServletRegistrationBean}s for the filter.
   82 | 	 * @param servletRegistrationBeans the servlet registration beans to add
   83 | 	 * @see #setServletRegistrationBeans
   84 | 	 */
   85 | 	public void addServletRegistrationBeans(ServletRegistrationBean<?>... servletRegistrationBeans) {
   86 | 		Assert.notNull(servletRegistrationBeans, "'servletRegistrationBeans' must not be null");
   87 | 		Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * Set servlet names that the filter will be registered against. This will replace any
   92 | 	 * previously specified servlet names.
   93 | 	 * @param servletNames the servlet names
   94 | 	 * @see #setServletRegistrationBeans
   95 | 	 * @see #setUrlPatterns
   96 | 	 */
   97 | 	public void setServletNames(Collection<String> servletNames) {
   98 | 		Assert.notNull(servletNames, "'servletNames' must not be null");
   99 | 		this.servletNames = new LinkedHashSet<>(servletNames);
  100 | 	}
  101 | 
  102 | 	/**
  103 | 	 * Return a mutable collection of servlet names that the filter will be registered
  104 | 	 * against.
  105 | 	 * @return the servlet names
  106 | 	 */
  107 | 	public Collection<String> getServletNames() {
  108 | 		return this.servletNames;
  109 | 	}
  110 | 
  111 | 	/**
  112 | 	 * Add servlet names for the filter.
  113 | 	 * @param servletNames the servlet names to add
  114 | 	 */
  115 | 	public void addServletNames(String... servletNames) {
  116 | 		Assert.notNull(servletNames, "'servletNames' must not be null");
  117 | 		this.servletNames.addAll(Arrays.asList(servletNames));
  118 | 	}
  119 | 
  120 | 	/**
  121 | 	 * Set the URL patterns that the filter will be registered against. This will replace
  122 | 	 * any previously specified URL patterns.
  123 | 	 * @param urlPatterns the URL patterns
  124 | 	 * @see #setServletRegistrationBeans
  125 | 	 * @see #setServletNames
  126 | 	 */
  127 | 	public void setUrlPatterns(Collection<String> urlPatterns) {
  128 | 		Assert.notNull(urlPatterns, "'urlPatterns' must not be null");
  129 | 		this.urlPatterns = new LinkedHashSet<>(urlPatterns);
  130 | 	}
  131 | 
  132 | 	/**
  133 | 	 * Return a mutable collection of URL patterns, as defined in the Servlet
  134 | 	 * specification, that the filter will be registered against.
  135 | 	 * @return the URL patterns
  136 | 	 */
  137 | 	public Collection<String> getUrlPatterns() {
  138 | 		return this.urlPatterns;
  139 | 	}
  140 | 
  141 | 	/**
  142 | 	 * Add URL patterns, as defined in the Servlet specification, that the filter will be
  143 | 	 * registered against.
  144 | 	 * @param urlPatterns the URL patterns
  145 | 	 */
  146 | 	public void addUrlPatterns(String... urlPatterns) {
  147 | 		Assert.notNull(urlPatterns, "'urlPatterns' must not be null");
  148 | 		Collections.addAll(this.urlPatterns, urlPatterns);
  149 | 	}
  150 | 
  151 | 	/**
  152 | 	 * Determines the {@link DispatcherType dispatcher types} for which the filter should
  153 | 	 * be registered. Applies defaults based on the type of filter being registered if
  154 | 	 * none have been configured. Modifications to the returned {@link EnumSet} will have
  155 | 	 * no effect on the registration.
  156 | 	 * @return the dispatcher types, never {@code null}
  157 | 	 * @since 3.2.0
  158 | 	 */
  159 | 	public EnumSet<DispatcherType> determineDispatcherTypes() {
  160 | 		if (CollectionUtils.isEmpty(this.dispatcherTypes)) {
  161 | 			T filter = getFilter();
  162 | 			Assert.state(filter != null, "'filter' must not be null");
  163 | 			if (ClassUtils.isPresent("org.springframework.web.filter.OncePerRequestFilter",
  164 | 					filter.getClass().getClassLoader()) && filter instanceof OncePerRequestFilter) {
  165 | 				return EnumSet.allOf(DispatcherType.class);
  166 | 			}
  167 | 			else {
  168 | 				return EnumSet.of(DispatcherType.REQUEST);
  169 | 			}
  170 | 		}
  171 | 		return EnumSet.copyOf(this.dispatcherTypes);
  172 | 	}
  173 | 
  174 | 	/**
  175 | 	 * Convenience method to {@link #setDispatcherTypes(EnumSet) set dispatcher types}
  176 | 	 * using the specified elements.
  177 | 	 * @param first the first dispatcher type
  178 | 	 * @param rest additional dispatcher types
  179 | 	 */
  180 | 	public void setDispatcherTypes(DispatcherType first, DispatcherType... rest) {
  181 | 		this.dispatcherTypes = EnumSet.of(first, rest);
  182 | 	}
  183 | 
  184 | 	/**
  185 | 	 * Sets the dispatcher types that should be used with the registration.
  186 | 	 * @param dispatcherTypes the dispatcher types
  187 | 	 */
  188 | 	public void setDispatcherTypes(@Nullable EnumSet<DispatcherType> dispatcherTypes) {
  189 | 		this.dispatcherTypes = dispatcherTypes;
  190 | 	}
  191 | 
  192 | 	/**
  193 | 	 * Set if the filter mappings should be matched after any declared filter mappings of
  194 | 	 * the ServletContext. Defaults to {@code false} indicating the filters are supposed
  195 | 	 * to be matched before any declared filter mappings of the ServletContext.
  196 | 	 * @param matchAfter if filter mappings are matched after
  197 | 	 */
  198 | 	public void setMatchAfter(boolean matchAfter) {
  199 | 		this.matchAfter = matchAfter;
  200 | 	}
  201 | 
  202 | 	/**
  203 | 	 * Return if filter mappings should be matched after any declared Filter mappings of
  204 | 	 * the ServletContext.
  205 | 	 * @return if filter mappings are matched after
  206 | 	 */
  207 | 	public boolean isMatchAfter() {
  208 | 		return this.matchAfter;
  209 | 	}
  210 | 
  211 | 	@Override
  212 | 	protected String getDescription() {
  213 | 		Filter filter = getFilter();
  214 | 		Assert.notNull(filter, "'filter' must not be null");
  215 | 		return "filter " + getOrDeduceName(filter);
  216 | 	}
  217 | 
  218 | 	@Override
  219 | 	protected Dynamic addRegistration(String description, ServletContext servletContext) {
  220 | 		Filter filter = getFilter();
  221 | 		return servletContext.addFilter(getOrDeduceName(filter), filter);
  222 | 	}
  223 | 
  224 | 	/**
  225 | 	 * Configure registration settings. Subclasses can override this method to perform
  226 | 	 * additional configuration if required.
  227 | 	 * @param registration the registration
  228 | 	 */
  229 | 	@Override
  230 | 	protected void configure(FilterRegistration.Dynamic registration) {
  231 | 		super.configure(registration);
  232 | 		EnumSet<DispatcherType> dispatcherTypes = determineDispatcherTypes();
  233 | 		Set<String> servletNames = new LinkedHashSet<>();
  234 | 		for (ServletRegistrationBean<?> servletRegistrationBean : this.servletRegistrationBeans) {
  235 | 			servletNames.add(servletRegistrationBean.getServletName());
  236 | 		}
  237 | 		servletNames.addAll(this.servletNames);
  238 | 		if (servletNames.isEmpty() && this.urlPatterns.isEmpty()) {
  239 | 			registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter, DEFAULT_URL_MAPPINGS);
  240 | 		}
  241 | 		else {
  242 | 			if (!servletNames.isEmpty()) {
  243 | 				registration.addMappingForServletNames(dispatcherTypes, this.matchAfter,
  244 | 						StringUtils.toStringArray(servletNames));
  245 | 			}
  246 | 			if (!this.urlPatterns.isEmpty()) {
  247 | 				registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter,
  248 | 						StringUtils.toStringArray(this.urlPatterns));
  249 | 			}
  250 | 		}
  251 | 	}
  252 | 
  253 | 	/**
  254 | 	 * Return the {@link Filter} to be registered.
  255 | 	 * @return the filter
  256 | 	 */
  257 | 	public abstract @Nullable T getFilter();
  258 | 
  259 | 	/**
  260 | 	 * Returns the filter name that will be registered.
  261 | 	 * @return the filter name
  262 | 	 * @since 3.2.0
  263 | 	 */
  264 | 	public String getFilterName() {
  265 | 		return getOrDeduceName(getFilter());
  266 | 	}
  267 | 
  268 | 	@Override
  269 | 	public String toString() {
  270 | 		StringBuilder builder = new StringBuilder(getOrDeduceName(this));
  271 | 		if (this.servletNames.isEmpty() && this.urlPatterns.isEmpty()) {
  272 | 			builder.append(" urls=").append(Arrays.toString(DEFAULT_URL_MAPPINGS));
  273 | 		}
  274 | 		else {
  275 | 			if (!this.servletNames.isEmpty()) {
  276 | 				builder.append(" servlets=").append(this.servletNames);
  277 | 			}
  278 | 			if (!this.urlPatterns.isEmpty()) {
  279 | 				builder.append(" urls=").append(this.urlPatterns);
  280 | 			}
  281 | 		}
  282 | 		builder.append(" order=").append(getOrder());
  283 | 		return builder.toString();
  284 | 	}
  285 | 
  286 | }
```
