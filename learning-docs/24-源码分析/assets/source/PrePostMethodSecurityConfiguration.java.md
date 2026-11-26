<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/method/configuration/PrePostMethodSecurityConfiguration.java -->
<!-- 正文引用：[[源码:PrePostMethodSecurityConfiguration.java#L起-L止]] ；代码块头部：(src: PrePostMethodSecurityConfiguration.java#L起-L止) -->

# PrePostMethodSecurityConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/method/configuration/PrePostMethodSecurityConfiguration.java` |
| 行数 | 215 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:52.470Z |

```java
    1 | /*
    2 |  * Copyright 2004-present the original author or authors.
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
   17 | package org.springframework.security.config.annotation.method.configuration;
   18 | 
   19 | import org.aopalliance.intercept.MethodInterceptor;
   20 | import org.aopalliance.intercept.MethodInvocation;
   21 | 
   22 | import org.springframework.aop.Pointcut;
   23 | import org.springframework.aop.framework.AopInfrastructureBean;
   24 | import org.springframework.beans.BeansException;
   25 | import org.springframework.beans.factory.ObjectProvider;
   26 | import org.springframework.beans.factory.annotation.Autowired;
   27 | import org.springframework.beans.factory.config.BeanDefinition;
   28 | import org.springframework.context.ApplicationContext;
   29 | import org.springframework.context.ApplicationContextAware;
   30 | import org.springframework.context.annotation.Bean;
   31 | import org.springframework.context.annotation.Configuration;
   32 | import org.springframework.context.annotation.ImportAware;
   33 | import org.springframework.context.annotation.Role;
   34 | import org.springframework.core.type.AnnotationMetadata;
   35 | import org.springframework.security.access.PermissionEvaluator;
   36 | import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
   37 | import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
   38 | import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
   39 | import org.springframework.security.aot.hint.PrePostAuthorizeHintsRegistrar;
   40 | import org.springframework.security.aot.hint.SecurityHintsRegistrar;
   41 | import org.springframework.security.authorization.AuthorizationEventPublisher;
   42 | import org.springframework.security.authorization.AuthorizationManager;
   43 | import org.springframework.security.authorization.AuthorizationManagerFactory;
   44 | import org.springframework.security.authorization.method.AuthorizationManagerAfterMethodInterceptor;
   45 | import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
   46 | import org.springframework.security.authorization.method.MethodInvocationResult;
   47 | import org.springframework.security.authorization.method.PostAuthorizeAuthorizationManager;
   48 | import org.springframework.security.authorization.method.PostFilterAuthorizationMethodInterceptor;
   49 | import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
   50 | import org.springframework.security.authorization.method.PreFilterAuthorizationMethodInterceptor;
   51 | import org.springframework.security.config.ObjectPostProcessor;
   52 | import org.springframework.security.config.core.GrantedAuthorityDefaults;
   53 | import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
   54 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   55 | 
   56 | /**
   57 |  * Base {@link Configuration} for enabling Spring Security Method Security.
   58 |  *
   59 |  * @author Evgeniy Cheban
   60 |  * @author Josh Cummings
   61 |  * @since 5.6
   62 |  * @see EnableMethodSecurity
   63 |  */
   64 | @Configuration(value = "_prePostMethodSecurityConfiguration", proxyBeanMethods = false)
   65 | @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
   66 | final class PrePostMethodSecurityConfiguration implements ImportAware, ApplicationContextAware, AopInfrastructureBean {
   67 | 
   68 | 	private static final Pointcut preFilterPointcut = new PreFilterAuthorizationMethodInterceptor().getPointcut();
   69 | 
   70 | 	private static final Pointcut preAuthorizePointcut = AuthorizationManagerBeforeMethodInterceptor.preAuthorize()
   71 | 		.getPointcut();
   72 | 
   73 | 	private static final Pointcut postAuthorizePointcut = AuthorizationManagerAfterMethodInterceptor.postAuthorize()
   74 | 		.getPointcut();
   75 | 
   76 | 	private static final Pointcut postFilterPointcut = new PostFilterAuthorizationMethodInterceptor().getPointcut();
   77 | 
   78 | 	private final PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();
   79 | 
   80 | 	private final PostAuthorizeAuthorizationManager postAuthorizeAuthorizationManager = new PostAuthorizeAuthorizationManager();
   81 | 
   82 | 	private final PreFilterAuthorizationMethodInterceptor preFilterMethodInterceptor = new PreFilterAuthorizationMethodInterceptor();
   83 | 
   84 | 	private final AuthorizationManagerBeforeMethodInterceptor preAuthorizeMethodInterceptor;
   85 | 
   86 | 	private final AuthorizationManagerAfterMethodInterceptor postAuthorizeMethodInterceptor;
   87 | 
   88 | 	private final PostFilterAuthorizationMethodInterceptor postFilterMethodInterceptor = new PostFilterAuthorizationMethodInterceptor();
   89 | 
   90 | 	private final DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
   91 | 
   92 | 	PrePostMethodSecurityConfiguration(
   93 | 			ObjectProvider<ObjectPostProcessor<AuthorizationManager<MethodInvocation>>> preAuthorizeProcessor,
   94 | 			ObjectProvider<ObjectPostProcessor<AuthorizationManager<MethodInvocationResult>>> postAuthorizeProcessor) {
   95 | 		this.preFilterMethodInterceptor.setExpressionHandler(this.expressionHandler);
   96 | 		this.preAuthorizeAuthorizationManager.setExpressionHandler(this.expressionHandler);
   97 | 		this.postAuthorizeAuthorizationManager.setExpressionHandler(this.expressionHandler);
   98 | 		this.postFilterMethodInterceptor.setExpressionHandler(this.expressionHandler);
   99 | 		AuthorizationManager<MethodInvocation> preAuthorize = preAuthorizeProcessor
  100 | 			.getIfUnique(ObjectPostProcessor::identity)
  101 | 			.postProcess(this.preAuthorizeAuthorizationManager);
  102 | 		this.preAuthorizeMethodInterceptor = AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorize);
  103 | 		AuthorizationManager<MethodInvocationResult> postAuthorize = postAuthorizeProcessor
  104 | 			.getIfUnique(ObjectPostProcessor::identity)
  105 | 			.postProcess(this.postAuthorizeAuthorizationManager);
  106 | 		this.postAuthorizeMethodInterceptor = AuthorizationManagerAfterMethodInterceptor.postAuthorize(postAuthorize);
  107 | 	}
  108 | 
  109 | 	@Override
  110 | 	public void setApplicationContext(ApplicationContext context) throws BeansException {
  111 | 		this.expressionHandler.setApplicationContext(context);
  112 | 		this.preAuthorizeAuthorizationManager.setApplicationContext(context);
  113 | 		this.postAuthorizeAuthorizationManager.setApplicationContext(context);
  114 | 	}
  115 | 
  116 | 	@Autowired(required = false)
  117 | 	void setGrantedAuthorityDefaults(GrantedAuthorityDefaults grantedAuthorityDefaults) {
  118 | 		this.expressionHandler.setDefaultRolePrefix(grantedAuthorityDefaults.getRolePrefix());
  119 | 	}
  120 | 
  121 | 	@Autowired(required = false)
  122 | 	void setRoleHierarchy(RoleHierarchy roleHierarchy) {
  123 | 		this.expressionHandler.setRoleHierarchy(roleHierarchy);
  124 | 	}
  125 | 
  126 | 	@Autowired(required = false)
  127 | 	void setAuthorizationManagerFactory(AuthorizationManagerFactory<MethodInvocation> authorizationManagerFactory) {
  128 | 		this.expressionHandler.setAuthorizationManagerFactory(authorizationManagerFactory);
  129 | 	}
  130 | 
  131 | 	@Autowired(required = false)
  132 | 	void setPermissionEvaluator(PermissionEvaluator permissionEvaluator) {
  133 | 		this.expressionHandler.setPermissionEvaluator(permissionEvaluator);
  134 | 	}
  135 | 
  136 | 	@Autowired(required = false)
  137 | 	void setTemplateDefaults(AnnotationTemplateExpressionDefaults templateDefaults) {
  138 | 		this.preFilterMethodInterceptor.setTemplateDefaults(templateDefaults);
  139 | 		this.preAuthorizeAuthorizationManager.setTemplateDefaults(templateDefaults);
  140 | 		this.postAuthorizeAuthorizationManager.setTemplateDefaults(templateDefaults);
  141 | 		this.postFilterMethodInterceptor.setTemplateDefaults(templateDefaults);
  142 | 	}
  143 | 
  144 | 	@Autowired(required = false)
  145 | 	void setExpressionHandler(MethodSecurityExpressionHandler expressionHandler) {
  146 | 		this.preFilterMethodInterceptor.setExpressionHandler(expressionHandler);
  147 | 		this.preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);
  148 | 		this.postAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);
  149 | 		this.postFilterMethodInterceptor.setExpressionHandler(expressionHandler);
  150 | 	}
  151 | 
  152 | 	@Autowired(required = false)
  153 | 	void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
  154 | 		this.preFilterMethodInterceptor.setSecurityContextHolderStrategy(securityContextHolderStrategy);
  155 | 		this.preAuthorizeMethodInterceptor.setSecurityContextHolderStrategy(securityContextHolderStrategy);
  156 | 		this.postAuthorizeMethodInterceptor.setSecurityContextHolderStrategy(securityContextHolderStrategy);
  157 | 		this.postFilterMethodInterceptor.setSecurityContextHolderStrategy(securityContextHolderStrategy);
  158 | 	}
  159 | 
  160 | 	@Autowired(required = false)
  161 | 	void setAuthorizationEventPublisher(AuthorizationEventPublisher publisher) {
  162 | 		this.preAuthorizeMethodInterceptor.setAuthorizationEventPublisher(publisher);
  163 | 		this.postAuthorizeMethodInterceptor.setAuthorizationEventPublisher(publisher);
  164 | 	}
  165 | 
  166 | 	@Bean
  167 | 	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  168 | 	static MethodInterceptor preFilterAuthorizationMethodInterceptor(
  169 | 			ObjectProvider<PrePostMethodSecurityConfiguration> _prePostMethodSecurityConfiguration) {
  170 | 		return new DeferringMethodInterceptor<>(preFilterPointcut,
  171 | 				() -> _prePostMethodSecurityConfiguration.getObject().preFilterMethodInterceptor);
  172 | 	}
  173 | 
  174 | 	@Bean
  175 | 	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  176 | 	static MethodInterceptor preAuthorizeAuthorizationMethodInterceptor(
  177 | 			ObjectProvider<PrePostMethodSecurityConfiguration> _prePostMethodSecurityConfiguration) {
  178 | 		return new DeferringMethodInterceptor<>(preAuthorizePointcut,
  179 | 				() -> _prePostMethodSecurityConfiguration.getObject().preAuthorizeMethodInterceptor);
  180 | 	}
  181 | 
  182 | 	@Bean
  183 | 	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  184 | 	static MethodInterceptor postAuthorizeAuthorizationMethodInterceptor(
  185 | 			ObjectProvider<PrePostMethodSecurityConfiguration> _prePostMethodSecurityConfiguration) {
  186 | 		return new DeferringMethodInterceptor<>(postAuthorizePointcut,
  187 | 				() -> _prePostMethodSecurityConfiguration.getObject().postAuthorizeMethodInterceptor);
  188 | 	}
  189 | 
  190 | 	@Bean
  191 | 	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  192 | 	static MethodInterceptor postFilterAuthorizationMethodInterceptor(
  193 | 			ObjectProvider<PrePostMethodSecurityConfiguration> _prePostMethodSecurityConfiguration) {
  194 | 		return new DeferringMethodInterceptor<>(postFilterPointcut,
  195 | 				() -> _prePostMethodSecurityConfiguration.getObject().postFilterMethodInterceptor);
  196 | 	}
  197 | 
  198 | 	@Bean
  199 | 	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  200 | 	static SecurityHintsRegistrar prePostAuthorizeExpressionHintsRegistrar() {
  201 | 		return new PrePostAuthorizeHintsRegistrar();
  202 | 	}
  203 | 
  204 | 	@Override
  205 | 	public void setImportMetadata(AnnotationMetadata importMetadata) {
  206 | 		EnableMethodSecurity annotation = importMetadata.getAnnotations().get(EnableMethodSecurity.class).synthesize();
  207 | 		this.preFilterMethodInterceptor.setOrder(this.preFilterMethodInterceptor.getOrder() + annotation.offset());
  208 | 		this.preAuthorizeMethodInterceptor
  209 | 			.setOrder(this.preAuthorizeMethodInterceptor.getOrder() + annotation.offset());
  210 | 		this.postAuthorizeMethodInterceptor
  211 | 			.setOrder(this.postAuthorizeMethodInterceptor.getOrder() + annotation.offset());
  212 | 		this.postFilterMethodInterceptor.setOrder(this.postFilterMethodInterceptor.getOrder() + annotation.offset());
  213 | 	}
  214 | 
  215 | }
```
