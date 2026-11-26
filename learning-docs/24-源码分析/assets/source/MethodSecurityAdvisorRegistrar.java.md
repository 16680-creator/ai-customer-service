<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/method/configuration/MethodSecurityAdvisorRegistrar.java -->
<!-- 正文引用：[[源码:MethodSecurityAdvisorRegistrar.java#L起-L止]] ；代码块头部：(src: MethodSecurityAdvisorRegistrar.java#L起-L止) -->

# MethodSecurityAdvisorRegistrar.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/method/configuration/MethodSecurityAdvisorRegistrar.java` |
| 行数 | 109 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:41.176Z |

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
   19 | import org.aopalliance.aop.Advice;
   20 | import org.aopalliance.intercept.MethodInterceptor;
   21 | import org.aopalliance.intercept.MethodInvocation;
   22 | 
   23 | import org.springframework.aop.Advisor;
   24 | import org.springframework.aop.Pointcut;
   25 | import org.springframework.aop.PointcutAdvisor;
   26 | import org.springframework.aop.framework.AopInfrastructureBean;
   27 | import org.springframework.beans.factory.config.BeanDefinition;
   28 | import org.springframework.beans.factory.support.BeanDefinitionBuilder;
   29 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   30 | import org.springframework.beans.factory.support.RootBeanDefinition;
   31 | import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
   32 | import org.springframework.core.Ordered;
   33 | import org.springframework.core.type.AnnotationMetadata;
   34 | import org.springframework.lang.NonNull;
   35 | import org.springframework.lang.Nullable;
   36 | import org.springframework.security.authorization.method.AuthorizationAdvisor;
   37 | 
   38 | class MethodSecurityAdvisorRegistrar implements ImportBeanDefinitionRegistrar {
   39 | 
   40 | 	@Override
   41 | 	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
   42 | 		registerAsAdvisor("preFilterAuthorization", registry);
   43 | 		registerAsAdvisor("preAuthorizeAuthorization", registry);
   44 | 		registerAsAdvisor("postFilterAuthorization", registry);
   45 | 		registerAsAdvisor("postAuthorizeAuthorization", registry);
   46 | 		registerAsAdvisor("securedAuthorization", registry);
   47 | 		registerAsAdvisor("jsr250Authorization", registry);
   48 | 		registerAsAdvisor("authorizeReturnObject", registry);
   49 | 	}
   50 | 
   51 | 	private void registerAsAdvisor(String prefix, BeanDefinitionRegistry registry) {
   52 | 		String advisorName = prefix + "Advisor";
   53 | 		if (registry.containsBeanDefinition(advisorName)) {
   54 | 			return;
   55 | 		}
   56 | 		String interceptorName = prefix + "MethodInterceptor";
   57 | 		if (!registry.containsBeanDefinition(interceptorName)) {
   58 | 			return;
   59 | 		}
   60 | 		BeanDefinition definition = registry.getBeanDefinition(interceptorName);
   61 | 		if (!(definition instanceof RootBeanDefinition)) {
   62 | 			return;
   63 | 		}
   64 | 		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AdvisorWrapper.class);
   65 | 		builder.setFactoryMethod("of");
   66 | 		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
   67 | 		builder.addConstructorArgReference(interceptorName);
   68 | 		RootBeanDefinition advisor = (RootBeanDefinition) builder.getBeanDefinition();
   69 | 		advisor.setTargetType(Advisor.class);
   70 | 		registry.registerBeanDefinition(advisorName, advisor);
   71 | 	}
   72 | 
   73 | 	public static final class AdvisorWrapper
   74 | 			implements PointcutAdvisor, MethodInterceptor, Ordered, AopInfrastructureBean {
   75 | 
   76 | 		private final AuthorizationAdvisor advisor;
   77 | 
   78 | 		private AdvisorWrapper(AuthorizationAdvisor advisor) {
   79 | 			this.advisor = advisor;
   80 | 		}
   81 | 
   82 | 		public static AdvisorWrapper of(AuthorizationAdvisor advisor) {
   83 | 			return new AdvisorWrapper(advisor);
   84 | 		}
   85 | 
   86 | 		@Override
   87 | 		public Advice getAdvice() {
   88 | 			return this.advisor.getAdvice();
   89 | 		}
   90 | 
   91 | 		@Override
   92 | 		public Pointcut getPointcut() {
   93 | 			return this.advisor.getPointcut();
   94 | 		}
   95 | 
   96 | 		@Override
   97 | 		public int getOrder() {
   98 | 			return this.advisor.getOrder();
   99 | 		}
  100 | 
  101 | 		@Nullable
  102 | 		@Override
  103 | 		public Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
  104 | 			return this.advisor.invoke(invocation);
  105 | 		}
  106 | 
  107 | 	}
  108 | 
  109 | }
```
