<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc-test:4.2.0 | path: org/springframework/boot/webmvc/test/autoconfigure/SpringBootMockMvcBuilderCustomizer.java -->
<!-- 正文引用：[[源码:SpringBootMockMvcBuilderCustomizer.java#L起-L止]] ；代码块头部：(src: SpringBootMockMvcBuilderCustomizer.java#L起-L止) -->

# SpringBootMockMvcBuilderCustomizer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc-test:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/test/autoconfigure/SpringBootMockMvcBuilderCustomizer.java` |
| 行数 | 375 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc-test/4.2.0/spring-boot-webmvc-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:16.820Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.test.autoconfigure;
    6 | 
    7 | import java.io.PrintStream;
    8 | import java.io.PrintWriter;
    9 | import java.io.StringWriter;
   10 | import java.util.ArrayList;
   11 | import java.util.Arrays;
   12 | import java.util.Collection;
   13 | import java.util.EnumSet;
   14 | import java.util.List;
   15 | 
   16 | import jakarta.servlet.Filter;
   17 | import jakarta.servlet.annotation.WebInitParam;
   18 | import org.apache.commons.logging.Log;
   19 | import org.apache.commons.logging.LogFactory;
   20 | import org.jspecify.annotations.Nullable;
   21 | 
   22 | import org.springframework.beans.factory.ListableBeanFactory;
   23 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   24 | import org.springframework.boot.web.servlet.AbstractFilterRegistrationBean;
   25 | import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
   26 | import org.springframework.boot.web.servlet.FilterRegistration;
   27 | import org.springframework.boot.web.servlet.FilterRegistrationBean;
   28 | import org.springframework.boot.web.servlet.RegistrationBean;
   29 | import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
   30 | import org.springframework.context.ApplicationContext;
   31 | import org.springframework.context.ConfigurableApplicationContext;
   32 | import org.springframework.core.annotation.Order;
   33 | import org.springframework.test.web.servlet.MvcResult;
   34 | import org.springframework.test.web.servlet.ResultHandler;
   35 | import org.springframework.test.web.servlet.result.PrintingResultHandler;
   36 | import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
   37 | import org.springframework.util.Assert;
   38 | import org.springframework.util.CollectionUtils;
   39 | import org.springframework.util.StringUtils;
   40 | import org.springframework.web.context.WebApplicationContext;
   41 | 
   42 | /**
   43 |  * {@link MockMvcBuilderCustomizer} for a typical Spring Boot application. Usually applied
   44 |  * automatically through {@link AutoConfigureMockMvc @AutoConfigureMockMvc}, but may also
   45 |  * be used directly.
   46 |  *
   47 |  * @author Phillip Webb
   48 |  * @author Andy Wilkinson
   49 |  * @author Moritz Halbritter
   50 |  * @since 4.0.0
   51 |  */
   52 | public class SpringBootMockMvcBuilderCustomizer implements MockMvcBuilderCustomizer {
   53 | 
   54 | 	private final WebApplicationContext context;
   55 | 
   56 | 	/**
   57 | 	 * Whether to add servlet filters.
   58 | 	 */
   59 | 	private boolean addFilters = true;
   60 | 
   61 | 	/**
   62 | 	 * MVC Print option.
   63 | 	 */
   64 | 	private MockMvcPrint print = MockMvcPrint.DEFAULT;
   65 | 
   66 | 	/**
   67 | 	 * Whether to only print failed exchanges.
   68 | 	 */
   69 | 	private boolean printOnlyOnFailure = true;
   70 | 
   71 | 	/**
   72 | 	 * Create a new {@link SpringBootMockMvcBuilderCustomizer} instance.
   73 | 	 * @param context the source application context
   74 | 	 */
   75 | 	public SpringBootMockMvcBuilderCustomizer(WebApplicationContext context) {
   76 | 		Assert.notNull(context, "'context' must not be null");
   77 | 		this.context = context;
   78 | 	}
   79 | 
   80 | 	@Override
   81 | 	public void customize(ConfigurableMockMvcBuilder<?> builder) {
   82 | 		if (this.addFilters) {
   83 | 			addFilters(builder);
   84 | 		}
   85 | 		ResultHandler printHandler = getPrintHandler();
   86 | 		if (printHandler != null) {
   87 | 			builder.alwaysDo(printHandler);
   88 | 		}
   89 | 	}
   90 | 
   91 | 	private @Nullable ResultHandler getPrintHandler() {
   92 | 		LinesWriter writer = getLinesWriter();
   93 | 		if (writer == null) {
   94 | 			return null;
   95 | 		}
   96 | 		if (this.printOnlyOnFailure) {
   97 | 			writer = new DeferredLinesWriter(this.context, writer);
   98 | 		}
   99 | 		return new LinesWritingResultHandler(writer);
  100 | 	}
  101 | 
  102 | 	private @Nullable LinesWriter getLinesWriter() {
  103 | 		if (this.print == MockMvcPrint.NONE) {
  104 | 			return null;
  105 | 		}
  106 | 		if (this.print == MockMvcPrint.LOG_DEBUG) {
  107 | 			return (LoggingLinesWriter.isDebugEnabled()) ? new LoggingLinesWriter() : null;
  108 | 		}
  109 | 		return new SystemLinesWriter(this.print);
  110 | 	}
  111 | 
  112 | 	private void addFilters(ConfigurableMockMvcBuilder<?> builder) {
  113 | 		FilterRegistrationBeans registrations = new FilterRegistrationBeans(this.context);
  114 | 		registrations.stream()
  115 | 			.map(AbstractFilterRegistrationBean.class::cast)
  116 | 			.filter(AbstractFilterRegistrationBean<?>::isEnabled)
  117 | 			.forEach((registration) -> addFilter(builder, registration));
  118 | 	}
  119 | 
  120 | 	private void addFilter(ConfigurableMockMvcBuilder<?> builder, AbstractFilterRegistrationBean<?> registration) {
  121 | 		Collection<String> urls = registration.getUrlPatterns();
  122 | 		Filter filter = registration.getFilter();
  123 | 		Assert.state(filter != null, "'filter' must not be null");
  124 | 		builder.addFilter(filter, registration.getFilterName(), registration.getInitParameters(),
  125 | 				registration.determineDispatcherTypes(), StringUtils.toStringArray(urls));
  126 | 	}
  127 | 
  128 | 	public void setAddFilters(boolean addFilters) {
  129 | 		this.addFilters = addFilters;
  130 | 	}
  131 | 
  132 | 	public boolean isAddFilters() {
  133 | 		return this.addFilters;
  134 | 	}
  135 | 
  136 | 	public void setPrint(MockMvcPrint print) {
  137 | 		this.print = print;
  138 | 	}
  139 | 
  140 | 	public MockMvcPrint getPrint() {
  141 | 		return this.print;
  142 | 	}
  143 | 
  144 | 	public void setPrintOnlyOnFailure(boolean printOnlyOnFailure) {
  145 | 		this.printOnlyOnFailure = printOnlyOnFailure;
  146 | 	}
  147 | 
  148 | 	public boolean isPrintOnlyOnFailure() {
  149 | 		return this.printOnlyOnFailure;
  150 | 	}
  151 | 
  152 | 	/**
  153 | 	 * {@link ResultHandler} that prints {@link MvcResult} details to a given
  154 | 	 * {@link LinesWriter}.
  155 | 	 */
  156 | 	private static class LinesWritingResultHandler implements ResultHandler {
  157 | 
  158 | 		private final LinesWriter writer;
  159 | 
  160 | 		LinesWritingResultHandler(LinesWriter writer) {
  161 | 			this.writer = writer;
  162 | 		}
  163 | 
  164 | 		@Override
  165 | 		public void handle(MvcResult result) throws Exception {
  166 | 			LinesPrintingResultHandler delegate = new LinesPrintingResultHandler();
  167 | 			delegate.handle(result);
  168 | 			delegate.write(this.writer);
  169 | 		}
  170 | 
  171 | 		private static class LinesPrintingResultHandler extends PrintingResultHandler {
  172 | 
  173 | 			protected LinesPrintingResultHandler() {
  174 | 				super(new Printer());
  175 | 			}
  176 | 
  177 | 			void write(LinesWriter writer) {
  178 | 				writer.write(((Printer) getPrinter()).getLines());
  179 | 			}
  180 | 
  181 | 			private static final class Printer implements ResultValuePrinter {
  182 | 
  183 | 				private final List<String> lines = new ArrayList<>();
  184 | 
  185 | 				@Override
  186 | 				public void printHeading(String heading) {
  187 | 					this.lines.add("");
  188 | 					this.lines.add(String.format("%s:", heading));
  189 | 				}
  190 | 
  191 | 				@Override
  192 | 				public void printValue(String label, @Nullable Object value) {
  193 | 					if (value != null && value.getClass().isArray()) {
  194 | 						value = CollectionUtils.arrayToList(value);
  195 | 					}
  196 | 					try {
  197 | 						this.lines.add("%17s = %s".formatted(label, value));
  198 | 					}
  199 | 					catch (RuntimeException ex) {
  200 | 						this.lines.add("%17s = << Exception '%s' occurred while formatting >>".formatted(label, ex));
  201 | 					}
  202 | 				}
  203 | 
  204 | 				List<String> getLines() {
  205 | 					return this.lines;
  206 | 				}
  207 | 
  208 | 			}
  209 | 
  210 | 		}
  211 | 
  212 | 	}
  213 | 
  214 | 	/**
  215 | 	 * Strategy interface to write MVC result lines.
  216 | 	 */
  217 | 	interface LinesWriter {
  218 | 
  219 | 		void write(List<String> lines);
  220 | 
  221 | 	}
  222 | 
  223 | 	/**
  224 | 	 * {@link LinesWriter} used to defer writing until errors are detected.
  225 | 	 *
  226 | 	 * @see MockMvcPrintOnlyOnFailureTestExecutionListener
  227 | 	 */
  228 | 	static class DeferredLinesWriter implements LinesWriter {
  229 | 
  230 | 		private static final String BEAN_NAME = DeferredLinesWriter.class.getName();
  231 | 
  232 | 		private final LinesWriter delegate;
  233 | 
  234 | 		private final ThreadLocal<List<String>> lines = ThreadLocal.withInitial(ArrayList::new);
  235 | 
  236 | 		DeferredLinesWriter(WebApplicationContext context, LinesWriter delegate) {
  237 | 			Assert.state(context instanceof ConfigurableApplicationContext,
  238 | 					"A ConfigurableApplicationContext is required for printOnlyOnFailure");
  239 | 			((ConfigurableApplicationContext) context).getBeanFactory().registerSingleton(BEAN_NAME, this);
  240 | 			this.delegate = delegate;
  241 | 		}
  242 | 
  243 | 		@Override
  244 | 		public void write(List<String> lines) {
  245 | 			this.lines.get().addAll(lines);
  246 | 		}
  247 | 
  248 | 		void writeDeferredResult() {
  249 | 			this.delegate.write(this.lines.get());
  250 | 		}
  251 | 
  252 | 		static @Nullable DeferredLinesWriter get(ApplicationContext applicationContext) {
  253 | 			try {
  254 | 				return applicationContext.getBean(BEAN_NAME, DeferredLinesWriter.class);
  255 | 			}
  256 | 			catch (NoSuchBeanDefinitionException ex) {
  257 | 				return null;
  258 | 			}
  259 | 		}
  260 | 
  261 | 		void clear() {
  262 | 			this.lines.remove();
  263 | 		}
  264 | 
  265 | 	}
  266 | 
  267 | 	/**
  268 | 	 * {@link LinesWriter} to output results to the log.
  269 | 	 */
  270 | 	private static final class LoggingLinesWriter implements LinesWriter {
  271 | 
  272 | 		private static final Log logger = LogFactory.getLog("org.springframework.test.web.servlet.result");
  273 | 
  274 | 		@Override
  275 | 		public void write(List<String> lines) {
  276 | 			if (logger.isDebugEnabled()) {
  277 | 				StringWriter stringWriter = new StringWriter();
  278 | 				PrintWriter printWriter = new PrintWriter(stringWriter);
  279 | 				for (String line : lines) {
  280 | 					printWriter.println(line);
  281 | 				}
  282 | 				logger.debug("MvcResult details:\n" + stringWriter);
  283 | 			}
  284 | 		}
  285 | 
  286 | 		static boolean isDebugEnabled() {
  287 | 			return logger.isDebugEnabled();
  288 | 		}
  289 | 
  290 | 	}
  291 | 
  292 | 	/**
  293 | 	 * {@link LinesWriter} to output results to {@code System.out} or {@code System.err}.
  294 | 	 */
  295 | 	private static class SystemLinesWriter implements LinesWriter {
  296 | 
  297 | 		private final MockMvcPrint print;
  298 | 
  299 | 		SystemLinesWriter(MockMvcPrint print) {
  300 | 			this.print = print;
  301 | 		}
  302 | 
  303 | 		@Override
  304 | 		public void write(List<String> lines) {
  305 | 			PrintStream printStream = getPrintStream();
  306 | 			for (String line : lines) {
  307 | 				printStream.println(line);
  308 | 			}
  309 | 		}
  310 | 
  311 | 		private PrintStream getPrintStream() {
  312 | 			if (this.print == MockMvcPrint.SYSTEM_ERR) {
  313 | 				return System.err;
  314 | 			}
  315 | 			return System.out;
  316 | 		}
  317 | 
  318 | 	}
  319 | 
  320 | 	private static class FilterRegistrationBeans extends ServletContextInitializerBeans {
  321 | 
  322 | 		FilterRegistrationBeans(ListableBeanFactory beanFactory) {
  323 | 			super(beanFactory, FilterRegistrationBean.class, DelegatingFilterProxyRegistrationBean.class);
  324 | 		}
  325 | 
  326 | 		@Override
  327 | 		protected void addAdaptableBeans(ListableBeanFactory beanFactory) {
  328 | 			addAsRegistrationBean(beanFactory, Filter.class, new FilterRegistrationBeanAdapter(beanFactory));
  329 | 		}
  330 | 
  331 | 		private static final class FilterRegistrationBeanAdapter implements RegistrationBeanAdapter<Filter> {
  332 | 
  333 | 			private final ListableBeanFactory beanFactory;
  334 | 
  335 | 			private FilterRegistrationBeanAdapter(ListableBeanFactory beanFactory) {
  336 | 				this.beanFactory = beanFactory;
  337 | 			}
  338 | 
  339 | 			@Override
  340 | 			public RegistrationBean createRegistrationBean(String beanName, Filter source,
  341 | 					int totalNumberOfSourceBeans) {
  342 | 				FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(source);
  343 | 				bean.setName(beanName);
  344 | 				FilterRegistration registrationAnnotation = this.beanFactory.findAnnotationOnBean(beanName,
  345 | 						FilterRegistration.class);
  346 | 				if (registrationAnnotation != null) {
  347 | 					// Supports @Order annotation on @Bean methods
  348 | 					Order orderAnnotation = this.beanFactory.findAnnotationOnBean(beanName, Order.class);
  349 | 					Assert.state(orderAnnotation != null, "'orderAnnotation' must not be null");
  350 | 					configureFromAnnotation(bean, registrationAnnotation, orderAnnotation);
  351 | 				}
  352 | 				return bean;
  353 | 			}
  354 | 
  355 | 			private void configureFromAnnotation(FilterRegistrationBean<Filter> bean,
  356 | 					FilterRegistration registrationAnnotation, Order orderAnnotation) {
  357 | 				bean.setEnabled(registrationAnnotation.enabled());
  358 | 				bean.setOrder(orderAnnotation.value());
  359 | 				if (StringUtils.hasText(registrationAnnotation.name())) {
  360 | 					bean.setName(registrationAnnotation.name());
  361 | 				}
  362 | 				if (registrationAnnotation.dispatcherTypes().length > 0) {
  363 | 					bean.setDispatcherTypes(EnumSet.copyOf(Arrays.asList(registrationAnnotation.dispatcherTypes())));
  364 | 				}
  365 | 				for (WebInitParam param : registrationAnnotation.initParameters()) {
  366 | 					bean.addInitParameter(param.name(), param.value());
  367 | 				}
  368 | 				bean.setUrlPatterns(Arrays.asList(registrationAnnotation.urlPatterns()));
  369 | 			}
  370 | 
  371 | 		}
  372 | 
  373 | 	}
  374 | 
  375 | }
```
