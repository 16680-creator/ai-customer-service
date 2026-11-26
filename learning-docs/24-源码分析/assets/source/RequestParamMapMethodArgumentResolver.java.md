<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/annotation/RequestParamMapMethodArgumentResolver.java -->
<!-- 正文引用：[[源码:RequestParamMapMethodArgumentResolver.java#L起-L止]] ；代码块头部：(src: RequestParamMapMethodArgumentResolver.java#L起-L止) -->

# RequestParamMapMethodArgumentResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/annotation/RequestParamMapMethodArgumentResolver.java` |
| 行数 | 142 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:21.849Z |

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
   17 | package org.springframework.web.method.annotation;
   18 | 
   19 | import java.util.Collection;
   20 | import java.util.LinkedHashMap;
   21 | import java.util.Map;
   22 | 
   23 | import jakarta.servlet.http.HttpServletRequest;
   24 | import jakarta.servlet.http.Part;
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.core.MethodParameter;
   28 | import org.springframework.core.ResolvableType;
   29 | import org.springframework.util.CollectionUtils;
   30 | import org.springframework.util.LinkedMultiValueMap;
   31 | import org.springframework.util.MultiValueMap;
   32 | import org.springframework.util.StringUtils;
   33 | import org.springframework.web.bind.annotation.RequestParam;
   34 | import org.springframework.web.bind.support.WebDataBinderFactory;
   35 | import org.springframework.web.context.request.NativeWebRequest;
   36 | import org.springframework.web.method.support.HandlerMethodArgumentResolver;
   37 | import org.springframework.web.method.support.ModelAndViewContainer;
   38 | import org.springframework.web.multipart.MultipartFile;
   39 | import org.springframework.web.multipart.MultipartRequest;
   40 | import org.springframework.web.multipart.support.MultipartResolutionDelegate;
   41 | 
   42 | /**
   43 |  * Resolves {@link Map} method arguments annotated with an @{@link RequestParam}
   44 |  * where the annotation does not specify a request parameter name.
   45 |  *
   46 |  * <p>The created {@link Map} contains all request parameter name/value pairs,
   47 |  * or all multipart files for a given parameter name if specifically declared
   48 |  * with {@link MultipartFile} as the value type. If the method parameter type is
   49 |  * {@link MultiValueMap} instead, the created map contains all request parameters
   50 |  * and all their values for cases where request parameters have multiple values
   51 |  * (or multiple multipart files of the same name).
   52 |  *
   53 |  * @author Arjen Poutsma
   54 |  * @author Rossen Stoyanchev
   55 |  * @author Juergen Hoeller
   56 |  * @since 3.1
   57 |  * @see RequestParamMethodArgumentResolver
   58 |  * @see HttpServletRequest#getParameterMap()
   59 |  * @see MultipartRequest#getMultiFileMap()
   60 |  * @see MultipartRequest#getFileMap()
   61 |  */
   62 | public class RequestParamMapMethodArgumentResolver implements HandlerMethodArgumentResolver {
   63 | 
   64 | 	@Override
   65 | 	public boolean supportsParameter(MethodParameter parameter) {
   66 | 		RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
   67 | 		return (requestParam != null && Map.class.isAssignableFrom(parameter.getParameterType()) &&
   68 | 				!StringUtils.hasText(requestParam.name()));
   69 | 	}
   70 | 
   71 | 	@Override
   72 | 	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
   73 | 			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
   74 | 
   75 | 		ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
   76 | 
   77 | 		if (MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
   78 | 			// MultiValueMap
   79 | 			Class<?> valueType = resolvableType.as(MultiValueMap.class).getGeneric(1).resolve();
   80 | 			if (valueType == MultipartFile.class) {
   81 | 				MultipartRequest multipartRequest = MultipartResolutionDelegate.resolveMultipartRequest(webRequest);
   82 | 				return (multipartRequest != null ? multipartRequest.getMultiFileMap() : new LinkedMultiValueMap<>(0));
   83 | 			}
   84 | 			else if (valueType == Part.class) {
   85 | 				HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
   86 | 				if (servletRequest != null && MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
   87 | 					Collection<Part> parts = servletRequest.getParts();
   88 | 					LinkedMultiValueMap<String, Part> result = new LinkedMultiValueMap<>(parts.size());
   89 | 					for (Part part : parts) {
   90 | 						result.add(part.getName(), part);
   91 | 					}
   92 | 					return result;
   93 | 				}
   94 | 				return new LinkedMultiValueMap<>(0);
   95 | 			}
   96 | 			else {
   97 | 				Map<String, String[]> parameterMap = webRequest.getParameterMap();
   98 | 				MultiValueMap<String, String> result = new LinkedMultiValueMap<>(parameterMap.size());
   99 | 				parameterMap.forEach((key, values) -> {
  100 | 					for (String value : values) {
  101 | 						result.add(key, value);
  102 | 					}
  103 | 				});
  104 | 				return result;
  105 | 			}
  106 | 		}
  107 | 
  108 | 		else {
  109 | 			// Regular Map
  110 | 			Class<?> valueType = resolvableType.asMap().getGeneric(1).resolve();
  111 | 			if (valueType == MultipartFile.class) {
  112 | 				MultipartRequest multipartRequest = MultipartResolutionDelegate.resolveMultipartRequest(webRequest);
  113 | 				return (multipartRequest != null ? multipartRequest.getFileMap() : new LinkedHashMap<>(0));
  114 | 			}
  115 | 			else if (valueType == Part.class) {
  116 | 				HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
  117 | 				if (servletRequest != null && MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
  118 | 					Collection<Part> parts = servletRequest.getParts();
  119 | 					LinkedHashMap<String, Part> result = CollectionUtils.newLinkedHashMap(parts.size());
  120 | 					for (Part part : parts) {
  121 | 						if (!result.containsKey(part.getName())) {
  122 | 							result.put(part.getName(), part);
  123 | 						}
  124 | 					}
  125 | 					return result;
  126 | 				}
  127 | 				return new LinkedHashMap<>(0);
  128 | 			}
  129 | 			else {
  130 | 				Map<String, String[]> parameterMap = webRequest.getParameterMap();
  131 | 				Map<String, String> result = CollectionUtils.newLinkedHashMap(parameterMap.size());
  132 | 				parameterMap.forEach((key, values) -> {
  133 | 					if (values.length > 0) {
  134 | 						result.put(key, values[0]);
  135 | 					}
  136 | 				});
  137 | 				return result;
  138 | 			}
  139 | 		}
  140 | 	}
  141 | 
  142 | }
```
