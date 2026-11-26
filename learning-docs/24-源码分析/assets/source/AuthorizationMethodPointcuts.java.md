<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/AuthorizationMethodPointcuts.java -->
<!-- 正文引用：[[源码:AuthorizationMethodPointcuts.java#L起-L止]] ；代码块头部：(src: AuthorizationMethodPointcuts.java#L起-L止) -->

# AuthorizationMethodPointcuts.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/AuthorizationMethodPointcuts.java` |
| 行数 | 70 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.445Z |

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
   19 | import java.lang.annotation.Annotation;
   20 | import java.util.Arrays;
   21 | 
   22 | import org.springframework.aop.Pointcut;
   23 | import org.springframework.aop.support.ComposablePointcut;
   24 | import org.springframework.aop.support.Pointcuts;
   25 | import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
   26 | import org.springframework.security.access.prepost.PostAuthorize;
   27 | import org.springframework.security.access.prepost.PostFilter;
   28 | import org.springframework.security.access.prepost.PreAuthorize;
   29 | import org.springframework.security.access.prepost.PreFilter;
   30 | 
   31 | /**
   32 |  * Provides {@link Pointcut}s for Spring Security's method authorization annotations.
   33 |  *
   34 |  * @author Josh Cummings
   35 |  * @author Evgeniy Cheban
   36 |  */
   37 | final class AuthorizationMethodPointcuts {
   38 | 
   39 | 	static Pointcut forAllAnnotations() {
   40 | 		return forAnnotations(PreFilter.class, PreAuthorize.class, PostFilter.class, PostAuthorize.class);
   41 | 	}
   42 | 
   43 | 	@SafeVarargs
   44 | 	static Pointcut forAnnotations(Class<? extends Annotation>... annotations) {
   45 | 		ComposablePointcut pointcut = null;
   46 | 		for (Class<? extends Annotation> annotation : annotations) {
   47 | 			if (pointcut == null) {
   48 | 				pointcut = new ComposablePointcut(classOrMethod(annotation));
   49 | 			}
   50 | 			else {
   51 | 				pointcut.union(classOrMethod(annotation));
   52 | 			}
   53 | 		}
   54 | 		if (pointcut == null) {
   55 | 			throw new IllegalStateException(
   56 | 					"Unable to find a pointcut for annotations " + Arrays.toString(annotations));
   57 | 		}
   58 | 		return pointcut;
   59 | 	}
   60 | 
   61 | 	private static Pointcut classOrMethod(Class<? extends Annotation> annotation) {
   62 | 		return Pointcuts.union(new AnnotationMatchingPointcut(null, annotation, true),
   63 | 				new AnnotationMatchingPointcut(annotation, true));
   64 | 	}
   65 | 
   66 | 	private AuthorizationMethodPointcuts() {
   67 | 
   68 | 	}
   69 | 
   70 | }
```
