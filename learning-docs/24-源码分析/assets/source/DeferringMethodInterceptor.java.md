<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/method/configuration/DeferringMethodInterceptor.java -->
<!-- 正文引用：[[源码:DeferringMethodInterceptor.java#L起-L止]] ；代码块头部：(src: DeferringMethodInterceptor.java#L起-L止) -->

# DeferringMethodInterceptor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/method/configuration/DeferringMethodInterceptor.java` |
| 行数 | 67 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:49:26.649Z |

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
   19 | import java.util.function.Supplier;
   20 | 
   21 | import org.aopalliance.aop.Advice;
   22 | import org.aopalliance.intercept.MethodInvocation;
   23 | 
   24 | import org.springframework.aop.Pointcut;
   25 | import org.springframework.lang.NonNull;
   26 | import org.springframework.lang.Nullable;
   27 | import org.springframework.security.authorization.method.AuthorizationAdvisor;
   28 | import org.springframework.util.function.SingletonSupplier;
   29 | 
   30 | final class DeferringMethodInterceptor<M extends AuthorizationAdvisor> implements AuthorizationAdvisor {
   31 | 
   32 | 	private final Pointcut pointcut;
   33 | 
   34 | 	private final Supplier<M> delegate;
   35 | 
   36 | 	DeferringMethodInterceptor(Pointcut pointcut, Supplier<M> delegate) {
   37 | 		this.pointcut = pointcut;
   38 | 		this.delegate = SingletonSupplier.of(delegate);
   39 | 	}
   40 | 
   41 | 	@Nullable
   42 | 	@Override
   43 | 	public Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
   44 | 		return this.delegate.get().invoke(invocation);
   45 | 	}
   46 | 
   47 | 	@Override
   48 | 	public Pointcut getPointcut() {
   49 | 		return this.pointcut;
   50 | 	}
   51 | 
   52 | 	@Override
   53 | 	public Advice getAdvice() {
   54 | 		return this;
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	public int getOrder() {
   59 | 		return this.delegate.get().getOrder();
   60 | 	}
   61 | 
   62 | 	@Override
   63 | 	public boolean isPerInstance() {
   64 | 		return true;
   65 | 	}
   66 | 
   67 | }
```
