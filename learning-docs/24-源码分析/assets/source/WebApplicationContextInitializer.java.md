<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/web/context/servlet/WebApplicationContextInitializer.java -->
<!-- 正文引用：[[源码:WebApplicationContextInitializer.java#L起-L止]] ；代码块头部：(src: WebApplicationContextInitializer.java#L起-L止) -->

# WebApplicationContextInitializer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/web/context/servlet/WebApplicationContextInitializer.java` |
| 行数 | 82 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:08:14.683Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.context.servlet;
    6 | 
    7 | import jakarta.servlet.ServletContext;
    8 | import jakarta.servlet.ServletException;
    9 | import org.apache.commons.logging.Log;
   10 | import org.apache.commons.logging.LogFactory;
   11 | 
   12 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   13 | import org.springframework.boot.web.servlet.ServletContextInitializer;
   14 | import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
   15 | import org.springframework.web.context.ConfigurableWebApplicationContext;
   16 | import org.springframework.web.context.WebApplicationContext;
   17 | import org.springframework.web.context.support.ServletContextScope;
   18 | import org.springframework.web.context.support.WebApplicationContextUtils;
   19 | 
   20 | /**
   21 |  * Common initialization logic for Servlet web applications.
   22 |  *
   23 |  * @author Andy Wilkinson
   24 |  * @since 4.0.0
   25 |  */
   26 | public class WebApplicationContextInitializer {
   27 | 
   28 | 	private static final Log logger = LogFactory.getLog(WebApplicationContextInitializer.class);
   29 | 
   30 | 	private final ConfigurableWebApplicationContext context;
   31 | 
   32 | 	public WebApplicationContextInitializer(ConfigurableWebApplicationContext context) {
   33 | 		this.context = context;
   34 | 	}
   35 | 
   36 | 	public void initialize(ServletContext servletContext) throws ServletException {
   37 | 		prepareWebApplicationContext(servletContext);
   38 | 		registerApplicationScope(servletContext, this.context.getBeanFactory());
   39 | 		WebApplicationContextUtils.registerEnvironmentBeans(this.context.getBeanFactory(), servletContext);
   40 | 		for (ServletContextInitializer initializerBean : new ServletContextInitializerBeans(
   41 | 				this.context.getBeanFactory())) {
   42 | 			initializerBean.onStartup(servletContext);
   43 | 		}
   44 | 	}
   45 | 
   46 | 	private void prepareWebApplicationContext(ServletContext servletContext) {
   47 | 		Object rootContext = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
   48 | 		if (rootContext != null) {
   49 | 			if (rootContext == this) {
   50 | 				throw new IllegalStateException(
   51 | 						"Cannot initialize context because there is already a root application context present - "
   52 | 								+ "check whether you have multiple ServletContextInitializers!");
   53 | 			}
   54 | 			return;
   55 | 		}
   56 | 		try {
   57 | 			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
   58 | 			if (logger.isDebugEnabled()) {
   59 | 				logger.debug("Published root WebApplicationContext as ServletContext attribute with name ["
   60 | 						+ WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE + "]");
   61 | 			}
   62 | 			this.context.setServletContext(servletContext);
   63 | 			if (logger.isInfoEnabled()) {
   64 | 				long elapsedTime = System.currentTimeMillis() - this.context.getStartupDate();
   65 | 				logger.info("Root WebApplicationContext: initialization completed in " + elapsedTime + " ms");
   66 | 			}
   67 | 		}
   68 | 		catch (RuntimeException | Error ex) {
   69 | 			logger.error("Context initialization failed", ex);
   70 | 			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
   71 | 			throw ex;
   72 | 		}
   73 | 	}
   74 | 
   75 | 	private void registerApplicationScope(ServletContext servletContext, ConfigurableListableBeanFactory beanFactory) {
   76 | 		ServletContextScope appScope = new ServletContextScope(servletContext);
   77 | 		beanFactory.registerScope(WebApplicationContext.SCOPE_APPLICATION, appScope);
   78 | 		// Register as ServletContext attribute, for ContextCleanupListener to detect it.
   79 | 		servletContext.setAttribute(ServletContextScope.class.getName(), appScope);
   80 | 	}
   81 | 
   82 | }
```
