<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/context/scope/GenericScope.java -->
<!-- 正文引用：[[源码:GenericScope.java#L起-L止]] ；代码块头部：(src: GenericScope.java#L起-L止) -->

# GenericScope.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/context/scope/GenericScope.java` |
| 行数 | 515 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:37.183Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
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
   17 | package org.springframework.cloud.context.scope;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.lang.reflect.UndeclaredThrowableException;
   21 | import java.util.ArrayList;
   22 | import java.util.Arrays;
   23 | import java.util.Collection;
   24 | import java.util.Collections;
   25 | import java.util.LinkedHashSet;
   26 | import java.util.List;
   27 | import java.util.Map;
   28 | import java.util.UUID;
   29 | import java.util.concurrent.ConcurrentHashMap;
   30 | import java.util.concurrent.ConcurrentMap;
   31 | import java.util.concurrent.locks.Lock;
   32 | import java.util.concurrent.locks.ReadWriteLock;
   33 | import java.util.concurrent.locks.ReentrantReadWriteLock;
   34 | 
   35 | import org.aopalliance.intercept.MethodInterceptor;
   36 | import org.aopalliance.intercept.MethodInvocation;
   37 | import org.apache.commons.logging.Log;
   38 | import org.apache.commons.logging.LogFactory;
   39 | 
   40 | import org.springframework.aop.framework.Advised;
   41 | import org.springframework.aop.scope.ScopedObject;
   42 | import org.springframework.aop.scope.ScopedProxyFactoryBean;
   43 | import org.springframework.aop.support.AopUtils;
   44 | import org.springframework.beans.BeansException;
   45 | import org.springframework.beans.factory.BeanFactory;
   46 | import org.springframework.beans.factory.DisposableBean;
   47 | import org.springframework.beans.factory.ObjectFactory;
   48 | import org.springframework.beans.factory.config.BeanDefinition;
   49 | import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
   50 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   51 | import org.springframework.beans.factory.config.Scope;
   52 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   53 | import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
   54 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   55 | import org.springframework.beans.factory.support.RootBeanDefinition;
   56 | import org.springframework.expression.Expression;
   57 | import org.springframework.expression.ExpressionParser;
   58 | import org.springframework.expression.ParseException;
   59 | import org.springframework.expression.spel.standard.SpelExpressionParser;
   60 | import org.springframework.util.ReflectionUtils;
   61 | import org.springframework.util.StringUtils;
   62 | 
   63 | /**
   64 |  * <p>
   65 |  * A generic Scope implementation.
   66 |  * </p>
   67 |  *
   68 |  * @author Dave Syer
   69 |  * @author Yanming Zhou
   70 |  * @since 3.1
   71 |  *
   72 |  */
   73 | public class GenericScope
   74 | 		implements Scope, BeanFactoryPostProcessor, BeanDefinitionRegistryPostProcessor, DisposableBean {
   75 | 
   76 | 	private static final Log logger = LogFactory.getLog(GenericScope.class);
   77 | 
   78 | 	private BeanLifecycleWrapperCache cache = new BeanLifecycleWrapperCache(new StandardScopeCache());
   79 | 
   80 | 	private String name = "generic";
   81 | 
   82 | 	private ConfigurableListableBeanFactory beanFactory;
   83 | 
   84 | 	private String id;
   85 | 
   86 | 	private final Map<String, Exception> errors = new ConcurrentHashMap<>();
   87 | 
   88 | 	private final ConcurrentMap<String, ReadWriteLock> locks = new ConcurrentHashMap<>();
   89 | 
   90 | 	static RuntimeException wrapIfNecessary(Throwable throwable) {
   91 | 		if (throwable instanceof RuntimeException ex) {
   92 | 			return ex;
   93 | 		}
   94 | 		if (throwable instanceof Error error) {
   95 | 			throw error;
   96 | 		}
   97 | 		return new IllegalStateException(throwable);
   98 | 	}
   99 | 
  100 | 	/**
  101 | 	 * Manual override for the serialization ID that will be used to identify the bean
  102 | 	 * factory. The default is a unique key based on the bean names in the bean factory.
  103 | 	 * @param id The ID to set.
  104 | 	 */
  105 | 	public void setId(String id) {
  106 | 		this.id = id;
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * The cache implementation to use for bean instances in this scope.
  111 | 	 * @param cache The cache to use.
  112 | 	 */
  113 | 	public void setScopeCache(ScopeCache cache) {
  114 | 		this.cache = new BeanLifecycleWrapperCache(cache);
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * A map of bean name to errors when instantiating the bean.
  119 | 	 * @return The errors accumulated since the latest destroy.
  120 | 	 */
  121 | 	public Map<String, Exception> getErrors() {
  122 | 		return this.errors;
  123 | 	}
  124 | 
  125 | 	@Override
  126 | 	public void destroy() {
  127 | 		List<Throwable> errors = new ArrayList<>();
  128 | 		Collection<BeanLifecycleWrapper> wrappers = this.cache.clear();
  129 | 		for (BeanLifecycleWrapper wrapper : wrappers) {
  130 | 			try {
  131 | 				Lock lock = this.locks.get(wrapper.getName()).writeLock();
  132 | 				lock.lock();
  133 | 				try {
  134 | 					wrapper.destroy();
  135 | 				}
  136 | 				finally {
  137 | 					lock.unlock();
  138 | 				}
  139 | 			}
  140 | 			catch (RuntimeException e) {
  141 | 				errors.add(e);
  142 | 			}
  143 | 		}
  144 | 		if (!errors.isEmpty()) {
  145 | 			throw wrapIfNecessary(errors.get(0));
  146 | 		}
  147 | 		this.errors.clear();
  148 | 	}
  149 | 
  150 | 	/**
  151 | 	 * Destroys the named bean (i.e. flushes it from the cache by default).
  152 | 	 * @param name The bean name to flush.
  153 | 	 * @return True if the bean was already cached; false otherwise.
  154 | 	 */
  155 | 	protected boolean destroy(String name) {
  156 | 		BeanLifecycleWrapper wrapper = this.cache.remove(name);
  157 | 		if (wrapper != null) {
  158 | 			Lock lock = this.locks.get(wrapper.getName()).writeLock();
  159 | 			lock.lock();
  160 | 			try {
  161 | 				wrapper.destroy();
  162 | 			}
  163 | 			finally {
  164 | 				lock.unlock();
  165 | 			}
  166 | 			this.errors.remove(name);
  167 | 			return true;
  168 | 		}
  169 | 		return false;
  170 | 	}
  171 | 
  172 | 	@Override
  173 | 	public Object get(String name, ObjectFactory<?> objectFactory) {
  174 | 		BeanLifecycleWrapper value = this.cache.put(name, new BeanLifecycleWrapper(name, objectFactory));
  175 | 		this.locks.putIfAbsent(name, new ReentrantReadWriteLock());
  176 | 		try {
  177 | 			return value.getBean();
  178 | 		}
  179 | 		catch (RuntimeException e) {
  180 | 			this.errors.put(name, e);
  181 | 			throw e;
  182 | 		}
  183 | 	}
  184 | 
  185 | 	@Override
  186 | 	public String getConversationId() {
  187 | 		return this.name;
  188 | 	}
  189 | 
  190 | 	@Override
  191 | 	public void registerDestructionCallback(String name, Runnable callback) {
  192 | 		BeanLifecycleWrapper value = this.cache.get(name);
  193 | 		if (value == null) {
  194 | 			return;
  195 | 		}
  196 | 		value.setDestroyCallback(callback);
  197 | 	}
  198 | 
  199 | 	@Override
  200 | 	public Object remove(String name) {
  201 | 		BeanLifecycleWrapper value = this.cache.remove(name);
  202 | 		if (value == null) {
  203 | 			return null;
  204 | 		}
  205 | 		// Someone might have added another object with the same key, but we
  206 | 		// keep the method contract by removing the
  207 | 		// value we found anyway
  208 | 		return value.getBean();
  209 | 	}
  210 | 
  211 | 	@Override
  212 | 	public Object resolveContextualObject(String key) {
  213 | 		Expression expression = parseExpression(key);
  214 | 		return expression.getValue(this.beanFactory);
  215 | 	}
  216 | 
  217 | 	private Expression parseExpression(String input) {
  218 | 		if (StringUtils.hasText(input)) {
  219 | 			ExpressionParser parser = new SpelExpressionParser();
  220 | 			try {
  221 | 				return parser.parseExpression(input);
  222 | 			}
  223 | 			catch (ParseException e) {
  224 | 				throw new IllegalArgumentException("Cannot parse expression: " + input, e);
  225 | 			}
  226 | 
  227 | 		}
  228 | 		else {
  229 | 			return null;
  230 | 		}
  231 | 	}
  232 | 
  233 | 	@Override
  234 | 	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
  235 | 		this.beanFactory = beanFactory;
  236 | 		beanFactory.registerScope(this.name, this);
  237 | 		setSerializationId(beanFactory);
  238 | 	}
  239 | 
  240 | 	@Override
  241 | 	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
  242 | 		for (String name : registry.getBeanDefinitionNames()) {
  243 | 			BeanDefinition definition = registry.getBeanDefinition(name);
  244 | 			if (definition instanceof RootBeanDefinition root) {
  245 | 				if (root.getDecoratedDefinition() != null && root.hasBeanClass()
  246 | 						&& root.getBeanClass() == ScopedProxyFactoryBean.class) {
  247 | 					if (getName().equals(root.getDecoratedDefinition().getBeanDefinition().getScope())) {
  248 | 						root.setBeanClass(LockedScopedProxyFactoryBean.class);
  249 | 						root.getConstructorArgumentValues().addGenericArgumentValue(this);
  250 | 						// surprising that a scoped proxy bean definition is not already
  251 | 						// marked as synthetic?
  252 | 						root.setSynthetic(true);
  253 | 					}
  254 | 				}
  255 | 			}
  256 | 		}
  257 | 	}
  258 | 
  259 | 	/**
  260 | 	 * If the bean factory is a DefaultListableBeanFactory, then it can serialize scoped
  261 | 	 * beans and deserialize them in another context (even in another JVM), as long as the
  262 | 	 * IDs of the bean factories match. This method sets up the serialization ID to be
  263 | 	 * either the ID provided to the scope instance, or if that is null, a hash of all the
  264 | 	 * bean names.
  265 | 	 * @param beanFactory The bean factory to configure.
  266 | 	 */
  267 | 	private void setSerializationId(ConfigurableListableBeanFactory beanFactory) {
  268 | 
  269 | 		if (beanFactory instanceof DefaultListableBeanFactory defaultListableBeanFactory) {
  270 | 
  271 | 			String id = this.id;
  272 | 			if (id == null) {
  273 | 				List<String> list = new ArrayList<>(Arrays.asList(defaultListableBeanFactory.getBeanDefinitionNames()));
  274 | 				Collections.sort(list);
  275 | 				String names = list.toString();
  276 | 				logger.debug("Generating bean factory id from names: " + names);
  277 | 				id = UUID.nameUUIDFromBytes(names.getBytes()).toString();
  278 | 			}
  279 | 
  280 | 			logger.info("BeanFactory id=" + id);
  281 | 			defaultListableBeanFactory.setSerializationId(id);
  282 | 
  283 | 		}
  284 | 		else {
  285 | 			logger.warn(
  286 | 					"BeanFactory was not a DefaultListableBeanFactory, scoped proxy beans " + "cannot be serialized.");
  287 | 		}
  288 | 
  289 | 	}
  290 | 
  291 | 	protected String getName() {
  292 | 		return this.name;
  293 | 	}
  294 | 
  295 | 	/**
  296 | 	 * The name of this scope. Default "generic".
  297 | 	 * @param name The name value to set.
  298 | 	 */
  299 | 	public void setName(String name) {
  300 | 		this.name = name;
  301 | 	}
  302 | 
  303 | 	protected ReadWriteLock getLock(String beanName) {
  304 | 		return this.locks.get(beanName);
  305 | 	}
  306 | 
  307 | 	boolean requiresProxyLocking() {
  308 | 		return this.cache.requiresLocking();
  309 | 	}
  310 | 
  311 | 	private static class BeanLifecycleWrapperCache {
  312 | 
  313 | 		private final ScopeCache cache;
  314 | 
  315 | 		BeanLifecycleWrapperCache(ScopeCache cache) {
  316 | 			this.cache = cache;
  317 | 		}
  318 | 
  319 | 		boolean requiresLocking() {
  320 | 			return this.cache.requiresLocking();
  321 | 		}
  322 | 
  323 | 		public BeanLifecycleWrapper remove(String name) {
  324 | 			return (BeanLifecycleWrapper) this.cache.remove(name);
  325 | 		}
  326 | 
  327 | 		public Collection<BeanLifecycleWrapper> clear() {
  328 | 			Collection<Object> values = this.cache.clear();
  329 | 			Collection<BeanLifecycleWrapper> wrappers = new LinkedHashSet<>();
  330 | 			for (Object object : values) {
  331 | 				wrappers.add((BeanLifecycleWrapper) object);
  332 | 			}
  333 | 			return wrappers;
  334 | 		}
  335 | 
  336 | 		public BeanLifecycleWrapper get(String name) {
  337 | 			return (BeanLifecycleWrapper) this.cache.get(name);
  338 | 		}
  339 | 
  340 | 		public BeanLifecycleWrapper put(String name, BeanLifecycleWrapper value) {
  341 | 			return (BeanLifecycleWrapper) this.cache.put(name, value);
  342 | 		}
  343 | 
  344 | 	}
  345 | 
  346 | 	/**
  347 | 	 * Wrapper for a bean instance and any destruction callback (DisposableBean etc.) that
  348 | 	 * is registered for it. Also decorates the bean to optionally guard it from
  349 | 	 * concurrent access (for instance).
  350 | 	 *
  351 | 	 * @author Dave Syer
  352 | 	 *
  353 | 	 */
  354 | 	private static class BeanLifecycleWrapper {
  355 | 
  356 | 		private final String name;
  357 | 
  358 | 		private final ObjectFactory<?> objectFactory;
  359 | 
  360 | 		private volatile Object bean;
  361 | 
  362 | 		private Runnable callback;
  363 | 
  364 | 		BeanLifecycleWrapper(String name, ObjectFactory<?> objectFactory) {
  365 | 			this.name = name;
  366 | 			this.objectFactory = objectFactory;
  367 | 		}
  368 | 
  369 | 		public String getName() {
  370 | 			return this.name;
  371 | 		}
  372 | 
  373 | 		public void setDestroyCallback(Runnable callback) {
  374 | 			this.callback = callback;
  375 | 		}
  376 | 
  377 | 		public Object getBean() {
  378 | 			if (this.bean == null) {
  379 | 				synchronized (this) {
  380 | 					if (this.bean == null) {
  381 | 						this.bean = this.objectFactory.getObject();
  382 | 					}
  383 | 				}
  384 | 			}
  385 | 			return this.bean;
  386 | 		}
  387 | 
  388 | 		public void destroy() {
  389 | 			if (this.callback == null) {
  390 | 				return;
  391 | 			}
  392 | 			synchronized (this) {
  393 | 				Runnable callback = this.callback;
  394 | 				if (callback != null) {
  395 | 					callback.run();
  396 | 				}
  397 | 				this.callback = null;
  398 | 				this.bean = null;
  399 | 			}
  400 | 		}
  401 | 
  402 | 		@Override
  403 | 		public int hashCode() {
  404 | 			final int prime = 31;
  405 | 			int result = 1;
  406 | 			result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
  407 | 			return result;
  408 | 		}
  409 | 
  410 | 		@Override
  411 | 		public boolean equals(Object obj) {
  412 | 			if (this == obj) {
  413 | 				return true;
  414 | 			}
  415 | 			if (obj == null) {
  416 | 				return false;
  417 | 			}
  418 | 			if (getClass() != obj.getClass()) {
  419 | 				return false;
  420 | 			}
  421 | 			BeanLifecycleWrapper other = (BeanLifecycleWrapper) obj;
  422 | 			if (this.name == null) {
  423 | 				if (other.name != null) {
  424 | 					return false;
  425 | 				}
  426 | 			}
  427 | 			else if (!this.name.equals(other.name)) {
  428 | 				return false;
  429 | 			}
  430 | 			return true;
  431 | 		}
  432 | 
  433 | 	}
  434 | 
  435 | 	/**
  436 | 	 * A factory bean with a locked scope.
  437 | 	 *
  438 | 	 * @param <S> - a generic scope extension
  439 | 	 */
  440 | 	@SuppressWarnings("serial")
  441 | 	public static class LockedScopedProxyFactoryBean<S extends GenericScope> extends ScopedProxyFactoryBean
  442 | 			implements MethodInterceptor {
  443 | 
  444 | 		private final S scope;
  445 | 
  446 | 		private String targetBeanName;
  447 | 
  448 | 		public LockedScopedProxyFactoryBean(S scope) {
  449 | 			this.scope = scope;
  450 | 		}
  451 | 
  452 | 		@Override
  453 | 		public void setBeanFactory(BeanFactory beanFactory) {
  454 | 			super.setBeanFactory(beanFactory);
  455 | 			Object proxy = getObject();
  456 | 			if (proxy instanceof Advised advised) {
  457 | 				advised.addAdvice(0, this);
  458 | 			}
  459 | 		}
  460 | 
  461 | 		@Override
  462 | 		public void setTargetBeanName(String targetBeanName) {
  463 | 			super.setTargetBeanName(targetBeanName);
  464 | 			this.targetBeanName = targetBeanName;
  465 | 		}
  466 | 
  467 | 		@Override
  468 | 		public Object invoke(MethodInvocation invocation) throws Throwable {
  469 | 			Method method = invocation.getMethod();
  470 | 			if (AopUtils.isEqualsMethod(method) || AopUtils.isToStringMethod(method)
  471 | 					|| AopUtils.isHashCodeMethod(method) || isScopedObjectGetTargetObject(method)) {
  472 | 				return invocation.proceed();
  473 | 			}
  474 | 			Object proxy = getObject();
  475 | 			Lock lock = null;
  476 | 			if (this.scope.requiresProxyLocking()) {
  477 | 				ReadWriteLock readWriteLock = this.scope.getLock(this.targetBeanName);
  478 | 				if (readWriteLock == null) {
  479 | 					if (logger.isDebugEnabled()) {
  480 | 						logger.debug("For bean with name [" + this.targetBeanName
  481 | 								+ "] there is no read write lock. Will create a new one to avoid NPE");
  482 | 					}
  483 | 					readWriteLock = new ReentrantReadWriteLock();
  484 | 				}
  485 | 				lock = readWriteLock.readLock();
  486 | 				lock.lock();
  487 | 			}
  488 | 			try {
  489 | 				if (proxy instanceof Advised advised) {
  490 | 					ReflectionUtils.makeAccessible(method);
  491 | 					return ReflectionUtils.invokeMethod(method, advised.getTargetSource().getTarget(),
  492 | 							invocation.getArguments());
  493 | 				}
  494 | 				return invocation.proceed();
  495 | 			}
  496 | 			// see gh-349. Throw the original exception rather than the
  497 | 			// UndeclaredThrowableException
  498 | 			catch (UndeclaredThrowableException e) {
  499 | 				throw e.getUndeclaredThrowable();
  500 | 			}
  501 | 			finally {
  502 | 				if (lock != null) {
  503 | 					lock.unlock();
  504 | 				}
  505 | 			}
  506 | 		}
  507 | 
  508 | 		private boolean isScopedObjectGetTargetObject(Method method) {
  509 | 			return method.getDeclaringClass().equals(ScopedObject.class) && method.getName().equals("getTargetObject")
  510 | 					&& method.getParameterTypes().length == 0;
  511 | 		}
  512 | 
  513 | 	}
  514 | 
  515 | }
```
