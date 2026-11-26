<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/web/servlet/DelegatingFilterProxyRegistrationBean.java -->
<!-- 正文引用：[[源码:DelegatingFilterProxyRegistrationBean.java#L起-L止]] ；代码块头部：(src: DelegatingFilterProxyRegistrationBean.java#L起-L止) -->

# DelegatingFilterProxyRegistrationBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/web/servlet/DelegatingFilterProxyRegistrationBean.java` |
| 行数 | 93 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:30:50.898Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.servlet;
    6 | 
    7 | import jakarta.servlet.Filter;
    8 | import jakarta.servlet.ServletContext;
    9 | import jakarta.servlet.ServletException;
   10 | 
   11 | import org.springframework.beans.BeansException;
   12 | import org.springframework.context.ApplicationContext;
   13 | import org.springframework.context.ApplicationContextAware;
   14 | import org.springframework.util.Assert;
   15 | import org.springframework.web.context.WebApplicationContext;
   16 | import org.springframework.web.filter.DelegatingFilterProxy;
   17 | 
   18 | /**
   19 |  * A {@link ServletContextInitializer} to register {@link DelegatingFilterProxy}s in a
   20 |  * Servlet 3.0+ container. Similar to the {@link ServletContext#addFilter(String, Filter)
   21 |  * registration} features provided by {@link ServletContext} but with a Spring Bean
   22 |  * friendly design.
   23 |  * <p>
   24 |  * The bean name of the actual delegate {@link Filter} should be specified using the
   25 |  * {@code targetBeanName} constructor argument. Unlike the {@link FilterRegistrationBean},
   26 |  * referenced filters are not instantiated early. In fact, if the delegate filter bean is
   27 |  * marked {@code @Lazy} it won't be instantiated at all until the filter is called.
   28 |  * <p>
   29 |  * Registrations can be associated with {@link #setUrlPatterns URL patterns} and/or
   30 |  * servlets (either by {@link #setServletNames name} or through a
   31 |  * {@link #setServletRegistrationBeans ServletRegistrationBean}s). When no URL pattern or
   32 |  * servlets are specified the filter will be associated to '/*'. The targetBeanName will
   33 |  * be used as the filter name if not otherwise specified.
   34 |  *
   35 |  * @author Phillip Webb
   36 |  * @since 1.4.0
   37 |  * @see ServletContextInitializer
   38 |  * @see ServletContext#addFilter(String, Filter)
   39 |  * @see FilterRegistrationBean
   40 |  * @see DelegatingFilterProxy
   41 |  */
   42 | public class DelegatingFilterProxyRegistrationBean extends AbstractFilterRegistrationBean<DelegatingFilterProxy>
   43 | 		implements ApplicationContextAware {
   44 | 
   45 | 	@SuppressWarnings("NullAway.Init")
   46 | 	private ApplicationContext applicationContext;
   47 | 
   48 | 	private final String targetBeanName;
   49 | 
   50 | 	/**
   51 | 	 * Create a new {@link DelegatingFilterProxyRegistrationBean} instance to be
   52 | 	 * registered with the specified {@link ServletRegistrationBean}s.
   53 | 	 * @param targetBeanName name of the target filter bean to look up in the Spring
   54 | 	 * application context (must not be {@code null}).
   55 | 	 * @param servletRegistrationBeans associate {@link ServletRegistrationBean}s
   56 | 	 */
   57 | 	public DelegatingFilterProxyRegistrationBean(String targetBeanName,
   58 | 			ServletRegistrationBean<?>... servletRegistrationBeans) {
   59 | 		super(servletRegistrationBeans);
   60 | 		Assert.hasLength(targetBeanName, "'targetBeanName' must not be empty");
   61 | 		this.targetBeanName = targetBeanName;
   62 | 		setName(targetBeanName);
   63 | 	}
   64 | 
   65 | 	@Override
   66 | 	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
   67 | 		this.applicationContext = applicationContext;
   68 | 	}
   69 | 
   70 | 	protected String getTargetBeanName() {
   71 | 		return this.targetBeanName;
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	public DelegatingFilterProxy getFilter() {
   76 | 		return new DelegatingFilterProxy(this.targetBeanName, getWebApplicationContext()) {
   77 | 
   78 | 			@Override
   79 | 			protected void initFilterBean() throws ServletException {
   80 | 				// Don't initialize filter bean on init()
   81 | 			}
   82 | 
   83 | 		};
   84 | 	}
   85 | 
   86 | 	private WebApplicationContext getWebApplicationContext() {
   87 | 		Assert.state(this.applicationContext != null, "ApplicationContext has not been injected");
   88 | 		Assert.state(this.applicationContext instanceof WebApplicationContext,
   89 | 				"Injected ApplicationContext is not a WebApplicationContext");
   90 | 		return (WebApplicationContext) this.applicationContext;
   91 | 	}
   92 | 
   93 | }
```
