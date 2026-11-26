<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/PathVariableMapMethodArgumentResolver.java -->
<!-- 正文引用：[[源码:PathVariableMapMethodArgumentResolver.java#L起-L止]] ；代码块头部：(src: PathVariableMapMethodArgumentResolver.java#L起-L止) -->

# PathVariableMapMethodArgumentResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/PathVariableMapMethodArgumentResolver.java` |
| 行数 | 74 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:33.056Z |

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
   19 | import java.util.Collections;
   20 | import java.util.Map;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.core.MethodParameter;
   25 | import org.springframework.util.CollectionUtils;
   26 | import org.springframework.util.StringUtils;
   27 | import org.springframework.web.bind.annotation.PathVariable;
   28 | import org.springframework.web.bind.support.WebDataBinderFactory;
   29 | import org.springframework.web.context.request.NativeWebRequest;
   30 | import org.springframework.web.context.request.RequestAttributes;
   31 | import org.springframework.web.method.support.HandlerMethodArgumentResolver;
   32 | import org.springframework.web.method.support.ModelAndViewContainer;
   33 | import org.springframework.web.servlet.HandlerMapping;
   34 | 
   35 | /**
   36 |  * Resolves {@link Map} method arguments annotated with
   37 |  * {@link PathVariable @PathVariable} where the annotation does not specify a
   38 |  * path variable name. The resulting {@link Map} argument is a copy of all URI
   39 |  * template name-value pairs.
   40 |  *
   41 |  * @author Rossen Stoyanchev
   42 |  * @since 3.2
   43 |  * @see PathVariableMethodArgumentResolver
   44 |  */
   45 | public class PathVariableMapMethodArgumentResolver implements HandlerMethodArgumentResolver {
   46 | 
   47 | 	@Override
   48 | 	public boolean supportsParameter(MethodParameter parameter) {
   49 | 		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
   50 | 		return (ann != null && Map.class.isAssignableFrom(parameter.getParameterType()) &&
   51 | 				!StringUtils.hasText(ann.value()));
   52 | 	}
   53 | 
   54 | 	/**
   55 | 	 * Return a Map with all URI template variables or an empty map.
   56 | 	 */
   57 | 	@Override
   58 | 	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
   59 | 			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
   60 | 
   61 | 		@SuppressWarnings("unchecked")
   62 | 		Map<String, String> uriTemplateVars =
   63 | 				(Map<String, String>) webRequest.getAttribute(
   64 | 						HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
   65 | 
   66 | 		if (!CollectionUtils.isEmpty(uriTemplateVars)) {
   67 | 			return Collections.unmodifiableMap(uriTemplateVars);
   68 | 		}
   69 | 		else {
   70 | 			return Collections.emptyMap();
   71 | 		}
   72 | 	}
   73 | 
   74 | }
```
