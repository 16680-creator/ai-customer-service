<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/HttpServletBean.java -->
<!-- 正文引用：[[源码:HttpServletBean.java#L起-L止]] ；代码块头部：(src: HttpServletBean.java#L起-L止) -->

# HttpServletBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/HttpServletBean.java` |
| 行数 | 244 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:08.733Z |

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
   19 | import java.util.Enumeration;
   20 | import java.util.HashSet;
   21 | import java.util.Set;
   22 | 
   23 | import jakarta.servlet.ServletConfig;
   24 | import jakarta.servlet.ServletException;
   25 | import jakarta.servlet.http.HttpServlet;
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.beans.BeanWrapper;
   31 | import org.springframework.beans.BeansException;
   32 | import org.springframework.beans.MutablePropertyValues;
   33 | import org.springframework.beans.PropertyAccessorFactory;
   34 | import org.springframework.beans.PropertyValue;
   35 | import org.springframework.beans.PropertyValues;
   36 | import org.springframework.context.EnvironmentAware;
   37 | import org.springframework.core.env.ConfigurableEnvironment;
   38 | import org.springframework.core.env.Environment;
   39 | import org.springframework.core.env.EnvironmentCapable;
   40 | import org.springframework.core.io.Resource;
   41 | import org.springframework.core.io.ResourceEditor;
   42 | import org.springframework.core.io.ResourceLoader;
   43 | import org.springframework.util.Assert;
   44 | import org.springframework.util.CollectionUtils;
   45 | import org.springframework.util.StringUtils;
   46 | import org.springframework.web.context.support.ServletContextResourceLoader;
   47 | import org.springframework.web.context.support.StandardServletEnvironment;
   48 | 
   49 | /**
   50 |  * Simple extension of {@link jakarta.servlet.http.HttpServlet} which treats
   51 |  * its config parameters ({@code init-param} entries within the
   52 |  * {@code servlet} tag in {@code web.xml}) as bean properties.
   53 |  *
   54 |  * <p>A handy superclass for any type of servlet. Type conversion of config
   55 |  * parameters is automatic, with the corresponding setter method getting
   56 |  * invoked with the converted value. It is also possible for subclasses to
   57 |  * specify required properties. Parameters without matching bean property
   58 |  * setters will simply be ignored.
   59 |  *
   60 |  * <p>This servlet leaves request handling to subclasses, inheriting the default
   61 |  * behavior of HttpServlet ({@code doGet}, {@code doPost}, etc).
   62 |  *
   63 |  * <p>This generic servlet base class has no dependency on the Spring
   64 |  * {@link org.springframework.context.ApplicationContext} concept. Simple
   65 |  * servlets usually don't load their own context but rather access service
   66 |  * beans from the Spring root application context, accessible via the
   67 |  * filter's {@link #getServletContext() ServletContext} (see
   68 |  * {@link org.springframework.web.context.support.WebApplicationContextUtils}).
   69 |  *
   70 |  * <p>The {@link FrameworkServlet} class is a more specific servlet base
   71 |  * class which loads its own application context. FrameworkServlet serves
   72 |  * as the direct base class of Spring's full-fledged {@link DispatcherServlet}.
   73 |  *
   74 |  * @author Rod Johnson
   75 |  * @author Juergen Hoeller
   76 |  * @see #addRequiredProperty
   77 |  * @see #initServletBean
   78 |  * @see #doGet
   79 |  * @see #doPost
   80 |  */
   81 | @SuppressWarnings("serial")
   82 | public abstract class HttpServletBean extends HttpServlet implements EnvironmentCapable, EnvironmentAware {
   83 | 
   84 | 	/** Logger available to subclasses. */
   85 | 	protected final Log logger = LogFactory.getLog(getClass());
   86 | 
   87 | 	private @Nullable ConfigurableEnvironment environment;
   88 | 
   89 | 	private final Set<String> requiredProperties = new HashSet<>(4);
   90 | 
   91 | 
   92 | 	/**
   93 | 	 * Subclasses can invoke this method to specify that this property
   94 | 	 * (which must match a JavaBean property they expose) is mandatory,
   95 | 	 * and must be supplied as a config parameter. This should be called
   96 | 	 * from the constructor of a subclass.
   97 | 	 * <p>This method is only relevant in case of traditional initialization
   98 | 	 * driven by a ServletConfig instance.
   99 | 	 * @param property name of the required property
  100 | 	 */
  101 | 	protected final void addRequiredProperty(String property) {
  102 | 		this.requiredProperties.add(property);
  103 | 	}
  104 | 
  105 | 	/**
  106 | 	 * Set the {@code Environment} that this servlet runs in.
  107 | 	 * <p>Any environment set here overrides the {@link StandardServletEnvironment}
  108 | 	 * provided by default.
  109 | 	 * @throws IllegalArgumentException if environment is not assignable to
  110 | 	 * {@code ConfigurableEnvironment}
  111 | 	 * @since 3.1
  112 | 	 */
  113 | 	@Override
  114 | 	public void setEnvironment(Environment environment) {
  115 | 		Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "ConfigurableEnvironment required");
  116 | 		this.environment = (ConfigurableEnvironment) environment;
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * Return the {@link Environment} associated with this servlet.
  121 | 	 * <p>If none specified, a default environment will be initialized via
  122 | 	 * {@link #createEnvironment()}.
  123 | 	 * @since 3.1
  124 | 	 */
  125 | 	@Override
  126 | 	public ConfigurableEnvironment getEnvironment() {
  127 | 		if (this.environment == null) {
  128 | 			this.environment = createEnvironment();
  129 | 		}
  130 | 		return this.environment;
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Create and return a new {@link StandardServletEnvironment}.
  135 | 	 * <p>Subclasses may override this in order to configure the environment or
  136 | 	 * specialize the environment type returned.
  137 | 	 * @since 3.1
  138 | 	 */
  139 | 	protected ConfigurableEnvironment createEnvironment() {
  140 | 		return new StandardServletEnvironment();
  141 | 	}
  142 | 
  143 | 	/**
  144 | 	 * Map config parameters onto bean properties of this servlet, and
  145 | 	 * invoke subclass initialization.
  146 | 	 * @throws ServletException if bean properties are invalid (or required
  147 | 	 * properties are missing), or if subclass initialization fails.
  148 | 	 */
  149 | 	@Override
  150 | 	public final void init() throws ServletException {
  151 | 
  152 | 		// Set bean properties from init parameters.
  153 | 		PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
  154 | 		if (!pvs.isEmpty()) {
  155 | 			try {
  156 | 				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
  157 | 				ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
  158 | 				bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
  159 | 				initBeanWrapper(bw);
  160 | 				bw.setPropertyValues(pvs, true);
  161 | 			}
  162 | 			catch (BeansException ex) {
  163 | 				if (logger.isErrorEnabled()) {
  164 | 					logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
  165 | 				}
  166 | 				throw ex;
  167 | 			}
  168 | 		}
  169 | 
  170 | 		// Let subclasses do whatever initialization they like.
  171 | 		initServletBean();
  172 | 	}
  173 | 
  174 | 	/**
  175 | 	 * Initialize the BeanWrapper for this HttpServletBean,
  176 | 	 * possibly with custom editors.
  177 | 	 * <p>This default implementation is empty.
  178 | 	 * @param bw the BeanWrapper to initialize
  179 | 	 * @throws BeansException if thrown by BeanWrapper methods
  180 | 	 * @see org.springframework.beans.BeanWrapper#registerCustomEditor
  181 | 	 */
  182 | 	protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
  183 | 	}
  184 | 
  185 | 	/**
  186 | 	 * Subclasses may override this to perform custom initialization.
  187 | 	 * All bean properties of this servlet will have been set before this
  188 | 	 * method is invoked.
  189 | 	 * <p>This default implementation is empty.
  190 | 	 * @throws ServletException if subclass initialization fails
  191 | 	 */
  192 | 	protected void initServletBean() throws ServletException {
  193 | 	}
  194 | 
  195 | 	/**
  196 | 	 * Overridden method that simply returns {@code null} when no
  197 | 	 * ServletConfig set yet.
  198 | 	 * @see #getServletConfig()
  199 | 	 */
  200 | 	@Override
  201 | 	public @Nullable String getServletName() {
  202 | 		return (getServletConfig() != null ? getServletConfig().getServletName() : null);
  203 | 	}
  204 | 
  205 | 
  206 | 	/**
  207 | 	 * PropertyValues implementation created from ServletConfig init parameters.
  208 | 	 */
  209 | 	private static class ServletConfigPropertyValues extends MutablePropertyValues {
  210 | 
  211 | 		/**
  212 | 		 * Create new ServletConfigPropertyValues.
  213 | 		 * @param config the ServletConfig we'll use to take PropertyValues from
  214 | 		 * @param requiredProperties set of property names we need, where
  215 | 		 * we can't accept default values
  216 | 		 * @throws ServletException if any required properties are missing
  217 | 		 */
  218 | 		public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
  219 | 				throws ServletException {
  220 | 
  221 | 			Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ?
  222 | 					new HashSet<>(requiredProperties) : null);
  223 | 
  224 | 			Enumeration<String> paramNames = config.getInitParameterNames();
  225 | 			while (paramNames.hasMoreElements()) {
  226 | 				String property = paramNames.nextElement();
  227 | 				Object value = config.getInitParameter(property);
  228 | 				addPropertyValue(new PropertyValue(property, value));
  229 | 				if (missingProps != null) {
  230 | 					missingProps.remove(property);
  231 | 				}
  232 | 			}
  233 | 
  234 | 			// Fail if we are still missing properties.
  235 | 			if (!CollectionUtils.isEmpty(missingProps)) {
  236 | 				throw new ServletException(
  237 | 						"Initialization from ServletConfig for servlet '" + config.getServletName() +
  238 | 						"' failed; the following required properties were missing: " +
  239 | 						StringUtils.collectionToDelimitedString(missingProps, ", "));
  240 | 			}
  241 | 		}
  242 | 	}
  243 | 
  244 | }
```
