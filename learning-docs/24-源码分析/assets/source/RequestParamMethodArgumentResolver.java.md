<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/annotation/RequestParamMethodArgumentResolver.java -->
<!-- 正文引用：[[源码:RequestParamMethodArgumentResolver.java#L起-L止]] ；代码块头部：(src: RequestParamMethodArgumentResolver.java#L起-L止) -->

# RequestParamMethodArgumentResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/annotation/RequestParamMethodArgumentResolver.java` |
| 行数 | 289 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:21.582Z |

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
   19 | import java.beans.PropertyEditor;
   20 | import java.util.Collection;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.Optional;
   24 | 
   25 | import jakarta.servlet.http.HttpServletRequest;
   26 | import jakarta.servlet.http.Part;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.beans.BeanUtils;
   30 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   31 | import org.springframework.core.MethodParameter;
   32 | import org.springframework.core.convert.ConversionService;
   33 | import org.springframework.core.convert.TypeDescriptor;
   34 | import org.springframework.core.convert.converter.Converter;
   35 | import org.springframework.util.Assert;
   36 | import org.springframework.util.StringUtils;
   37 | import org.springframework.web.bind.MissingServletRequestParameterException;
   38 | import org.springframework.web.bind.WebDataBinder;
   39 | import org.springframework.web.bind.annotation.RequestParam;
   40 | import org.springframework.web.bind.annotation.RequestPart;
   41 | import org.springframework.web.bind.annotation.ValueConstants;
   42 | import org.springframework.web.context.request.NativeWebRequest;
   43 | import org.springframework.web.method.support.UriComponentsContributor;
   44 | import org.springframework.web.multipart.MultipartException;
   45 | import org.springframework.web.multipart.MultipartFile;
   46 | import org.springframework.web.multipart.MultipartRequest;
   47 | import org.springframework.web.multipart.MultipartResolver;
   48 | import org.springframework.web.multipart.support.MissingServletRequestPartException;
   49 | import org.springframework.web.multipart.support.MultipartResolutionDelegate;
   50 | import org.springframework.web.util.UriComponentsBuilder;
   51 | 
   52 | /**
   53 |  * Resolves method arguments annotated with @{@link RequestParam}, arguments of
   54 |  * type {@link MultipartFile} in conjunction with Spring's {@link MultipartResolver}
   55 |  * abstraction, and arguments of type {@code jakarta.servlet.http.Part} in conjunction
   56 |  * with Servlet multipart requests. This resolver can also be created in default
   57 |  * resolution mode in which simple types (int, long, etc.) not annotated with
   58 |  * {@link RequestParam @RequestParam} are also treated as request parameters with
   59 |  * the parameter name derived from the argument name.
   60 |  *
   61 |  * <p>If the method parameter type is {@link Map}, the name specified in the
   62 |  * annotation is used to resolve the request parameter String value. The value is
   63 |  * then converted to a {@link Map} via type conversion assuming a suitable
   64 |  * {@link Converter} or {@link PropertyEditor} has been registered.
   65 |  * Or if a request parameter name is not specified the
   66 |  * {@link RequestParamMapMethodArgumentResolver} is used instead to provide
   67 |  * access to all request parameters in the form of a map.
   68 |  *
   69 |  * <p>A {@link WebDataBinder} is invoked to apply type conversion to resolved request
   70 |  * header values that don't yet match the method parameter type.
   71 |  *
   72 |  * @author Arjen Poutsma
   73 |  * @author Rossen Stoyanchev
   74 |  * @author Brian Clozel
   75 |  * @since 3.1
   76 |  * @see RequestParamMapMethodArgumentResolver
   77 |  */
   78 | public class RequestParamMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver
   79 | 		implements UriComponentsContributor {
   80 | 
   81 | 	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);
   82 | 
   83 | 	private final boolean useDefaultResolution;
   84 | 
   85 | 
   86 | 	/**
   87 | 	 * Create a new {@link RequestParamMethodArgumentResolver} instance.
   88 | 	 * @param useDefaultResolution in default resolution mode a method argument
   89 | 	 * that is a simple type, as defined in {@link BeanUtils#isSimpleProperty},
   90 | 	 * is treated as a request parameter even if it isn't annotated, the
   91 | 	 * request parameter name is derived from the method parameter name.
   92 | 	 */
   93 | 	public RequestParamMethodArgumentResolver(boolean useDefaultResolution) {
   94 | 		this.useDefaultResolution = useDefaultResolution;
   95 | 	}
   96 | 
   97 | 	/**
   98 | 	 * Create a new {@link RequestParamMethodArgumentResolver} instance.
   99 | 	 * @param beanFactory a bean factory used for resolving  ${...} placeholder
  100 | 	 * and #{...} SpEL expressions in default values, or {@code null} if default
  101 | 	 * values are not expected to contain expressions
  102 | 	 * @param useDefaultResolution in default resolution mode a method argument
  103 | 	 * that is a simple type, as defined in {@link BeanUtils#isSimpleProperty},
  104 | 	 * is treated as a request parameter even if it isn't annotated, the
  105 | 	 * request parameter name is derived from the method parameter name.
  106 | 	 */
  107 | 	public RequestParamMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory,
  108 | 			boolean useDefaultResolution) {
  109 | 
  110 | 		super(beanFactory);
  111 | 		this.useDefaultResolution = useDefaultResolution;
  112 | 	}
  113 | 
  114 | 
  115 | 	/**
  116 | 	 * Supports the following:
  117 | 	 * <ul>
  118 | 	 * <li>@RequestParam-annotated method arguments.
  119 | 	 * This excludes {@link Map} params where the annotation does not specify a name.
  120 | 	 * See {@link RequestParamMapMethodArgumentResolver} instead for such params.
  121 | 	 * <li>Arguments of type {@link MultipartFile} unless annotated with @{@link RequestPart}.
  122 | 	 * <li>Arguments of type {@code Part} unless annotated with @{@link RequestPart}.
  123 | 	 * <li>In default resolution mode, simple type arguments even if not with @{@link RequestParam}.
  124 | 	 * </ul>
  125 | 	 */
  126 | 	@Override
  127 | 	public boolean supportsParameter(MethodParameter parameter) {
  128 | 		if (parameter.hasParameterAnnotation(RequestParam.class)) {
  129 | 			if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
  130 | 				RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
  131 | 				return (requestParam != null && StringUtils.hasText(requestParam.name()));
  132 | 			}
  133 | 			else {
  134 | 				return true;
  135 | 			}
  136 | 		}
  137 | 		else {
  138 | 			if (parameter.hasParameterAnnotation(RequestPart.class)) {
  139 | 				return false;
  140 | 			}
  141 | 			parameter = parameter.nestedIfOptional();
  142 | 			if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
  143 | 				return true;
  144 | 			}
  145 | 			else if (this.useDefaultResolution) {
  146 | 				return BeanUtils.isSimpleProperty(parameter.getNestedParameterType());
  147 | 			}
  148 | 			else {
  149 | 				return false;
  150 | 			}
  151 | 		}
  152 | 	}
  153 | 
  154 | 	@Override
  155 | 	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
  156 | 		RequestParam ann = parameter.getParameterAnnotation(RequestParam.class);
  157 | 		return (ann != null ? new RequestParamNamedValueInfo(ann) : new RequestParamNamedValueInfo());
  158 | 	}
  159 | 
  160 | 	@Override
  161 | 	protected @Nullable Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
  162 | 		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
  163 | 
  164 | 		if (servletRequest != null) {
  165 | 			Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, servletRequest);
  166 | 			if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
  167 | 				return mpArg;
  168 | 			}
  169 | 		}
  170 | 
  171 | 		Object arg = null;
  172 | 		MultipartRequest multipartRequest = request.getNativeRequest(MultipartRequest.class);
  173 | 		if (multipartRequest != null) {
  174 | 			List<MultipartFile> files = multipartRequest.getFiles(name);
  175 | 			if (!files.isEmpty()) {
  176 | 				arg = (files.size() == 1 ? files.get(0) : files);
  177 | 			}
  178 | 		}
  179 | 		if (arg == null) {
  180 | 			String[] paramValues = request.getParameterValues(name);
  181 | 			if (paramValues == null) {
  182 | 				paramValues = request.getParameterValues(name + "[]");
  183 | 			}
  184 | 			if (paramValues != null) {
  185 | 				arg = (paramValues.length == 1 ? paramValues[0] : paramValues);
  186 | 			}
  187 | 		}
  188 | 		return arg;
  189 | 	}
  190 | 
  191 | 	@Override
  192 | 	protected void handleMissingValue(String name, MethodParameter parameter, NativeWebRequest request)
  193 | 			throws Exception {
  194 | 
  195 | 		handleMissingValueInternal(name, parameter, request, false);
  196 | 	}
  197 | 
  198 | 	@Override
  199 | 	protected void handleMissingValueAfterConversion(
  200 | 			String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
  201 | 
  202 | 		handleMissingValueInternal(name, parameter, request, true);
  203 | 	}
  204 | 
  205 | 	protected void handleMissingValueInternal(
  206 | 			String name, MethodParameter parameter, NativeWebRequest request, boolean missingAfterConversion)
  207 | 			throws Exception {
  208 | 
  209 | 		HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
  210 | 		if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
  211 | 			if (servletRequest == null || !MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
  212 | 				throw new MultipartException("Current request is not a multipart request");
  213 | 			}
  214 | 			else {
  215 | 				throw new MissingServletRequestPartException(name);
  216 | 			}
  217 | 		}
  218 | 		else {
  219 | 			throw new MissingServletRequestParameterException(name, parameter, missingAfterConversion);
  220 | 		}
  221 | 	}
  222 | 
  223 | 	@Override
  224 | 	public void contributeMethodArgument(MethodParameter parameter, @Nullable Object value,
  225 | 			UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {
  226 | 
  227 | 		Class<?> paramType = parameter.getNestedParameterType();
  228 | 		if (Map.class.isAssignableFrom(paramType) || MultipartFile.class == paramType || Part.class == paramType) {
  229 | 			return;
  230 | 		}
  231 | 
  232 | 		RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
  233 | 		String name = (requestParam != null && StringUtils.hasLength(requestParam.name()) ?
  234 | 				requestParam.name() : parameter.getParameterName());
  235 | 		Assert.state(name != null, "Unresolvable parameter name");
  236 | 
  237 | 		parameter = parameter.nestedIfOptional();
  238 | 		if (value instanceof Optional<?> optional) {
  239 | 			value = optional.orElse(null);
  240 | 		}
  241 | 
  242 | 		if (value == null) {
  243 | 			if (requestParam != null &&
  244 | 					(!requestParam.required() || !requestParam.defaultValue().equals(ValueConstants.DEFAULT_NONE))) {
  245 | 				return;
  246 | 			}
  247 | 			builder.queryParam(name);
  248 | 		}
  249 | 		else if (value instanceof Collection<?> elements) {
  250 | 			for (Object element : elements) {
  251 | 				element = formatUriValue(conversionService, TypeDescriptor.nested(parameter, 1), element);
  252 | 				builder.queryParam(name, element);
  253 | 			}
  254 | 		}
  255 | 		else {
  256 | 			builder.queryParam(name, formatUriValue(conversionService, new TypeDescriptor(parameter), value));
  257 | 		}
  258 | 	}
  259 | 
  260 | 	protected @Nullable String formatUriValue(
  261 | 			@Nullable ConversionService cs, @Nullable TypeDescriptor sourceType, @Nullable Object value) {
  262 | 
  263 | 		if (value == null) {
  264 | 			return null;
  265 | 		}
  266 | 		else if (value instanceof String string) {
  267 | 			return string;
  268 | 		}
  269 | 		else if (cs != null) {
  270 | 			return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
  271 | 		}
  272 | 		else {
  273 | 			return value.toString();
  274 | 		}
  275 | 	}
  276 | 
  277 | 
  278 | 	private static class RequestParamNamedValueInfo extends NamedValueInfo {
  279 | 
  280 | 		public RequestParamNamedValueInfo() {
  281 | 			super("", false, ValueConstants.DEFAULT_NONE);
  282 | 		}
  283 | 
  284 | 		public RequestParamNamedValueInfo(RequestParam annotation) {
  285 | 			super(annotation.name(), annotation.required(), annotation.defaultValue());
  286 | 		}
  287 | 	}
  288 | 
  289 | }
```
