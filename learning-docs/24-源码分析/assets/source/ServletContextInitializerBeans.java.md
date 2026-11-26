<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/web/servlet/ServletContextInitializerBeans.java -->
<!-- 正文引用：[[源码:ServletContextInitializerBeans.java#L起-L止]] ；代码块头部：(src: ServletContextInitializerBeans.java#L起-L止) -->

# ServletContextInitializerBeans.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/web/servlet/ServletContextInitializerBeans.java` |
| 行数 | 429 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:48.993Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.servlet;
    6 | 
    7 | import java.util.AbstractCollection;
    8 | import java.util.ArrayList;
    9 | import java.util.Arrays;
   10 | import java.util.Collections;
   11 | import java.util.EnumSet;
   12 | import java.util.EventListener;
   13 | import java.util.HashMap;
   14 | import java.util.HashSet;
   15 | import java.util.Iterator;
   16 | import java.util.LinkedHashMap;
   17 | import java.util.List;
   18 | import java.util.Map;
   19 | import java.util.Map.Entry;
   20 | import java.util.Set;
   21 | import java.util.stream.Collectors;
   22 | 
   23 | import jakarta.servlet.Filter;
   24 | import jakarta.servlet.MultipartConfigElement;
   25 | import jakarta.servlet.Servlet;
   26 | import jakarta.servlet.annotation.WebInitParam;
   27 | import org.apache.commons.logging.Log;
   28 | import org.apache.commons.logging.LogFactory;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | import org.springframework.aop.scope.ScopedProxyUtils;
   32 | import org.springframework.beans.factory.ListableBeanFactory;
   33 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   34 | import org.springframework.core.Ordered;
   35 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   36 | import org.springframework.core.annotation.Order;
   37 | import org.springframework.util.Assert;
   38 | import org.springframework.util.LinkedMultiValueMap;
   39 | import org.springframework.util.MultiValueMap;
   40 | import org.springframework.util.StringUtils;
   41 | 
   42 | /**
   43 |  * A collection {@link ServletContextInitializer}s obtained from a
   44 |  * {@link ListableBeanFactory}. Includes all {@link ServletContextInitializer} beans and
   45 |  * also adapts {@link Servlet}, {@link Filter} and certain {@link EventListener} beans.
   46 |  * <p>
   47 |  * Items are sorted so that adapted beans are top ({@link Servlet}, {@link Filter} then
   48 |  * {@link EventListener}) and direct {@link ServletContextInitializer} beans are at the
   49 |  * end. Further sorting is applied within these groups using the
   50 |  * {@link AnnotationAwareOrderComparator}.
   51 |  *
   52 |  * @author Dave Syer
   53 |  * @author Phillip Webb
   54 |  * @author Brian Clozel
   55 |  * @author Moritz Halbritter
   56 |  * @author Daeho Kwon
   57 |  * @author Dmytro Danilenkov
   58 |  * @since 1.4.0
   59 |  */
   60 | public class ServletContextInitializerBeans extends AbstractCollection<ServletContextInitializer> {
   61 | 
   62 | 	private static final String DISPATCHER_SERVLET_NAME = "dispatcherServlet";
   63 | 
   64 | 	private static final Log logger = LogFactory.getLog(ServletContextInitializerBeans.class);
   65 | 
   66 | 	/**
   67 | 	 * Seen bean instances or bean names.
   68 | 	 */
   69 | 	private final Seen seen = new Seen();
   70 | 
   71 | 	private final MultiValueMap<Class<?>, ServletContextInitializer> initializers;
   72 | 
   73 | 	private final List<Class<? extends ServletContextInitializer>> initializerTypes;
   74 | 
   75 | 	private final List<ServletContextInitializer> sortedList;
   76 | 
   77 | 	@SafeVarargs
   78 | 	@SuppressWarnings("varargs")
   79 | 	public ServletContextInitializerBeans(ListableBeanFactory beanFactory,
   80 | 			Class<? extends ServletContextInitializer>... initializerTypes) {
   81 | 		this.initializers = new LinkedMultiValueMap<>();
   82 | 		this.initializerTypes = (initializerTypes.length != 0) ? Arrays.asList(initializerTypes)
   83 | 				: Collections.singletonList(ServletContextInitializer.class);
   84 | 		addServletContextInitializerBeans(beanFactory);
   85 | 		addAdaptableBeans(beanFactory);
   86 | 		this.sortedList = this.initializers.values()
   87 | 			.stream()
   88 | 			.flatMap((value) -> value.stream().sorted(AnnotationAwareOrderComparator.INSTANCE))
   89 | 			.toList();
   90 | 		logMappings(this.initializers);
   91 | 	}
   92 | 
   93 | 	private void addServletContextInitializerBeans(ListableBeanFactory beanFactory) {
   94 | 		for (Class<? extends ServletContextInitializer> initializerType : this.initializerTypes) {
   95 | 			for (Entry<String, ? extends ServletContextInitializer> initializerBean : getOrderedBeansOfType(beanFactory,
   96 | 					initializerType)) {
   97 | 				addServletContextInitializerBean(initializerBean.getKey(), initializerBean.getValue(), beanFactory);
   98 | 			}
   99 | 		}
  100 | 	}
  101 | 
  102 | 	private void addServletContextInitializerBean(String beanName, ServletContextInitializer initializer,
  103 | 			ListableBeanFactory beanFactory) {
  104 | 		if (initializer instanceof ServletRegistrationBean<?> servletRegistrationBean) {
  105 | 			Servlet source = servletRegistrationBean.getServlet();
  106 | 			addServletContextInitializerBean(Servlet.class, beanName, servletRegistrationBean, beanFactory, source);
  107 | 		}
  108 | 		else if (initializer instanceof FilterRegistrationBean<?> filterRegistrationBean) {
  109 | 			Filter source = filterRegistrationBean.getFilter();
  110 | 			addServletContextInitializerBean(Filter.class, beanName, filterRegistrationBean, beanFactory, source);
  111 | 		}
  112 | 		else if (initializer instanceof DelegatingFilterProxyRegistrationBean registrationBean) {
  113 | 			String source = registrationBean.getTargetBeanName();
  114 | 			addServletContextInitializerBean(Filter.class, beanName, registrationBean, beanFactory, source);
  115 | 		}
  116 | 		else if (initializer instanceof ServletListenerRegistrationBean<?> registrationBean) {
  117 | 			EventListener source = registrationBean.getListener();
  118 | 			addServletContextInitializerBean(EventListener.class, beanName, registrationBean, beanFactory, source);
  119 | 		}
  120 | 		else {
  121 | 			addServletContextInitializerBean(ServletContextInitializer.class, beanName, initializer, beanFactory,
  122 | 					initializer);
  123 | 		}
  124 | 	}
  125 | 
  126 | 	private void addServletContextInitializerBean(Class<?> type, String beanName, ServletContextInitializer initializer,
  127 | 			ListableBeanFactory beanFactory, @Nullable Object source) {
  128 | 		this.initializers.add(type, initializer);
  129 | 		if (source != null) {
  130 | 			// Mark the underlying source as seen in case it wraps an existing bean
  131 | 			this.seen.add(type, source);
  132 | 		}
  133 | 		if (logger.isTraceEnabled()) {
  134 | 			String resourceDescription = getResourceDescription(beanName, beanFactory);
  135 | 			int order = getOrder(initializer);
  136 | 			logger.trace("Added existing " + type.getSimpleName() + " initializer bean '" + beanName + "'; order="
  137 | 					+ order + ", resource=" + resourceDescription);
  138 | 		}
  139 | 	}
  140 | 
  141 | 	private @Nullable String getResourceDescription(String beanName, ListableBeanFactory beanFactory) {
  142 | 		if (beanFactory instanceof BeanDefinitionRegistry registry) {
  143 | 			return registry.getBeanDefinition(beanName).getResourceDescription();
  144 | 		}
  145 | 		return "unknown";
  146 | 	}
  147 | 
  148 | 	@SuppressWarnings("unchecked")
  149 | 	protected void addAdaptableBeans(ListableBeanFactory beanFactory) {
  150 | 		MultipartConfigElement multipartConfig = getMultipartConfig(beanFactory);
  151 | 		addAsRegistrationBean(beanFactory, Servlet.class,
  152 | 				new ServletRegistrationBeanAdapter(multipartConfig, beanFactory));
  153 | 		addAsRegistrationBean(beanFactory, Filter.class, new FilterRegistrationBeanAdapter(beanFactory));
  154 | 		for (Class<?> listenerType : ServletListenerRegistrationBean.getSupportedTypes()) {
  155 | 			addAsRegistrationBean(beanFactory, EventListener.class, (Class<EventListener>) listenerType,
  156 | 					new ServletListenerRegistrationBeanAdapter());
  157 | 		}
  158 | 	}
  159 | 
  160 | 	private @Nullable MultipartConfigElement getMultipartConfig(ListableBeanFactory beanFactory) {
  161 | 		List<Entry<String, MultipartConfigElement>> beans = getOrderedBeansOfType(beanFactory,
  162 | 				MultipartConfigElement.class);
  163 | 		return beans.isEmpty() ? null : beans.get(0).getValue();
  164 | 	}
  165 | 
  166 | 	protected <T> void addAsRegistrationBean(ListableBeanFactory beanFactory, Class<T> type,
  167 | 			RegistrationBeanAdapter<T> adapter) {
  168 | 		addAsRegistrationBean(beanFactory, type, type, adapter);
  169 | 	}
  170 | 
  171 | 	private <T, B extends T> void addAsRegistrationBean(ListableBeanFactory beanFactory, Class<T> type,
  172 | 			Class<B> beanType, RegistrationBeanAdapter<T> adapter) {
  173 | 		List<Map.Entry<String, B>> entries = getOrderedBeansOfType(beanFactory, beanType, this.seen);
  174 | 		for (Entry<String, B> entry : entries) {
  175 | 			String beanName = entry.getKey();
  176 | 			B bean = entry.getValue();
  177 | 			if (this.seen.add(type, bean)) {
  178 | 				// One that we haven't already seen
  179 | 				RegistrationBean registration = adapter.createRegistrationBean(beanName, bean, entries.size());
  180 | 				Integer order = findOrder(bean);
  181 | 				if (order != null) {
  182 | 					registration.setOrder(order);
  183 | 				}
  184 | 				this.initializers.add(type, registration);
  185 | 				if (logger.isTraceEnabled()) {
  186 | 					logger.trace("Created " + type.getSimpleName() + " initializer for bean '" + beanName + "'; order="
  187 | 							+ order + ", resource=" + getResourceDescription(beanName, beanFactory));
  188 | 				}
  189 | 			}
  190 | 		}
  191 | 	}
  192 | 
  193 | 	private int getOrder(Object value) {
  194 | 		Integer order = findOrder(value);
  195 | 		return (order != null) ? order : Ordered.LOWEST_PRECEDENCE;
  196 | 	}
  197 | 
  198 | 	private @Nullable Integer findOrder(Object value) {
  199 | 		return new AnnotationAwareOrderComparator() {
  200 | 
  201 | 			@Override
  202 | 			public @Nullable Integer findOrder(Object obj) {
  203 | 				return super.findOrder(obj);
  204 | 			}
  205 | 
  206 | 		}.findOrder(value);
  207 | 	}
  208 | 
  209 | 	private <T> List<Entry<String, T>> getOrderedBeansOfType(ListableBeanFactory beanFactory, Class<T> type) {
  210 | 		return getOrderedBeansOfType(beanFactory, type, Seen.empty());
  211 | 	}
  212 | 
  213 | 	private <T> List<Entry<String, T>> getOrderedBeansOfType(ListableBeanFactory beanFactory, Class<T> type,
  214 | 			Seen seen) {
  215 | 		String[] names = beanFactory.getBeanNamesForType(type, true, false);
  216 | 		Map<String, T> map = new LinkedHashMap<>();
  217 | 		for (String name : names) {
  218 | 			if (!seen.contains(type, name) && !ScopedProxyUtils.isScopedTarget(name)) {
  219 | 				T bean = beanFactory.getBean(name, type);
  220 | 				if (!seen.contains(type, bean)) {
  221 | 					map.put(name, bean);
  222 | 				}
  223 | 			}
  224 | 		}
  225 | 		List<Entry<String, T>> beans = new ArrayList<>(map.entrySet());
  226 | 		beans.sort((o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getValue(), o2.getValue()));
  227 | 		return beans;
  228 | 	}
  229 | 
  230 | 	private void logMappings(MultiValueMap<Class<?>, ServletContextInitializer> initializers) {
  231 | 		if (logger.isDebugEnabled()) {
  232 | 			logMappings("filters", initializers, Filter.class, FilterRegistrationBean.class);
  233 | 			logMappings("servlets", initializers, Servlet.class, ServletRegistrationBean.class);
  234 | 		}
  235 | 	}
  236 | 
  237 | 	private void logMappings(String name, MultiValueMap<Class<?>, ServletContextInitializer> initializers,
  238 | 			Class<?> type, Class<? extends RegistrationBean> registrationType) {
  239 | 		List<ServletContextInitializer> registrations = new ArrayList<>();
  240 | 		registrations.addAll(initializers.getOrDefault(registrationType, Collections.emptyList()));
  241 | 		registrations.addAll(initializers.getOrDefault(type, Collections.emptyList()));
  242 | 		String info = registrations.stream().map(Object::toString).collect(Collectors.joining(", "));
  243 | 		logger.debug("Mapping " + name + ": " + info);
  244 | 	}
  245 | 
  246 | 	@Override
  247 | 	public Iterator<ServletContextInitializer> iterator() {
  248 | 		return this.sortedList.iterator();
  249 | 	}
  250 | 
  251 | 	@Override
  252 | 	public int size() {
  253 | 		return this.sortedList.size();
  254 | 	}
  255 | 
  256 | 	/**
  257 | 	 * Adapter to convert a given Bean type into a {@link RegistrationBean} (and hence a
  258 | 	 * {@link ServletContextInitializer}).
  259 | 	 *
  260 | 	 * @param <T> the type of the Bean to adapt
  261 | 	 */
  262 | 	@FunctionalInterface
  263 | 	protected interface RegistrationBeanAdapter<T> {
  264 | 
  265 | 		RegistrationBean createRegistrationBean(String beanName, T source, int totalNumberOfSourceBeans);
  266 | 
  267 | 	}
  268 | 
  269 | 	/**
  270 | 	 * {@link RegistrationBeanAdapter} for {@link Servlet} beans.
  271 | 	 */
  272 | 	private static class ServletRegistrationBeanAdapter implements RegistrationBeanAdapter<Servlet> {
  273 | 
  274 | 		private final @Nullable MultipartConfigElement multipartConfig;
  275 | 
  276 | 		private final ListableBeanFactory beanFactory;
  277 | 
  278 | 		ServletRegistrationBeanAdapter(@Nullable MultipartConfigElement multipartConfig,
  279 | 				ListableBeanFactory beanFactory) {
  280 | 			this.multipartConfig = multipartConfig;
  281 | 			this.beanFactory = beanFactory;
  282 | 		}
  283 | 
  284 | 		@Override
  285 | 		public RegistrationBean createRegistrationBean(String beanName, Servlet source, int totalNumberOfSourceBeans) {
  286 | 			String url = (totalNumberOfSourceBeans != 1) ? "/" + beanName + "/" : "/";
  287 | 			if (beanName.equals(DISPATCHER_SERVLET_NAME)) {
  288 | 				url = "/"; // always map the main dispatcherServlet to "/"
  289 | 			}
  290 | 			ServletRegistrationBean<Servlet> bean = new ServletRegistrationBean<>(source, url);
  291 | 			bean.setName(beanName);
  292 | 			bean.setMultipartConfig(this.multipartConfig);
  293 | 			ServletRegistration registrationAnnotation = this.beanFactory.findAnnotationOnBean(beanName,
  294 | 					ServletRegistration.class);
  295 | 			if (registrationAnnotation != null) {
  296 | 				Order orderAnnotation = this.beanFactory.findAnnotationOnBean(beanName, Order.class);
  297 | 				Assert.notNull(orderAnnotation, "'orderAnnotation' must not be null");
  298 | 				configureFromAnnotation(bean, registrationAnnotation, orderAnnotation);
  299 | 			}
  300 | 			return bean;
  301 | 		}
  302 | 
  303 | 		private void configureFromAnnotation(ServletRegistrationBean<Servlet> bean, ServletRegistration registration,
  304 | 				Order order) {
  305 | 			bean.setEnabled(registration.enabled());
  306 | 			bean.setOrder(order.value());
  307 | 			if (StringUtils.hasText(registration.name())) {
  308 | 				bean.setName(registration.name());
  309 | 			}
  310 | 			bean.setAsyncSupported(registration.asyncSupported());
  311 | 			bean.setIgnoreRegistrationFailure(registration.ignoreRegistrationFailure());
  312 | 			bean.setLoadOnStartup(registration.loadOnStartup());
  313 | 			bean.setUrlMappings(Arrays.asList(registration.urlMappings()));
  314 | 			for (WebInitParam param : registration.initParameters()) {
  315 | 				bean.addInitParameter(param.name(), param.value());
  316 | 			}
  317 | 			bean.setMultipartConfig(new MultipartConfigElement(registration.multipartConfig()));
  318 | 		}
  319 | 
  320 | 	}
  321 | 
  322 | 	/**
  323 | 	 * {@link RegistrationBeanAdapter} implementation for {@link Filter} beans.
  324 | 	 * <p>
  325 | 	 * <b>NOTE:</b> A similar implementation is used in
  326 | 	 * {@code SpringBootMockMvcBuilderCustomizer} for registering
  327 | 	 * {@code @FilterRegistration} beans with {@code @MockMvc}. If you modify this class,
  328 | 	 * please also update {@code SpringBootMockMvcBuilderCustomizer} if needed.
  329 | 	 * </p>
  330 | 	 */
  331 | 	private static class FilterRegistrationBeanAdapter implements RegistrationBeanAdapter<Filter> {
  332 | 
  333 | 		private final ListableBeanFactory beanFactory;
  334 | 
  335 | 		FilterRegistrationBeanAdapter(ListableBeanFactory beanFactory) {
  336 | 			this.beanFactory = beanFactory;
  337 | 		}
  338 | 
  339 | 		@Override
  340 | 		public RegistrationBean createRegistrationBean(String beanName, Filter source, int totalNumberOfSourceBeans) {
  341 | 			FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(source);
  342 | 			bean.setName(beanName);
  343 | 			FilterRegistration registrationAnnotation = this.beanFactory.findAnnotationOnBean(beanName,
  344 | 					FilterRegistration.class);
  345 | 			if (registrationAnnotation != null) {
  346 | 				Order orderAnnotation = this.beanFactory.findAnnotationOnBean(beanName, Order.class);
  347 | 				Assert.notNull(orderAnnotation, "'orderAnnotation' must not be null");
  348 | 				configureFromAnnotation(bean, registrationAnnotation, orderAnnotation);
  349 | 			}
  350 | 			return bean;
  351 | 		}
  352 | 
  353 | 		private void configureFromAnnotation(FilterRegistrationBean<Filter> bean, FilterRegistration registration,
  354 | 				Order order) {
  355 | 			bean.setEnabled(registration.enabled());
  356 | 			bean.setOrder(order.value());
  357 | 			if (StringUtils.hasText(registration.name())) {
  358 | 				bean.setName(registration.name());
  359 | 			}
  360 | 			bean.setAsyncSupported(registration.asyncSupported());
  361 | 			if (registration.dispatcherTypes().length > 0) {
  362 | 				bean.setDispatcherTypes(EnumSet.copyOf(Arrays.asList(registration.dispatcherTypes())));
  363 | 			}
  364 | 			bean.setIgnoreRegistrationFailure(registration.ignoreRegistrationFailure());
  365 | 			bean.setMatchAfter(registration.matchAfter());
  366 | 			bean.setServletNames(Arrays.asList(registration.servletNames()));
  367 | 			bean.setUrlPatterns(Arrays.asList(registration.urlPatterns()));
  368 | 			for (WebInitParam param : registration.initParameters()) {
  369 | 				bean.addInitParameter(param.name(), param.value());
  370 | 			}
  371 | 			this.beanFactory.getBeanProvider(ServletRegistrationBean.class).forEach((servletRegistrationBean) -> {
  372 | 				for (Class<?> servletClass : registration.servletClasses()) {
  373 | 					if (servletClass.isInstance(servletRegistrationBean.getServlet())) {
  374 | 						bean.addServletRegistrationBeans(servletRegistrationBean);
  375 | 					}
  376 | 				}
  377 | 			});
  378 | 		}
  379 | 
  380 | 	}
  381 | 
  382 | 	/**
  383 | 	 * {@link RegistrationBeanAdapter} for certain {@link EventListener} beans.
  384 | 	 */
  385 | 	private static final class ServletListenerRegistrationBeanAdapter
  386 | 			implements RegistrationBeanAdapter<EventListener> {
  387 | 
  388 | 		@Override
  389 | 		public RegistrationBean createRegistrationBean(String beanName, EventListener source,
  390 | 				int totalNumberOfSourceBeans) {
  391 | 			return new ServletListenerRegistrationBean<>(source);
  392 | 		}
  393 | 
  394 | 	}
  395 | 
  396 | 	/**
  397 | 	 * Tracks seen initializers.
  398 | 	 */
  399 | 	private static final class Seen {
  400 | 
  401 | 		private final Map<Class<?>, Set<Object>> seen = new HashMap<>();
  402 | 
  403 | 		boolean add(Class<?> type, Object object) {
  404 | 			if (contains(type, object)) {
  405 | 				return false;
  406 | 			}
  407 | 			return this.seen.computeIfAbsent(type, (ignore) -> new HashSet<>()).add(object);
  408 | 		}
  409 | 
  410 | 		boolean contains(Class<?> type, Object object) {
  411 | 			if (this.seen.isEmpty()) {
  412 | 				return false;
  413 | 			}
  414 | 			// If it has been directly seen, or the implemented ServletContextInitializer
  415 | 			// has been seen already
  416 | 			if (type != ServletContextInitializer.class
  417 | 					&& this.seen.getOrDefault(type, Collections.emptySet()).contains(object)) {
  418 | 				return true;
  419 | 			}
  420 | 			return this.seen.getOrDefault(ServletContextInitializer.class, Collections.emptySet()).contains(object);
  421 | 		}
  422 | 
  423 | 		static Seen empty() {
  424 | 			return new Seen();
  425 | 		}
  426 | 
  427 | 	}
  428 | 
  429 | }
```
