<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/annotation/ModelFactory.java -->
<!-- 正文引用：[[源码:ModelFactory.java#L起-L止]] ；代码块头部：(src: ModelFactory.java#L起-L止) -->

# ModelFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/annotation/ModelFactory.java` |
| 行数 | 319 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:23.061Z |

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
   19 | import java.lang.reflect.Method;
   20 | import java.util.ArrayList;
   21 | import java.util.Collection;
   22 | import java.util.HashSet;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | import java.util.Set;
   26 | 
   27 | import org.apache.commons.logging.Log;
   28 | import org.apache.commons.logging.LogFactory;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | import org.springframework.beans.BeanUtils;
   32 | import org.springframework.core.Conventions;
   33 | import org.springframework.core.GenericTypeResolver;
   34 | import org.springframework.core.MethodParameter;
   35 | import org.springframework.ui.Model;
   36 | import org.springframework.ui.ModelMap;
   37 | import org.springframework.util.Assert;
   38 | import org.springframework.util.StringUtils;
   39 | import org.springframework.validation.BindingResult;
   40 | import org.springframework.web.bind.WebDataBinder;
   41 | import org.springframework.web.bind.annotation.ModelAttribute;
   42 | import org.springframework.web.bind.support.WebDataBinderFactory;
   43 | import org.springframework.web.context.request.NativeWebRequest;
   44 | import org.springframework.web.method.HandlerMethod;
   45 | import org.springframework.web.method.support.InvocableHandlerMethod;
   46 | import org.springframework.web.method.support.ModelAndViewContainer;
   47 | 
   48 | /**
   49 |  * Assist with initialization of the {@link Model} before controller method
   50 |  * invocation and with updates to it after the invocation.
   51 |  *
   52 |  * <p>On initialization the model is populated with attributes temporarily stored
   53 |  * in the session and through the invocation of {@code @ModelAttribute} methods.
   54 |  *
   55 |  * <p>On update model attributes are synchronized with the session and also
   56 |  * {@link BindingResult} attributes are added if missing.
   57 |  *
   58 |  * @author Rossen Stoyanchev
   59 |  * @since 3.1
   60 |  */
   61 | public final class ModelFactory {
   62 | 
   63 | 	private static final Log logger = LogFactory.getLog(ModelFactory.class);
   64 | 
   65 | 
   66 | 	private final List<ModelMethod> modelMethods = new ArrayList<>();
   67 | 
   68 | 	private final WebDataBinderFactory dataBinderFactory;
   69 | 
   70 | 	private final SessionAttributesHandler sessionAttributesHandler;
   71 | 
   72 | 
   73 | 	/**
   74 | 	 * Create a new instance with the given {@code @ModelAttribute} methods.
   75 | 	 * @param handlerMethods the {@code @ModelAttribute} methods to invoke
   76 | 	 * @param binderFactory for preparation of {@link BindingResult} attributes
   77 | 	 * @param attributeHandler for access to session attributes
   78 | 	 */
   79 | 	public ModelFactory(@Nullable List<InvocableHandlerMethod> handlerMethods,
   80 | 			WebDataBinderFactory binderFactory, SessionAttributesHandler attributeHandler) {
   81 | 
   82 | 		if (handlerMethods != null) {
   83 | 			for (InvocableHandlerMethod handlerMethod : handlerMethods) {
   84 | 				this.modelMethods.add(new ModelMethod(handlerMethod));
   85 | 			}
   86 | 		}
   87 | 		this.dataBinderFactory = binderFactory;
   88 | 		this.sessionAttributesHandler = attributeHandler;
   89 | 	}
   90 | 
   91 | 
   92 | 	/**
   93 | 	 * Populate the model in the following order:
   94 | 	 * <ol>
   95 | 	 * <li>Retrieve "known" session attributes listed as {@code @SessionAttributes}.
   96 | 	 * <li>Invoke {@code @ModelAttribute} methods
   97 | 	 * <li>Find {@code @ModelAttribute} method arguments also listed as
   98 | 	 * {@code @SessionAttributes} and ensure they're present in the model raising
   99 | 	 * an exception if necessary.
  100 | 	 * </ol>
  101 | 	 * @param request the current request
  102 | 	 * @param container a container with the model to be initialized
  103 | 	 * @param handlerMethod the method for which the model is initialized
  104 | 	 * @throws Exception may arise from {@code @ModelAttribute} methods
  105 | 	 */
  106 | 	public void initModel(NativeWebRequest request, ModelAndViewContainer container, HandlerMethod handlerMethod)
  107 | 			throws Exception {
  108 | 
  109 | 		Map<String, ?> sessionAttributes = this.sessionAttributesHandler.retrieveAttributes(request);
  110 | 		container.mergeAttributes(sessionAttributes);
  111 | 		invokeModelAttributeMethods(request, container);
  112 | 
  113 | 		for (String name : findSessionAttributeArguments(handlerMethod)) {
  114 | 			if (!container.containsAttribute(name)) {
  115 | 				Object value = this.sessionAttributesHandler.retrieveAttribute(request, name);
  116 | 				if (value == null) {
  117 | 					throw new IllegalStateException("Expected session attribute '" + name + "'");
  118 | 				}
  119 | 				container.addAttribute(name, value);
  120 | 			}
  121 | 		}
  122 | 	}
  123 | 
  124 | 	/**
  125 | 	 * Invoke model attribute methods to populate the model.
  126 | 	 * Attributes are added only if not already present in the model.
  127 | 	 */
  128 | 	private void invokeModelAttributeMethods(NativeWebRequest request, ModelAndViewContainer container)
  129 | 			throws Exception {
  130 | 
  131 | 		while (!this.modelMethods.isEmpty()) {
  132 | 			InvocableHandlerMethod modelMethod = getNextModelMethod(container).getHandlerMethod();
  133 | 			ModelAttribute ann = modelMethod.getMethodAnnotation(ModelAttribute.class);
  134 | 			Assert.state(ann != null, "No ModelAttribute annotation");
  135 | 			if (container.containsAttribute(ann.name())) {
  136 | 				if (!ann.binding()) {
  137 | 					container.setBindingDisabled(ann.name());
  138 | 				}
  139 | 				continue;
  140 | 			}
  141 | 
  142 | 			Object returnValue = modelMethod.invokeForRequest(request, container);
  143 | 			if (modelMethod.isVoid()) {
  144 | 				if (StringUtils.hasText(ann.value())) {
  145 | 					if (logger.isDebugEnabled()) {
  146 | 						logger.debug("Name in @ModelAttribute is ignored because method returns void: " +
  147 | 								modelMethod.getShortLogMessage());
  148 | 					}
  149 | 				}
  150 | 				continue;
  151 | 			}
  152 | 
  153 | 			String returnValueName = getNameForReturnValue(returnValue, modelMethod.getReturnType());
  154 | 			if (!ann.binding()) {
  155 | 				container.setBindingDisabled(returnValueName);
  156 | 			}
  157 | 			if (!container.containsAttribute(returnValueName)) {
  158 | 				container.addAttribute(returnValueName, returnValue);
  159 | 			}
  160 | 		}
  161 | 	}
  162 | 
  163 | 	private ModelMethod getNextModelMethod(ModelAndViewContainer container) {
  164 | 		for (ModelMethod modelMethod : this.modelMethods) {
  165 | 			if (modelMethod.checkDependencies(container)) {
  166 | 				this.modelMethods.remove(modelMethod);
  167 | 				return modelMethod;
  168 | 			}
  169 | 		}
  170 | 		ModelMethod modelMethod = this.modelMethods.get(0);
  171 | 		this.modelMethods.remove(modelMethod);
  172 | 		return modelMethod;
  173 | 	}
  174 | 
  175 | 	/**
  176 | 	 * Find {@code @ModelAttribute} arguments also listed as {@code @SessionAttributes}.
  177 | 	 */
  178 | 	private List<String> findSessionAttributeArguments(HandlerMethod handlerMethod) {
  179 | 		List<String> result = new ArrayList<>();
  180 | 		for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
  181 | 			if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
  182 | 				String name = getNameForParameter(parameter);
  183 | 				Class<?> paramType = parameter.getParameterType();
  184 | 				if (this.sessionAttributesHandler.isHandlerSessionAttribute(name, paramType)) {
  185 | 					result.add(name);
  186 | 				}
  187 | 			}
  188 | 		}
  189 | 		return result;
  190 | 	}
  191 | 
  192 | 	/**
  193 | 	 * Promote model attributes listed as {@code @SessionAttributes} to the session.
  194 | 	 * Add {@link BindingResult} attributes where necessary.
  195 | 	 * @param request the current request
  196 | 	 * @param container contains the model to update
  197 | 	 * @throws Exception if creating BindingResult attributes fails
  198 | 	 */
  199 | 	public void updateModel(NativeWebRequest request, ModelAndViewContainer container) throws Exception {
  200 | 		ModelMap defaultModel = container.getDefaultModel();
  201 | 		if (container.getSessionStatus().isComplete()){
  202 | 			this.sessionAttributesHandler.cleanupAttributes(request);
  203 | 		}
  204 | 		else {
  205 | 			this.sessionAttributesHandler.storeAttributes(request, defaultModel);
  206 | 		}
  207 | 		if (!container.isRequestHandled() && container.getModel() == defaultModel) {
  208 | 			updateBindingResult(request, defaultModel);
  209 | 		}
  210 | 	}
  211 | 
  212 | 	/**
  213 | 	 * Add {@link BindingResult} attributes to the model for attributes that require it.
  214 | 	 */
  215 | 	private void updateBindingResult(NativeWebRequest request, ModelMap model) throws Exception {
  216 | 		List<String> keyNames = new ArrayList<>(model.keySet());
  217 | 		for (String name : keyNames) {
  218 | 			Object value = model.get(name);
  219 | 			if (value != null && isBindingCandidate(name, value)) {
  220 | 				String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + name;
  221 | 				if (!model.containsAttribute(bindingResultKey)) {
  222 | 					WebDataBinder dataBinder = this.dataBinderFactory.createBinder(request, value, name);
  223 | 					model.put(bindingResultKey, dataBinder.getBindingResult());
  224 | 				}
  225 | 			}
  226 | 		}
  227 | 	}
  228 | 
  229 | 	/**
  230 | 	 * Whether the given attribute requires a {@link BindingResult} in the model.
  231 | 	 */
  232 | 	private boolean isBindingCandidate(String attributeName, Object value) {
  233 | 		if (attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
  234 | 			return false;
  235 | 		}
  236 | 		if (this.sessionAttributesHandler.isHandlerSessionAttribute(attributeName, value.getClass())) {
  237 | 			return true;
  238 | 		}
  239 | 		return (!value.getClass().isArray() && !(value instanceof Collection) &&
  240 | 				!(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
  241 | 	}
  242 | 
  243 | 
  244 | 	/**
  245 | 	 * Derive the model attribute name for the given method parameter based on
  246 | 	 * a {@code @ModelAttribute} parameter annotation (if present) or falling
  247 | 	 * back on parameter type based conventions.
  248 | 	 * @param parameter a descriptor for the method parameter
  249 | 	 * @return the derived name
  250 | 	 * @see Conventions#getVariableNameForParameter(MethodParameter)
  251 | 	 */
  252 | 	public static String getNameForParameter(MethodParameter parameter) {
  253 | 		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
  254 | 		String name = (ann != null ? ann.value() : null);
  255 | 		return (StringUtils.hasText(name) ? name : Conventions.getVariableNameForParameter(parameter));
  256 | 	}
  257 | 
  258 | 	/**
  259 | 	 * Derive the model attribute name for the given return value. Results will be
  260 | 	 * based on:
  261 | 	 * <ol>
  262 | 	 * <li>the method {@code ModelAttribute} annotation value
  263 | 	 * <li>the declared return type if it is more specific than {@code Object}
  264 | 	 * <li>the actual return value type
  265 | 	 * </ol>
  266 | 	 * @param returnValue the value returned from a method invocation
  267 | 	 * @param returnType a descriptor for the return type of the method
  268 | 	 * @return the derived name (never {@code null} or empty String)
  269 | 	 */
  270 | 	public static String getNameForReturnValue(@Nullable Object returnValue, MethodParameter returnType) {
  271 | 		ModelAttribute ann = returnType.getMethodAnnotation(ModelAttribute.class);
  272 | 		if (ann != null && StringUtils.hasText(ann.value())) {
  273 | 			return ann.value();
  274 | 		}
  275 | 		else {
  276 | 			Method method = returnType.getMethod();
  277 | 			Assert.state(method != null, "No handler method");
  278 | 			Class<?> containingClass = returnType.getContainingClass();
  279 | 			Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
  280 | 			return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
  281 | 		}
  282 | 	}
  283 | 
  284 | 
  285 | 	private static class ModelMethod {
  286 | 
  287 | 		private final InvocableHandlerMethod handlerMethod;
  288 | 
  289 | 		private final Set<String> dependencies = new HashSet<>();
  290 | 
  291 | 		public ModelMethod(InvocableHandlerMethod handlerMethod) {
  292 | 			this.handlerMethod = handlerMethod;
  293 | 			for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
  294 | 				if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
  295 | 					this.dependencies.add(getNameForParameter(parameter));
  296 | 				}
  297 | 			}
  298 | 		}
  299 | 
  300 | 		public InvocableHandlerMethod getHandlerMethod() {
  301 | 			return this.handlerMethod;
  302 | 		}
  303 | 
  304 | 		public boolean checkDependencies(ModelAndViewContainer mavContainer) {
  305 | 			for (String name : this.dependencies) {
  306 | 				if (!mavContainer.containsAttribute(name)) {
  307 | 					return false;
  308 | 				}
  309 | 			}
  310 | 			return true;
  311 | 		}
  312 | 
  313 | 		@Override
  314 | 		public String toString() {
  315 | 			return this.handlerMethod.getMethod().toGenericString();
  316 | 		}
  317 | 	}
  318 | 
  319 | }
```
