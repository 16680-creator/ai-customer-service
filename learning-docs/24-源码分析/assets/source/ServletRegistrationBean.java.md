<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/web/servlet/ServletRegistrationBean.java -->
<!-- 正文引用：[[源码:ServletRegistrationBean.java#L起-L止]] ；代码块头部：(src: ServletRegistrationBean.java#L起-L止) -->

# ServletRegistrationBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/web/servlet/ServletRegistrationBean.java` |
| 行数 | 205 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:49.187Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.servlet;
    6 | 
    7 | import java.util.Arrays;
    8 | import java.util.Collection;
    9 | import java.util.LinkedHashSet;
   10 | import java.util.Set;
   11 | 
   12 | import jakarta.servlet.MultipartConfigElement;
   13 | import jakarta.servlet.Servlet;
   14 | import jakarta.servlet.ServletContext;
   15 | import jakarta.servlet.ServletRegistration;
   16 | import org.jspecify.annotations.Nullable;
   17 | 
   18 | import org.springframework.util.Assert;
   19 | import org.springframework.util.ObjectUtils;
   20 | import org.springframework.util.StringUtils;
   21 | 
   22 | /**
   23 |  * A {@link ServletContextInitializer} to register {@link Servlet}s in a Servlet 3.0+
   24 |  * container. Similar to the {@link ServletContext#addServlet(String, Servlet)
   25 |  * registration} features provided by {@link ServletContext} but with a Spring Bean
   26 |  * friendly design.
   27 |  * <p>
   28 |  * The {@link #setServlet(Servlet) servlet} must be specified before calling
   29 |  * {@link #onStartup}. URL mapping can be configured used {@link #setUrlMappings} or
   30 |  * omitted when mapping to '/*' (unless
   31 |  * {@link #ServletRegistrationBean(Servlet, boolean, String...) alwaysMapUrl} is set to
   32 |  * {@code false}). The servlet name will be deduced if not specified.
   33 |  *
   34 |  * @param <T> the type of the {@link Servlet} to register
   35 |  * @author Phillip Webb
   36 |  * @since 1.4.0
   37 |  * @see ServletContextInitializer
   38 |  * @see ServletContext#addServlet(String, Servlet)
   39 |  * @see org.springframework.boot.web.servlet.ServletRegistration
   40 |  */
   41 | public class ServletRegistrationBean<T extends Servlet> extends DynamicRegistrationBean<ServletRegistration.Dynamic> {
   42 | 
   43 | 	private static final String[] DEFAULT_MAPPINGS = { "/*" };
   44 | 
   45 | 	private @Nullable T servlet;
   46 | 
   47 | 	private Set<String> urlMappings = new LinkedHashSet<>();
   48 | 
   49 | 	private boolean alwaysMapUrl = true;
   50 | 
   51 | 	private int loadOnStartup = -1;
   52 | 
   53 | 	private @Nullable MultipartConfigElement multipartConfig;
   54 | 
   55 | 	/**
   56 | 	 * Create a new {@link ServletRegistrationBean} instance.
   57 | 	 */
   58 | 	public ServletRegistrationBean() {
   59 | 	}
   60 | 
   61 | 	/**
   62 | 	 * Create a new {@link ServletRegistrationBean} instance with the specified
   63 | 	 * {@link Servlet} and URL mappings.
   64 | 	 * @param servlet the servlet being mapped
   65 | 	 * @param urlMappings the URLs being mapped
   66 | 	 */
   67 | 	public ServletRegistrationBean(T servlet, String... urlMappings) {
   68 | 		this(servlet, true, urlMappings);
   69 | 	}
   70 | 
   71 | 	/**
   72 | 	 * Create a new {@link ServletRegistrationBean} instance with the specified
   73 | 	 * {@link Servlet} and URL mappings.
   74 | 	 * @param servlet the servlet being mapped
   75 | 	 * @param alwaysMapUrl if omitted URL mappings should be replaced with '/*'
   76 | 	 * @param urlMappings the URLs being mapped
   77 | 	 */
   78 | 	public ServletRegistrationBean(T servlet, boolean alwaysMapUrl, String... urlMappings) {
   79 | 		Assert.notNull(servlet, "'servlet' must not be null");
   80 | 		Assert.notNull(urlMappings, "'urlMappings' must not be null");
   81 | 		this.servlet = servlet;
   82 | 		this.alwaysMapUrl = alwaysMapUrl;
   83 | 		this.urlMappings.addAll(Arrays.asList(urlMappings));
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Sets the servlet to be registered.
   88 | 	 * @param servlet the servlet
   89 | 	 */
   90 | 	public void setServlet(T servlet) {
   91 | 		Assert.notNull(servlet, "'servlet' must not be null");
   92 | 		this.servlet = servlet;
   93 | 	}
   94 | 
   95 | 	/**
   96 | 	 * Return the servlet being registered.
   97 | 	 * @return the servlet
   98 | 	 */
   99 | 	public @Nullable T getServlet() {
  100 | 		return this.servlet;
  101 | 	}
  102 | 
  103 | 	/**
  104 | 	 * Set the URL mappings for the servlet. If not specified the mapping will default to
  105 | 	 * '/'. This will replace any previously specified mappings.
  106 | 	 * @param urlMappings the mappings to set
  107 | 	 * @see #addUrlMappings(String...)
  108 | 	 */
  109 | 	public void setUrlMappings(Collection<String> urlMappings) {
  110 | 		Assert.notNull(urlMappings, "'urlMappings' must not be null");
  111 | 		this.urlMappings = new LinkedHashSet<>(urlMappings);
  112 | 	}
  113 | 
  114 | 	/**
  115 | 	 * Return a mutable collection of the URL mappings, as defined in the Servlet
  116 | 	 * specification, for the servlet.
  117 | 	 * @return the urlMappings
  118 | 	 */
  119 | 	public Collection<String> getUrlMappings() {
  120 | 		return this.urlMappings;
  121 | 	}
  122 | 
  123 | 	/**
  124 | 	 * Add URL mappings, as defined in the Servlet specification, for the servlet.
  125 | 	 * @param urlMappings the mappings to add
  126 | 	 * @see #setUrlMappings(Collection)
  127 | 	 */
  128 | 	public void addUrlMappings(String... urlMappings) {
  129 | 		Assert.notNull(urlMappings, "'urlMappings' must not be null");
  130 | 		this.urlMappings.addAll(Arrays.asList(urlMappings));
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Sets the {@code loadOnStartup} priority. See
  135 | 	 * {@link ServletRegistration.Dynamic#setLoadOnStartup} for details.
  136 | 	 * @param loadOnStartup if load on startup is enabled
  137 | 	 */
  138 | 	public void setLoadOnStartup(int loadOnStartup) {
  139 | 		this.loadOnStartup = loadOnStartup;
  140 | 	}
  141 | 
  142 | 	/**
  143 | 	 * Set the {@link MultipartConfigElement multi-part configuration}.
  144 | 	 * @param multipartConfig the multipart configuration to set or {@code null}
  145 | 	 */
  146 | 	public void setMultipartConfig(@Nullable MultipartConfigElement multipartConfig) {
  147 | 		this.multipartConfig = multipartConfig;
  148 | 	}
  149 | 
  150 | 	/**
  151 | 	 * Returns the {@link MultipartConfigElement multi-part configuration} to be applied
  152 | 	 * or {@code null}.
  153 | 	 * @return the multipart config
  154 | 	 */
  155 | 	public @Nullable MultipartConfigElement getMultipartConfig() {
  156 | 		return this.multipartConfig;
  157 | 	}
  158 | 
  159 | 	@Override
  160 | 	protected String getDescription() {
  161 | 		Assert.state(this.servlet != null, "Unable to return description for null servlet");
  162 | 		return "servlet " + getServletName();
  163 | 	}
  164 | 
  165 | 	@Override
  166 | 	protected ServletRegistration.Dynamic addRegistration(String description, ServletContext servletContext) {
  167 | 		String name = getServletName();
  168 | 		return servletContext.addServlet(name, this.servlet);
  169 | 	}
  170 | 
  171 | 	/**
  172 | 	 * Configure registration settings. Subclasses can override this method to perform
  173 | 	 * additional configuration if required.
  174 | 	 * @param registration the registration
  175 | 	 */
  176 | 	@Override
  177 | 	protected void configure(ServletRegistration.Dynamic registration) {
  178 | 		super.configure(registration);
  179 | 		String[] urlMapping = StringUtils.toStringArray(this.urlMappings);
  180 | 		if (urlMapping.length == 0 && this.alwaysMapUrl) {
  181 | 			urlMapping = DEFAULT_MAPPINGS;
  182 | 		}
  183 | 		if (!ObjectUtils.isEmpty(urlMapping)) {
  184 | 			registration.addMapping(urlMapping);
  185 | 		}
  186 | 		registration.setLoadOnStartup(this.loadOnStartup);
  187 | 		if (this.multipartConfig != null) {
  188 | 			registration.setMultipartConfig(this.multipartConfig);
  189 | 		}
  190 | 	}
  191 | 
  192 | 	/**
  193 | 	 * Returns the servlet name that will be registered.
  194 | 	 * @return the servlet name
  195 | 	 */
  196 | 	public String getServletName() {
  197 | 		return getOrDeduceName(this.servlet);
  198 | 	}
  199 | 
  200 | 	@Override
  201 | 	public String toString() {
  202 | 		return getServletName() + " urls=" + getUrlMappings();
  203 | 	}
  204 | 
  205 | }
```
