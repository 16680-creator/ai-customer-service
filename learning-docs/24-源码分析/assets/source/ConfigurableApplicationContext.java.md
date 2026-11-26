<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/ConfigurableApplicationContext.java -->
<!-- 正文引用：[[源码:ConfigurableApplicationContext.java#L起-L止]] ；代码块头部：(src: ConfigurableApplicationContext.java#L起-L止) -->

# ConfigurableApplicationContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/ConfigurableApplicationContext.java` |
| 行数 | 312 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-26T09:24:52.382Z |

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
   17 | package org.springframework.context;
   18 | 
   19 | import java.io.Closeable;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.beans.BeansException;
   24 | import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
   25 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   26 | import org.springframework.core.env.ConfigurableEnvironment;
   27 | import org.springframework.core.io.ProtocolResolver;
   28 | import org.springframework.core.metrics.ApplicationStartup;
   29 | 
   30 | /**
   31 |  * SPI interface to be implemented by most if not all application contexts.
   32 |  * Provides facilities to configure an application context in addition
   33 |  * to the application context client methods in the
   34 |  * {@link org.springframework.context.ApplicationContext} interface.
   35 |  *
   36 |  * <p>Configuration and lifecycle methods are encapsulated here to avoid
   37 |  * making them obvious to ApplicationContext client code. The present
   38 |  * methods should only be used by startup and shutdown code.
   39 |  *
   40 |  * @author Juergen Hoeller
   41 |  * @author Chris Beams
   42 |  * @author Sam Brannen
   43 |  * @since 03.11.2003
   44 |  */
   45 | public interface ConfigurableApplicationContext extends ApplicationContext, Lifecycle, Closeable {
   46 | 
   47 | 	/**
   48 | 	 * Any number of these characters are considered delimiters between multiple
   49 | 	 * context config paths in a single {@code String} value: {@value}.
   50 | 	 * @see org.springframework.context.support.AbstractXmlApplicationContext#setConfigLocation
   51 | 	 * @see org.springframework.web.context.ContextLoader#CONFIG_LOCATION_PARAM
   52 | 	 * @see org.springframework.web.servlet.FrameworkServlet#setContextConfigLocation
   53 | 	 */
   54 | 	String CONFIG_LOCATION_DELIMITERS = ",; \t\n";
   55 | 
   56 | 	/**
   57 | 	 * The name of the {@linkplain java.util.concurrent.Executor bootstrap executor}
   58 | 	 * bean in the context: {@value}.
   59 | 	 * <p>If none is supplied, no background bootstrapping will be active.
   60 | 	 * @since 6.2
   61 | 	 * @see java.util.concurrent.Executor
   62 | 	 * @see org.springframework.core.task.TaskExecutor
   63 | 	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setBootstrapExecutor
   64 | 	 */
   65 | 	String BOOTSTRAP_EXECUTOR_BEAN_NAME = "bootstrapExecutor";
   66 | 
   67 | 	/**
   68 | 	 * Name of the {@code ConversionService} bean in the factory: {@value}.
   69 | 	 * <p>If none is supplied, default conversion rules apply.
   70 | 	 * @since 3.0
   71 | 	 * @see org.springframework.core.convert.ConversionService
   72 | 	 */
   73 | 	String CONVERSION_SERVICE_BEAN_NAME = "conversionService";
   74 | 
   75 | 	/**
   76 | 	 * Name of the {@code LoadTimeWeaver} bean in the factory: {@value}.
   77 | 	 * <p>If such a bean is supplied, the context will use a temporary {@link ClassLoader}
   78 | 	 * for type matching, in order to allow the {@code LoadTimeWeaver} to process
   79 | 	 * all actual bean classes.
   80 | 	 * @since 2.5
   81 | 	 * @see org.springframework.instrument.classloading.LoadTimeWeaver
   82 | 	 */
   83 | 	String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";
   84 | 
   85 | 	/**
   86 | 	 * Name of the {@link org.springframework.core.env.Environment Environment}
   87 | 	 * bean in the factory: {@value}.
   88 | 	 * @since 3.1
   89 | 	 */
   90 | 	String ENVIRONMENT_BEAN_NAME = "environment";
   91 | 
   92 | 	/**
   93 | 	 * Name of the JVM System properties bean in the factory: {@value}.
   94 | 	 * @see java.lang.System#getProperties()
   95 | 	 */
   96 | 	String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";
   97 | 
   98 | 	/**
   99 | 	 * Name of the Operating System environment bean in the factory: {@value}.
  100 | 	 * @see java.lang.System#getenv()
  101 | 	 */
  102 | 	String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";
  103 | 
  104 | 	/**
  105 | 	 * Name of the {@link ApplicationStartup} bean in the factory: {@value}.
  106 | 	 * @since 5.3
  107 | 	 */
  108 | 	String APPLICATION_STARTUP_BEAN_NAME = "applicationStartup";
  109 | 
  110 | 	/**
  111 | 	 * {@linkplain Thread#getName() Name} of the {@linkplain #registerShutdownHook()
  112 | 	 * shutdown hook} thread: {@value}.
  113 | 	 * @since 5.2
  114 | 	 * @see #registerShutdownHook()
  115 | 	 */
  116 | 	String SHUTDOWN_HOOK_THREAD_NAME = "SpringContextShutdownHook";
  117 | 
  118 | 
  119 | 	/**
  120 | 	 * Set the unique ID of this application context.
  121 | 	 * @since 3.0
  122 | 	 */
  123 | 	void setId(String id);
  124 | 
  125 | 	/**
  126 | 	 * Set the parent of this application context.
  127 | 	 * <p>Note that the parent shouldn't be changed: It should only be set outside
  128 | 	 * a constructor if it isn't available when an object of this class is created,
  129 | 	 * for example in case of WebApplicationContext setup.
  130 | 	 * @param parent the parent context
  131 | 	 * @see org.springframework.web.context.ConfigurableWebApplicationContext
  132 | 	 */
  133 | 	void setParent(@Nullable ApplicationContext parent);
  134 | 
  135 | 	/**
  136 | 	 * Set the {@code Environment} for this application context.
  137 | 	 * @param environment the new environment
  138 | 	 * @since 3.1
  139 | 	 */
  140 | 	void setEnvironment(ConfigurableEnvironment environment);
  141 | 
  142 | 	/**
  143 | 	 * Return the {@code Environment} for this application context in configurable
  144 | 	 * form, allowing for further customization.
  145 | 	 * @since 3.1
  146 | 	 */
  147 | 	@Override
  148 | 	ConfigurableEnvironment getEnvironment();
  149 | 
  150 | 	/**
  151 | 	 * Set the {@link ApplicationStartup} for this application context.
  152 | 	 * <p>This allows the application context to record metrics
  153 | 	 * during startup.
  154 | 	 * @param applicationStartup the new context event factory
  155 | 	 * @since 5.3
  156 | 	 */
  157 | 	void setApplicationStartup(ApplicationStartup applicationStartup);
  158 | 
  159 | 	/**
  160 | 	 * Return the {@link ApplicationStartup} for this application context.
  161 | 	 * @since 5.3
  162 | 	 */
  163 | 	ApplicationStartup getApplicationStartup();
  164 | 
  165 | 	/**
  166 | 	 * Add a new BeanFactoryPostProcessor that will get applied to the internal
  167 | 	 * bean factory of this application context on refresh, before any of the
  168 | 	 * bean definitions get evaluated. To be invoked during context configuration.
  169 | 	 * @param postProcessor the factory processor to register
  170 | 	 */
  171 | 	void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);
  172 | 
  173 | 	/**
  174 | 	 * Add a new ApplicationListener that will be notified on context events
  175 | 	 * such as context refresh and context shutdown.
  176 | 	 * <p>Note that any ApplicationListener registered here will be applied
  177 | 	 * on refresh if the context is not active yet, or on the fly with the
  178 | 	 * current event multicaster in case of a context that is already active.
  179 | 	 * @param listener the ApplicationListener to register
  180 | 	 * @see org.springframework.context.event.ContextRefreshedEvent
  181 | 	 * @see org.springframework.context.event.ContextClosedEvent
  182 | 	 */
  183 | 	void addApplicationListener(ApplicationListener<?> listener);
  184 | 
  185 | 	/**
  186 | 	 * Remove the given ApplicationListener from this context's set of listeners,
  187 | 	 * assuming it got registered via {@link #addApplicationListener} before.
  188 | 	 * @param listener the ApplicationListener to deregister
  189 | 	 * @since 6.0
  190 | 	 */
  191 | 	void removeApplicationListener(ApplicationListener<?> listener);
  192 | 
  193 | 	/**
  194 | 	 * Specify the ClassLoader to load class path resources and bean classes with.
  195 | 	 * <p>This context class loader will be passed to the internal bean factory.
  196 | 	 * @since 5.2.7
  197 | 	 * @see org.springframework.core.io.DefaultResourceLoader#DefaultResourceLoader(ClassLoader)
  198 | 	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setBeanClassLoader
  199 | 	 */
  200 | 	void setClassLoader(ClassLoader classLoader);
  201 | 
  202 | 	/**
  203 | 	 * Register the given protocol resolver with this application context,
  204 | 	 * allowing for additional resource protocols to be handled.
  205 | 	 * <p>Any such resolver will be invoked ahead of this context's standard
  206 | 	 * resolution rules. It may therefore also override any default rules.
  207 | 	 * @since 4.3
  208 | 	 */
  209 | 	void addProtocolResolver(ProtocolResolver resolver);
  210 | 
  211 | 	/**
  212 | 	 * Load or refresh the persistent representation of the configuration, which
  213 | 	 * might be from Java-based configuration, an XML file, a properties file, a
  214 | 	 * relational database schema, or some other format.
  215 | 	 * <p>As this is a startup method, it should destroy already created singletons
  216 | 	 * if it fails, to avoid dangling resources. In other words, after invocation
  217 | 	 * of this method, either all or no singletons at all should be instantiated.
  218 | 	 * @throws BeansException if the bean factory could not be initialized
  219 | 	 * @throws IllegalStateException if already initialized and multiple refresh
  220 | 	 * attempts are not supported
  221 | 	 */
  222 | 	void refresh() throws BeansException, IllegalStateException;
  223 | 
  224 | 	/**
  225 | 	 * Pause all beans in this application context if necessary, and subsequently
  226 | 	 * restart all auto-startup beans, effectively restoring the lifecycle state
  227 | 	 * after {@link #refresh()} (typically after a preceding {@link #pause()} call
  228 | 	 * when a full {@link #start()} of even lazy-starting beans is to be avoided).
  229 | 	 * @since 7.0
  230 | 	 * @see #pause()
  231 | 	 * @see #start()
  232 | 	 * @see SmartLifecycle#isAutoStartup()
  233 | 	 */
  234 | 	void restart();
  235 | 
  236 | 	/**
  237 | 	 * Stop all beans in this application context unless they explicitly opt out of
  238 | 	 * pausing through {@link SmartLifecycle#isPauseable()} returning {@code false}.
  239 | 	 * @since 7.0
  240 | 	 * @see #restart()
  241 | 	 * @see #stop()
  242 | 	 * @see SmartLifecycle#isPauseable()
  243 | 	 */
  244 | 	void pause();
  245 | 
  246 | 	/**
  247 | 	 * Register a shutdown hook with the JVM runtime, closing this context
  248 | 	 * on JVM shutdown unless it has already been closed at that time.
  249 | 	 * <p>This method can be called multiple times. Only one shutdown hook
  250 | 	 * (at max) will be registered for each context instance.
  251 | 	 * <p>The {@linkplain Thread#getName() name} of the shutdown hook thread
  252 | 	 * should be {@link #SHUTDOWN_HOOK_THREAD_NAME}.
  253 | 	 * @see java.lang.Runtime#addShutdownHook
  254 | 	 * @see #close()
  255 | 	 */
  256 | 	void registerShutdownHook();
  257 | 
  258 | 	/**
  259 | 	 * Close this application context, releasing all resources and locks that the
  260 | 	 * implementation might hold. This includes destroying all cached singleton beans.
  261 | 	 * <p>Note: Does <i>not</i> invoke {@code close} on a parent context;
  262 | 	 * parent contexts have their own, independent lifecycle.
  263 | 	 * <p>This method can be called multiple times without side effects: Subsequent
  264 | 	 * {@code close} calls on an already closed context will be ignored.
  265 | 	 */
  266 | 	@Override
  267 | 	void close();
  268 | 
  269 | 	/**
  270 | 	 * Return whether this context has been closed already, that is,
  271 | 	 * whether {@link #close()} has been called on an active context
  272 | 	 * in order to initiate its shutdown.
  273 | 	 * <p>Note: This does not indicate whether context shutdown has completed.
  274 | 	 * Use {@link #isActive()} for differentiating between those scenarios:
  275 | 	 * a context becomes inactive once it has been fully shut down and the
  276 | 	 * original {@code close()} call has returned.
  277 | 	 * @since 6.2
  278 | 	 */
  279 | 	boolean isClosed();
  280 | 
  281 | 	/**
  282 | 	 * Determine whether this application context is active, that is,
  283 | 	 * whether it has been refreshed at least once and has not been closed yet.
  284 | 	 * @return whether the context is still active
  285 | 	 * @see #refresh()
  286 | 	 * @see #close()
  287 | 	 * @see #getBeanFactory()
  288 | 	 */
  289 | 	boolean isActive();
  290 | 
  291 | 	/**
  292 | 	 * Return the internal bean factory of this application context.
  293 | 	 * Can be used to access specific functionality of the underlying factory.
  294 | 	 * <p>Note: Do not use this to post-process the bean factory; singletons
  295 | 	 * will already have been instantiated before. Use a BeanFactoryPostProcessor
  296 | 	 * to intercept the BeanFactory setup process before beans get touched.
  297 | 	 * <p>Generally, this internal factory will only be accessible while the context
  298 | 	 * is active, that is, in-between {@link #refresh()} and {@link #close()}.
  299 | 	 * The {@link #isActive()} flag can be used to check whether the context
  300 | 	 * is in an appropriate state.
  301 | 	 * @return the underlying bean factory
  302 | 	 * @throws IllegalStateException if the context does not hold an internal
  303 | 	 * bean factory (usually if {@link #refresh()} hasn't been called yet or
  304 | 	 * if {@link #close()} has already been called)
  305 | 	 * @see #isActive()
  306 | 	 * @see #refresh()
  307 | 	 * @see #close()
  308 | 	 * @see #addBeanFactoryPostProcessor
  309 | 	 */
  310 | 	ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;
  311 | 
  312 | }
```
