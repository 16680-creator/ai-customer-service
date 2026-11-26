<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/handler/HandlerExceptionResolverComposite.java -->
<!-- 正文引用：[[源码:HandlerExceptionResolverComposite.java#L起-L止]] ；代码块头部：(src: HandlerExceptionResolverComposite.java#L起-L止) -->

# HandlerExceptionResolverComposite.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/handler/HandlerExceptionResolverComposite.java` |
| 行数 | 87 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:10.290Z |

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
   17 | package org.springframework.web.servlet.handler;
   18 | 
   19 | import java.util.Collections;
   20 | import java.util.List;
   21 | 
   22 | import jakarta.servlet.http.HttpServletRequest;
   23 | import jakarta.servlet.http.HttpServletResponse;
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.core.Ordered;
   27 | import org.springframework.web.servlet.HandlerExceptionResolver;
   28 | import org.springframework.web.servlet.ModelAndView;
   29 | 
   30 | /**
   31 |  * A {@link HandlerExceptionResolver} that delegates to a list of other
   32 |  * {@link HandlerExceptionResolver HandlerExceptionResolvers}.
   33 |  *
   34 |  * @author Rossen Stoyanchev
   35 |  * @since 3.1
   36 |  */
   37 | public class HandlerExceptionResolverComposite implements HandlerExceptionResolver, Ordered {
   38 | 
   39 | 	private @Nullable List<HandlerExceptionResolver> resolvers;
   40 | 
   41 | 	private int order = Ordered.LOWEST_PRECEDENCE;
   42 | 
   43 | 
   44 | 	/**
   45 | 	 * Set the list of exception resolvers to delegate to.
   46 | 	 */
   47 | 	public void setExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
   48 | 		this.resolvers = exceptionResolvers;
   49 | 	}
   50 | 
   51 | 	/**
   52 | 	 * Return the list of exception resolvers to delegate to.
   53 | 	 */
   54 | 	public List<HandlerExceptionResolver> getExceptionResolvers() {
   55 | 		return (this.resolvers != null ? Collections.unmodifiableList(this.resolvers) : Collections.emptyList());
   56 | 	}
   57 | 
   58 | 	public void setOrder(int order) {
   59 | 		this.order = order;
   60 | 	}
   61 | 
   62 | 	@Override
   63 | 	public int getOrder() {
   64 | 		return this.order;
   65 | 	}
   66 | 
   67 | 
   68 | 	/**
   69 | 	 * Resolve the exception by iterating over the list of configured exception resolvers.
   70 | 	 * <p>The first one to return a {@link ModelAndView} wins. Otherwise {@code null} is returned.
   71 | 	 */
   72 | 	@Override
   73 | 	public @Nullable ModelAndView resolveException(
   74 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {
   75 | 
   76 | 		if (this.resolvers != null) {
   77 | 			for (HandlerExceptionResolver handlerExceptionResolver : this.resolvers) {
   78 | 				ModelAndView mav = handlerExceptionResolver.resolveException(request, response, handler, ex);
   79 | 				if (mav != null) {
   80 | 					return mav;
   81 | 				}
   82 | 			}
   83 | 		}
   84 | 		return null;
   85 | 	}
   86 | 
   87 | }
```
