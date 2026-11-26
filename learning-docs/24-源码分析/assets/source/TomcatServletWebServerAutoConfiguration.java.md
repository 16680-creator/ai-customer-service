<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-tomcat:4.2.0 | path: org/springframework/boot/tomcat/autoconfigure/servlet/TomcatServletWebServerAutoConfiguration.java -->
<!-- 正文引用：[[源码:TomcatServletWebServerAutoConfiguration.java#L起-L止]] ；代码块头部：(src: TomcatServletWebServerAutoConfiguration.java#L起-L止) -->

# TomcatServletWebServerAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-tomcat:4.2.0` |
| 路径 | `org/springframework/boot/tomcat/autoconfigure/servlet/TomcatServletWebServerAutoConfiguration.java` |
| 行数 | 83 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-tomcat/4.2.0/spring-boot-tomcat-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:01.400Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.tomcat.autoconfigure.servlet;
    6 | 
    7 | import jakarta.servlet.ServletRequest;
    8 | import org.apache.catalina.startup.Tomcat;
    9 | import org.apache.coyote.UpgradeProtocol;
   10 | 
   11 | import org.springframework.beans.factory.ObjectProvider;
   12 | import org.springframework.boot.autoconfigure.AutoConfiguration;
   13 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   14 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   15 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   16 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
   17 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
   18 | import org.springframework.boot.autoconfigure.condition.SearchStrategy;
   19 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   20 | import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
   21 | import org.springframework.boot.tomcat.TomcatContextCustomizer;
   22 | import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
   23 | import org.springframework.boot.tomcat.TomcatServerCustomizer;
   24 | import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
   25 | import org.springframework.boot.tomcat.autoconfigure.TomcatWebServerConfiguration;
   26 | import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
   27 | import org.springframework.boot.web.server.autoconfigure.ConditionalOnForwardedHeaderStrategy;
   28 | import org.springframework.boot.web.server.autoconfigure.ForwardedHeaderStrategy;
   29 | import org.springframework.boot.web.server.autoconfigure.ServerProperties;
   30 | import org.springframework.boot.web.server.autoconfigure.servlet.ForwardedHeaderFilterCustomizer;
   31 | import org.springframework.boot.web.server.autoconfigure.servlet.ServletWebServerConfiguration;
   32 | import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
   33 | import org.springframework.context.annotation.Bean;
   34 | import org.springframework.context.annotation.Import;
   35 | 
   36 | /**
   37 |  * {@link EnableAutoConfiguration Auto-configuration} for a Tomcat-based servlet web
   38 |  * server.
   39 |  *
   40 |  * @author Andy Wilkinson
   41 |  * @since 4.0.0
   42 |  */
   43 | @AutoConfiguration
   44 | @ConditionalOnClass({ ServletRequest.class, Tomcat.class, UpgradeProtocol.class, TomcatServletWebServerFactory.class })
   45 | @ConditionalOnWebApplication(type = Type.SERVLET)
   46 | @EnableConfigurationProperties(TomcatServerProperties.class)
   47 | @Import({ ServletWebServerConfiguration.class, TomcatWebServerConfiguration.class })
   48 | public final class TomcatServletWebServerAutoConfiguration {
   49 | 
   50 | 	private final TomcatServerProperties tomcatProperties;
   51 | 
   52 | 	TomcatServletWebServerAutoConfiguration(TomcatServerProperties tomcatProperties) {
   53 | 		this.tomcatProperties = tomcatProperties;
   54 | 	}
   55 | 
   56 | 	@Bean
   57 | 	@ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
   58 | 	TomcatServletWebServerFactory tomcatServletWebServerFactory(
   59 | 			ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
   60 | 			ObjectProvider<TomcatContextCustomizer> contextCustomizers,
   61 | 			ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers,
   62 | 			ObjectProvider<TomcatServerCustomizer> serverCustomizers) {
   63 | 		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
   64 | 		factory.getConnectorCustomizers().addAll(connectorCustomizers.orderedStream().toList());
   65 | 		factory.getContextCustomizers().addAll(contextCustomizers.orderedStream().toList());
   66 | 		factory.getProtocolHandlerCustomizers().addAll(protocolHandlerCustomizers.orderedStream().toList());
   67 | 		factory.getServerCustomizers().addAll(serverCustomizers.orderedStream().toList());
   68 | 		return factory;
   69 | 	}
   70 | 
   71 | 	@Bean
   72 | 	TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(
   73 | 			TomcatServerProperties tomcatProperties) {
   74 | 		return new TomcatServletWebServerFactoryCustomizer(tomcatProperties);
   75 | 	}
   76 | 
   77 | 	@Bean
   78 | 	@ConditionalOnForwardedHeaderStrategy(ForwardedHeaderStrategy.FRAMEWORK)
   79 | 	ForwardedHeaderFilterCustomizer tomcatForwardedHeaderFilterCustomizer(ServerProperties serverProperties) {
   80 | 		return (filter) -> filter.setRelativeRedirects(this.tomcatProperties.getServlet().isUseRelativeRedirects());
   81 | 	}
   82 | 
   83 | }
```
