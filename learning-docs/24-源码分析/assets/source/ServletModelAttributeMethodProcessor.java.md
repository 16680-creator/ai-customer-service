<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/ServletModelAttributeMethodProcessor.java -->
<!-- 正文引用：[[源码:ServletModelAttributeMethodProcessor.java#L起-L止]] ；代码块头部：(src: ServletModelAttributeMethodProcessor.java#L起-L止) -->

# ServletModelAttributeMethodProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/ServletModelAttributeMethodProcessor.java` |
| 行数 | 168 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:18:37.409Z |

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
   22 | import jakarta.servlet.ServletRequest;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.core.MethodParameter;
   26 | import org.springframework.core.convert.ConversionService;
   27 | import org.springframework.core.convert.TypeDescriptor;
   28 | import org.springframework.core.convert.converter.Converter;
   29 | import org.springframework.util.Assert;
   30 | import org.springframework.util.StringUtils;
   31 | import org.springframework.validation.DataBinder;
   32 | import org.springframework.web.bind.ServletRequestDataBinder;
   33 | import org.springframework.web.bind.WebDataBinder;
   34 | import org.springframework.web.bind.support.WebDataBinderFactory;
   35 | import org.springframework.web.context.request.NativeWebRequest;
   36 | import org.springframework.web.context.request.RequestAttributes;
   37 | import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
   38 | import org.springframework.web.servlet.HandlerMapping;
   39 | 
   40 | /**
   41 |  * A Servlet-specific {@link ModelAttributeMethodProcessor} that applies data
   42 |  * binding through a WebDataBinder of type {@link ServletRequestDataBinder}.
   43 |  *
   44 |  * <p>Also adds a fall-back strategy to instantiate the model attribute from a
   45 |  * URI template variable or from a request parameter if the name matches the
   46 |  * model attribute name and there is an appropriate type conversion strategy.
   47 |  *
   48 |  * @author Rossen Stoyanchev
   49 |  * @author Juergen Hoeller
   50 |  * @since 3.1
   51 |  */
   52 | public class ServletModelAttributeMethodProcessor extends ModelAttributeMethodProcessor {
   53 | 
   54 | 	/**
   55 | 	 * Class constructor.
   56 | 	 * @param annotationNotRequired if "true", non-simple method arguments and
   57 | 	 * return values are considered model attributes with or without a
   58 | 	 * {@code @ModelAttribute} annotation
   59 | 	 */
   60 | 	public ServletModelAttributeMethodProcessor(boolean annotationNotRequired) {
   61 | 		super(annotationNotRequired);
   62 | 	}
   63 | 
   64 | 
   65 | 	/**
   66 | 	 * Instantiate the model attribute from a URI template variable or from a
   67 | 	 * request parameter if the name matches to the model attribute name and
   68 | 	 * if there is an appropriate type conversion strategy. If none of these
   69 | 	 * are true delegate back to the base class.
   70 | 	 * @see #createAttributeFromRequestValue
   71 | 	 */
   72 | 	@Override
   73 | 	protected final @Nullable Object createAttribute(String attributeName, MethodParameter parameter,
   74 | 			WebDataBinderFactory binderFactory, NativeWebRequest request) throws Exception {
   75 | 
   76 | 		String value = getRequestValueForAttribute(attributeName, request);
   77 | 		if (value != null) {
   78 | 			Object attribute = createAttributeFromRequestValue(
   79 | 					value, attributeName, parameter, binderFactory, request);
   80 | 			if (attribute != null) {
   81 | 				return attribute;
   82 | 			}
   83 | 		}
   84 | 
   85 | 		return super.createAttribute(attributeName, parameter, binderFactory, request);
   86 | 	}
   87 | 
   88 | 	/**
   89 | 	 * Obtain a value from the request that may be used to instantiate the
   90 | 	 * model attribute through type conversion from String to the target type.
   91 | 	 * <p>The default implementation looks for the attribute name to match
   92 | 	 * a URI variable first and then a request parameter.
   93 | 	 * @param attributeName the model attribute name
   94 | 	 * @param request the current request
   95 | 	 * @return the request value to try to convert, or {@code null} if none
   96 | 	 */
   97 | 	protected @Nullable String getRequestValueForAttribute(String attributeName, NativeWebRequest request) {
   98 | 		Map<String, String> variables = getUriTemplateVariables(request);
   99 | 		String variableValue = variables.get(attributeName);
  100 | 		if (StringUtils.hasText(variableValue)) {
  101 | 			return variableValue;
  102 | 		}
  103 | 		String parameterValue = request.getParameter(attributeName);
  104 | 		if (StringUtils.hasText(parameterValue)) {
  105 | 			return parameterValue;
  106 | 		}
  107 | 		return null;
  108 | 	}
  109 | 
  110 | 	protected final Map<String, String> getUriTemplateVariables(NativeWebRequest request) {
  111 | 		@SuppressWarnings("unchecked")
  112 | 		Map<String, String> variables = (Map<String, String>) request.getAttribute(
  113 | 				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
  114 | 		return (variables != null ? variables : Collections.emptyMap());
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * Create a model attribute from a String request value (for example, URI template
  119 | 	 * variable, request parameter) using type conversion.
  120 | 	 * <p>The default implementation converts only if there is a registered
  121 | 	 * {@link Converter} that can perform the conversion.
  122 | 	 * @param sourceValue the source value to create the model attribute from
  123 | 	 * @param attributeName the name of the attribute (never {@code null})
  124 | 	 * @param parameter the method parameter
  125 | 	 * @param binderFactory for creating WebDataBinder instance
  126 | 	 * @param request the current request
  127 | 	 * @return the created model attribute, or {@code null} if no suitable
  128 | 	 * conversion found
  129 | 	 */
  130 | 	protected @Nullable Object createAttributeFromRequestValue(String sourceValue, String attributeName,
  131 | 			MethodParameter parameter, WebDataBinderFactory binderFactory, NativeWebRequest request)
  132 | 			throws Exception {
  133 | 
  134 | 		DataBinder binder = binderFactory.createBinder(request, null, attributeName);
  135 | 		ConversionService conversionService = binder.getConversionService();
  136 | 		if (conversionService != null) {
  137 | 			TypeDescriptor source = TypeDescriptor.valueOf(String.class);
  138 | 			TypeDescriptor target = new TypeDescriptor(parameter);
  139 | 			if (conversionService.canConvert(source, target)) {
  140 | 				return binder.convertIfNecessary(sourceValue, parameter.getParameterType(), parameter);
  141 | 			}
  142 | 		}
  143 | 		return null;
  144 | 	}
  145 | 
  146 | 	/**
  147 | 	 * Downcast to {@link ServletRequestDataBinder} to invoke {@code constructTarget(ServletRequest)}.
  148 | 	 */
  149 | 	@Override
  150 | 	protected void constructAttribute(WebDataBinder binder, NativeWebRequest request) {
  151 | 		ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
  152 | 		Assert.state(servletRequest != null, "No ServletRequest");
  153 | 		ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;
  154 | 		servletBinder.construct(servletRequest);
  155 | 	}
  156 | 
  157 | 	/**
  158 | 	 * Downcast to {@link ServletRequestDataBinder} to invoke {@code bind(ServletRequest)}.
  159 | 	 */
  160 | 	@Override
  161 | 	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
  162 | 		ServletRequest servletRequest = request.getNativeRequest(ServletRequest.class);
  163 | 		Assert.state(servletRequest != null, "No ServletRequest");
  164 | 		ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;
  165 | 		servletBinder.bind(servletRequest);
  166 | 	}
  167 | 
  168 | }
```
