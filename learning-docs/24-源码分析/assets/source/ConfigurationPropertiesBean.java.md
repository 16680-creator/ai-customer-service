<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/ConfigurationPropertiesBean.java -->
<!-- 正文引用：[[源码:ConfigurationPropertiesBean.java#L起-L止]] ；代码块头部：(src: ConfigurationPropertiesBean.java#L起-L止) -->

# ConfigurationPropertiesBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/ConfigurationPropertiesBean.java` |
| 行数 | 300 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:52.557Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties;
    6 | 
    7 | import java.lang.annotation.Annotation;
    8 | import java.lang.reflect.AnnotatedElement;
    9 | import java.lang.reflect.Constructor;
   10 | import java.lang.reflect.Method;
   11 | import java.util.Iterator;
   12 | import java.util.LinkedHashMap;
   13 | import java.util.Map;
   14 | 
   15 | import org.jspecify.annotations.Nullable;
   16 | 
   17 | import org.springframework.aop.support.AopUtils;
   18 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   19 | import org.springframework.beans.factory.config.BeanDefinition;
   20 | import org.springframework.beans.factory.config.BeanPostProcessor;
   21 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   22 | import org.springframework.beans.factory.support.RootBeanDefinition;
   23 | import org.springframework.boot.context.properties.bind.BindConstructorProvider;
   24 | import org.springframework.boot.context.properties.bind.BindMethod;
   25 | import org.springframework.boot.context.properties.bind.Bindable;
   26 | import org.springframework.boot.context.properties.bind.Binder;
   27 | import org.springframework.context.ApplicationContext;
   28 | import org.springframework.context.ConfigurableApplicationContext;
   29 | import org.springframework.context.annotation.Bean;
   30 | import org.springframework.core.ResolvableType;
   31 | import org.springframework.core.annotation.MergedAnnotation;
   32 | import org.springframework.core.annotation.MergedAnnotations;
   33 | import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
   34 | import org.springframework.lang.Contract;
   35 | import org.springframework.util.Assert;
   36 | import org.springframework.validation.annotation.Validated;
   37 | 
   38 | /**
   39 |  * Provides access to {@link ConfigurationProperties @ConfigurationProperties} bean
   40 |  * details, regardless of if the annotation was used directly or on a {@link Bean @Bean}
   41 |  * factory method. This class can be used to access {@link #getAll(ApplicationContext)
   42 |  * all} configuration properties beans in an ApplicationContext, or
   43 |  * {@link #get(ApplicationContext, Object, String) individual beans} on a case-by-case
   44 |  * basis (for example, in a {@link BeanPostProcessor}).
   45 |  *
   46 |  * @author Phillip Webb
   47 |  * @since 2.2.0
   48 |  * @see #getAll(ApplicationContext)
   49 |  * @see #get(ApplicationContext, Object, String)
   50 |  */
   51 | public final class ConfigurationPropertiesBean {
   52 | 
   53 | 	private final String name;
   54 | 
   55 | 	private final @Nullable Object instance;
   56 | 
   57 | 	private final Bindable<?> bindTarget;
   58 | 
   59 | 	private ConfigurationPropertiesBean(String name, @Nullable Object instance, Bindable<?> bindTarget) {
   60 | 		this.name = name;
   61 | 		this.instance = instance;
   62 | 		this.bindTarget = bindTarget;
   63 | 	}
   64 | 
   65 | 	/**
   66 | 	 * Return the name of the Spring bean.
   67 | 	 * @return the bean name
   68 | 	 */
   69 | 	public String getName() {
   70 | 		return this.name;
   71 | 	}
   72 | 
   73 | 	/**
   74 | 	 * Return the actual Spring bean instance.
   75 | 	 * @return the bean instance
   76 | 	 */
   77 | 	public @Nullable Object getInstance() {
   78 | 		return this.instance;
   79 | 	}
   80 | 
   81 | 	/**
   82 | 	 * Return the bean type.
   83 | 	 * @return the bean type
   84 | 	 */
   85 | 	Class<?> getType() {
   86 | 		Class<?> resolved = this.bindTarget.getType().resolve();
   87 | 		Assert.state(resolved != null, "'resolved' must not be null");
   88 | 		return resolved;
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * Return the {@link ConfigurationProperties} annotation for the bean. The annotation
   93 | 	 * may be defined on the bean itself or from the factory method that create the bean
   94 | 	 * (usually a {@link Bean @Bean} method).
   95 | 	 * @return the configuration properties annotation
   96 | 	 */
   97 | 	public ConfigurationProperties getAnnotation() {
   98 | 		ConfigurationProperties annotation = this.bindTarget.getAnnotation(ConfigurationProperties.class);
   99 | 		Assert.state(annotation != null, "'annotation' must not be null");
  100 | 		return annotation;
  101 | 	}
  102 | 
  103 | 	/**
  104 | 	 * Return a {@link Bindable} instance suitable that can be used as a target for the
  105 | 	 * {@link Binder}.
  106 | 	 * @return a bind target for use with the {@link Binder}
  107 | 	 */
  108 | 	public Bindable<?> asBindTarget() {
  109 | 		return this.bindTarget;
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Return all {@link ConfigurationProperties @ConfigurationProperties} beans contained
  114 | 	 * in the given application context. Both directly annotated beans, as well as beans
  115 | 	 * that have {@link ConfigurationProperties @ConfigurationProperties} annotated
  116 | 	 * factory methods are included.
  117 | 	 * @param applicationContext the source application context
  118 | 	 * @return a map of all configuration properties beans keyed by the bean name
  119 | 	 */
  120 | 	public static Map<String, ConfigurationPropertiesBean> getAll(ApplicationContext applicationContext) {
  121 | 		Assert.notNull(applicationContext, "'applicationContext' must not be null");
  122 | 		if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
  123 | 			return getAll(configurableContext);
  124 | 		}
  125 | 		Map<String, ConfigurationPropertiesBean> propertiesBeans = new LinkedHashMap<>();
  126 | 		applicationContext.getBeansWithAnnotation(ConfigurationProperties.class).forEach((name, instance) -> {
  127 | 			ConfigurationPropertiesBean propertiesBean = get(applicationContext, instance, name);
  128 | 			if (propertiesBean != null) {
  129 | 				propertiesBeans.put(name, propertiesBean);
  130 | 			}
  131 | 		});
  132 | 		return propertiesBeans;
  133 | 	}
  134 | 
  135 | 	private static Map<String, ConfigurationPropertiesBean> getAll(ConfigurableApplicationContext applicationContext) {
  136 | 		Map<String, ConfigurationPropertiesBean> propertiesBeans = new LinkedHashMap<>();
  137 | 		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
  138 | 		Iterator<String> beanNames = beanFactory.getBeanNamesIterator();
  139 | 		while (beanNames.hasNext()) {
  140 | 			String beanName = beanNames.next();
  141 | 			if (isConfigurationPropertiesBean(beanFactory, beanName)) {
  142 | 				try {
  143 | 					Object bean = beanFactory.getBean(beanName);
  144 | 					ConfigurationPropertiesBean propertiesBean = get(applicationContext, bean, beanName);
  145 | 					if (propertiesBean != null) {
  146 | 						propertiesBeans.put(beanName, propertiesBean);
  147 | 					}
  148 | 				}
  149 | 				catch (Exception ex) {
  150 | 					// Ignore
  151 | 				}
  152 | 			}
  153 | 		}
  154 | 		return propertiesBeans;
  155 | 	}
  156 | 
  157 | 	private static boolean isConfigurationPropertiesBean(ConfigurableListableBeanFactory beanFactory, String beanName) {
  158 | 		try {
  159 | 			if (beanFactory.getBeanDefinition(beanName).isAbstract()) {
  160 | 				return false;
  161 | 			}
  162 | 			if (beanFactory.findAnnotationOnBean(beanName, ConfigurationProperties.class) != null) {
  163 | 				return true;
  164 | 			}
  165 | 			Method factoryMethod = findFactoryMethod(beanFactory, beanName);
  166 | 			return findMergedAnnotation(factoryMethod, ConfigurationProperties.class).isPresent();
  167 | 		}
  168 | 		catch (NoSuchBeanDefinitionException ex) {
  169 | 			return false;
  170 | 		}
  171 | 	}
  172 | 
  173 | 	/**
  174 | 	 * Return a {@link ConfigurationPropertiesBean @ConfigurationPropertiesBean} instance
  175 | 	 * for the given bean details or {@code null} if the bean is not a
  176 | 	 * {@link ConfigurationProperties @ConfigurationProperties} object. Annotations are
  177 | 	 * considered both on the bean itself, as well as any factory method (for example a
  178 | 	 * {@link Bean @Bean} method).
  179 | 	 * @param applicationContext the source application context
  180 | 	 * @param bean the bean to consider
  181 | 	 * @param beanName the bean name
  182 | 	 * @return a configuration properties bean or {@code null} if the neither the bean nor
  183 | 	 * factory method are annotated with
  184 | 	 * {@link ConfigurationProperties @ConfigurationProperties}
  185 | 	 */
  186 | 	public static @Nullable ConfigurationPropertiesBean get(ApplicationContext applicationContext, Object bean,
  187 | 			String beanName) {
  188 | 		Method factoryMethod = findFactoryMethod(applicationContext, beanName);
  189 | 		Bindable<Object> bindTarget = createBindTarget(bean, bean.getClass(), factoryMethod);
  190 | 		if (bindTarget == null) {
  191 | 			return null;
  192 | 		}
  193 | 		bindTarget = bindTarget.withBindMethod(BindMethodAttribute.get(applicationContext, beanName));
  194 | 		if (bindTarget.getBindMethod() == null && factoryMethod != null) {
  195 | 			bindTarget = bindTarget.withBindMethod(BindMethod.JAVA_BEAN);
  196 | 		}
  197 | 		if (bindTarget.getBindMethod() != BindMethod.VALUE_OBJECT) {
  198 | 			bindTarget = bindTarget.withExistingValue(bean);
  199 | 		}
  200 | 		return create(beanName, bean, bindTarget);
  201 | 	}
  202 | 
  203 | 	private static @Nullable Method findFactoryMethod(ApplicationContext applicationContext, String beanName) {
  204 | 		if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
  205 | 			return findFactoryMethod(configurableContext, beanName);
  206 | 		}
  207 | 		return null;
  208 | 	}
  209 | 
  210 | 	private static @Nullable Method findFactoryMethod(ConfigurableApplicationContext applicationContext,
  211 | 			String beanName) {
  212 | 		return findFactoryMethod(applicationContext.getBeanFactory(), beanName);
  213 | 	}
  214 | 
  215 | 	private static @Nullable Method findFactoryMethod(ConfigurableListableBeanFactory beanFactory, String beanName) {
  216 | 		if (beanFactory.containsBeanDefinition(beanName)) {
  217 | 			BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
  218 | 			if (beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
  219 | 				return rootBeanDefinition.getResolvedFactoryMethod();
  220 | 			}
  221 | 		}
  222 | 		return null;
  223 | 	}
  224 | 
  225 | 	static ConfigurationPropertiesBean forValueObject(Class<?> beanType, String beanName) {
  226 | 		Bindable<Object> bindTarget = createBindTarget(null, beanType, null);
  227 | 		Assert.state(bindTarget != null && deduceBindMethod(bindTarget) == BindMethod.VALUE_OBJECT,
  228 | 				() -> "Bean '" + beanName + "' is not a @ConfigurationProperties value object");
  229 | 		return create(beanName, null, bindTarget.withBindMethod(BindMethod.VALUE_OBJECT));
  230 | 	}
  231 | 
  232 | 	private static @Nullable Bindable<Object> createBindTarget(@Nullable Object bean, Class<?> beanType,
  233 | 			@Nullable Method factoryMethod) {
  234 | 		ResolvableType type = (factoryMethod != null) ? ResolvableType.forMethodReturnType(factoryMethod)
  235 | 				: ResolvableType.forClass(beanType);
  236 | 		Annotation[] annotations = findAnnotations(bean, beanType, factoryMethod);
  237 | 		return (annotations != null) ? Bindable.of(type).withAnnotations(annotations) : null;
  238 | 	}
  239 | 
  240 | 	private static Annotation @Nullable [] findAnnotations(@Nullable Object instance, Class<?> type,
  241 | 			@Nullable Method factory) {
  242 | 		ConfigurationProperties annotation = findAnnotation(instance, type, factory, ConfigurationProperties.class);
  243 | 		if (annotation == null) {
  244 | 			return null;
  245 | 		}
  246 | 		Validated validated = findAnnotation(instance, type, factory, Validated.class);
  247 | 		return (validated != null) ? new Annotation[] { annotation, validated } : new Annotation[] { annotation };
  248 | 	}
  249 | 
  250 | 	private static <A extends Annotation> @Nullable A findAnnotation(@Nullable Object instance, Class<?> type,
  251 | 			@Nullable Method factory, Class<A> annotationType) {
  252 | 		MergedAnnotation<A> annotation = MergedAnnotation.missing();
  253 | 		if (factory != null) {
  254 | 			annotation = findMergedAnnotation(factory, annotationType);
  255 | 		}
  256 | 		if (!annotation.isPresent()) {
  257 | 			annotation = findMergedAnnotation(type, annotationType);
  258 | 		}
  259 | 		if (!annotation.isPresent() && AopUtils.isAopProxy(instance)) {
  260 | 			annotation = MergedAnnotations.from(AopUtils.getTargetClass(instance), SearchStrategy.TYPE_HIERARCHY)
  261 | 				.get(annotationType);
  262 | 		}
  263 | 		return annotation.isPresent() ? annotation.synthesize() : null;
  264 | 	}
  265 | 
  266 | 	private static <A extends Annotation> MergedAnnotation<A> findMergedAnnotation(@Nullable AnnotatedElement element,
  267 | 			Class<A> annotationType) {
  268 | 		return (element != null) ? MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY).get(annotationType)
  269 | 				: MergedAnnotation.missing();
  270 | 	}
  271 | 
  272 | 	@Contract("_, _, !null -> !null")
  273 | 	private static @Nullable ConfigurationPropertiesBean create(String name, @Nullable Object instance,
  274 | 			@Nullable Bindable<Object> bindTarget) {
  275 | 		return (bindTarget != null) ? new ConfigurationPropertiesBean(name, instance, bindTarget) : null;
  276 | 	}
  277 | 
  278 | 	/**
  279 | 	 * Deduce the {@code BindMethod} that should be used for the given type.
  280 | 	 * @param type the source type
  281 | 	 * @return the bind method to use
  282 | 	 */
  283 | 	static BindMethod deduceBindMethod(Class<?> type) {
  284 | 		return deduceBindMethod(BindConstructorProvider.DEFAULT.getBindConstructor(type, false));
  285 | 	}
  286 | 
  287 | 	/**
  288 | 	 * Deduce the {@code BindMethod} that should be used for the given {@link Bindable}.
  289 | 	 * @param bindable the source bindable
  290 | 	 * @return the bind method to use
  291 | 	 */
  292 | 	static BindMethod deduceBindMethod(Bindable<Object> bindable) {
  293 | 		return deduceBindMethod(BindConstructorProvider.DEFAULT.getBindConstructor(bindable, false));
  294 | 	}
  295 | 
  296 | 	private static BindMethod deduceBindMethod(@Nullable Constructor<?> bindConstructor) {
  297 | 		return (bindConstructor != null) ? BindMethod.VALUE_OBJECT : BindMethod.JAVA_BEAN;
  298 | 	}
  299 | 
  300 | }
```
