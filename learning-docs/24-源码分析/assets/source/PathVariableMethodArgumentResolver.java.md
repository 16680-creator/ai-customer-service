<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/PathVariableMethodArgumentResolver.java -->
<!-- 正文引用：[[源码:PathVariableMethodArgumentResolver.java#L起-L止]] ；代码块头部：(src: PathVariableMethodArgumentResolver.java#L起-L止) -->

# PathVariableMethodArgumentResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/PathVariableMethodArgumentResolver.java` |
| 行数 | 160 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:18:37.545Z |

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
   19 | import java.beans.PropertyEditor;
   20 | import java.util.HashMap;
   21 | import java.util.Map;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.core.MethodParameter;
   26 | import org.springframework.core.convert.ConversionService;
   27 | import org.springframework.core.convert.TypeDescriptor;
   28 | import org.springframework.core.convert.converter.Converter;
   29 | import org.springframework.util.Assert;
   30 | import org.springframework.util.StringUtils;
   31 | import org.springframework.web.bind.MissingPathVariableException;
   32 | import org.springframework.web.bind.ServletRequestBindingException;
   33 | import org.springframework.web.bind.WebDataBinder;
   34 | import org.springframework.web.bind.annotation.PathVariable;
   35 | import org.springframework.web.bind.annotation.ValueConstants;
   36 | import org.springframework.web.context.request.NativeWebRequest;
   37 | import org.springframework.web.context.request.RequestAttributes;
   38 | import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
   39 | import org.springframework.web.method.support.ModelAndViewContainer;
   40 | import org.springframework.web.method.support.UriComponentsContributor;
   41 | import org.springframework.web.servlet.HandlerMapping;
   42 | import org.springframework.web.servlet.View;
   43 | import org.springframework.web.util.UriComponentsBuilder;
   44 | 
   45 | /**
   46 |  * Resolves method arguments annotated with an @{@link PathVariable}.
   47 |  *
   48 |  * <p>An @{@link PathVariable} is a named value that gets resolved from a URI template variable.
   49 |  * It is always required and does not have a default value to fall back on. See the base class
   50 |  * {@link org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver}
   51 |  * for more information on how named values are processed.
   52 |  *
   53 |  * <p>If the method parameter type is {@link Map}, the name specified in the annotation is used
   54 |  * to resolve the URI variable String value. The value is then converted to a {@link Map} via
   55 |  * type conversion, assuming a suitable {@link Converter} or {@link PropertyEditor} has been
   56 |  * registered.
   57 |  *
   58 |  * <p>A {@link WebDataBinder} is invoked to apply type conversion to resolved path variable
   59 |  * values that don't yet match the method parameter type.
   60 |  *
   61 |  * @author Rossen Stoyanchev
   62 |  * @author Arjen Poutsma
   63 |  * @author Juergen Hoeller
   64 |  * @since 3.1
   65 |  */
   66 | public class PathVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver
   67 | 		implements UriComponentsContributor {
   68 | 
   69 | 	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);
   70 | 
   71 | 
   72 | 	@Override
   73 | 	public boolean supportsParameter(MethodParameter parameter) {
   74 | 		if (!parameter.hasParameterAnnotation(PathVariable.class)) {
   75 | 			return false;
   76 | 		}
   77 | 		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
   78 | 			PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
   79 | 			return (pathVariable != null && StringUtils.hasText(pathVariable.value()));
   80 | 		}
   81 | 		return true;
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
   86 | 		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
   87 | 		Assert.state(ann != null, "No PathVariable annotation");
   88 | 		return new PathVariableNamedValueInfo(ann);
   89 | 	}
   90 | 
   91 | 	@Override
   92 | 	@SuppressWarnings("unchecked")
   93 | 	protected @Nullable Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
   94 | 		Map<String, String> uriTemplateVars = (Map<String, String>) request.getAttribute(
   95 | 				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
   96 | 		return (uriTemplateVars != null ? uriTemplateVars.get(name) : null);
   97 | 	}
   98 | 
   99 | 	@Override
  100 | 	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
  101 | 		throw new MissingPathVariableException(name, parameter);
  102 | 	}
  103 | 
  104 | 	@Override
  105 | 	protected void handleMissingValueAfterConversion(
  106 | 			String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
  107 | 
  108 | 		throw new MissingPathVariableException(name, parameter, true);
  109 | 	}
  110 | 
  111 | 	@Override
  112 | 	@SuppressWarnings("unchecked")
  113 | 	protected void handleResolvedValue(@Nullable Object arg, String name, MethodParameter parameter,
  114 | 			@Nullable ModelAndViewContainer mavContainer, NativeWebRequest request) {
  115 | 
  116 | 		String key = View.PATH_VARIABLES;
  117 | 		int scope = RequestAttributes.SCOPE_REQUEST;
  118 | 		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(key, scope);
  119 | 		if (pathVars == null) {
  120 | 			pathVars = new HashMap<>();
  121 | 			request.setAttribute(key, pathVars, scope);
  122 | 		}
  123 | 		pathVars.put(name, arg);
  124 | 	}
  125 | 
  126 | 	@Override
  127 | 	public void contributeMethodArgument(MethodParameter parameter, Object value,
  128 | 			UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {
  129 | 
  130 | 		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
  131 | 			return;
  132 | 		}
  133 | 
  134 | 		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
  135 | 		String name = (ann != null && StringUtils.hasLength(ann.value()) ? ann.value() : parameter.getParameterName());
  136 | 		String formatted = formatUriValue(conversionService, new TypeDescriptor(parameter.nestedIfOptional()), value);
  137 | 		uriVariables.put(name, formatted);
  138 | 	}
  139 | 
  140 | 	protected @Nullable String formatUriValue(@Nullable ConversionService cs, @Nullable TypeDescriptor sourceType, Object value) {
  141 | 		if (value instanceof String string) {
  142 | 			return string;
  143 | 		}
  144 | 		else if (cs != null) {
  145 | 			return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
  146 | 		}
  147 | 		else {
  148 | 			return value.toString();
  149 | 		}
  150 | 	}
  151 | 
  152 | 
  153 | 	private static class PathVariableNamedValueInfo extends NamedValueInfo {
  154 | 
  155 | 		public PathVariableNamedValueInfo(PathVariable annotation) {
  156 | 			super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
  157 | 		}
  158 | 	}
  159 | 
  160 | }
```
