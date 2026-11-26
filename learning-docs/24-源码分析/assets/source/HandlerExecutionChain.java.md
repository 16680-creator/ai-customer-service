<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/HandlerExecutionChain.java -->
<!-- 正文引用：[[源码:HandlerExecutionChain.java#L起-L止]] ；代码块头部：(src: HandlerExecutionChain.java#L起-L止) -->

# HandlerExecutionChain.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/HandlerExecutionChain.java` |
| 行数 | 211 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:08.910Z |

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
   17 | package org.springframework.web.servlet;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Arrays;
   21 | import java.util.Collections;
   22 | import java.util.List;
   23 | 
   24 | import jakarta.servlet.http.HttpServletRequest;
   25 | import jakarta.servlet.http.HttpServletResponse;
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.util.CollectionUtils;
   31 | 
   32 | /**
   33 |  * Handler execution chain, consisting of handler object and any handler interceptors.
   34 |  * Returned by HandlerMapping's {@link HandlerMapping#getHandler} method.
   35 |  *
   36 |  * @author Juergen Hoeller
   37 |  * @since 20.06.2003
   38 |  * @see HandlerInterceptor
   39 |  */
   40 | public class HandlerExecutionChain {
   41 | 
   42 | 	private static final Log logger = LogFactory.getLog(HandlerExecutionChain.class);
   43 | 
   44 | 	private final Object handler;
   45 | 
   46 | 	private final List<HandlerInterceptor> interceptorList = new ArrayList<>();
   47 | 
   48 | 	private int interceptorIndex = -1;
   49 | 
   50 | 
   51 | 	/**
   52 | 	 * Create a new HandlerExecutionChain.
   53 | 	 * @param handler the handler object to execute
   54 | 	 */
   55 | 	public HandlerExecutionChain(Object handler) {
   56 | 		this(handler, (HandlerInterceptor[]) null);
   57 | 	}
   58 | 
   59 | 	/**
   60 | 	 * Create a new HandlerExecutionChain.
   61 | 	 * @param handler the handler object to execute
   62 | 	 * @param interceptors the array of interceptors to apply
   63 | 	 * (in the given order) before the handler itself executes
   64 | 	 */
   65 | 	public HandlerExecutionChain(Object handler, HandlerInterceptor @Nullable ... interceptors) {
   66 | 		this(handler, (interceptors != null ? Arrays.asList(interceptors) : Collections.emptyList()));
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Create a new HandlerExecutionChain.
   71 | 	 * @param handler the handler object to execute
   72 | 	 * @param interceptorList the list of interceptors to apply
   73 | 	 * (in the given order) before the handler itself executes
   74 | 	 * @since 5.3
   75 | 	 */
   76 | 	public HandlerExecutionChain(Object handler, List<HandlerInterceptor> interceptorList) {
   77 | 		if (handler instanceof HandlerExecutionChain originalChain) {
   78 | 			this.handler = originalChain.getHandler();
   79 | 			this.interceptorList.addAll(originalChain.interceptorList);
   80 | 		}
   81 | 		else {
   82 | 			this.handler = handler;
   83 | 		}
   84 | 		this.interceptorList.addAll(interceptorList);
   85 | 	}
   86 | 
   87 | 
   88 | 	/**
   89 | 	 * Return the handler object to execute.
   90 | 	 */
   91 | 	public Object getHandler() {
   92 | 		return this.handler;
   93 | 	}
   94 | 
   95 | 	/**
   96 | 	 * Add the given interceptor to the end of this chain.
   97 | 	 */
   98 | 	public void addInterceptor(HandlerInterceptor interceptor) {
   99 | 		this.interceptorList.add(interceptor);
  100 | 	}
  101 | 
  102 | 	/**
  103 | 	 * Add the given interceptor at the specified index of this chain.
  104 | 	 * @since 5.2
  105 | 	 */
  106 | 	public void addInterceptor(int index, HandlerInterceptor interceptor) {
  107 | 		this.interceptorList.add(index, interceptor);
  108 | 	}
  109 | 
  110 | 	/**
  111 | 	 * Add the given interceptors to the end of this chain.
  112 | 	 */
  113 | 	public void addInterceptors(HandlerInterceptor... interceptors) {
  114 | 		CollectionUtils.mergeArrayIntoCollection(interceptors, this.interceptorList);
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * Return the array of interceptors to apply (in the given order).
  119 | 	 * @return the array of HandlerInterceptors instances (may be {@code null})
  120 | 	 */
  121 | 	public HandlerInterceptor @Nullable [] getInterceptors() {
  122 | 		return (!this.interceptorList.isEmpty() ? this.interceptorList.toArray(new HandlerInterceptor[0]) : null);
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Return the list of interceptors to apply (in the given order).
  127 | 	 * @return the list of HandlerInterceptors instances (potentially empty)
  128 | 	 * @since 5.3
  129 | 	 */
  130 | 	public List<HandlerInterceptor> getInterceptorList() {
  131 | 		return (!this.interceptorList.isEmpty() ? Collections.unmodifiableList(this.interceptorList) :
  132 | 				Collections.emptyList());
  133 | 	}
  134 | 
  135 | 
  136 | 	/**
  137 | 	 * Apply preHandle methods of registered interceptors.
  138 | 	 * @return {@code true} if the execution chain should proceed with the
  139 | 	 * next interceptor or the handler itself. Else, DispatcherServlet assumes
  140 | 	 * that this interceptor has already dealt with the response itself.
  141 | 	 */
  142 | 	boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
  143 | 		for (int i = 0; i < this.interceptorList.size(); i++) {
  144 | 			HandlerInterceptor interceptor = this.interceptorList.get(i);
  145 | 			if (!interceptor.preHandle(request, response, this.handler)) {
  146 | 				triggerAfterCompletion(request, response, null);
  147 | 				return false;
  148 | 			}
  149 | 			this.interceptorIndex = i;
  150 | 		}
  151 | 		return true;
  152 | 	}
  153 | 
  154 | 	/**
  155 | 	 * Apply postHandle methods of registered interceptors.
  156 | 	 */
  157 | 	void applyPostHandle(HttpServletRequest request, HttpServletResponse response, @Nullable ModelAndView mv)
  158 | 			throws Exception {
  159 | 
  160 | 		for (int i = this.interceptorList.size() - 1; i >= 0; i--) {
  161 | 			HandlerInterceptor interceptor = this.interceptorList.get(i);
  162 | 			interceptor.postHandle(request, response, this.handler, mv);
  163 | 		}
  164 | 	}
  165 | 
  166 | 	/**
  167 | 	 * Trigger afterCompletion callbacks on the mapped HandlerInterceptors.
  168 | 	 * Will just invoke afterCompletion for all interceptors whose preHandle invocation
  169 | 	 * has successfully completed and returned true.
  170 | 	 */
  171 | 	void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, @Nullable Exception ex) {
  172 | 		for (int i = this.interceptorIndex; i >= 0; i--) {
  173 | 			HandlerInterceptor interceptor = this.interceptorList.get(i);
  174 | 			try {
  175 | 				interceptor.afterCompletion(request, response, this.handler, ex);
  176 | 			}
  177 | 			catch (Throwable ex2) {
  178 | 				logger.error("HandlerInterceptor.afterCompletion threw exception in interceptor [" + interceptor + "]", ex2);
  179 | 			}
  180 | 		}
  181 | 	}
  182 | 
  183 | 	/**
  184 | 	 * Apply afterConcurrentHandlerStarted callback on mapped AsyncHandlerInterceptors.
  185 | 	 */
  186 | 	void applyAfterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response) {
  187 | 		for (int i = this.interceptorList.size() - 1; i >= 0; i--) {
  188 | 			HandlerInterceptor interceptor = this.interceptorList.get(i);
  189 | 			if (interceptor instanceof AsyncHandlerInterceptor asyncInterceptor) {
  190 | 				try {
  191 | 					asyncInterceptor.afterConcurrentHandlingStarted(request, response, this.handler);
  192 | 				}
  193 | 				catch (Throwable ex) {
  194 | 					if (logger.isErrorEnabled()) {
  195 | 						logger.error("Interceptor [" + interceptor + "] failed in afterConcurrentHandlingStarted", ex);
  196 | 					}
  197 | 				}
  198 | 			}
  199 | 		}
  200 | 	}
  201 | 
  202 | 
  203 | 	/**
  204 | 	 * Delegates to the handler's {@code toString()} implementation.
  205 | 	 */
  206 | 	@Override
  207 | 	public String toString() {
  208 | 		return "HandlerExecutionChain with [" + getHandler() + "] and " + this.interceptorList.size() + " interceptors";
  209 | 	}
  210 | 
  211 | }
```
