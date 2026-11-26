<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/ConfigurationClassEnhancer.java -->
<!-- 正文引用：[[源码:ConfigurationClassEnhancer.java#L起-L止]] ；代码块头部：(src: ConfigurationClassEnhancer.java#L起-L止) -->

# ConfigurationClassEnhancer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/ConfigurationClassEnhancer.java` |
| 行数 | 661 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:20.292Z |

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
   17 | package org.springframework.context.annotation;
   18 | 
   19 | import java.lang.reflect.Constructor;
   20 | import java.lang.reflect.Field;
   21 | import java.lang.reflect.Method;
   22 | import java.lang.reflect.Modifier;
   23 | import java.lang.reflect.Proxy;
   24 | import java.util.Arrays;
   25 | 
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.aop.scope.ScopedProxyFactoryBean;
   31 | import org.springframework.aot.AotDetector;
   32 | import org.springframework.asm.Opcodes;
   33 | import org.springframework.asm.Type;
   34 | import org.springframework.beans.factory.BeanDefinitionStoreException;
   35 | import org.springframework.beans.factory.BeanFactory;
   36 | import org.springframework.beans.factory.BeanFactoryAware;
   37 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   38 | import org.springframework.beans.factory.config.BeanDefinition;
   39 | import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
   40 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   41 | import org.springframework.beans.factory.support.SimpleInstantiationStrategy;
   42 | import org.springframework.cglib.core.ClassGenerator;
   43 | import org.springframework.cglib.core.ClassLoaderAwareGeneratorStrategy;
   44 | import org.springframework.cglib.core.CodeGenerationException;
   45 | import org.springframework.cglib.core.SpringNamingPolicy;
   46 | import org.springframework.cglib.proxy.Callback;
   47 | import org.springframework.cglib.proxy.CallbackFilter;
   48 | import org.springframework.cglib.proxy.Enhancer;
   49 | import org.springframework.cglib.proxy.Factory;
   50 | import org.springframework.cglib.proxy.MethodInterceptor;
   51 | import org.springframework.cglib.proxy.MethodProxy;
   52 | import org.springframework.cglib.proxy.NoOp;
   53 | import org.springframework.cglib.transform.ClassEmitterTransformer;
   54 | import org.springframework.cglib.transform.TransformingClassGenerator;
   55 | import org.springframework.core.KotlinDetector;
   56 | import org.springframework.core.SmartClassLoader;
   57 | import org.springframework.objenesis.ObjenesisException;
   58 | import org.springframework.objenesis.SpringObjenesis;
   59 | import org.springframework.util.Assert;
   60 | import org.springframework.util.ClassUtils;
   61 | import org.springframework.util.ObjectUtils;
   62 | import org.springframework.util.ReflectionUtils;
   63 | 
   64 | /**
   65 |  * Enhances {@link Configuration} classes by generating a CGLIB subclass which
   66 |  * interacts with the Spring container to respect bean scoping semantics for
   67 |  * {@code @Bean} methods. Each such {@code @Bean} method will be overridden in
   68 |  * the generated subclass, only delegating to the actual {@code @Bean} method
   69 |  * implementation if the container actually requests the construction of a new
   70 |  * instance. Otherwise, a call to such an {@code @Bean} method serves as a
   71 |  * reference back to the container, obtaining the corresponding bean by name.
   72 |  *
   73 |  * @author Chris Beams
   74 |  * @author Juergen Hoeller
   75 |  * @since 3.0
   76 |  * @see #enhance
   77 |  * @see ConfigurationClassPostProcessor
   78 |  */
   79 | class ConfigurationClassEnhancer {
   80 | 
   81 | 	// The callbacks to use. Note that these callbacks must be stateless.
   82 | 	static final Callback[] CALLBACKS = new Callback[] {
   83 | 			new BeanMethodInterceptor(),
   84 | 			new BeanFactoryAwareMethodInterceptor(),
   85 | 			NoOp.INSTANCE
   86 | 	};
   87 | 
   88 | 	private static final ConditionalCallbackFilter CALLBACK_FILTER = new ConditionalCallbackFilter(CALLBACKS);
   89 | 
   90 | 	private static final String BEAN_FACTORY_FIELD = "$$beanFactory";
   91 | 
   92 | 	private static final String KOTLIN_DEFAULT_CONSTRUCTOR_MARKER =
   93 | 			"kotlin.jvm.internal.DefaultConstructorMarker";
   94 | 
   95 | 
   96 | 	private static final Log logger = LogFactory.getLog(ConfigurationClassEnhancer.class);
   97 | 
   98 | 	private static final SpringObjenesis objenesis = new SpringObjenesis();
   99 | 
  100 | 
  101 | 	/**
  102 | 	 * Loads the specified class and generates a CGLIB subclass of it equipped with
  103 | 	 * container-aware callbacks capable of respecting scoping and other bean semantics.
  104 | 	 * @return the enhanced subclass
  105 | 	 */
  106 | 	public Class<?> enhance(Class<?> configClass, @Nullable ClassLoader classLoader) {
  107 | 		if (EnhancedConfiguration.class.isAssignableFrom(configClass)) {
  108 | 			if (logger.isDebugEnabled()) {
  109 | 				logger.debug(String.format("Ignoring request to enhance %s as it has " +
  110 | 						"already been enhanced. This usually indicates that more than one " +
  111 | 						"ConfigurationClassPostProcessor has been registered (for example, via " +
  112 | 						"<context:annotation-config>). This is harmless, but you may " +
  113 | 						"want check your configuration and remove one CCPP if possible",
  114 | 						configClass.getName()));
  115 | 			}
  116 | 			return configClass;
  117 | 		}
  118 | 
  119 | 		if (KotlinDetector.isKotlinType(configClass) && hasKotlinConstructorDefaultValues(configClass)) {
  120 | 			throw new BeanDefinitionStoreException("Kotlin configuration class [" + configClass.getName() +
  121 | 					"] declares constructor parameters with default values, which are not supported " +
  122 | 					"with CGLIB enhancement. Remove the default values or declare " +
  123 | 					"@Configuration(proxyBeanMethods=false) without inter-bean references between " +
  124 | 					"@Bean methods on the configuration class.");
  125 | 		}
  126 | 
  127 | 		try {
  128 | 			// Use original ClassLoader if config class not locally loaded in overriding class loader
  129 | 			boolean classLoaderMismatch = (classLoader != null && classLoader != configClass.getClassLoader());
  130 | 			if (classLoaderMismatch && classLoader instanceof SmartClassLoader smartClassLoader) {
  131 | 				classLoader = smartClassLoader.getOriginalClassLoader();
  132 | 				classLoaderMismatch = (classLoader != configClass.getClassLoader());
  133 | 			}
  134 | 			// Use original ClassLoader if config class relies on package visibility
  135 | 			if (classLoaderMismatch && reliesOnPackageVisibility(configClass)) {
  136 | 				classLoader = configClass.getClassLoader();
  137 | 				classLoaderMismatch = false;
  138 | 			}
  139 | 			Enhancer enhancer = newEnhancer(configClass, classLoader);
  140 | 			Class<?> enhancedClass = createClass(enhancer, classLoaderMismatch);
  141 | 			if (logger.isTraceEnabled()) {
  142 | 				logger.trace(String.format("Successfully enhanced %s; enhanced class name is: %s",
  143 | 						configClass.getName(), enhancedClass.getName()));
  144 | 			}
  145 | 			return enhancedClass;
  146 | 		}
  147 | 		catch (CodeGenerationException ex) {
  148 | 			throw new BeanDefinitionStoreException("Could not enhance configuration class [" + configClass.getName() +
  149 | 					"]. Consider declaring @Configuration(proxyBeanMethods=false) without inter-bean references " +
  150 | 					"between @Bean methods on the configuration class, avoiding the need for CGLIB enhancement.", ex);
  151 | 		}
  152 | 	}
  153 | 
  154 | 	/**
  155 | 	 * Check whether the given Kotlin config class declares constructor parameters
  156 | 	 * with default values.
  157 | 	 * <p>For each constructor with default values, the Kotlin compiler generates
  158 | 	 * a synthetic constructor with the same parameters followed by an {@code int}
  159 | 	 * bit mask and a {@code DefaultConstructorMarker}. The original constructor
  160 | 	 * is required to exist, which excludes the synthetic accessor constructors
  161 | 	 * generated for private constructors, since those only append the marker.
  162 | 	 */
  163 | 	private static boolean hasKotlinConstructorDefaultValues(Class<?> configClass) {
  164 | 		for (Constructor<?> ctor : configClass.getDeclaredConstructors()) {
  165 | 			Class<?>[] parameterTypes = ctor.getParameterTypes();
  166 | 			int count = parameterTypes.length;
  167 | 			if (count >= 3 && parameterTypes[count - 2] == int.class &&
  168 | 					parameterTypes[count - 1].getName().equals(KOTLIN_DEFAULT_CONSTRUCTOR_MARKER) &&
  169 | 					hasDeclaredConstructor(configClass, Arrays.copyOf(parameterTypes, count - 2))) {
  170 | 				return true;
  171 | 			}
  172 | 		}
  173 | 		return false;
  174 | 	}
  175 | 
  176 | 	private static boolean hasDeclaredConstructor(Class<?> configClass, Class<?>[] parameterTypes) {
  177 | 		try {
  178 | 			configClass.getDeclaredConstructor(parameterTypes);
  179 | 			return true;
  180 | 		}
  181 | 		catch (NoSuchMethodException ex) {
  182 | 			return false;
  183 | 		}
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Checks whether the given config class relies on package visibility, either for
  188 | 	 * the class and any of its constructors or for any of its {@code @Bean} methods.
  189 | 	 */
  190 | 	private boolean reliesOnPackageVisibility(Class<?> configSuperClass) {
  191 | 		int mod = configSuperClass.getModifiers();
  192 | 		if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
  193 | 			return true;
  194 | 		}
  195 | 		for (Constructor<?> ctor : configSuperClass.getDeclaredConstructors()) {
  196 | 			mod = ctor.getModifiers();
  197 | 			if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
  198 | 				return true;
  199 | 			}
  200 | 		}
  201 | 		for (Method method : ReflectionUtils.getDeclaredMethods(configSuperClass)) {
  202 | 			if (BeanAnnotationHelper.isBeanAnnotated(method)) {
  203 | 				mod = method.getModifiers();
  204 | 				if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
  205 | 					return true;
  206 | 				}
  207 | 			}
  208 | 		}
  209 | 		return false;
  210 | 	}
  211 | 
  212 | 	/**
  213 | 	 * Creates a new CGLIB {@link Enhancer} instance.
  214 | 	 */
  215 | 	private Enhancer newEnhancer(Class<?> configSuperClass, @Nullable ClassLoader classLoader) {
  216 | 		Enhancer enhancer = new Enhancer();
  217 | 		if (classLoader != null) {
  218 | 			enhancer.setClassLoader(classLoader);
  219 | 			if (classLoader instanceof SmartClassLoader smartClassLoader &&
  220 | 					smartClassLoader.isClassReloadable(configSuperClass)) {
  221 | 				enhancer.setUseCache(false);
  222 | 			}
  223 | 		}
  224 | 		enhancer.setSuperclass(configSuperClass);
  225 | 		enhancer.setInterfaces(new Class<?>[] {EnhancedConfiguration.class});
  226 | 		enhancer.setUseFactory(false);
  227 | 		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
  228 | 		enhancer.setAttemptLoad(enhancer.getUseCache() && AotDetector.useGeneratedArtifacts());
  229 | 		enhancer.setStrategy(new BeanFactoryAwareGeneratorStrategy(classLoader));
  230 | 		enhancer.setCallbackFilter(CALLBACK_FILTER);
  231 | 		enhancer.setCallbackTypes(CALLBACK_FILTER.getCallbackTypes());
  232 | 		return enhancer;
  233 | 	}
  234 | 
  235 | 	/**
  236 | 	 * Uses enhancer to generate a subclass of superclass,
  237 | 	 * ensuring that callbacks are registered for the new subclass.
  238 | 	 */
  239 | 	private Class<?> createClass(Enhancer enhancer, boolean fallback) {
  240 | 		Class<?> subclass;
  241 | 		try {
  242 | 			subclass = enhancer.createClass();
  243 | 		}
  244 | 		catch (Throwable ex) {
  245 | 			if (!fallback) {
  246 | 				throw (ex instanceof CodeGenerationException cgex ? cgex : new CodeGenerationException(ex));
  247 | 			}
  248 | 			// Possibly a package-visible @Bean method declaration not accessible
  249 | 			// in the given ClassLoader -> retry with original ClassLoader
  250 | 			enhancer.setClassLoader(null);
  251 | 			subclass = enhancer.createClass();
  252 | 		}
  253 | 
  254 | 		// Registering callbacks statically (as opposed to thread-local)
  255 | 		// is critical for usage in an OSGi environment (SPR-5932)...
  256 | 		Enhancer.registerStaticCallbacks(subclass, CALLBACKS);
  257 | 		return subclass;
  258 | 	}
  259 | 
  260 | 
  261 | 	/**
  262 | 	 * Marker interface to be implemented by all @Configuration CGLIB subclasses.
  263 | 	 * Facilitates idempotent behavior for {@link ConfigurationClassEnhancer#enhance}
  264 | 	 * through checking to see if candidate classes are already assignable to it.
  265 | 	 * <p>Also extends {@link BeanFactoryAware}, as all enhanced {@code @Configuration}
  266 | 	 * classes require access to the {@link BeanFactory} that created them.
  267 | 	 * <p>Note that this interface is intended for framework-internal use only, however
  268 | 	 * must remain public in order to allow access to subclasses generated from other
  269 | 	 * packages (i.e. user code).
  270 | 	 */
  271 | 	public interface EnhancedConfiguration extends BeanFactoryAware {
  272 | 	}
  273 | 
  274 | 
  275 | 	/**
  276 | 	 * Conditional {@link Callback}.
  277 | 	 * @see ConditionalCallbackFilter
  278 | 	 */
  279 | 	private interface ConditionalCallback extends Callback {
  280 | 
  281 | 		boolean isMatch(Method candidateMethod);
  282 | 	}
  283 | 
  284 | 
  285 | 	/**
  286 | 	 * A {@link CallbackFilter} that works by interrogating {@link Callback Callbacks} in the order
  287 | 	 * that they are defined via {@link ConditionalCallback}.
  288 | 	 */
  289 | 	private static class ConditionalCallbackFilter implements CallbackFilter {
  290 | 
  291 | 		private final Callback[] callbacks;
  292 | 
  293 | 		private final Class<?>[] callbackTypes;
  294 | 
  295 | 		public ConditionalCallbackFilter(Callback[] callbacks) {
  296 | 			this.callbacks = callbacks;
  297 | 			this.callbackTypes = new Class<?>[callbacks.length];
  298 | 			for (int i = 0; i < callbacks.length; i++) {
  299 | 				this.callbackTypes[i] = callbacks[i].getClass();
  300 | 			}
  301 | 		}
  302 | 
  303 | 		@Override
  304 | 		public int accept(Method method) {
  305 | 			for (int i = 0; i < this.callbacks.length; i++) {
  306 | 				Callback callback = this.callbacks[i];
  307 | 				if (!(callback instanceof ConditionalCallback conditional) || conditional.isMatch(method)) {
  308 | 					return i;
  309 | 				}
  310 | 			}
  311 | 			throw new IllegalStateException("No callback available for method " + method.getName());
  312 | 		}
  313 | 
  314 | 		public Class<?>[] getCallbackTypes() {
  315 | 			return this.callbackTypes;
  316 | 		}
  317 | 	}
  318 | 
  319 | 
  320 | 	/**
  321 | 	 * Custom extension of CGLIB's DefaultGeneratorStrategy, introducing a {@link BeanFactory} field.
  322 | 	 * Also exposes the application ClassLoader as thread context ClassLoader for the time of
  323 | 	 * class generation (in order for ASM to pick it up when doing common superclass resolution).
  324 | 	 */
  325 | 	private static class BeanFactoryAwareGeneratorStrategy extends ClassLoaderAwareGeneratorStrategy {
  326 | 
  327 | 		public BeanFactoryAwareGeneratorStrategy(@Nullable ClassLoader classLoader) {
  328 | 			super(classLoader);
  329 | 		}
  330 | 
  331 | 		@Override
  332 | 		protected ClassGenerator transform(ClassGenerator cg) throws Exception {
  333 | 			ClassEmitterTransformer transformer = new ClassEmitterTransformer() {
  334 | 				@Override
  335 | 				public void end_class() {
  336 | 					declare_field(Opcodes.ACC_PUBLIC, BEAN_FACTORY_FIELD, Type.getType(BeanFactory.class), null);
  337 | 					super.end_class();
  338 | 				}
  339 | 			};
  340 | 			return new TransformingClassGenerator(cg, transformer);
  341 | 		}
  342 | 	}
  343 | 
  344 | 
  345 | 	/**
  346 | 	 * Intercepts the invocation of any {@link BeanFactoryAware#setBeanFactory(BeanFactory)} on
  347 | 	 * {@code @Configuration} class instances for the purpose of recording the {@link BeanFactory}.
  348 | 	 * @see EnhancedConfiguration
  349 | 	 */
  350 | 	private static class BeanFactoryAwareMethodInterceptor implements MethodInterceptor, ConditionalCallback {
  351 | 
  352 | 		@Override
  353 | 		public @Nullable Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
  354 | 			Field field = ReflectionUtils.findField(obj.getClass(), BEAN_FACTORY_FIELD);
  355 | 			Assert.state(field != null, "Unable to find generated BeanFactory field");
  356 | 			field.set(obj, args[0]);
  357 | 
  358 | 			// Does the actual (non-CGLIB) superclass implement BeanFactoryAware?
  359 | 			// If so, call its setBeanFactory() method. If not, just exit.
  360 | 			if (BeanFactoryAware.class.isAssignableFrom(ClassUtils.getUserClass(obj.getClass().getSuperclass()))) {
  361 | 				return proxy.invokeSuper(obj, args);
  362 | 			}
  363 | 			return null;
  364 | 		}
  365 | 
  366 | 		@Override
  367 | 		public boolean isMatch(Method candidateMethod) {
  368 | 			return isSetBeanFactory(candidateMethod);
  369 | 		}
  370 | 
  371 | 		public static boolean isSetBeanFactory(Method candidateMethod) {
  372 | 			return (candidateMethod.getName().equals("setBeanFactory") &&
  373 | 					candidateMethod.getParameterCount() == 1 &&
  374 | 					BeanFactory.class == candidateMethod.getParameterTypes()[0] &&
  375 | 					BeanFactoryAware.class.isAssignableFrom(candidateMethod.getDeclaringClass()));
  376 | 		}
  377 | 	}
  378 | 
  379 | 
  380 | 	/**
  381 | 	 * Intercepts the invocation of any {@link Bean}-annotated methods in order to ensure proper
  382 | 	 * handling of bean semantics such as scoping and AOP proxying.
  383 | 	 * @see Bean
  384 | 	 * @see ConfigurationClassEnhancer
  385 | 	 */
  386 | 	private static class BeanMethodInterceptor implements MethodInterceptor, ConditionalCallback {
  387 | 
  388 | 		/**
  389 | 		 * Enhance a {@link Bean @Bean} method to check the supplied BeanFactory for the
  390 | 		 * existence of this bean object.
  391 | 		 * @throws Throwable as a catch-all for any exception that may be thrown when invoking the
  392 | 		 * super implementation of the proxied method i.e., the actual {@code @Bean} method
  393 | 		 */
  394 | 		@Override
  395 | 		public @Nullable Object intercept(Object enhancedConfigInstance, Method beanMethod, Object[] beanMethodArgs,
  396 | 					MethodProxy cglibMethodProxy) throws Throwable {
  397 | 
  398 | 			ConfigurableBeanFactory beanFactory = getBeanFactory(enhancedConfigInstance);
  399 | 			String beanName = BeanAnnotationHelper.determineBeanNameFor(beanMethod, beanFactory);
  400 | 
  401 | 			// Determine whether this bean is a scoped-proxy
  402 | 			if (BeanAnnotationHelper.isScopedProxy(beanMethod)) {
  403 | 				String scopedBeanName = ScopedProxyCreator.getTargetBeanName(beanName);
  404 | 				if (beanFactory.isCurrentlyInCreation(scopedBeanName)) {
  405 | 					beanName = scopedBeanName;
  406 | 				}
  407 | 			}
  408 | 
  409 | 			// To handle the case of an inter-bean method reference, we must explicitly check the
  410 | 			// container for already cached instances.
  411 | 
  412 | 			// First, check to see if the requested bean is a FactoryBean. If so, create a subclass
  413 | 			// proxy that intercepts calls to getObject() and returns any cached bean instance.
  414 | 			// This ensures that the semantics of calling a FactoryBean from within @Bean methods
  415 | 			// is the same as that of referring to a FactoryBean within XML. See SPR-6602.
  416 | 			String factoryBeanName = BeanFactory.FACTORY_BEAN_PREFIX + beanName;
  417 | 			if (factoryContainsBean(beanFactory, factoryBeanName) && factoryContainsBean(beanFactory, beanName)) {
  418 | 				Object factoryBean = beanFactory.getBean(factoryBeanName);
  419 | 				if (factoryBean instanceof ScopedProxyFactoryBean) {
  420 | 					// Scoped proxy factory beans are a special case and should not be further proxied
  421 | 				}
  422 | 				else {
  423 | 					// It is a candidate FactoryBean - go ahead with enhancement
  424 | 					return enhanceFactoryBean(factoryBean, beanMethod.getReturnType(), beanFactory, beanName);
  425 | 				}
  426 | 			}
  427 | 
  428 | 			if (isCurrentlyInvokedFactoryMethod(beanMethod)) {
  429 | 				// The factory is calling the bean method in order to instantiate and register the bean
  430 | 				// (i.e. via a getBean() call) -> invoke the super implementation of the method to actually
  431 | 				// create the bean instance.
  432 | 				if (logger.isInfoEnabled() &&
  433 | 						BeanFactoryPostProcessor.class.isAssignableFrom(beanMethod.getReturnType())) {
  434 | 					logger.info("""
  435 | 							@Bean method %s.%s is non-static and returns an object assignable to Spring's \
  436 | 							BeanFactoryPostProcessor interface. This will result in a failure to process \
  437 | 							annotations such as @Autowired, @Resource, and @PostConstruct within the method's \
  438 | 							declaring @Configuration class. Add the 'static' modifier to this method to avoid \
  439 | 							these container lifecycle issues; see @Bean javadoc for complete details."""
  440 | 								.formatted(beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName()));
  441 | 				}
  442 | 				return cglibMethodProxy.invokeSuper(enhancedConfigInstance, beanMethodArgs);
  443 | 			}
  444 | 
  445 | 			return resolveBeanReference(beanMethod, beanMethodArgs, beanFactory, beanName);
  446 | 		}
  447 | 
  448 | 		private @Nullable Object resolveBeanReference(Method beanMethod, Object[] beanMethodArgs,
  449 | 				ConfigurableBeanFactory beanFactory, String beanName) {
  450 | 
  451 | 			// The user (i.e. not the factory) is requesting this bean through a call to
  452 | 			// the bean method, direct or indirect. The bean may have already been marked
  453 | 			// as 'in creation' in certain autowiring scenarios; if so, temporarily set
  454 | 			// the in-creation status to false in order to avoid an exception.
  455 | 			boolean alreadyInCreation = beanFactory.isCurrentlyInCreation(beanName);
  456 | 			try {
  457 | 				if (alreadyInCreation) {
  458 | 					beanFactory.setCurrentlyInCreation(beanName, false);
  459 | 				}
  460 | 				boolean useArgs = !ObjectUtils.isEmpty(beanMethodArgs);
  461 | 				if (useArgs && beanFactory.isSingleton(beanName)) {
  462 | 					// Stubbed null arguments just for reference purposes,
  463 | 					// expecting them to be autowired for regular singleton references?
  464 | 					// A safe assumption since @Bean singleton arguments cannot be optional...
  465 | 					for (Object arg : beanMethodArgs) {
  466 | 						if (arg == null) {
  467 | 							useArgs = false;
  468 | 							break;
  469 | 						}
  470 | 					}
  471 | 				}
  472 | 				Object beanInstance = (useArgs ? beanFactory.getBean(beanName, beanMethodArgs) :
  473 | 						beanFactory.getBean(beanName));
  474 | 				if (!ClassUtils.isAssignableValue(beanMethod.getReturnType(), beanInstance)) {
  475 | 					// Detect package-protected NullBean instance through equals(null) check
  476 | 					if (beanInstance.equals(null)) {
  477 | 						if (logger.isDebugEnabled()) {
  478 | 							logger.debug(String.format("@Bean method %s.%s called as bean reference " +
  479 | 									"for type [%s] returned null bean; resolving to null value.",
  480 | 									beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName(),
  481 | 									beanMethod.getReturnType().getName()));
  482 | 						}
  483 | 						beanInstance = null;
  484 | 					}
  485 | 					else {
  486 | 						String msg = String.format("@Bean method %s.%s called as bean reference " +
  487 | 								"for type [%s] but overridden by non-compatible bean instance of type [%s].",
  488 | 								beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName(),
  489 | 								beanMethod.getReturnType().getName(), beanInstance.getClass().getName());
  490 | 						try {
  491 | 							BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
  492 | 							msg += " Overriding bean of same name declared in: " + beanDefinition.getResourceDescription();
  493 | 						}
  494 | 						catch (NoSuchBeanDefinitionException ex) {
  495 | 							// Ignore - simply no detailed message then.
  496 | 						}
  497 | 						throw new IllegalStateException(msg);
  498 | 					}
  499 | 				}
  500 | 				Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
  501 | 				if (currentlyInvoked != null) {
  502 | 					String outerBeanName = BeanAnnotationHelper.determineBeanNameFor(currentlyInvoked, beanFactory);
  503 | 					beanFactory.registerDependentBean(beanName, outerBeanName);
  504 | 				}
  505 | 				return beanInstance;
  506 | 			}
  507 | 			finally {
  508 | 				if (alreadyInCreation) {
  509 | 					beanFactory.setCurrentlyInCreation(beanName, true);
  510 | 				}
  511 | 			}
  512 | 		}
  513 | 
  514 | 		@Override
  515 | 		public boolean isMatch(Method candidateMethod) {
  516 | 			return (candidateMethod.getDeclaringClass() != Object.class &&
  517 | 					!BeanFactoryAwareMethodInterceptor.isSetBeanFactory(candidateMethod) &&
  518 | 					BeanAnnotationHelper.isBeanAnnotated(candidateMethod));
  519 | 		}
  520 | 
  521 | 		private ConfigurableBeanFactory getBeanFactory(Object enhancedConfigInstance) {
  522 | 			Field field = ReflectionUtils.findField(enhancedConfigInstance.getClass(), BEAN_FACTORY_FIELD);
  523 | 			Assert.state(field != null, "Unable to find generated bean factory field");
  524 | 			Object beanFactory = ReflectionUtils.getField(field, enhancedConfigInstance);
  525 | 			Assert.state(beanFactory != null, "BeanFactory has not been injected into @Configuration class");
  526 | 			Assert.state(beanFactory instanceof ConfigurableBeanFactory,
  527 | 					"Injected BeanFactory is not a ConfigurableBeanFactory");
  528 | 			return (ConfigurableBeanFactory) beanFactory;
  529 | 		}
  530 | 
  531 | 		/**
  532 | 		 * Check the BeanFactory to see whether the bean named <var>beanName</var> already
  533 | 		 * exists. Accounts for the fact that the requested bean may be "in creation", i.e.:
  534 | 		 * we're in the middle of servicing the initial request for this bean. From an enhanced
  535 | 		 * factory method's perspective, this means that the bean does not actually yet exist,
  536 | 		 * and that it is now our job to create it for the first time by executing the logic
  537 | 		 * in the corresponding factory method.
  538 | 		 * <p>Said another way, this check repurposes
  539 | 		 * {@link ConfigurableBeanFactory#isCurrentlyInCreation(String)} to determine whether
  540 | 		 * the container is calling this method or the user is calling this method.
  541 | 		 * @param beanName name of bean to check for
  542 | 		 * @return whether <var>beanName</var> already exists in the factory
  543 | 		 */
  544 | 		private boolean factoryContainsBean(ConfigurableBeanFactory beanFactory, String beanName) {
  545 | 			return (beanFactory.containsBean(beanName) && !beanFactory.isCurrentlyInCreation(beanName));
  546 | 		}
  547 | 
  548 | 		/**
  549 | 		 * Check whether the given method corresponds to the container's currently invoked
  550 | 		 * factory method. Compares method name and parameter types only in order to work
  551 | 		 * around a potential problem with covariant return types (currently only known
  552 | 		 * to happen on Groovy classes).
  553 | 		 */
  554 | 		private boolean isCurrentlyInvokedFactoryMethod(Method method) {
  555 | 			Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
  556 | 			return (currentlyInvoked != null && method.getName().equals(currentlyInvoked.getName()) &&
  557 | 					Arrays.equals(method.getParameterTypes(), currentlyInvoked.getParameterTypes()));
  558 | 		}
  559 | 
  560 | 		/**
  561 | 		 * Create a subclass proxy that intercepts calls to getObject(), delegating to the current BeanFactory
  562 | 		 * instead of creating a new instance. These proxies are created only when calling a FactoryBean from
  563 | 		 * within a Bean method, allowing for proper scoping semantics even when working against the FactoryBean
  564 | 		 * instance directly. If a FactoryBean instance is fetched through the container via &-dereferencing,
  565 | 		 * it will not be proxied. This too is aligned with the way XML configuration works.
  566 | 		 */
  567 | 		private Object enhanceFactoryBean(Object factoryBean, Class<?> exposedType,
  568 | 				ConfigurableBeanFactory beanFactory, String beanName) {
  569 | 
  570 | 			try {
  571 | 				Class<?> clazz = factoryBean.getClass();
  572 | 				boolean finalClass = Modifier.isFinal(clazz.getModifiers());
  573 | 				boolean finalMethod = Modifier.isFinal(clazz.getMethod("getObject").getModifiers());
  574 | 				if (finalClass || finalMethod) {
  575 | 					if (exposedType.isInterface()) {
  576 | 						if (logger.isTraceEnabled()) {
  577 | 							logger.trace("Creating interface proxy for FactoryBean '" + beanName + "' of type [" +
  578 | 									clazz.getName() + "] for use within another @Bean method because its " +
  579 | 									(finalClass ? "implementation class" : "getObject() method") +
  580 | 									" is final: Otherwise a getObject() call would not be routed to the factory.");
  581 | 						}
  582 | 						return createInterfaceProxyForFactoryBean(factoryBean, exposedType, beanFactory, beanName);
  583 | 					}
  584 | 					else {
  585 | 						if (logger.isDebugEnabled()) {
  586 | 							logger.debug("Unable to proxy FactoryBean '" + beanName + "' of type [" +
  587 | 									clazz.getName() + "] for use within another @Bean method because its " +
  588 | 									(finalClass ? "implementation class" : "getObject() method") +
  589 | 									" is final: A getObject() call will NOT be routed to the factory. " +
  590 | 									"Consider declaring the return type as a FactoryBean interface.");
  591 | 						}
  592 | 						return factoryBean;
  593 | 					}
  594 | 				}
  595 | 			}
  596 | 			catch (NoSuchMethodException ex) {
  597 | 				// No getObject() method -> shouldn't happen, but as long as nobody is trying to call it...
  598 | 			}
  599 | 
  600 | 			return createCglibProxyForFactoryBean(factoryBean, beanFactory, beanName);
  601 | 		}
  602 | 
  603 | 		private Object createInterfaceProxyForFactoryBean(Object factoryBean, Class<?> interfaceType,
  604 | 				ConfigurableBeanFactory beanFactory, String beanName) {
  605 | 
  606 | 			return Proxy.newProxyInstance(
  607 | 					factoryBean.getClass().getClassLoader(), new Class<?>[] {interfaceType},
  608 | 					(proxy, method, args) -> {
  609 | 						if (method.getName().equals("getObject") && args == null) {
  610 | 							return beanFactory.getBean(beanName);
  611 | 						}
  612 | 						return ReflectionUtils.invokeMethod(method, factoryBean, args);
  613 | 					});
  614 | 		}
  615 | 
  616 | 		private Object createCglibProxyForFactoryBean(Object factoryBean,
  617 | 				ConfigurableBeanFactory beanFactory, String beanName) {
  618 | 
  619 | 			Enhancer enhancer = new Enhancer();
  620 | 			enhancer.setSuperclass(factoryBean.getClass());
  621 | 			enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
  622 | 			enhancer.setAttemptLoad(AotDetector.useGeneratedArtifacts());
  623 | 			enhancer.setCallbackType(MethodInterceptor.class);
  624 | 
  625 | 			// Ideally create enhanced FactoryBean proxy without constructor side effects,
  626 | 			// analogous to AOP proxy creation in ObjenesisCglibAopProxy...
  627 | 			Class<?> fbClass = enhancer.createClass();
  628 | 			Object fbProxy = null;
  629 | 
  630 | 			if (objenesis.isWorthTrying()) {
  631 | 				try {
  632 | 					fbProxy = objenesis.newInstance(fbClass, enhancer.getUseCache());
  633 | 				}
  634 | 				catch (ObjenesisException ex) {
  635 | 					logger.debug("Unable to instantiate enhanced FactoryBean using Objenesis, " +
  636 | 							"falling back to regular construction", ex);
  637 | 				}
  638 | 			}
  639 | 
  640 | 			if (fbProxy == null) {
  641 | 				try {
  642 | 					fbProxy = ReflectionUtils.accessibleConstructor(fbClass).newInstance();
  643 | 				}
  644 | 				catch (Throwable ex) {
  645 | 					throw new IllegalStateException("Unable to instantiate enhanced FactoryBean using Objenesis, " +
  646 | 							"and regular FactoryBean instantiation via default constructor fails as well", ex);
  647 | 				}
  648 | 			}
  649 | 
  650 | 			((Factory) fbProxy).setCallback(0, (MethodInterceptor) (obj, method, args, proxy) -> {
  651 | 				if (method.getName().equals("getObject") && args.length == 0) {
  652 | 					return beanFactory.getBean(beanName);
  653 | 				}
  654 | 				return method.invoke(factoryBean, args);
  655 | 			});
  656 | 
  657 | 			return fbProxy;
  658 | 		}
  659 | 	}
  660 | 
  661 | }
```
