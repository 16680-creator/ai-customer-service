<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/support/HandlerMethodArgumentResolverComposite.java -->
<!-- 正文引用：[[源码:HandlerMethodArgumentResolverComposite.java#L起-L止]] ；代码块头部：(src: HandlerMethodArgumentResolverComposite.java#L起-L止) -->

# HandlerMethodArgumentResolverComposite.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/support/HandlerMethodArgumentResolverComposite.java` |
| 行数 | 143 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:19.699Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.web.method.support;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collections;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.concurrent.ConcurrentHashMap;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.core.MethodParameter;
   28 | import org.springframework.web.bind.support.WebDataBinderFactory;
   29 | import org.springframework.web.context.request.NativeWebRequest;
   30 | 
   31 | /**
   32 |  * Resolves method parameters by delegating to a list of registered
   33 |  * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}.
   34 |  * Previously resolved method parameters are cached for faster lookups.
   35 |  *
   36 |  * @author Rossen Stoyanchev
   37 |  * @author Juergen Hoeller
   38 |  * @since 3.1
   39 |  */
   40 | public class HandlerMethodArgumentResolverComposite implements HandlerMethodArgumentResolver {
   41 | 
   42 | 	private final List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();
   43 | 
   44 | 	private final Map<MethodParameter, HandlerMethodArgumentResolver> argumentResolverCache =
   45 | 			new ConcurrentHashMap<>(256);
   46 | 
   47 | 
   48 | 	/**
   49 | 	 * Add the given {@link HandlerMethodArgumentResolver}.
   50 | 	 */
   51 | 	public HandlerMethodArgumentResolverComposite addResolver(HandlerMethodArgumentResolver resolver) {
   52 | 		this.argumentResolvers.add(resolver);
   53 | 		return this;
   54 | 	}
   55 | 
   56 | 	/**
   57 | 	 * Add the given {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}.
   58 | 	 * @since 4.3
   59 | 	 */
   60 | 	public HandlerMethodArgumentResolverComposite addResolvers(
   61 | 			@Nullable HandlerMethodArgumentResolver... resolvers) {
   62 | 
   63 | 		if (resolvers != null) {
   64 | 			Collections.addAll(this.argumentResolvers, resolvers);
   65 | 		}
   66 | 		return this;
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Add the given {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}.
   71 | 	 */
   72 | 	public HandlerMethodArgumentResolverComposite addResolvers(
   73 | 			@Nullable List<? extends HandlerMethodArgumentResolver> resolvers) {
   74 | 
   75 | 		if (resolvers != null) {
   76 | 			this.argumentResolvers.addAll(resolvers);
   77 | 		}
   78 | 		return this;
   79 | 	}
   80 | 
   81 | 	/**
   82 | 	 * Return a read-only list with the contained resolvers, or an empty list.
   83 | 	 */
   84 | 	public List<HandlerMethodArgumentResolver> getResolvers() {
   85 | 		return Collections.unmodifiableList(this.argumentResolvers);
   86 | 	}
   87 | 
   88 | 	/**
   89 | 	 * Clear the list of configured resolvers and the resolver cache.
   90 | 	 * @since 4.3
   91 | 	 */
   92 | 	public void clear() {
   93 | 		this.argumentResolvers.clear();
   94 | 		this.argumentResolverCache.clear();
   95 | 	}
   96 | 
   97 | 
   98 | 	/**
   99 | 	 * Whether the given {@linkplain MethodParameter method parameter} is
  100 | 	 * supported by any registered {@link HandlerMethodArgumentResolver}.
  101 | 	 */
  102 | 	@Override
  103 | 	public boolean supportsParameter(MethodParameter parameter) {
  104 | 		return getArgumentResolver(parameter) != null;
  105 | 	}
  106 | 
  107 | 	/**
  108 | 	 * Iterate over registered
  109 | 	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
  110 | 	 * and invoke the one that supports it.
  111 | 	 * @throws IllegalArgumentException if no suitable argument resolver is found
  112 | 	 */
  113 | 	@Override
  114 | 	public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
  115 | 			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
  116 | 
  117 | 		HandlerMethodArgumentResolver resolver = getArgumentResolver(parameter);
  118 | 		if (resolver == null) {
  119 | 			throw new IllegalArgumentException("Unsupported parameter type [" +
  120 | 					parameter.getParameterType().getName() + "]. supportsParameter should be called first.");
  121 | 		}
  122 | 		return resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Find a registered {@link HandlerMethodArgumentResolver} that supports
  127 | 	 * the given method parameter.
  128 | 	 */
  129 | 	public @Nullable HandlerMethodArgumentResolver getArgumentResolver(MethodParameter parameter) {
  130 | 		HandlerMethodArgumentResolver result = this.argumentResolverCache.get(parameter);
  131 | 		if (result == null) {
  132 | 			for (HandlerMethodArgumentResolver resolver : this.argumentResolvers) {
  133 | 				if (resolver.supportsParameter(parameter)) {
  134 | 					result = resolver;
  135 | 					this.argumentResolverCache.put(parameter, result);
  136 | 					break;
  137 | 				}
  138 | 			}
  139 | 		}
  140 | 		return result;
  141 | 	}
  142 | 
  143 | }
```
