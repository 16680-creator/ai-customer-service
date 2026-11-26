<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc:4.2.0 | path: org/springframework/boot/webmvc/autoconfigure/DispatcherServletAutoConfiguration.java -->
<!-- 正文引用：[[源码:DispatcherServletAutoConfiguration.java#L起-L止]] ；代码块头部：(src: DispatcherServletAutoConfiguration.java#L起-L止) -->

# DispatcherServletAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/autoconfigure/DispatcherServletAutoConfiguration.java` |
| 行数 | 205 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc/4.2.0/spring-boot-webmvc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:01.693Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.autoconfigure;
    6 | 
    7 | import java.util.Arrays;
    8 | import java.util.List;
    9 | 
   10 | import jakarta.servlet.MultipartConfigElement;
   11 | import jakarta.servlet.ServletRegistration;
   12 | 
   13 | import org.springframework.beans.factory.ObjectProvider;
   14 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   15 | import org.springframework.boot.autoconfigure.AutoConfiguration;
   16 | import org.springframework.boot.autoconfigure.AutoConfigureOrder;
   17 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   18 | import org.springframework.boot.autoconfigure.condition.ConditionMessage;
   19 | import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
   20 | import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
   21 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   22 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   23 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   24 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
   25 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
   26 | import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
   27 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   28 | import org.springframework.boot.web.servlet.ServletRegistrationBean;
   29 | import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
   30 | import org.springframework.context.annotation.Bean;
   31 | import org.springframework.context.annotation.ConditionContext;
   32 | import org.springframework.context.annotation.Conditional;
   33 | import org.springframework.context.annotation.Configuration;
   34 | import org.springframework.context.annotation.Import;
   35 | import org.springframework.core.Ordered;
   36 | import org.springframework.core.annotation.Order;
   37 | import org.springframework.core.type.AnnotatedTypeMetadata;
   38 | import org.springframework.util.Assert;
   39 | import org.springframework.web.multipart.MultipartResolver;
   40 | import org.springframework.web.servlet.DispatcherServlet;
   41 | 
   42 | /**
   43 |  * {@link EnableAutoConfiguration Auto-configuration} for the Spring
   44 |  * {@link DispatcherServlet}. Should work for a standalone application where an embedded
   45 |  * web server is already present and also for a deployable application using
   46 |  * {@link SpringBootServletInitializer}.
   47 |  *
   48 |  * @author Phillip Webb
   49 |  * @author Dave Syer
   50 |  * @author Stephane Nicoll
   51 |  * @author Brian Clozel
   52 |  * @since 4.0.0
   53 |  */
   54 | @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
   55 | @AutoConfiguration
   56 | @ConditionalOnWebApplication(type = Type.SERVLET)
   57 | @ConditionalOnClass(DispatcherServlet.class)
   58 | public final class DispatcherServletAutoConfiguration {
   59 | 
   60 | 	/**
   61 | 	 * The bean name for a DispatcherServlet that will be mapped to the root URL "/".
   62 | 	 */
   63 | 	public static final String DEFAULT_DISPATCHER_SERVLET_BEAN_NAME = "dispatcherServlet";
   64 | 
   65 | 	/**
   66 | 	 * The bean name for a ServletRegistrationBean for the DispatcherServlet "/".
   67 | 	 */
   68 | 	public static final String DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME = "dispatcherServletRegistration";
   69 | 
   70 | 	@Configuration(proxyBeanMethods = false)
   71 | 	@Conditional(DefaultDispatcherServletCondition.class)
   72 | 	@ConditionalOnClass(ServletRegistration.class)
   73 | 	@EnableConfigurationProperties(WebMvcProperties.class)
   74 | 	protected static class DispatcherServletConfiguration {
   75 | 
   76 | 		@Bean(name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
   77 | 		DispatcherServlet dispatcherServlet(WebMvcProperties webMvcProperties) {
   78 | 			DispatcherServlet dispatcherServlet = new DispatcherServlet();
   79 | 			dispatcherServlet.setDispatchOptionsRequest(webMvcProperties.isDispatchOptionsRequest());
   80 | 			dispatcherServlet.setDispatchTraceRequest(webMvcProperties.isDispatchTraceRequest());
   81 | 			dispatcherServlet.setPublishEvents(webMvcProperties.isPublishRequestHandledEvents());
   82 | 			dispatcherServlet.setEnableLoggingRequestDetails(webMvcProperties.isLogRequestDetails());
   83 | 			return dispatcherServlet;
   84 | 		}
   85 | 
   86 | 		@Bean
   87 | 		@ConditionalOnBean(MultipartResolver.class)
   88 | 		@ConditionalOnMissingBean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)
   89 | 		MultipartResolver multipartResolver(MultipartResolver resolver) {
   90 | 			// Detect if the user has created a MultipartResolver but named it incorrectly
   91 | 			return resolver;
   92 | 		}
   93 | 
   94 | 	}
   95 | 
   96 | 	@Configuration(proxyBeanMethods = false)
   97 | 	@Conditional(DispatcherServletRegistrationCondition.class)
   98 | 	@ConditionalOnClass(ServletRegistration.class)
   99 | 	@EnableConfigurationProperties(WebMvcProperties.class)
  100 | 	@Import(DispatcherServletConfiguration.class)
  101 | 	protected static class DispatcherServletRegistrationConfiguration {
  102 | 
  103 | 		@Bean(name = DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
  104 | 		@ConditionalOnBean(value = DispatcherServlet.class, name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
  105 | 		DispatcherServletRegistrationBean dispatcherServletRegistration(DispatcherServlet dispatcherServlet,
  106 | 				WebMvcProperties webMvcProperties, ObjectProvider<MultipartConfigElement> multipartConfig) {
  107 | 			DispatcherServletRegistrationBean registration = new DispatcherServletRegistrationBean(dispatcherServlet,
  108 | 					webMvcProperties.getServlet().getPath());
  109 | 			registration.setName(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
  110 | 			registration.setLoadOnStartup(webMvcProperties.getServlet().getLoadOnStartup());
  111 | 			multipartConfig.ifAvailable(registration::setMultipartConfig);
  112 | 			return registration;
  113 | 		}
  114 | 
  115 | 	}
  116 | 
  117 | 	@Order(Ordered.LOWEST_PRECEDENCE - 10)
  118 | 	private static final class DefaultDispatcherServletCondition extends SpringBootCondition {
  119 | 
  120 | 		@Override
  121 | 		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
  122 | 			ConditionMessage.Builder message = ConditionMessage.forCondition("Default DispatcherServlet");
  123 | 			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
  124 | 			Assert.state(beanFactory != null, "'beanFactory' must not be null");
  125 | 			List<String> dispatchServletBeans = Arrays
  126 | 				.asList(beanFactory.getBeanNamesForType(DispatcherServlet.class, false, false));
  127 | 			if (dispatchServletBeans.contains(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)) {
  128 | 				return ConditionOutcome
  129 | 					.noMatch(message.found("dispatcher servlet bean").items(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME));
  130 | 			}
  131 | 			if (beanFactory.containsBean(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)) {
  132 | 				return ConditionOutcome
  133 | 					.noMatch(message.found("non dispatcher servlet bean").items(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME));
  134 | 			}
  135 | 			if (dispatchServletBeans.isEmpty()) {
  136 | 				return ConditionOutcome.match(message.didNotFind("dispatcher servlet beans").atAll());
  137 | 			}
  138 | 			return ConditionOutcome.match(message.found("dispatcher servlet bean", "dispatcher servlet beans")
  139 | 				.items(Style.QUOTE, dispatchServletBeans)
  140 | 				.append("and none is named " + DEFAULT_DISPATCHER_SERVLET_BEAN_NAME));
  141 | 		}
  142 | 
  143 | 	}
  144 | 
  145 | 	@Order(Ordered.LOWEST_PRECEDENCE - 10)
  146 | 	private static final class DispatcherServletRegistrationCondition extends SpringBootCondition {
  147 | 
  148 | 		@Override
  149 | 		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
  150 | 			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
  151 | 			Assert.state(beanFactory != null, "'beanFactory' must not be null");
  152 | 			ConditionOutcome outcome = checkDefaultDispatcherName(beanFactory);
  153 | 			if (!outcome.isMatch()) {
  154 | 				return outcome;
  155 | 			}
  156 | 			return checkServletRegistration(beanFactory);
  157 | 		}
  158 | 
  159 | 		private ConditionOutcome checkDefaultDispatcherName(ConfigurableListableBeanFactory beanFactory) {
  160 | 			boolean containsDispatcherBean = beanFactory.containsBean(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
  161 | 			if (!containsDispatcherBean) {
  162 | 				return ConditionOutcome.match();
  163 | 			}
  164 | 			List<String> servlets = Arrays
  165 | 				.asList(beanFactory.getBeanNamesForType(DispatcherServlet.class, false, false));
  166 | 			if (!servlets.contains(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)) {
  167 | 				return ConditionOutcome.noMatch(
  168 | 						startMessage().found("non dispatcher servlet").items(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME));
  169 | 			}
  170 | 			return ConditionOutcome.match();
  171 | 		}
  172 | 
  173 | 		private ConditionOutcome checkServletRegistration(ConfigurableListableBeanFactory beanFactory) {
  174 | 			ConditionMessage.Builder message = startMessage();
  175 | 			List<String> registrations = Arrays
  176 | 				.asList(beanFactory.getBeanNamesForType(ServletRegistrationBean.class, false, false));
  177 | 			boolean containsDispatcherRegistrationBean = beanFactory
  178 | 				.containsBean(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME);
  179 | 			if (registrations.isEmpty()) {
  180 | 				if (containsDispatcherRegistrationBean) {
  181 | 					return ConditionOutcome.noMatch(message.found("non servlet registration bean")
  182 | 						.items(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME));
  183 | 				}
  184 | 				return ConditionOutcome.match(message.didNotFind("servlet registration bean").atAll());
  185 | 			}
  186 | 			if (registrations.contains(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)) {
  187 | 				return ConditionOutcome.noMatch(message.found("servlet registration bean")
  188 | 					.items(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME));
  189 | 			}
  190 | 			if (containsDispatcherRegistrationBean) {
  191 | 				return ConditionOutcome.noMatch(message.found("non servlet registration bean")
  192 | 					.items(DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME));
  193 | 			}
  194 | 			return ConditionOutcome.match(message.found("servlet registration beans")
  195 | 				.items(Style.QUOTE, registrations)
  196 | 				.append("and none is named " + DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME));
  197 | 		}
  198 | 
  199 | 		private ConditionMessage.Builder startMessage() {
  200 | 			return ConditionMessage.forCondition("DispatcherServlet Registration");
  201 | 		}
  202 | 
  203 | 	}
  204 | 
  205 | }
```
