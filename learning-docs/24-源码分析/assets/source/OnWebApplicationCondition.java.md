<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/condition/OnWebApplicationCondition.java -->
<!-- 正文引用：[[源码:OnWebApplicationCondition.java#L起-L止]] ；代码块头部：(src: OnWebApplicationCondition.java#L起-L止) -->

# OnWebApplicationCondition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/condition/OnWebApplicationCondition.java` |
| 行数 | 161 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:47.849Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.autoconfigure.condition;
    6 | 
    7 | import java.util.Map;
    8 | 
    9 | import org.jspecify.annotations.Nullable;
   10 | 
   11 | import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
   12 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
   13 | import org.springframework.boot.web.context.reactive.ConfigurableReactiveWebEnvironment;
   14 | import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;
   15 | import org.springframework.context.annotation.Condition;
   16 | import org.springframework.context.annotation.ConditionContext;
   17 | import org.springframework.core.Ordered;
   18 | import org.springframework.core.annotation.Order;
   19 | import org.springframework.core.type.AnnotatedTypeMetadata;
   20 | import org.springframework.util.Assert;
   21 | import org.springframework.util.ClassUtils;
   22 | import org.springframework.util.ObjectUtils;
   23 | import org.springframework.web.context.ConfigurableWebEnvironment;
   24 | import org.springframework.web.context.WebApplicationContext;
   25 | 
   26 | /**
   27 |  * {@link Condition} that checks for the presence or absence of
   28 |  * {@link WebApplicationContext}.
   29 |  *
   30 |  * @author Dave Syer
   31 |  * @author Phillip Webb
   32 |  * @see ConditionalOnWebApplication
   33 |  * @see ConditionalOnNotWebApplication
   34 |  */
   35 | @Order(Ordered.HIGHEST_PRECEDENCE + 20)
   36 | class OnWebApplicationCondition extends FilteringSpringBootCondition {
   37 | 
   38 | 	private static final String SERVLET_WEB_APPLICATION_CLASS = "org.springframework.web.context.support.GenericWebApplicationContext";
   39 | 
   40 | 	private static final String REACTIVE_WEB_APPLICATION_CLASS = "org.springframework.web.reactive.HandlerResult";
   41 | 
   42 | 	@Override
   43 | 	protected @Nullable ConditionOutcome[] getOutcomes(@Nullable String[] autoConfigurationClasses,
   44 | 			AutoConfigurationMetadata autoConfigurationMetadata) {
   45 | 		@Nullable ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
   46 | 		for (int i = 0; i < outcomes.length; i++) {
   47 | 			String autoConfigurationClass = autoConfigurationClasses[i];
   48 | 			if (autoConfigurationClass != null) {
   49 | 				outcomes[i] = getOutcome(
   50 | 						autoConfigurationMetadata.get(autoConfigurationClass, "ConditionalOnWebApplication"));
   51 | 			}
   52 | 		}
   53 | 		return outcomes;
   54 | 	}
   55 | 
   56 | 	private @Nullable ConditionOutcome getOutcome(@Nullable String type) {
   57 | 		if (type == null) {
   58 | 			return null;
   59 | 		}
   60 | 		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnWebApplication.class);
   61 | 		ClassNameFilter missingClassFilter = ClassNameFilter.MISSING;
   62 | 		if (ConditionalOnWebApplication.Type.SERVLET.name().equals(type)) {
   63 | 			if (missingClassFilter.matches(SERVLET_WEB_APPLICATION_CLASS, getBeanClassLoader())) {
   64 | 				return ConditionOutcome.noMatch(message.didNotFind("servlet web application classes").atAll());
   65 | 			}
   66 | 		}
   67 | 		if (ConditionalOnWebApplication.Type.REACTIVE.name().equals(type)) {
   68 | 			if (missingClassFilter.matches(REACTIVE_WEB_APPLICATION_CLASS, getBeanClassLoader())) {
   69 | 				return ConditionOutcome.noMatch(message.didNotFind("reactive web application classes").atAll());
   70 | 			}
   71 | 		}
   72 | 		if (missingClassFilter.matches(SERVLET_WEB_APPLICATION_CLASS, getBeanClassLoader())
   73 | 				&& !ClassUtils.isPresent(REACTIVE_WEB_APPLICATION_CLASS, getBeanClassLoader())) {
   74 | 			return ConditionOutcome.noMatch(message.didNotFind("reactive or servlet web application classes").atAll());
   75 | 		}
   76 | 		return null;
   77 | 	}
   78 | 
   79 | 	@Override
   80 | 	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
   81 | 		boolean required = metadata.isAnnotated(ConditionalOnWebApplication.class.getName());
   82 | 		ConditionOutcome outcome = isWebApplication(context, metadata, required);
   83 | 		if (required && !outcome.isMatch()) {
   84 | 			return ConditionOutcome.noMatch(outcome.getConditionMessage());
   85 | 		}
   86 | 		if (!required && outcome.isMatch()) {
   87 | 			return ConditionOutcome.noMatch(outcome.getConditionMessage());
   88 | 		}
   89 | 		return ConditionOutcome.match(outcome.getConditionMessage());
   90 | 	}
   91 | 
   92 | 	private ConditionOutcome isWebApplication(ConditionContext context, AnnotatedTypeMetadata metadata,
   93 | 			boolean required) {
   94 | 		return switch (deduceType(metadata)) {
   95 | 			case SERVLET -> isServletWebApplication(context);
   96 | 			case REACTIVE -> isReactiveWebApplication(context);
   97 | 			default -> isAnyWebApplication(context, required);
   98 | 		};
   99 | 	}
  100 | 
  101 | 	private ConditionOutcome isAnyWebApplication(ConditionContext context, boolean required) {
  102 | 		ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnWebApplication.class,
  103 | 				required ? "(required)" : "");
  104 | 		ConditionOutcome servletOutcome = isServletWebApplication(context);
  105 | 		if (servletOutcome.isMatch() && required) {
  106 | 			return new ConditionOutcome(servletOutcome.isMatch(), message.because(servletOutcome.getMessage()));
  107 | 		}
  108 | 		ConditionOutcome reactiveOutcome = isReactiveWebApplication(context);
  109 | 		if (reactiveOutcome.isMatch() && required) {
  110 | 			return new ConditionOutcome(reactiveOutcome.isMatch(), message.because(reactiveOutcome.getMessage()));
  111 | 		}
  112 | 		return new ConditionOutcome(servletOutcome.isMatch() || reactiveOutcome.isMatch(),
  113 | 				message.because(servletOutcome.getMessage()).append("and").append(reactiveOutcome.getMessage()));
  114 | 	}
  115 | 
  116 | 	private ConditionOutcome isServletWebApplication(ConditionContext context) {
  117 | 		ConditionMessage.Builder message = ConditionMessage.forCondition("");
  118 | 		if (ClassNameFilter.MISSING.matches(SERVLET_WEB_APPLICATION_CLASS, context.getClassLoader())) {
  119 | 			return ConditionOutcome.noMatch(message.didNotFind("servlet web application classes").atAll());
  120 | 		}
  121 | 		if (context.getBeanFactory() != null) {
  122 | 			String[] scopes = context.getBeanFactory().getRegisteredScopeNames();
  123 | 			if (ObjectUtils.containsElement(scopes, "session")) {
  124 | 				return ConditionOutcome.match(message.foundExactly("'session' scope"));
  125 | 			}
  126 | 		}
  127 | 		if (context.getEnvironment() instanceof ConfigurableWebEnvironment) {
  128 | 			return ConditionOutcome.match(message.foundExactly("ConfigurableWebEnvironment"));
  129 | 		}
  130 | 		if (context.getResourceLoader() instanceof WebApplicationContext) {
  131 | 			return ConditionOutcome.match(message.foundExactly("WebApplicationContext"));
  132 | 		}
  133 | 		return ConditionOutcome.noMatch(message.because("not a servlet web application"));
  134 | 	}
  135 | 
  136 | 	private ConditionOutcome isReactiveWebApplication(ConditionContext context) {
  137 | 		ConditionMessage.Builder message = ConditionMessage.forCondition("");
  138 | 		if (ClassNameFilter.MISSING.matches(REACTIVE_WEB_APPLICATION_CLASS, context.getClassLoader())) {
  139 | 			return ConditionOutcome.noMatch(message.didNotFind("reactive web application classes").atAll());
  140 | 		}
  141 | 		if (context.getEnvironment() instanceof ConfigurableReactiveWebEnvironment) {
  142 | 			return ConditionOutcome.match(message.foundExactly("ConfigurableReactiveWebEnvironment"));
  143 | 		}
  144 | 		if (context.getResourceLoader() instanceof ReactiveWebApplicationContext) {
  145 | 			return ConditionOutcome.match(message.foundExactly("ReactiveWebApplicationContext"));
  146 | 		}
  147 | 		return ConditionOutcome.noMatch(message.because("not a reactive web application"));
  148 | 	}
  149 | 
  150 | 	private Type deduceType(AnnotatedTypeMetadata metadata) {
  151 | 		Map<String, @Nullable Object> attributes = metadata
  152 | 			.getAnnotationAttributes(ConditionalOnWebApplication.class.getName());
  153 | 		if (attributes != null) {
  154 | 			Object type = attributes.get("type");
  155 | 			Assert.state(type != null, "'type' must not be null");
  156 | 			return (Type) type;
  157 | 		}
  158 | 		return Type.ANY;
  159 | 	}
  160 | 
  161 | }
```
