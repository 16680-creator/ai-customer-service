<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/PreAuthorizeExpressionAttributeRegistry.java -->
<!-- 正文引用：[[源码:PreAuthorizeExpressionAttributeRegistry.java#L起-L止]] ；代码块头部：(src: PreAuthorizeExpressionAttributeRegistry.java#L起-L止) -->

# PreAuthorizeExpressionAttributeRegistry.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/PreAuthorizeExpressionAttributeRegistry.java` |
| 行数 | 112 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.503Z |

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
   17 | package org.springframework.security.authorization.method;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.util.Arrays;
   21 | import java.util.function.Function;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.context.ApplicationContext;
   26 | import org.springframework.expression.Expression;
   27 | import org.springframework.security.access.prepost.PreAuthorize;
   28 | import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
   29 | import org.springframework.security.core.annotation.SecurityAnnotationScanner;
   30 | import org.springframework.security.core.annotation.SecurityAnnotationScanners;
   31 | import org.springframework.util.Assert;
   32 | 
   33 | /**
   34 |  * For internal use only, as this contract is likely to change.
   35 |  *
   36 |  * @author Evgeniy Cheban
   37 |  * @author DingHao
   38 |  * @since 5.8
   39 |  */
   40 | final class PreAuthorizeExpressionAttributeRegistry extends AbstractExpressionAttributeRegistry<ExpressionAttribute> {
   41 | 
   42 | 	private final MethodAuthorizationDeniedHandler defaultHandler = new ThrowingMethodAuthorizationDeniedHandler();
   43 | 
   44 | 	private final SecurityAnnotationScanner<HandleAuthorizationDenied> handleAuthorizationDeniedScanner = SecurityAnnotationScanners
   45 | 		.requireUnique(HandleAuthorizationDenied.class);
   46 | 
   47 | 	private Function<Class<? extends MethodAuthorizationDeniedHandler>, MethodAuthorizationDeniedHandler> handlerResolver;
   48 | 
   49 | 	private SecurityAnnotationScanner<PreAuthorize> preAuthorizeScanner = SecurityAnnotationScanners
   50 | 		.requireUnique(PreAuthorize.class);
   51 | 
   52 | 	PreAuthorizeExpressionAttributeRegistry() {
   53 | 		this.handlerResolver = (clazz) -> new ReflectiveMethodAuthorizationDeniedHandler(clazz,
   54 | 				PreAuthorizeAuthorizationManager.class);
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	@Nullable ExpressionAttribute resolveAttribute(Method method, @Nullable Class<?> targetClass) {
   59 | 		PreAuthorize preAuthorize = findPreAuthorizeAnnotation(method, targetClass);
   60 | 		if (preAuthorize == null) {
   61 | 			return null;
   62 | 		}
   63 | 		Expression expression = getExpressionHandler().getExpressionParser().parseExpression(preAuthorize.value());
   64 | 		MethodAuthorizationDeniedHandler handler = resolveHandler(method, targetClass);
   65 | 		return new PreAuthorizeExpressionAttribute(expression, handler);
   66 | 	}
   67 | 
   68 | 	private MethodAuthorizationDeniedHandler resolveHandler(Method method, @Nullable Class<?> targetClass) {
   69 | 		Class<?> targetClassToUse = targetClass(method, targetClass);
   70 | 		HandleAuthorizationDenied deniedHandler = this.handleAuthorizationDeniedScanner.scan(method, targetClassToUse);
   71 | 		if (deniedHandler != null) {
   72 | 			return this.handlerResolver.apply(deniedHandler.handlerClass());
   73 | 		}
   74 | 		return this.defaultHandler;
   75 | 	}
   76 | 
   77 | 	private @Nullable PreAuthorize findPreAuthorizeAnnotation(Method method, @Nullable Class<?> targetClass) {
   78 | 		Class<?> targetClassToUse = targetClass(method, targetClass);
   79 | 		return this.preAuthorizeScanner.scan(method, targetClassToUse);
   80 | 	}
   81 | 
   82 | 	/**
   83 | 	 * Uses the provided {@link ApplicationContext} to resolve the
   84 | 	 * {@link MethodAuthorizationDeniedHandler} from {@link PreAuthorize}.
   85 | 	 * @param context the {@link ApplicationContext} to use
   86 | 	 */
   87 | 	void setApplicationContext(ApplicationContext context) {
   88 | 		Assert.notNull(context, "context cannot be null");
   89 | 		this.handlerResolver = (clazz) -> resolveHandler(context, clazz);
   90 | 	}
   91 | 
   92 | 	void setTemplateDefaults(AnnotationTemplateExpressionDefaults defaults) {
   93 | 		this.preAuthorizeScanner = SecurityAnnotationScanners.requireUnique(PreAuthorize.class, defaults);
   94 | 	}
   95 | 
   96 | 	private MethodAuthorizationDeniedHandler resolveHandler(ApplicationContext context,
   97 | 			Class<? extends MethodAuthorizationDeniedHandler> handlerClass) {
   98 | 		if (handlerClass == this.defaultHandler.getClass()) {
   99 | 			return this.defaultHandler;
  100 | 		}
  101 | 		String[] beanNames = context.getBeanNamesForType(handlerClass);
  102 | 		if (beanNames.length == 0) {
  103 | 			throw new IllegalStateException("Could not find a bean of type " + handlerClass.getName());
  104 | 		}
  105 | 		if (beanNames.length > 1) {
  106 | 			throw new IllegalStateException("Expected to find a single bean of type " + handlerClass.getName()
  107 | 					+ " but found " + Arrays.toString(beanNames));
  108 | 		}
  109 | 		return context.getBean(beanNames[0], handlerClass);
  110 | 	}
  111 | 
  112 | }
```
