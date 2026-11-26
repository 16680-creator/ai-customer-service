<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator.java -->
<!-- 正文引用：[[源码:AbstractAutoProxyCreator.java#L起-L止]] ；代码块头部：(src: AbstractAutoProxyCreator.java#L起-L止) -->

# AbstractAutoProxyCreator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/autoproxy/AbstractAutoProxyCreator.java` |
| 行数 | 622 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:58.833Z |

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
   17 | package org.springframework.aop.framework.autoproxy;
   18 | 
   19 | import java.lang.reflect.Constructor;
   20 | import java.lang.reflect.Proxy;
   21 | import java.util.ArrayList;
   22 | import java.util.Arrays;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | import java.util.Set;
   26 | import java.util.concurrent.ConcurrentHashMap;
   27 | 
   28 | import org.aopalliance.aop.Advice;
   29 | import org.apache.commons.logging.Log;
   30 | import org.apache.commons.logging.LogFactory;
   31 | import org.jspecify.annotations.Nullable;
   32 | 
   33 | import org.springframework.aop.Advisor;
   34 | import org.springframework.aop.Pointcut;
   35 | import org.springframework.aop.TargetSource;
   36 | import org.springframework.aop.framework.AopInfrastructureBean;
   37 | import org.springframework.aop.framework.ProxyFactory;
   38 | import org.springframework.aop.framework.ProxyProcessorSupport;
   39 | import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
   40 | import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
   41 | import org.springframework.aop.target.EmptyTargetSource;
   42 | import org.springframework.aop.target.SingletonTargetSource;
   43 | import org.springframework.beans.BeansException;
   44 | import org.springframework.beans.PropertyValues;
   45 | import org.springframework.beans.factory.BeanFactory;
   46 | import org.springframework.beans.factory.BeanFactoryAware;
   47 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   48 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   49 | import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
   50 | import org.springframework.core.SmartClassLoader;
   51 | import org.springframework.util.Assert;
   52 | import org.springframework.util.ClassUtils;
   53 | import org.springframework.util.StringUtils;
   54 | 
   55 | /**
   56 |  * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
   57 |  * that wraps each eligible bean with an AOP proxy, delegating to specified interceptors
   58 |  * before invoking the bean itself.
   59 |  *
   60 |  * <p>This class distinguishes between "common" interceptors: shared for all proxies it
   61 |  * creates, and "specific" interceptors: unique per bean instance. There need not be any
   62 |  * common interceptors. If there are, they are set using the interceptorNames property.
   63 |  * As with {@link org.springframework.aop.framework.ProxyFactoryBean}, interceptors names
   64 |  * in the current factory are used rather than bean references to allow correct handling
   65 |  * of prototype advisors and interceptors: for example, to support stateful mixins.
   66 |  * Any advice type is supported for {@link #setInterceptorNames "interceptorNames"} entries.
   67 |  *
   68 |  * <p>Such auto-proxying is particularly useful if there's a large number of beans that
   69 |  * need to be wrapped with similar proxies, i.e. delegating to the same interceptors.
   70 |  * Instead of x repetitive proxy definitions for x target beans, you can register
   71 |  * one single such post processor with the bean factory to achieve the same effect.
   72 |  *
   73 |  * <p>Subclasses can apply any strategy to decide if a bean is to be proxied, for example, by type,
   74 |  * by name, by definition details, etc. They can also return additional interceptors that
   75 |  * should just be applied to the specific bean instance. A simple concrete implementation is
   76 |  * {@link BeanNameAutoProxyCreator}, identifying the beans to be proxied via given names.
   77 |  *
   78 |  * <p>Any number of {@link TargetSourceCreator} implementations can be used to create
   79 |  * a custom target source: for example, to pool prototype objects. Auto-proxying will
   80 |  * occur even if there is no advice, as long as a TargetSourceCreator specifies a custom
   81 |  * {@link org.springframework.aop.TargetSource}. If there are no TargetSourceCreators set,
   82 |  * or if none matches, a {@link org.springframework.aop.target.SingletonTargetSource}
   83 |  * will be used by default to wrap the target bean instance.
   84 |  *
   85 |  * @author Juergen Hoeller
   86 |  * @author Rod Johnson
   87 |  * @author Rob Harrop
   88 |  * @author Sam Brannen
   89 |  * @since 13.10.2003
   90 |  * @see #setInterceptorNames
   91 |  * @see #getAdvicesAndAdvisorsForBean
   92 |  * @see BeanNameAutoProxyCreator
   93 |  * @see DefaultAdvisorAutoProxyCreator
   94 |  */
   95 | @SuppressWarnings("serial")
   96 | public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
   97 | 		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
   98 | 
   99 | 	/**
  100 | 	 * Convenience constant for subclasses: Return value for "do not proxy".
  101 | 	 * @see #getAdvicesAndAdvisorsForBean
  102 | 	 */
  103 | 	protected static final Object @Nullable [] DO_NOT_PROXY = null;
  104 | 
  105 | 	/**
  106 | 	 * Convenience constant for subclasses: Return value for
  107 | 	 * "proxy without additional interceptors, just the common ones".
  108 | 	 * @see #getAdvicesAndAdvisorsForBean
  109 | 	 */
  110 | 	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];
  111 | 
  112 | 
  113 | 	/** Logger available to subclasses. */
  114 | 	protected final Log logger = LogFactory.getLog(getClass());
  115 | 
  116 | 	/** Default is global AdvisorAdapterRegistry. */
  117 | 	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();
  118 | 
  119 | 	/** Default is no common interceptors. */
  120 | 	private String[] interceptorNames = new String[0];
  121 | 
  122 | 	private boolean applyCommonInterceptorsFirst = true;
  123 | 
  124 | 	private TargetSourceCreator @Nullable [] customTargetSourceCreators;
  125 | 
  126 | 	private @Nullable BeanFactory beanFactory;
  127 | 
  128 | 	private final Set<String> targetSourcedBeans = ConcurrentHashMap.newKeySet(16);
  129 | 
  130 | 	private final Map<Object, Object> earlyBeanReferences = new ConcurrentHashMap<>(16);
  131 | 
  132 | 	private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16);
  133 | 
  134 | 	private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);
  135 | 
  136 | 
  137 | 	/**
  138 | 	 * Specify the {@link AdvisorAdapterRegistry} to use.
  139 | 	 * <p>Default is the global {@link AdvisorAdapterRegistry}.
  140 | 	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
  141 | 	 */
  142 | 	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
  143 | 		this.advisorAdapterRegistry = advisorAdapterRegistry;
  144 | 	}
  145 | 
  146 | 	/**
  147 | 	 * Set custom {@code TargetSourceCreators} to be applied in this order.
  148 | 	 * If the list is empty, or they all return null, a {@link SingletonTargetSource}
  149 | 	 * will be created for each bean.
  150 | 	 * <p>Note that TargetSourceCreators will kick in even for target beans
  151 | 	 * where no advices or advisors have been found. If a {@code TargetSourceCreator}
  152 | 	 * returns a {@link TargetSource} for a specific bean, that bean will be proxied
  153 | 	 * in any case.
  154 | 	 * <p>{@code TargetSourceCreators} can only be invoked if this post processor is used
  155 | 	 * in a {@link BeanFactory} and its {@link BeanFactoryAware} callback is triggered.
  156 | 	 * @param targetSourceCreators the list of {@code TargetSourceCreators}.
  157 | 	 * Ordering is significant: The {@code TargetSource} returned from the first matching
  158 | 	 * {@code TargetSourceCreator} (that is, the first that returns non-null) will be used.
  159 | 	 */
  160 | 	public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
  161 | 		this.customTargetSourceCreators = targetSourceCreators;
  162 | 	}
  163 | 
  164 | 	/**
  165 | 	 * Set the common interceptors. These must be bean names in the current factory.
  166 | 	 * They can be of any advice or advisor type Spring supports.
  167 | 	 * <p>If this property isn't set, there will be zero common interceptors.
  168 | 	 * This is perfectly valid, if "specific" interceptors such as matching
  169 | 	 * Advisors are all we want.
  170 | 	 */
  171 | 	public void setInterceptorNames(String... interceptorNames) {
  172 | 		this.interceptorNames = interceptorNames;
  173 | 	}
  174 | 
  175 | 	/**
  176 | 	 * Set whether the common interceptors should be applied before bean-specific ones.
  177 | 	 * Default is "true"; else, bean-specific interceptors will get applied first.
  178 | 	 */
  179 | 	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
  180 | 		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
  181 | 	}
  182 | 
  183 | 	@Override
  184 | 	public void setBeanFactory(BeanFactory beanFactory) {
  185 | 		this.beanFactory = beanFactory;
  186 | 		AutoProxyUtils.applyDefaultProxyConfig(this, beanFactory);
  187 | 	}
  188 | 
  189 | 	/**
  190 | 	 * Return the owning {@link BeanFactory}.
  191 | 	 * May be {@code null}, as this post-processor doesn't need to belong to a bean factory.
  192 | 	 */
  193 | 	protected @Nullable BeanFactory getBeanFactory() {
  194 | 		return this.beanFactory;
  195 | 	}
  196 | 
  197 | 
  198 | 	@Override
  199 | 	public @Nullable Class<?> predictBeanType(Class<?> beanClass, String beanName) {
  200 | 		if (this.proxyTypes.isEmpty()) {
  201 | 			return null;
  202 | 		}
  203 | 		Object cacheKey = getCacheKey(beanClass, beanName);
  204 | 		return this.proxyTypes.get(cacheKey);
  205 | 	}
  206 | 
  207 | 	@Override
  208 | 	public Class<?> determineBeanType(Class<?> beanClass, String beanName) {
  209 | 		Object cacheKey = getCacheKey(beanClass, beanName);
  210 | 		Class<?> proxyType = this.proxyTypes.get(cacheKey);
  211 | 		if (proxyType == null) {
  212 | 			TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
  213 | 			if (targetSource != null) {
  214 | 				if (StringUtils.hasLength(beanName)) {
  215 | 					this.targetSourcedBeans.add(beanName);
  216 | 				}
  217 | 			}
  218 | 			else {
  219 | 				targetSource = EmptyTargetSource.forClass(beanClass);
  220 | 			}
  221 | 			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
  222 | 			if (specificInterceptors != DO_NOT_PROXY) {
  223 | 				this.advisedBeans.put(cacheKey, Boolean.TRUE);
  224 | 				proxyType = createProxyClass(beanClass, beanName, specificInterceptors, targetSource);
  225 | 				this.proxyTypes.put(cacheKey, proxyType);
  226 | 			}
  227 | 		}
  228 | 		return (proxyType != null ? proxyType : beanClass);
  229 | 	}
  230 | 
  231 | 	@Override
  232 | 	public Constructor<?> @Nullable [] determineCandidateConstructors(Class<?> beanClass, String beanName) {
  233 | 		return null;
  234 | 	}
  235 | 
  236 | 	@Override
  237 | 	public Object getEarlyBeanReference(Object bean, String beanName) {
  238 | 		Object cacheKey = getCacheKey(bean.getClass(), beanName);
  239 | 		this.earlyBeanReferences.put(cacheKey, bean);
  240 | 		return wrapIfNecessary(bean, beanName, cacheKey);
  241 | 	}
  242 | 
  243 | 	@Override
  244 | 	public @Nullable Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
  245 | 		Object cacheKey = getCacheKey(beanClass, beanName);
  246 | 
  247 | 		if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
  248 | 			if (this.advisedBeans.containsKey(cacheKey)) {
  249 | 				return null;
  250 | 			}
  251 | 			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
  252 | 				this.advisedBeans.put(cacheKey, Boolean.FALSE);
  253 | 				return null;
  254 | 			}
  255 | 		}
  256 | 
  257 | 		// Create proxy here if we have a custom TargetSource.
  258 | 		// Suppresses unnecessary default instantiation of the target bean:
  259 | 		// The TargetSource will handle target instances in a custom fashion.
  260 | 		TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
  261 | 		if (targetSource != null) {
  262 | 			if (StringUtils.hasLength(beanName)) {
  263 | 				this.targetSourcedBeans.add(beanName);
  264 | 			}
  265 | 			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
  266 | 			Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
  267 | 			this.proxyTypes.put(cacheKey, proxy.getClass());
  268 | 			return proxy;
  269 | 		}
  270 | 
  271 | 		return null;
  272 | 	}
  273 | 
  274 | 	@Override
  275 | 	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
  276 | 		return pvs;  // skip postProcessPropertyValues
  277 | 	}
  278 | 
  279 | 	/**
  280 | 	 * Create a proxy with the configured interceptors if the bean is
  281 | 	 * identified as one to proxy by the subclass.
  282 | 	 * @see #getAdvicesAndAdvisorsForBean
  283 | 	 */
  284 | 	@Override
  285 | 	public @Nullable Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
  286 | 		if (bean != null) {
  287 | 			Object cacheKey = getCacheKey(bean.getClass(), beanName);
  288 | 			if (this.earlyBeanReferences.remove(cacheKey) != bean) {
  289 | 				return wrapIfNecessary(bean, beanName, cacheKey);
  290 | 			}
  291 | 		}
  292 | 		return bean;
  293 | 	}
  294 | 
  295 | 
  296 | 	/**
  297 | 	 * Build a cache key for the given bean class and bean name.
  298 | 	 * <p>Note: As of 7.0.2, this implementation returns a composed cache key
  299 | 	 * for bean class plus bean name; or if no bean name specified, then the
  300 | 	 * given bean {@code Class} as-is.
  301 | 	 * @param beanClass the bean class
  302 | 	 * @param beanName the bean name
  303 | 	 * @return the cache key for the given class and name
  304 | 	 */
  305 | 	protected Object getCacheKey(Class<?> beanClass, @Nullable String beanName) {
  306 | 		if (StringUtils.hasLength(beanName)) {
  307 | 			return new ComposedCacheKey(beanClass, beanName);
  308 | 		}
  309 | 		else {
  310 | 			return beanClass;
  311 | 		}
  312 | 	}
  313 | 
  314 | 	/**
  315 | 	 * Wrap the given bean if necessary, i.e. if it is eligible for being proxied.
  316 | 	 * @param bean the raw bean instance
  317 | 	 * @param beanName the name of the bean
  318 | 	 * @param cacheKey the cache key for metadata access
  319 | 	 * @return a proxy wrapping the bean, or the raw bean instance as-is
  320 | 	 */
  321 | 	protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
  322 | 		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
  323 | 			return bean;
  324 | 		}
  325 | 		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
  326 | 			return bean;
  327 | 		}
  328 | 		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
  329 | 			this.advisedBeans.put(cacheKey, Boolean.FALSE);
  330 | 			return bean;
  331 | 		}
  332 | 
  333 | 		// Create proxy if we have advice.
  334 | 		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
  335 | 		if (specificInterceptors != DO_NOT_PROXY) {
  336 | 			this.advisedBeans.put(cacheKey, Boolean.TRUE);
  337 | 			Object proxy = createProxy(
  338 | 					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
  339 | 			this.proxyTypes.put(cacheKey, proxy.getClass());
  340 | 			return proxy;
  341 | 		}
  342 | 
  343 | 		this.advisedBeans.put(cacheKey, Boolean.FALSE);
  344 | 		return bean;
  345 | 	}
  346 | 
  347 | 	/**
  348 | 	 * Return whether the given bean class represents an infrastructure class
  349 | 	 * that should never be proxied.
  350 | 	 * <p>The default implementation considers Advices, Advisors and
  351 | 	 * AopInfrastructureBeans as infrastructure classes.
  352 | 	 * @param beanClass the class of the bean
  353 | 	 * @return whether the bean represents an infrastructure class
  354 | 	 * @see org.aopalliance.aop.Advice
  355 | 	 * @see org.springframework.aop.Advisor
  356 | 	 * @see org.springframework.aop.framework.AopInfrastructureBean
  357 | 	 * @see #shouldSkip
  358 | 	 */
  359 | 	protected boolean isInfrastructureClass(Class<?> beanClass) {
  360 | 		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
  361 | 				Pointcut.class.isAssignableFrom(beanClass) ||
  362 | 				Advisor.class.isAssignableFrom(beanClass) ||
  363 | 				AopInfrastructureBean.class.isAssignableFrom(beanClass);
  364 | 		if (retVal && logger.isTraceEnabled()) {
  365 | 			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
  366 | 		}
  367 | 		return retVal;
  368 | 	}
  369 | 
  370 | 	/**
  371 | 	 * Subclasses should override this method to return {@code true} if the
  372 | 	 * given bean should not be considered for auto-proxying by this post-processor.
  373 | 	 * <p>Sometimes we need to be able to avoid this happening, for example, if it will lead to
  374 | 	 * a circular reference or if the existing target instance needs to be preserved.
  375 | 	 * This implementation returns {@code false} unless the bean name indicates an
  376 | 	 * "original instance" according to {@code AutowireCapableBeanFactory} conventions.
  377 | 	 * @param beanClass the class of the bean
  378 | 	 * @param beanName the name of the bean
  379 | 	 * @return whether to skip the given bean
  380 | 	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX
  381 | 	 */
  382 | 	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
  383 | 		return AutoProxyUtils.isOriginalInstance(beanName, beanClass);
  384 | 	}
  385 | 
  386 | 	/**
  387 | 	 * Create a target source for bean instances. Uses any TargetSourceCreators if set.
  388 | 	 * Returns {@code null} if no custom TargetSource should be used.
  389 | 	 * <p>This implementation uses the "customTargetSourceCreators" property.
  390 | 	 * Subclasses can override this method to use a different mechanism.
  391 | 	 * @param beanClass the class of the bean to create a TargetSource for
  392 | 	 * @param beanName the name of the bean
  393 | 	 * @return a TargetSource for this bean
  394 | 	 * @see #setCustomTargetSourceCreators
  395 | 	 */
  396 | 	protected @Nullable TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
  397 | 		// We can't create fancy target sources for directly registered singletons.
  398 | 		if (this.customTargetSourceCreators != null &&
  399 | 				this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
  400 | 			for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
  401 | 				TargetSource ts = tsc.getTargetSource(beanClass, beanName);
  402 | 				if (ts != null) {
  403 | 					// Found a matching TargetSource.
  404 | 					if (logger.isTraceEnabled()) {
  405 | 						logger.trace("TargetSourceCreator [" + tsc +
  406 | 								"] found custom TargetSource for bean with name '" + beanName + "'");
  407 | 					}
  408 | 					return ts;
  409 | 				}
  410 | 			}
  411 | 		}
  412 | 
  413 | 		// No custom TargetSource found.
  414 | 		return null;
  415 | 	}
  416 | 
  417 | 	/**
  418 | 	 * Create an AOP proxy for the given bean.
  419 | 	 * @param beanClass the class of the bean
  420 | 	 * @param beanName the name of the bean
  421 | 	 * @param specificInterceptors the set of interceptors that is
  422 | 	 * specific to this bean (may be empty, but not null)
  423 | 	 * @param targetSource the TargetSource for the proxy,
  424 | 	 * already pre-configured to access the bean
  425 | 	 * @return the AOP proxy for the bean
  426 | 	 * @see #buildAdvisors
  427 | 	 */
  428 | 	protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
  429 | 			Object @Nullable [] specificInterceptors, TargetSource targetSource) {
  430 | 
  431 | 		return buildProxy(beanClass, beanName, specificInterceptors, targetSource, false);
  432 | 	}
  433 | 
  434 | 	private Class<?> createProxyClass(Class<?> beanClass, @Nullable String beanName,
  435 | 			Object @Nullable [] specificInterceptors, TargetSource targetSource) {
  436 | 
  437 | 		return (Class<?>) buildProxy(beanClass, beanName, specificInterceptors, targetSource, true);
  438 | 	}
  439 | 
  440 | 	private Object buildProxy(Class<?> beanClass, @Nullable String beanName,
  441 | 			Object @Nullable [] specificInterceptors, TargetSource targetSource, boolean classOnly) {
  442 | 
  443 | 		if (this.beanFactory instanceof ConfigurableListableBeanFactory clbf) {
  444 | 			AutoProxyUtils.exposeTargetClass(clbf, beanName, beanClass);
  445 | 		}
  446 | 
  447 | 		ProxyFactory proxyFactory = new ProxyFactory();
  448 | 		proxyFactory.copyFrom(this);
  449 | 		proxyFactory.setFrozen(false);
  450 | 
  451 | 		if (shouldProxyTargetClass(beanClass, beanName)) {
  452 | 			proxyFactory.setProxyTargetClass(true);
  453 | 		}
  454 | 		else {
  455 | 			Class<?>[] ifcs = (this.beanFactory instanceof ConfigurableListableBeanFactory clbf ?
  456 | 					AutoProxyUtils.determineExposedInterfaces(clbf, beanName) : null);
  457 | 			if (ifcs != null) {
  458 | 				proxyFactory.setProxyTargetClass(false);
  459 | 				for (Class<?> ifc : ifcs) {
  460 | 					proxyFactory.addInterface(ifc);
  461 | 				}
  462 | 			}
  463 | 			if (ifcs != null ? ifcs.length == 0 : !proxyFactory.isProxyTargetClass()) {
  464 | 				evaluateProxyInterfaces(beanClass, proxyFactory);
  465 | 			}
  466 | 		}
  467 | 
  468 | 		if (proxyFactory.isProxyTargetClass()) {
  469 | 			// Explicit handling of JDK proxy targets and lambdas (for introduction advice scenarios)
  470 | 			if (Proxy.isProxyClass(beanClass) || ClassUtils.isLambdaClass(beanClass)) {
  471 | 				// Must allow for introductions; can't just set interfaces to the proxy's interfaces only.
  472 | 				for (Class<?> ifc : beanClass.getInterfaces()) {
  473 | 					proxyFactory.addInterface(ifc);
  474 | 				}
  475 | 			}
  476 | 		}
  477 | 
  478 | 		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
  479 | 		proxyFactory.addAdvisors(advisors);
  480 | 		proxyFactory.setTargetSource(targetSource);
  481 | 		customizeProxyFactory(proxyFactory);
  482 | 
  483 | 		proxyFactory.setFrozen(isFrozen());
  484 | 		if (advisorsPreFiltered()) {
  485 | 			proxyFactory.setPreFiltered(true);
  486 | 		}
  487 | 
  488 | 		// Use original ClassLoader if bean class not locally loaded in overriding class loader
  489 | 		ClassLoader classLoader = getProxyClassLoader();
  490 | 		if (classLoader instanceof SmartClassLoader smartClassLoader && classLoader != beanClass.getClassLoader()) {
  491 | 			classLoader = smartClassLoader.getOriginalClassLoader();
  492 | 		}
  493 | 		return (classOnly ? proxyFactory.getProxyClass(classLoader) : proxyFactory.getProxy(classLoader));
  494 | 	}
  495 | 
  496 | 	/**
  497 | 	 * Determine whether the given bean should be proxied with its target class rather than its interfaces.
  498 | 	 * <p>Checks the {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
  499 | 	 * of the corresponding bean definition.
  500 | 	 * @param beanClass the class of the bean
  501 | 	 * @param beanName the name of the bean
  502 | 	 * @return whether the given bean should be proxied with its target class
  503 | 	 * @see AutoProxyUtils#shouldProxyTargetClass
  504 | 	 */
  505 | 	protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
  506 | 		return (this.beanFactory instanceof ConfigurableListableBeanFactory clbf &&
  507 | 				AutoProxyUtils.shouldProxyTargetClass(clbf, beanName));
  508 | 	}
  509 | 
  510 | 	/**
  511 | 	 * Return whether the Advisors returned by the subclass are pre-filtered
  512 | 	 * to match the bean's target class already, allowing the ClassFilter check
  513 | 	 * to be skipped when building advisors chains for AOP invocations.
  514 | 	 * <p>Default is {@code false}. Subclasses may override this if they
  515 | 	 * will always return pre-filtered Advisors.
  516 | 	 * @return whether the Advisors are pre-filtered
  517 | 	 * @see #getAdvicesAndAdvisorsForBean
  518 | 	 * @see org.springframework.aop.framework.Advised#setPreFiltered
  519 | 	 */
  520 | 	protected boolean advisorsPreFiltered() {
  521 | 		return false;
  522 | 	}
  523 | 
  524 | 	/**
  525 | 	 * Determine the advisors for the given bean, including the specific interceptors
  526 | 	 * as well as the common interceptor, all adapted to the Advisor interface.
  527 | 	 * @param beanName the name of the bean
  528 | 	 * @param specificInterceptors the set of interceptors that is
  529 | 	 * specific to this bean (may be empty, but not null)
  530 | 	 * @return the list of Advisors for the given bean
  531 | 	 */
  532 | 	protected Advisor[] buildAdvisors(@Nullable String beanName, Object @Nullable [] specificInterceptors) {
  533 | 		// Handle prototypes correctly...
  534 | 		Advisor[] commonInterceptors = resolveInterceptorNames();
  535 | 
  536 | 		List<Object> allInterceptors = new ArrayList<>();
  537 | 		if (specificInterceptors != null) {
  538 | 			if (specificInterceptors.length > 0) {
  539 | 				// specificInterceptors may equal PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
  540 | 				allInterceptors.addAll(Arrays.asList(specificInterceptors));
  541 | 			}
  542 | 			if (commonInterceptors.length > 0) {
  543 | 				if (this.applyCommonInterceptorsFirst) {
  544 | 					allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
  545 | 				}
  546 | 				else {
  547 | 					allInterceptors.addAll(Arrays.asList(commonInterceptors));
  548 | 				}
  549 | 			}
  550 | 		}
  551 | 		if (logger.isTraceEnabled()) {
  552 | 			int nrOfCommonInterceptors = commonInterceptors.length;
  553 | 			int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
  554 | 			logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
  555 | 					" common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
  556 | 		}
  557 | 
  558 | 		Advisor[] advisors = new Advisor[allInterceptors.size()];
  559 | 		for (int i = 0; i < allInterceptors.size(); i++) {
  560 | 			advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
  561 | 		}
  562 | 		return advisors;
  563 | 	}
  564 | 
  565 | 	/**
  566 | 	 * Resolves the specified interceptor names to Advisor objects.
  567 | 	 * @see #setInterceptorNames
  568 | 	 */
  569 | 	private Advisor[] resolveInterceptorNames() {
  570 | 		BeanFactory bf = this.beanFactory;
  571 | 		ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory _cbf ? _cbf : null);
  572 | 		List<Advisor> advisors = new ArrayList<>();
  573 | 		for (String beanName : this.interceptorNames) {
  574 | 			if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
  575 | 				Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
  576 | 				Object next = bf.getBean(beanName);
  577 | 				advisors.add(this.advisorAdapterRegistry.wrap(next));
  578 | 			}
  579 | 		}
  580 | 		return advisors.toArray(new Advisor[0]);
  581 | 	}
  582 | 
  583 | 	/**
  584 | 	 * Subclasses may choose to implement this: for example,
  585 | 	 * to change the interfaces exposed.
  586 | 	 * <p>The default implementation is empty.
  587 | 	 * @param proxyFactory a ProxyFactory that is already configured with
  588 | 	 * TargetSource and interfaces and will be used to create the proxy
  589 | 	 * immediately after this method returns
  590 | 	 */
  591 | 	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
  592 | 	}
  593 | 
  594 | 
  595 | 	/**
  596 | 	 * Return whether the given bean is to be proxied, what additional
  597 | 	 * advices (for example, AOP Alliance interceptors) and advisors to apply.
  598 | 	 * @param beanClass the class of the bean to advise
  599 | 	 * @param beanName the name of the bean
  600 | 	 * @param customTargetSource the TargetSource returned by the
  601 | 	 * {@link #getCustomTargetSource} method: may be ignored.
  602 | 	 * Will be {@code null} if no custom target source is in use.
  603 | 	 * @return an array of additional interceptors for the particular bean;
  604 | 	 * or an empty array if no additional interceptors but just the common ones;
  605 | 	 * or {@code null} if no proxy at all, not even with the common interceptors.
  606 | 	 * See constants DO_NOT_PROXY and PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS.
  607 | 	 * @throws BeansException in case of errors
  608 | 	 * @see #DO_NOT_PROXY
  609 | 	 * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
  610 | 	 */
  611 | 	protected abstract Object @Nullable [] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
  612 | 			@Nullable TargetSource customTargetSource) throws BeansException;
  613 | 
  614 | 
  615 | 	/**
  616 | 	 * Composed cache key for bean class plus bean name.
  617 | 	 * @see #getCacheKey(Class, String)
  618 | 	 */
  619 | 	private record ComposedCacheKey(Class<?> beanClass, String beanName) {
  620 | 	}
  621 | 
  622 | }
```
