<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/support/ModelAndViewContainer.java -->
<!-- 正文引用：[[源码:ModelAndViewContainer.java#L起-L止]] ；代码块头部：(src: ModelAndViewContainer.java#L起-L止) -->

# ModelAndViewContainer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/support/ModelAndViewContainer.java` |
| 行数 | 323 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:18:38.267Z |

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
   19 | import java.util.HashSet;
   20 | import java.util.Map;
   21 | import java.util.Set;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.http.HttpStatusCode;
   26 | import org.springframework.ui.Model;
   27 | import org.springframework.ui.ModelMap;
   28 | import org.springframework.validation.support.BindingAwareModelMap;
   29 | import org.springframework.web.bind.support.SessionStatus;
   30 | import org.springframework.web.bind.support.SimpleSessionStatus;
   31 | 
   32 | /**
   33 |  * Records model and view related decisions made by
   34 |  * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} and
   35 |  * {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers} during the course of invocation of
   36 |  * a controller method.
   37 |  *
   38 |  * <p>The {@link #setRequestHandled} flag can be used to indicate the request
   39 |  * has been handled directly and view resolution is not required.
   40 |  *
   41 |  * <p>A default {@link Model} is automatically created at instantiation.
   42 |  * An alternate model instance may be provided via {@link #setRedirectModel}
   43 |  * for use in a redirect scenario. When {@link #setRedirectModelScenario} is set
   44 |  * to {@code true} signaling a redirect scenario, the {@link #getModel()}
   45 |  * returns the redirect model instead of the default model.
   46 |  *
   47 |  * @author Rossen Stoyanchev
   48 |  * @author Juergen Hoeller
   49 |  * @since 3.1
   50 |  */
   51 | public class ModelAndViewContainer {
   52 | 
   53 | 	private @Nullable Object view;
   54 | 
   55 | 	private final ModelMap defaultModel = new BindingAwareModelMap();
   56 | 
   57 | 	private @Nullable ModelMap redirectModel;
   58 | 
   59 | 	private boolean redirectModelScenario = false;
   60 | 
   61 | 	private @Nullable HttpStatusCode status;
   62 | 
   63 | 	private final Set<String> noBinding = new HashSet<>(4);
   64 | 
   65 | 	private final Set<String> bindingDisabled = new HashSet<>(4);
   66 | 
   67 | 	private final SessionStatus sessionStatus = new SimpleSessionStatus();
   68 | 
   69 | 	private boolean requestHandled = false;
   70 | 
   71 | 
   72 | 	/**
   73 | 	 * Set a view name to be resolved by the DispatcherServlet via a ViewResolver.
   74 | 	 * Will override any pre-existing view name or View.
   75 | 	 */
   76 | 	public void setViewName(@Nullable String viewName) {
   77 | 		this.view = viewName;
   78 | 	}
   79 | 
   80 | 	/**
   81 | 	 * Return the view name to be resolved by the DispatcherServlet via a
   82 | 	 * ViewResolver, or {@code null} if a View object is set.
   83 | 	 */
   84 | 	public @Nullable String getViewName() {
   85 | 		return (this.view instanceof String viewName ? viewName : null);
   86 | 	}
   87 | 
   88 | 	/**
   89 | 	 * Set a View object to be used by the DispatcherServlet.
   90 | 	 * Will override any pre-existing view name or View.
   91 | 	 */
   92 | 	public void setView(@Nullable Object view) {
   93 | 		this.view = view;
   94 | 	}
   95 | 
   96 | 	/**
   97 | 	 * Return the View object, or {@code null} if we are using a view name
   98 | 	 * to be resolved by the DispatcherServlet via a ViewResolver.
   99 | 	 */
  100 | 	public @Nullable Object getView() {
  101 | 		return this.view;
  102 | 	}
  103 | 
  104 | 	/**
  105 | 	 * Whether the view is a view reference specified via a name to be
  106 | 	 * resolved by the DispatcherServlet via a ViewResolver.
  107 | 	 */
  108 | 	public boolean isViewReference() {
  109 | 		return (this.view instanceof String);
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Return the model to use -- either the "default" or the "redirect" model.
  114 | 	 * The default model is used if {@code redirectModelScenario=false} or
  115 | 	 * there is no redirect model (i.e. RedirectAttributes was not declared as
  116 | 	 * a method argument) and {@code ignoreDefaultModelOnRedirect=false}.
  117 | 	 */
  118 | 	public ModelMap getModel() {
  119 | 		if (!this.redirectModelScenario) {
  120 | 			return this.defaultModel;
  121 | 		}
  122 | 		if (this.redirectModel == null) {
  123 | 			this.redirectModel = new ModelMap();
  124 | 		}
  125 | 		return this.redirectModel;
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Return the "default" model created at instantiation.
  130 | 	 * <p>In general it is recommended to use {@link #getModel()} instead which
  131 | 	 * returns either the "default" model (template rendering) or the "redirect"
  132 | 	 * model (redirect URL preparation). Use of this method may be needed for
  133 | 	 * advanced cases when access to the "default" model is needed regardless,
  134 | 	 * for example, to save model attributes specified via {@code @SessionAttributes}.
  135 | 	 * @return the default model (never {@code null})
  136 | 	 * @since 4.1.4
  137 | 	 */
  138 | 	public ModelMap getDefaultModel() {
  139 | 		return this.defaultModel;
  140 | 	}
  141 | 
  142 | 	/**
  143 | 	 * Provide a separate model instance to use in a redirect scenario.
  144 | 	 * <p>The provided additional model however is not used unless
  145 | 	 * {@link #setRedirectModelScenario} gets set to {@code true}
  146 | 	 * to signal an actual redirect scenario.
  147 | 	 */
  148 | 	public void setRedirectModel(ModelMap redirectModel) {
  149 | 		this.redirectModel = redirectModel;
  150 | 	}
  151 | 
  152 | 	/**
  153 | 	 * Whether the controller has returned a redirect instruction, for example, a
  154 | 	 * "redirect:" prefixed view name, a RedirectView instance, etc.
  155 | 	 */
  156 | 	public void setRedirectModelScenario(boolean redirectModelScenario) {
  157 | 		this.redirectModelScenario = redirectModelScenario;
  158 | 	}
  159 | 
  160 | 	/**
  161 | 	 * Provide an HTTP status that will be passed on to with the
  162 | 	 * {@code ModelAndView} used for view rendering purposes.
  163 | 	 * @since 4.3
  164 | 	 */
  165 | 	public void setStatus(@Nullable HttpStatusCode status) {
  166 | 		this.status = status;
  167 | 	}
  168 | 
  169 | 	/**
  170 | 	 * Return the configured HTTP status, if any.
  171 | 	 * @since 4.3
  172 | 	 */
  173 | 	public @Nullable HttpStatusCode getStatus() {
  174 | 		return this.status;
  175 | 	}
  176 | 
  177 | 	/**
  178 | 	 * Programmatically register an attribute for which data binding should not occur,
  179 | 	 * not even for a subsequent {@code @ModelAttribute} declaration.
  180 | 	 * @param attributeName the name of the attribute
  181 | 	 * @since 4.3
  182 | 	 */
  183 | 	public void setBindingDisabled(String attributeName) {
  184 | 		this.bindingDisabled.add(attributeName);
  185 | 	}
  186 | 
  187 | 	/**
  188 | 	 * Whether binding is disabled for the given model attribute.
  189 | 	 * @since 4.3
  190 | 	 */
  191 | 	public boolean isBindingDisabled(String name) {
  192 | 		return (this.bindingDisabled.contains(name) || this.noBinding.contains(name));
  193 | 	}
  194 | 
  195 | 	/**
  196 | 	 * Register whether data binding should occur for a corresponding model attribute,
  197 | 	 * corresponding to an {@code @ModelAttribute(binding=true/false)} declaration.
  198 | 	 * <p>Note: While this flag will be taken into account by {@link #isBindingDisabled},
  199 | 	 * a hard {@link #setBindingDisabled} declaration will always override it.
  200 | 	 * @param attributeName the name of the attribute
  201 | 	 * @since 4.3.13
  202 | 	 */
  203 | 	public void setBinding(String attributeName, boolean enabled) {
  204 | 		if (!enabled) {
  205 | 			this.noBinding.add(attributeName);
  206 | 		}
  207 | 		else {
  208 | 			this.noBinding.remove(attributeName);
  209 | 		}
  210 | 	}
  211 | 
  212 | 	/**
  213 | 	 * Return the {@link SessionStatus} instance to use that can be used to
  214 | 	 * signal that session processing is complete.
  215 | 	 */
  216 | 	public SessionStatus getSessionStatus() {
  217 | 		return this.sessionStatus;
  218 | 	}
  219 | 
  220 | 	/**
  221 | 	 * Whether the request has been handled fully within the handler, for example,
  222 | 	 * {@code @ResponseBody} method, and therefore view resolution is not
  223 | 	 * necessary. This flag can also be set when controller methods declare an
  224 | 	 * argument of type {@code ServletResponse} or {@code OutputStream}).
  225 | 	 * <p>The default value is {@code false}.
  226 | 	 */
  227 | 	public void setRequestHandled(boolean requestHandled) {
  228 | 		this.requestHandled = requestHandled;
  229 | 	}
  230 | 
  231 | 	/**
  232 | 	 * Whether the request has been handled fully within the handler.
  233 | 	 */
  234 | 	public boolean isRequestHandled() {
  235 | 		return this.requestHandled;
  236 | 	}
  237 | 
  238 | 	/**
  239 | 	 * Add the supplied attribute to the underlying model.
  240 | 	 * A shortcut for {@code getModel().addAttribute(String, Object)}.
  241 | 	 */
  242 | 	public ModelAndViewContainer addAttribute(String name, @Nullable Object value) {
  243 | 		getModel().addAttribute(name, value);
  244 | 		return this;
  245 | 	}
  246 | 
  247 | 	/**
  248 | 	 * Add the supplied attribute to the underlying model.
  249 | 	 * A shortcut for {@code getModel().addAttribute(Object)}.
  250 | 	 */
  251 | 	public ModelAndViewContainer addAttribute(Object value) {
  252 | 		getModel().addAttribute(value);
  253 | 		return this;
  254 | 	}
  255 | 
  256 | 	/**
  257 | 	 * Copy all attributes to the underlying model.
  258 | 	 * A shortcut for {@code getModel().addAllAttributes(Map)}.
  259 | 	 */
  260 | 	public ModelAndViewContainer addAllAttributes(@Nullable Map<String, ?> attributes) {
  261 | 		getModel().addAllAttributes(attributes);
  262 | 		return this;
  263 | 	}
  264 | 
  265 | 	/**
  266 | 	 * Copy attributes in the supplied {@code Map} with existing objects of
  267 | 	 * the same name taking precedence (i.e. not getting replaced).
  268 | 	 * A shortcut for {@code getModel().mergeAttributes(Map<String, ?>)}.
  269 | 	 */
  270 | 	public ModelAndViewContainer mergeAttributes(@Nullable Map<String, ?> attributes) {
  271 | 		getModel().mergeAttributes(attributes);
  272 | 		return this;
  273 | 	}
  274 | 
  275 | 	/**
  276 | 	 * Remove the given attributes from the model.
  277 | 	 */
  278 | 	public ModelAndViewContainer removeAttributes(@Nullable Map<String, ?> attributes) {
  279 | 		if (attributes != null) {
  280 | 			for (String key : attributes.keySet()) {
  281 | 				getModel().remove(key);
  282 | 			}
  283 | 		}
  284 | 		return this;
  285 | 	}
  286 | 
  287 | 	/**
  288 | 	 * Whether the underlying model contains the given attribute name.
  289 | 	 * A shortcut for {@code getModel().containsAttribute(String)}.
  290 | 	 */
  291 | 	public boolean containsAttribute(String name) {
  292 | 		return getModel().containsAttribute(name);
  293 | 	}
  294 | 
  295 | 
  296 | 	/**
  297 | 	 * Return diagnostic information.
  298 | 	 */
  299 | 	@Override
  300 | 	public String toString() {
  301 | 		StringBuilder sb = new StringBuilder("ModelAndViewContainer: ");
  302 | 		if (!isRequestHandled()) {
  303 | 			if (isViewReference()) {
  304 | 				sb.append("reference to view with name '").append(this.view).append('\'');
  305 | 			}
  306 | 			else {
  307 | 				sb.append("View is [").append(this.view).append(']');
  308 | 			}
  309 | 			if (!this.redirectModelScenario) {
  310 | 				sb.append("; default model ");
  311 | 			}
  312 | 			else {
  313 | 				sb.append("; redirect model ");
  314 | 			}
  315 | 			sb.append(getModel());
  316 | 		}
  317 | 		else {
  318 | 			sb.append("Request handled directly");
  319 | 		}
  320 | 		return sb.toString();
  321 | 	}
  322 | 
  323 | }
```
