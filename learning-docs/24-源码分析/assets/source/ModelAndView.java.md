<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/ModelAndView.java -->
<!-- 正文引用：[[源码:ModelAndView.java#L起-L止]] ；代码块头部：(src: ModelAndView.java#L起-L止) -->

# ModelAndView.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/ModelAndView.java` |
| 行数 | 356 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:09.776Z |

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
   19 | import java.util.Map;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.http.HttpStatusCode;
   24 | import org.springframework.ui.ModelMap;
   25 | import org.springframework.util.CollectionUtils;
   26 | 
   27 | /**
   28 |  * Holder for both Model and View in the web MVC framework.
   29 |  * Note that these are entirely distinct. This class merely holds
   30 |  * both to make it possible for a controller to return both model
   31 |  * and view in a single return value.
   32 |  *
   33 |  * <p>Represents a model and view returned by a handler, to be resolved
   34 |  * by a DispatcherServlet. The view can take the form of a String
   35 |  * view name which will need to be resolved by a ViewResolver object;
   36 |  * alternatively a View object can be specified directly. The model
   37 |  * is a Map, allowing the use of multiple objects keyed by name.
   38 |  *
   39 |  * @author Rod Johnson
   40 |  * @author Juergen Hoeller
   41 |  * @author Rob Harrop
   42 |  * @author Rossen Stoyanchev
   43 |  * @see DispatcherServlet
   44 |  * @see ViewResolver
   45 |  * @see HandlerAdapter#handle
   46 |  * @see org.springframework.web.servlet.mvc.Controller#handleRequest
   47 |  */
   48 | public class ModelAndView {
   49 | 
   50 | 	/** View instance or view name String. */
   51 | 	private @Nullable Object view;
   52 | 
   53 | 	/** Model Map. */
   54 | 	private @Nullable ModelMap model;
   55 | 
   56 | 	/** Optional HTTP status for the response. */
   57 | 	private @Nullable HttpStatusCode status;
   58 | 
   59 | 	/** Indicates whether this instance has been cleared with a call to {@link #clear()}. */
   60 | 	private boolean cleared = false;
   61 | 
   62 | 
   63 | 	/**
   64 | 	 * Default constructor for bean-style usage: populating bean
   65 | 	 * properties instead of passing in constructor arguments.
   66 | 	 * @see #setView(View)
   67 | 	 * @see #setViewName(String)
   68 | 	 */
   69 | 	public ModelAndView() {
   70 | 	}
   71 | 
   72 | 	/**
   73 | 	 * Convenient constructor when there is no model data to expose.
   74 | 	 * Can also be used in conjunction with {@code addObject}.
   75 | 	 * @param viewName name of the View to render, to be resolved
   76 | 	 * by the DispatcherServlet's ViewResolver
   77 | 	 * @see #addObject
   78 | 	 */
   79 | 	public ModelAndView(String viewName) {
   80 | 		this.view = viewName;
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Convenient constructor when there is no model data to expose.
   85 | 	 * Can also be used in conjunction with {@code addObject}.
   86 | 	 * @param view the View object to render
   87 | 	 * @see #addObject
   88 | 	 */
   89 | 	public ModelAndView(View view) {
   90 | 		this.view = view;
   91 | 	}
   92 | 
   93 | 	/**
   94 | 	 * Create a new ModelAndView given a view name and a model.
   95 | 	 * @param viewName name of the View to render, to be resolved
   96 | 	 * by the DispatcherServlet's ViewResolver
   97 | 	 * @param model a Map of model names (Strings) to model objects
   98 | 	 * (Objects). Model entries may not be {@code null}, but the
   99 | 	 * model Map may be {@code null} if there is no model data.
  100 | 	 */
  101 | 	public ModelAndView(String viewName, @Nullable Map<String, ?> model) {
  102 | 		this.view = viewName;
  103 | 		if (model != null) {
  104 | 			getModelMap().addAllAttributes(model);
  105 | 		}
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * Create a new ModelAndView given a View object and a model.
  110 | 	 * <em>Note: the supplied model data is copied into the internal
  111 | 	 * storage of this class. You should not consider to modify the supplied
  112 | 	 * Map after supplying it to this class</em>
  113 | 	 * @param view the View object to render
  114 | 	 * @param model a Map of model names (Strings) to model objects
  115 | 	 * (Objects). Model entries may not be {@code null}, but the
  116 | 	 * model Map may be {@code null} if there is no model data.
  117 | 	 */
  118 | 	public ModelAndView(View view, @Nullable Map<String, ?> model) {
  119 | 		this.view = view;
  120 | 		if (model != null) {
  121 | 			getModelMap().addAllAttributes(model);
  122 | 		}
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Create a new ModelAndView given a view name and HTTP status.
  127 | 	 * @param viewName name of the View to render, to be resolved
  128 | 	 * by the DispatcherServlet's ViewResolver
  129 | 	 * @param status an HTTP status code to use for the response
  130 | 	 * (to be set just prior to View rendering)
  131 | 	 * @since 4.3.8
  132 | 	 */
  133 | 	public ModelAndView(String viewName, HttpStatusCode status) {
  134 | 		this.view = viewName;
  135 | 		this.status = status;
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * Create a new ModelAndView given a view name, model, and HTTP status.
  140 | 	 * @param viewName name of the View to render, to be resolved
  141 | 	 * by the DispatcherServlet's ViewResolver
  142 | 	 * @param model a Map of model names (Strings) to model objects
  143 | 	 * (Objects). Model entries may not be {@code null}, but the
  144 | 	 * model Map may be {@code null} if there is no model data.
  145 | 	 * @param status an HTTP status code to use for the response
  146 | 	 * (to be set just prior to View rendering)
  147 | 	 * @since 4.3
  148 | 	 */
  149 | 	public ModelAndView(@Nullable String viewName, @Nullable Map<String, ?> model, @Nullable HttpStatusCode status) {
  150 | 		this.view = viewName;
  151 | 		if (model != null) {
  152 | 			getModelMap().addAllAttributes(model);
  153 | 		}
  154 | 		this.status = status;
  155 | 	}
  156 | 
  157 | 	/**
  158 | 	 * Convenient constructor to take a single model object.
  159 | 	 * @param viewName name of the View to render, to be resolved
  160 | 	 * by the DispatcherServlet's ViewResolver
  161 | 	 * @param modelName name of the single entry in the model
  162 | 	 * @param modelObject the single model object
  163 | 	 */
  164 | 	public ModelAndView(String viewName, String modelName, Object modelObject) {
  165 | 		this.view = viewName;
  166 | 		addObject(modelName, modelObject);
  167 | 	}
  168 | 
  169 | 	/**
  170 | 	 * Convenient constructor to take a single model object.
  171 | 	 * @param view the View object to render
  172 | 	 * @param modelName name of the single entry in the model
  173 | 	 * @param modelObject the single model object
  174 | 	 */
  175 | 	public ModelAndView(View view, String modelName, Object modelObject) {
  176 | 		this.view = view;
  177 | 		addObject(modelName, modelObject);
  178 | 	}
  179 | 
  180 | 
  181 | 	/**
  182 | 	 * Set a view name for this ModelAndView, to be resolved by the
  183 | 	 * DispatcherServlet via a ViewResolver. Will override any
  184 | 	 * pre-existing view name or View.
  185 | 	 */
  186 | 	public void setViewName(@Nullable String viewName) {
  187 | 		this.view = viewName;
  188 | 	}
  189 | 
  190 | 	/**
  191 | 	 * Return the view name to be resolved by the DispatcherServlet
  192 | 	 * via a ViewResolver, or {@code null} if we are using a View object.
  193 | 	 */
  194 | 	public @Nullable String getViewName() {
  195 | 		return (this.view instanceof String name ? name : null);
  196 | 	}
  197 | 
  198 | 	/**
  199 | 	 * Set a View object for this ModelAndView. Will override any
  200 | 	 * pre-existing view name or View.
  201 | 	 */
  202 | 	public void setView(@Nullable View view) {
  203 | 		this.view = view;
  204 | 	}
  205 | 
  206 | 	/**
  207 | 	 * Return the View object, or {@code null} if we are using a view name
  208 | 	 * to be resolved by the DispatcherServlet via a ViewResolver.
  209 | 	 */
  210 | 	public @Nullable View getView() {
  211 | 		return (this.view instanceof View v ? v : null);
  212 | 	}
  213 | 
  214 | 	/**
  215 | 	 * Indicate whether this {@code ModelAndView} has a view, either
  216 | 	 * as a view name or as a direct {@link View} instance.
  217 | 	 */
  218 | 	public boolean hasView() {
  219 | 		return (this.view != null);
  220 | 	}
  221 | 
  222 | 	/**
  223 | 	 * Return whether we use a view reference, i.e. {@code true}
  224 | 	 * if the view has been specified via a name to be resolved by the
  225 | 	 * DispatcherServlet via a ViewResolver.
  226 | 	 */
  227 | 	public boolean isReference() {
  228 | 		return (this.view instanceof String);
  229 | 	}
  230 | 
  231 | 	/**
  232 | 	 * Return the model map. May return {@code null}.
  233 | 	 * Called by DispatcherServlet for evaluation of the model.
  234 | 	 */
  235 | 	protected @Nullable Map<String, Object> getModelInternal() {
  236 | 		return this.model;
  237 | 	}
  238 | 
  239 | 	/**
  240 | 	 * Return the underlying {@code ModelMap} instance (never {@code null}).
  241 | 	 */
  242 | 	public ModelMap getModelMap() {
  243 | 		if (this.model == null) {
  244 | 			this.model = new ModelMap();
  245 | 		}
  246 | 		return this.model;
  247 | 	}
  248 | 
  249 | 	/**
  250 | 	 * Return the model map. Never returns {@code null}.
  251 | 	 * To be called by application code for modifying the model.
  252 | 	 */
  253 | 	public Map<String, Object> getModel() {
  254 | 		return getModelMap();
  255 | 	}
  256 | 
  257 | 	/**
  258 | 	 * Set the HTTP status to use for the response.
  259 | 	 * <p>The response status is set just prior to View rendering.
  260 | 	 * @since 4.3
  261 | 	 */
  262 | 	public void setStatus(@Nullable HttpStatusCode status) {
  263 | 		this.status = status;
  264 | 	}
  265 | 
  266 | 	/**
  267 | 	 * Return the configured HTTP status for the response, if any.
  268 | 	 * @since 4.3
  269 | 	 */
  270 | 	public @Nullable HttpStatusCode getStatus() {
  271 | 		return this.status;
  272 | 	}
  273 | 
  274 | 
  275 | 	/**
  276 | 	 * Add an attribute to the model.
  277 | 	 * @param attributeName name of the object to add to the model (never {@code null})
  278 | 	 * @param attributeValue object to add to the model (can be {@code null})
  279 | 	 * @see ModelMap#addAttribute(String, Object)
  280 | 	 * @see #getModelMap()
  281 | 	 */
  282 | 	public ModelAndView addObject(String attributeName, @Nullable Object attributeValue) {
  283 | 		getModelMap().addAttribute(attributeName, attributeValue);
  284 | 		return this;
  285 | 	}
  286 | 
  287 | 	/**
  288 | 	 * Add an attribute to the model using parameter name generation.
  289 | 	 * @param attributeValue the object to add to the model (never {@code null})
  290 | 	 * @see ModelMap#addAttribute(Object)
  291 | 	 * @see #getModelMap()
  292 | 	 */
  293 | 	public ModelAndView addObject(Object attributeValue) {
  294 | 		getModelMap().addAttribute(attributeValue);
  295 | 		return this;
  296 | 	}
  297 | 
  298 | 	/**
  299 | 	 * Add all attributes contained in the provided Map to the model.
  300 | 	 * @param modelMap a Map of attributeName &rarr; attributeValue pairs
  301 | 	 * @see ModelMap#addAllAttributes(Map)
  302 | 	 * @see #getModelMap()
  303 | 	 */
  304 | 	public ModelAndView addAllObjects(@Nullable Map<String, ?> modelMap) {
  305 | 		getModelMap().addAllAttributes(modelMap);
  306 | 		return this;
  307 | 	}
  308 | 
  309 | 
  310 | 	/**
  311 | 	 * Clear the state of this ModelAndView object.
  312 | 	 * The object will be empty afterwards.
  313 | 	 * <p>Can be used to suppress rendering of a given ModelAndView object
  314 | 	 * in the {@code postHandle} method of a HandlerInterceptor.
  315 | 	 * @see #isEmpty()
  316 | 	 * @see HandlerInterceptor#postHandle
  317 | 	 */
  318 | 	public void clear() {
  319 | 		this.view = null;
  320 | 		this.model = null;
  321 | 		this.cleared = true;
  322 | 	}
  323 | 
  324 | 	/**
  325 | 	 * Return whether this ModelAndView object is empty,
  326 | 	 * i.e. whether it does not hold any view and does not contain a model.
  327 | 	 */
  328 | 	public boolean isEmpty() {
  329 | 		return (this.view == null && CollectionUtils.isEmpty(this.model));
  330 | 	}
  331 | 
  332 | 	/**
  333 | 	 * Return whether this ModelAndView object is empty as a result of a call to {@link #clear}
  334 | 	 * i.e. whether it does not hold any view and does not contain a model.
  335 | 	 * <p>Returns {@code false} if any additional state was added to the instance
  336 | 	 * <strong>after</strong> the call to {@link #clear}.
  337 | 	 * @see #clear()
  338 | 	 */
  339 | 	public boolean wasCleared() {
  340 | 		return (this.cleared && isEmpty());
  341 | 	}
  342 | 
  343 | 
  344 | 	/**
  345 | 	 * Return diagnostic information about this model and view.
  346 | 	 */
  347 | 	@Override
  348 | 	public String toString() {
  349 | 		return "ModelAndView [view=" + formatView() + "; model=" + this.model + "]";
  350 | 	}
  351 | 
  352 | 	private String formatView() {
  353 | 		return isReference() ? "\"" + this.view + "\"" : "[" + this.view + "]";
  354 | 	}
  355 | 
  356 | }
```
