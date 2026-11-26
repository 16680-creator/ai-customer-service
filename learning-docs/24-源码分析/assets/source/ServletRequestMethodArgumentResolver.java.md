<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/ServletRequestMethodArgumentResolver.java -->
<!-- 正文引用：[[源码:ServletRequestMethodArgumentResolver.java#L起-L止]] ；代码块头部：(src: ServletRequestMethodArgumentResolver.java#L起-L止) -->

# ServletRequestMethodArgumentResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/ServletRequestMethodArgumentResolver.java` |
| 行数 | 184 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:33.202Z |

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
   17 | package org.springframework.web.servlet.mvc.method.annotation;
   18 | 
   19 | import java.io.IOException;
   20 | import java.io.InputStream;
   21 | import java.io.Reader;
   22 | import java.security.Principal;
   23 | import java.time.ZoneId;
   24 | import java.util.Locale;
   25 | import java.util.TimeZone;
   26 | 
   27 | import jakarta.servlet.ServletRequest;
   28 | import jakarta.servlet.http.HttpServletRequest;
   29 | import jakarta.servlet.http.HttpSession;
   30 | import jakarta.servlet.http.PushBuilder;
   31 | import org.jspecify.annotations.Nullable;
   32 | 
   33 | import org.springframework.core.MethodParameter;
   34 | import org.springframework.http.HttpMethod;
   35 | import org.springframework.web.bind.support.WebDataBinderFactory;
   36 | import org.springframework.web.context.request.NativeWebRequest;
   37 | import org.springframework.web.context.request.WebRequest;
   38 | import org.springframework.web.method.support.HandlerMethodArgumentResolver;
   39 | import org.springframework.web.method.support.ModelAndViewContainer;
   40 | import org.springframework.web.multipart.MultipartRequest;
   41 | import org.springframework.web.servlet.support.RequestContextUtils;
   42 | 
   43 | /**
   44 |  * Resolves servlet backed request-related method arguments.
   45 |  *
   46 |  * <p>Supports values of the following types:
   47 |  *
   48 |  * <ul>
   49 |  * <li>{@link WebRequest}
   50 |  * <li>{@link ServletRequest}
   51 |  * <li>{@link MultipartRequest}
   52 |  * <li>{@link HttpSession}
   53 |  * <li>{@link PushBuilder}
   54 |  * <li>{@link Principal} but only if not annotated in order to allow custom
   55 |  * resolvers to resolve it, and then falling back on
   56 |  * {@link PrincipalMethodArgumentResolver}
   57 |  * <li>{@link InputStream}
   58 |  * <li>{@link Reader}
   59 |  * <li>{@link HttpMethod}
   60 |  * <li>{@link Locale}
   61 |  * <li>{@link TimeZone}
   62 |  * <li>{@link ZoneId}
   63 |  * </ul>
   64 |  *
   65 |  * @author Arjen Poutsma
   66 |  * @author Rossen Stoyanchev
   67 |  * @author Juergen Hoeller
   68 |  * @since 3.1
   69 |  */
   70 | public class ServletRequestMethodArgumentResolver implements HandlerMethodArgumentResolver {
   71 | 
   72 | 	@Override
   73 | 	@SuppressWarnings("deprecation")
   74 | 	public boolean supportsParameter(MethodParameter parameter) {
   75 | 		Class<?> paramType = parameter.getParameterType();
   76 | 		return (WebRequest.class.isAssignableFrom(paramType) ||
   77 | 				ServletRequest.class.isAssignableFrom(paramType) ||
   78 | 				MultipartRequest.class.isAssignableFrom(paramType) ||
   79 | 				HttpSession.class.isAssignableFrom(paramType) ||
   80 | 				PushBuilder.class.isAssignableFrom(paramType) ||
   81 | 				(Principal.class.isAssignableFrom(paramType) && !parameter.hasParameterAnnotations()) ||
   82 | 				InputStream.class.isAssignableFrom(paramType) ||
   83 | 				Reader.class.isAssignableFrom(paramType) ||
   84 | 				HttpMethod.class == paramType ||
   85 | 				Locale.class == paramType ||
   86 | 				TimeZone.class == paramType ||
   87 | 				ZoneId.class == paramType);
   88 | 	}
   89 | 
   90 | 	@Override
   91 | 	public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
   92 | 			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
   93 | 
   94 | 		Class<?> paramType = parameter.getParameterType();
   95 | 
   96 | 		// WebRequest / NativeWebRequest / ServletWebRequest
   97 | 		if (WebRequest.class.isAssignableFrom(paramType)) {
   98 | 			if (!paramType.isInstance(webRequest)) {
   99 | 				throw new IllegalStateException(
  100 | 						"Current request is not of type [" + paramType.getName() + "]: " + webRequest);
  101 | 			}
  102 | 			return webRequest;
  103 | 		}
  104 | 
  105 | 		// ServletRequest / HttpServletRequest / MultipartRequest / MultipartHttpServletRequest
  106 | 		if (ServletRequest.class.isAssignableFrom(paramType) || MultipartRequest.class.isAssignableFrom(paramType)) {
  107 | 			return resolveNativeRequest(webRequest, paramType);
  108 | 		}
  109 | 
  110 | 		// HttpServletRequest required for all further argument types
  111 | 		return resolveArgument(paramType, resolveNativeRequest(webRequest, HttpServletRequest.class));
  112 | 	}
  113 | 
  114 | 	private <T> T resolveNativeRequest(NativeWebRequest webRequest, Class<T> requiredType) {
  115 | 		T nativeRequest = webRequest.getNativeRequest(requiredType);
  116 | 		if (nativeRequest == null) {
  117 | 			throw new IllegalStateException(
  118 | 					"Current request is not of type [" + requiredType.getName() + "]: " + webRequest);
  119 | 		}
  120 | 		return nativeRequest;
  121 | 	}
  122 | 
  123 | 	@SuppressWarnings("deprecation")
  124 | 	private @Nullable Object resolveArgument(Class<?> paramType, HttpServletRequest request) throws IOException {
  125 | 		if (HttpSession.class.isAssignableFrom(paramType)) {
  126 | 			HttpSession session = request.getSession();
  127 | 			if (session != null && !paramType.isInstance(session)) {
  128 | 				throw new IllegalStateException(
  129 | 						"Current session is not of type [" + paramType.getName() + "]: " + session);
  130 | 			}
  131 | 			return session;
  132 | 		}
  133 | 		else if (PushBuilder.class.isAssignableFrom(paramType)) {
  134 | 			PushBuilder pushBuilder = request.newPushBuilder();
  135 | 			if (pushBuilder != null && !paramType.isInstance(pushBuilder)) {
  136 | 				throw new IllegalStateException(
  137 | 						"Current push builder is not of type [" + paramType.getName() + "]: " + pushBuilder);
  138 | 			}
  139 | 			return pushBuilder;
  140 | 		}
  141 | 		else if (InputStream.class.isAssignableFrom(paramType)) {
  142 | 			InputStream inputStream = request.getInputStream();
  143 | 			if (inputStream != null && !paramType.isInstance(inputStream)) {
  144 | 				throw new IllegalStateException(
  145 | 						"Request input stream is not of type [" + paramType.getName() + "]: " + inputStream);
  146 | 			}
  147 | 			return inputStream;
  148 | 		}
  149 | 		else if (Reader.class.isAssignableFrom(paramType)) {
  150 | 			Reader reader = request.getReader();
  151 | 			if (reader != null && !paramType.isInstance(reader)) {
  152 | 				throw new IllegalStateException(
  153 | 						"Request body reader is not of type [" + paramType.getName() + "]: " + reader);
  154 | 			}
  155 | 			return reader;
  156 | 		}
  157 | 		else if (Principal.class.isAssignableFrom(paramType)) {
  158 | 			Principal userPrincipal = request.getUserPrincipal();
  159 | 			if (userPrincipal != null && !paramType.isInstance(userPrincipal)) {
  160 | 				throw new IllegalStateException(
  161 | 						"Current user principal is not of type [" + paramType.getName() + "]: " + userPrincipal);
  162 | 			}
  163 | 			return userPrincipal;
  164 | 		}
  165 | 		else if (HttpMethod.class == paramType) {
  166 | 			return HttpMethod.valueOf(request.getMethod());
  167 | 		}
  168 | 		else if (Locale.class == paramType) {
  169 | 			return RequestContextUtils.getLocale(request);
  170 | 		}
  171 | 		else if (TimeZone.class == paramType) {
  172 | 			TimeZone timeZone = RequestContextUtils.getTimeZone(request);
  173 | 			return (timeZone != null ? timeZone : TimeZone.getDefault());
  174 | 		}
  175 | 		else if (ZoneId.class == paramType) {
  176 | 			TimeZone timeZone = RequestContextUtils.getTimeZone(request);
  177 | 			return (timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault());
  178 | 		}
  179 | 
  180 | 		// Should never happen...
  181 | 		throw new UnsupportedOperationException("Unknown parameter type: " + paramType.getName());
  182 | 	}
  183 | 
  184 | }
```
